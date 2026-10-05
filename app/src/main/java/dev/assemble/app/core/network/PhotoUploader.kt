package dev.assemble.app.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

/** Envio da foto do perfil ao Cloudinary, com a assinatura que o backend fez. */
fun interface PhotoUploader {
    /** [jpegBase64]: o JPEG da foto em Base64. Devolve a URL https da imagem. Lança IOException em falha. */
    suspend fun upload(jpegBase64: String, signature: ApiPhotoSignature): String
}

private const val CONNECT_TIMEOUT_MILLIS = 15_000
private const val READ_TIMEOUT_MILLIS = 60_000
private const val HTTP_OK = 200
private const val CRLF = "\r\n"

@Serializable
private data class UploadResult(val secure_url: String)

/** Multipart com a JDK (sem biblioteca nova); o segredo da API nunca está no app, só a assinatura. */
class HttpPhotoUploader : PhotoUploader {
    override suspend fun upload(jpegBase64: String, signature: ApiPhotoSignature): String = withContext(Dispatchers.IO) {
        val bytes = try {
            Base64.getDecoder().decode(jpegBase64)
        } catch (error: IllegalArgumentException) {
            throw IOException("Foto ilegível", error)
        }
        val boundary = "assemble-" + UUID.randomUUID()
        val connection = (URL(signature.uploadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        try {
            connection.outputStream.use { out ->
                signature.fields.forEach { (name, value) ->
                    out.write(
                        ("--$boundary$CRLF" + "Content-Disposition: form-data; name=\"$name\"$CRLF$CRLF$value$CRLF")
                            .toByteArray(StandardCharsets.UTF_8),
                    )
                }
                out.write(
                    ("--$boundary$CRLF" + "Content-Disposition: form-data; name=\"file\"; filename=\"avatar.jpg\"$CRLF" +
                        "Content-Type: image/jpeg$CRLF$CRLF").toByteArray(StandardCharsets.UTF_8),
                )
                out.write(bytes)
                out.write("$CRLF--$boundary--$CRLF".toByteArray(StandardCharsets.UTF_8))
            }
            val status = connection.responseCode
            val body = (if (status >= 400) connection.errorStream else connection.inputStream)
                ?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status != HTTP_OK) throw IOException("Cloudinary respondeu $status")
            try {
                JSON.decodeFromString(UploadResult.serializer(), body).secure_url
            } catch (error: SerializationException) {
                throw IOException("Resposta inesperada do Cloudinary", error)
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
