package dev.assemble.app.core.network

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.Base64

/** Envio multipart contra um servidor local: campos assinados, arquivo e URL devolvida. */
class HttpPhotoUploaderTest {
    private lateinit var server: HttpServer
    private var contentType = ""
    private var body = ""
    private var status = 200
    private var reply = """{"secure_url":"https://res.cloudinary.com/demo/image/upload/v1/a.jpg","other":1}"""

    @Before
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            contentType = exchange.requestHeaders.getFirst("Content-Type")
            body = exchange.requestBody.readBytes().toString(StandardCharsets.ISO_8859_1)
            val bytes = reply.toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    @After
    fun stop() = server.stop(0)

    private fun signature() = ApiPhotoSignature(
        uploadUrl = "http://127.0.0.1:${server.address.port}/upload",
        fields = mapOf("public_id" to "uid-1", "signature" to "abc123", "api_key" to "key"),
    )

    private val jpeg = Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3, 4))

    @Test
    fun sendsTheSignedFieldsAndTheFileAndReturnsTheUrl() = runBlocking {
        val url = HttpPhotoUploader().upload(jpeg, signature())

        assertEquals("https://res.cloudinary.com/demo/image/upload/v1/a.jpg", url)
        assertTrue(contentType.startsWith("multipart/form-data; boundary="))
        assertTrue(body.contains("name=\"public_id\"\r\n\r\nuid-1"))
        assertTrue(body.contains("name=\"signature\"\r\n\r\nabc123"))
        assertTrue(body.contains("name=\"file\"; filename=\"avatar.jpg\""))
    }

    @Test
    fun anErrorStatusIsAnIoException() = runBlocking {
        status = 401
        reply = """{"error":{"message":"Invalid Signature"}}"""

        try {
            HttpPhotoUploader().upload(jpeg, signature())
            fail("esperava IOException")
        } catch (error: IOException) {
            assertTrue(error.message!!.contains("401"))
        }
    }

    @Test
    fun aResponseWithoutTheUrlIsAnIoException() = runBlocking {
        reply = """{"nope":true}"""

        try {
            HttpPhotoUploader().upload(jpeg, signature())
            fail("esperava IOException")
        } catch (_: IOException) {
        }
    }
}
