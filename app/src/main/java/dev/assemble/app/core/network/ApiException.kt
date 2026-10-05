package dev.assemble.app.core.network

import java.io.IOException

/** Códigos de erro do contrato (docs/Assemble-contrato-api.md §5). */
object ApiErrorCode {
    const val INVALID_REQUEST = "invalid_request"
    const val UNAUTHENTICATED = "unauthenticated"
    const val ACCOUNT_DEACTIVATED = "account_deactivated"
    const val NOT_FOUND = "not_found"
    const val NOTHING_TO_UNDO = "nothing_to_undo"
    const val ALREADY_DECIDED = "already_decided"
    const val BLOCKED_CONTENT = "blocked_content"
    const val RATE_LIMITED = "rate_limited"
    const val PROVIDER_UNAVAILABLE = "provider_unavailable"

    /** Resposta fora do contrato (corpo ilegível, proxy no meio do caminho). */
    const val UNEXPECTED = "unexpected"
}

/**
 * Erro devolvido pelo backend. É um [IOException] para as telas que já tratam falha de rede
 * mostrarem o estado de erro sem conhecer a API.
 */
class ApiException(
    val code: String,
    val httpStatus: Int,
    /** Segundos de espera do cabeçalho Retry-After (só no 429). */
    val retryAfterSeconds: Long? = null,
    message: String = "",
) : IOException("$httpStatus $code${if (message.isBlank()) "" else ": $message"}")

/** Não há usuário logado no Firebase: a chamada nem chega ao backend. */
class NotSignedInException : IOException("Sem usuário logado")
