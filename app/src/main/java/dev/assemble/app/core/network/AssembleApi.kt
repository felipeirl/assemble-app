package dev.assemble.app.core.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class DecisionChoice { PASS, ASSEMBLE }

/** Rotas do backend (contrato V2 §4). Toda chamada exige o token do Firebase. */
interface AssembleApi {
    suspend fun deck(): ApiDeck

    /** PASS devolve null (204); ASSEMBLE devolve o resultado do match. */
    suspend fun decide(characterId: String, choice: DecisionChoice, idempotencyKey: String): ApiMatchResult?

    suspend fun undo(): ApiDeckCard

    suspend fun character(characterId: String): ApiCharacterView

    suspend fun sendMessage(connectionId: String, text: String, idempotencyKey: String): ApiCharacterReply

    suspend fun stats(): ApiUserStats

    suspend fun hideChats()

    suspend fun deactivateAccount(): ApiDeactivation

    suspend fun reactivateAccount()
}

/** Token do Firebase do usuário logado. Lança [NotSignedInException] sem login. */
fun interface IdTokenProvider {
    suspend fun idToken(forceRefresh: Boolean): String
}

private const val CONNECT_TIMEOUT_MILLIS = 15_000
// O match gera a fala de abertura com o modelo antes de responder: a leitura pode passar de 30 s.
private const val READ_TIMEOUT_MILLIS = 90_000
private const val HTTP_NO_CONTENT = 204
private const val HTTP_UNAUTHORIZED = 401

/**
 * Cliente HTTP do contrato com a JDK (sem biblioteca nova). Em 401, renova o token uma vez e
 * repete; se falhar de novo, a [ApiException] sobe para o app voltar ao Login.
 */
class HttpAssembleApi(
    baseUrl: String,
    private val tokens: IdTokenProvider,
    private val languageTag: () -> String,
    private val timeZoneId: () -> String,
    /** Chamado quando o backend diz que a conta está em carência (403 account_deactivated). */
    private val onAccountDeactivated: () -> Unit = {},
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AssembleApi {
    private val base = baseUrl.trimEnd('/')

    override suspend fun deck(): ApiDeck = decode(call("GET", "/v2/deck"))

    override suspend fun decide(characterId: String, choice: DecisionChoice, idempotencyKey: String): ApiMatchResult? {
        val body = json.encodeToString(ApiDecisionRequest.serializer(), ApiDecisionRequest(characterId, choice.name))
        val response = call("POST", "/v2/decisions", body, idempotencyKey)
        return if (response.isEmpty()) null else decode(response)
    }

    override suspend fun undo(): ApiDeckCard = decode(call("POST", "/v2/decisions/undo"))

    override suspend fun character(characterId: String): ApiCharacterView =
        decode(call("GET", "/v2/characters/${encode(characterId)}"))

    override suspend fun sendMessage(connectionId: String, text: String, idempotencyKey: String): ApiCharacterReply {
        val body = json.encodeToString(ApiSendMessageRequest.serializer(), ApiSendMessageRequest(text))
        return decode(call("POST", "/v2/connections/${encode(connectionId)}/messages", body, idempotencyKey))
    }

    override suspend fun stats(): ApiUserStats = decode(call("GET", "/v2/me/stats"))

    override suspend fun hideChats() {
        call("POST", "/v2/chats/hide")
    }

    override suspend fun deactivateAccount(): ApiDeactivation = decode(call("POST", "/v2/account/deactivate"))

    override suspend fun reactivateAccount() {
        call("POST", "/v2/account/reactivate")
    }

    private suspend fun call(method: String, path: String, body: String? = null, idempotencyKey: String? = null): String {
        val first = execute(method, path, body, idempotencyKey, tokens.idToken(forceRefresh = false))
        val response = if (first.status == HTTP_UNAUTHORIZED) {
            execute(method, path, body, idempotencyKey, tokens.idToken(forceRefresh = true))
        } else {
            first
        }
        if (response.status in 200..299) return if (response.status == HTTP_NO_CONTENT) "" else response.body
        throw response.toException().also {
            if (it.code == ApiErrorCode.ACCOUNT_DEACTIVATED) onAccountDeactivated()
        }
    }

    private suspend fun execute(
        method: String,
        path: String,
        body: String?,
        idempotencyKey: String?,
        token: String,
    ): RawResponse = withContext(ioDispatcher) {
        val connection = URL(base + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Language", languageTag())
            connection.setRequestProperty("X-Timezone", timeZoneId())
            // O túnel ngrok gratuito mostra uma página de aviso a navegadores; o app não é um.
            connection.setRequestProperty("ngrok-skip-browser-warning", "1")
            idempotencyKey?.let { connection.setRequestProperty("Idempotency-Key", it) }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            RawResponse(status, text, connection.getHeaderField("Retry-After")?.toLongOrNull())
        } finally {
            connection.disconnect()
        }
    }

    private inline fun <reified T> decode(text: String): T = try {
        json.decodeFromString<T>(text)
    } catch (error: SerializationException) {
        throw ApiException(ApiErrorCode.UNEXPECTED, httpStatus = 200, message = "resposta fora do contrato").apply { initCause(error) }
    } catch (error: IllegalArgumentException) {
        throw ApiException(ApiErrorCode.UNEXPECTED, httpStatus = 200, message = "resposta fora do contrato").apply { initCause(error) }
    }

    private fun encode(segment: String): String = URLEncoder.encode(segment, StandardCharsets.UTF_8.name()).replace("+", "%20")

    private data class RawResponse(val status: Int, val body: String, val retryAfterSeconds: Long?) {
        fun toException(): ApiException {
            val error = try {
                json.decodeFromString(ApiErrorBody.serializer(), body)
            } catch (_: SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
            return ApiException(
                code = error?.error ?: ApiErrorCode.UNEXPECTED,
                httpStatus = status,
                retryAfterSeconds = retryAfterSeconds,
                message = error?.message.orEmpty(),
            )
        }
    }

    companion object {
        internal val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
