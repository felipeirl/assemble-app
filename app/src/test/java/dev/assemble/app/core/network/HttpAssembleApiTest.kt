package dev.assemble.app.core.network

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList

/** Cliente HTTP contra um servidor local da JDK: cabeçalhos, corpos e erros do contrato. */
class HttpAssembleApiTest {
    private data class Recorded(val method: String, val path: String, val headers: Map<String, String>, val body: String)

    private data class Reply(val status: Int, val body: String = "", val headers: Map<String, String> = emptyMap())

    private lateinit var server: HttpServer
    private val requests = CopyOnWriteArrayList<Recorded>()
    private var replies: MutableList<Reply> = mutableListOf()
    private val tokens = mutableListOf<Boolean>()
    private var deactivatedCalls = 0

    @Before
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange -> handle(exchange) }
        server.start()
    }

    @After
    fun stop() = server.stop(0)

    private fun handle(exchange: HttpExchange) {
        val headers = exchange.requestHeaders.entries.associate { (key, values) -> key.lowercase() to values.first() }
        val body = exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).readText()
        requests += Recorded(exchange.requestMethod, exchange.requestURI.rawPath, headers, body)
        val reply = replies.removeAt(0)
        reply.headers.forEach { (key, value) -> exchange.responseHeaders.add(key, value) }
        val bytes = reply.body.toByteArray(StandardCharsets.UTF_8)
        exchange.sendResponseHeaders(reply.status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
        if (bytes.isNotEmpty()) exchange.responseBody.use { it.write(bytes) }
        exchange.close()
    }

    private fun api() = HttpAssembleApi(
        baseUrl = "http://127.0.0.1:${server.address.port}/",
        tokens = { force -> tokens += force; if (force) "token-novo" else "token-velho" },
        languageTag = { "pt-BR" },
        timeZoneId = { "America/Sao_Paulo" },
        onAccountDeactivated = { deactivatedCalls++ },
    )

    private fun reply(vararg all: Reply) {
        replies = all.toMutableList()
    }

    @Test
    fun `deck sends contract headers and decodes the deck`() = runBlocking {
        reply(
            Reply(
                200,
                """{"date":"2026-10-04","cards":[{"characterId":"storm","name":"Tempestade",
                   "imageUrl":"https://img/storm.jpg","traitsInCommon":["Mutant","XMen"]},
                   {"characterId":"rocket","name":"Rocket Raccoon","traitsInCommon":[]}],
                   "remaining":2,"total":30,"nextDeckAt":"2026-10-05T03:00:00Z","canUndo":false,"extra":1}""",
            ),
        )

        val deck = api().deck()

        assertEquals(2, deck.cards.size)
        assertEquals("Tempestade", deck.cards[0].name)
        assertEquals(listOf("Mutant", "XMen"), deck.cards[0].traitsInCommon)
        assertNull(deck.cards[1].imageUrl)
        assertEquals(30, deck.total)
        val request = requests.single()
        assertEquals("GET", request.method)
        assertEquals("/v2/deck", request.path)
        assertEquals("Bearer token-velho", request.headers["authorization"])
        assertEquals("pt-BR", request.headers["accept-language"])
        assertEquals("America/Sao_Paulo", request.headers["x-timezone"])
        assertEquals("1", request.headers["ngrok-skip-browser-warning"])
    }

    @Test
    fun `pass returns null on 204 and sends the idempotency key and body`() = runBlocking {
        reply(Reply(204))

        val result = api().decide("storm", DecisionChoice.PASS, "chave-1")

        assertNull(result)
        val request = requests.single()
        assertEquals("POST", request.method)
        assertEquals("/v2/decisions", request.path)
        assertEquals("chave-1", request.headers["idempotency-key"])
        assertEquals("""{"characterId":"storm","choice":"PASS"}""", request.body)
    }

    @Test
    fun `assemble decodes a match and a non-match`() = runBlocking {
        reply(
            Reply(200, """{"matched":true,"connectionId":"storm","character":{"characterId":"storm","name":"Storm"},"score":82,"reasons":["Mutant"]}"""),
            Reply(200, """{"matched":false}"""),
        )

        val match = api().decide("storm", DecisionChoice.ASSEMBLE, "k1")!!
        val noMatch = api().decide("rocket", DecisionChoice.ASSEMBLE, "k2")!!

        assertTrue(match.matched)
        assertEquals(82, match.score)
        assertEquals(listOf("Mutant"), match.reasons)
        assertFalse(noMatch.matched)
        assertNull(noMatch.score)
    }

    @Test
    fun `401 refreshes the token once and repeats the call`() = runBlocking {
        reply(
            Reply(401, """{"error":"unauthenticated","message":"x"}"""),
            Reply(200, """{"connections":1,"messagesSent":2,"charactersSeen":3,"distinctTeams":1}"""),
        )

        val stats = api().stats()

        assertEquals(3, stats.charactersSeen)
        assertEquals(listOf(false, true), tokens)
        assertEquals("Bearer token-novo", requests[1].headers["authorization"])
    }

    @Test
    fun `second 401 surfaces as unauthenticated`() = runBlocking {
        reply(
            Reply(401, """{"error":"unauthenticated","message":"x"}"""),
            Reply(401, """{"error":"unauthenticated","message":"x"}"""),
        )

        val error = runCatching { api().stats() }.exceptionOrNull() as ApiException

        assertEquals(ApiErrorCode.UNAUTHENTICATED, error.code)
        assertEquals(2, requests.size)
    }

    @Test
    fun `contract errors become ApiException that is also an IOException`() = runBlocking {
        reply(Reply(409, """{"error":"nothing_to_undo","message":"Não há nada para desfazer."}"""))

        val error = runCatching { api().undo() }.exceptionOrNull()

        assertTrue(error is IOException)
        assertEquals(ApiErrorCode.NOTHING_TO_UNDO, (error as ApiException).code)
        assertEquals(409, error.httpStatus)
    }

    @Test
    fun `429 keeps Retry-After`() = runBlocking {
        reply(Reply(429, """{"error":"rate_limited","message":"x"}""", mapOf("Retry-After" to "3000")))

        val error = runCatching { api().sendMessage("storm", "oi", "k") }.exceptionOrNull() as ApiException

        assertEquals(ApiErrorCode.RATE_LIMITED, error.code)
        assertEquals(3000L, error.retryAfterSeconds)
    }

    @Test
    fun `deactivated account notifies the app`() = runBlocking {
        reply(Reply(403, """{"error":"account_deactivated","message":"x"}"""))

        runCatching { api().deck() }

        assertEquals(1, deactivatedCalls)
    }

    @Test
    fun `body outside the contract is an unexpected error, not a crash`() = runBlocking {
        reply(Reply(502, "<html>Bad gateway</html>"), Reply(200, "não é json"))

        val gateway = runCatching { api().deck() }.exceptionOrNull() as ApiException
        val garbage = runCatching { api().deck() }.exceptionOrNull() as ApiException

        assertEquals(ApiErrorCode.UNEXPECTED, gateway.code)
        assertEquals(502, gateway.httpStatus)
        assertEquals(ApiErrorCode.UNEXPECTED, garbage.code)
    }

    @Test
    fun `character id is encoded in the path`() = runBlocking {
        reply(Reply(200, """{"characterId":"forge-77","name":"Forge","connected":false,"traitsInCommon":["Human"]}"""))

        val view = api().character("forge-77")

        assertEquals("/v2/characters/forge-77", requests.single().path)
        assertFalse(view.connected)
        assertNull(view.facts)
    }

    @Test
    fun `full profile decodes facts, sources, teammates and compare`() = runBlocking {
        reply(
            Reply(
                200,
                """{"characterId":"storm","name":"Tempestade","connected":true,"connectionId":"storm","score":82,
                   "whyYouMatch":[{"category":"origin","traits":["Mutant"]}],
                   "facts":{"realName":"Ororo Munroe","origin":"Mutant","powers":["Energy","Flight"],"teams":["XMen"],
                            "alignment":"Good","issueAppearances":4000,
                            "powerstats":{"intelligence":75,"strength":10,"speed":47,"durability":30,"power":88,"combat":75},
                            "appearance":{"heightCm":180,"eyeColor":"Blue"}},
                   "factSources":{"realName":"ComicVine","powerstats":"SuperheroApi"},
                   "translatedFields":["bio"],
                   "sources":[{"name":"Comic Vine","url":"https://cv"},{"name":"Superhero API"}],
                   "teammates":[{"characterId":"jean-grey","name":"Jean Grey","connected":true}],
                   "compareWith":[{"characterId":"iron-man","name":"Iron Man",
                     "powerstats":{"intelligence":1,"strength":2,"speed":3,"durability":4,"power":5,"combat":6}}]}""",
            ),
        )

        val view = api().character("storm")

        assertTrue(view.connected)
        assertEquals("Ororo Munroe", view.facts?.realName)
        assertEquals(88, view.facts?.powerstats?.power)
        assertEquals(180, view.facts?.appearance?.heightCm)
        assertEquals(listOf("bio"), view.translatedFields)
        assertEquals("jean-grey", view.teammates.single().characterId)
        assertEquals(6, view.compareWith.single().powerstats.combat)
        assertNull(view.sources[1].url)
    }

    @Test
    fun `send message decodes the reply and suggestions`() = runBlocking {
        reply(
            Reply(
                200,
                """{"userMessage":{"id":"m1","connectionId":"storm","author":"USER","text":"Oi","createdAt":"2026-10-04T15:00:00Z","fictional":false,"blocked":false},
                   "reply":{"id":"m2","connectionId":"storm","author":"CHARACTER","text":"Olá.","createdAt":"2026-10-04T15:00:01Z","fictional":true,"blocked":false},
                   "suggestions":["a","b","c"]}""",
            ),
        )

        val reply = api().sendMessage("storm", "Oi", "k")

        assertEquals("CHARACTER", reply.reply.author)
        assertTrue(reply.reply.fictional)
        assertEquals(3, reply.suggestions.size)
        assertEquals("""{"text":"Oi"}""", requests.single().body)
    }

    @Test
    fun `network failure without a server is a plain IOException`() = runBlocking {
        server.stop(0)

        try {
            api().deck()
            fail("devia falhar")
        } catch (error: IOException) {
            assertFalse(error is ApiException)
        }
    }
}
