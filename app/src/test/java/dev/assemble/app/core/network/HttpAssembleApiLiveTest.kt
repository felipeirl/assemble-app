package dev.assemble.app.core.network

import dev.assemble.app.feature.character.remoteProfileContent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

/**
 * O cliente contra um backend de verdade. Só roda com `ASSEMBLE_LIVE_URL` e `ASSEMBLE_LIVE_TOKEN`
 * (um backend local com verificador de token de teste); sem eles, é ignorado.
 */
class HttpAssembleApiLiveTest {
    private val baseUrl: String? = System.getenv("ASSEMBLE_LIVE_URL")
    private val token: String? = System.getenv("ASSEMBLE_LIVE_TOKEN")

    @Test
    fun fullFlow_againstRealBackend(): Unit = runBlocking {
        assumeTrue("ASSEMBLE_LIVE_URL não definido", !baseUrl.isNullOrBlank() && !token.isNullOrBlank())
        var deactivatedNotices = 0
        val api = HttpAssembleApi(
            baseUrl = baseUrl!!,
            tokens = { token!! },
            languageTag = { "pt-BR" },
            timeZoneId = { "America/Sao_Paulo" },
            onAccountDeactivated = { deactivatedNotices++ },
        )

        val deck = api.deck()
        assertTrue("baralho vazio", deck.cards.size >= 3)

        val preview = api.character(deck.cards[0].characterId)
        assertFalse(preview.connected)
        assertNull(preview.facts)

        val passed = deck.cards[1].characterId
        assertNull(api.decide(passed, DecisionChoice.PASS, UUID.randomUUID().toString()))
        try {
            api.decide(passed, DecisionChoice.ASSEMBLE, UUID.randomUUID().toString())
            fail("esperava already_decided")
        } catch (error: ApiException) {
            assertEquals(ApiErrorCode.ALREADY_DECIDED, error.code)
        }
        assertEquals(passed, api.undo().characterId)

        val match = deck.cards.drop(2).take(MAX_ASSEMBLES).firstNotNullOfOrNull { card ->
            val key = UUID.randomUUID().toString()
            api.decide(card.characterId, DecisionChoice.ASSEMBLE, key)?.takeIf { it.matched }?.also { result ->
                assertEquals(result, api.decide(card.characterId, DecisionChoice.ASSEMBLE, key))
            }
        }
        assertNotNull("nenhum match nos primeiros cards", match)
        val connectionId = match!!.connectionId!!

        val profile = api.character(connectionId)
        assertTrue(profile.connected)
        val content = remoteProfileContent(profile, unlockPending = true)
        assertEquals(match.score, content.score)

        val reply = api.sendMessage(connectionId, "Oi! Qual é a sua maior qualidade?", UUID.randomUUID().toString())
        assertTrue(reply.reply.fictional)
        assertEquals("USER", reply.userMessage.author)
        assertEquals(SUGGESTIONS, reply.suggestions.size)

        try {
            api.sendMessage(connectionId, "meu email é fulano@exemplo.com", UUID.randomUUID().toString())
            fail("esperava blocked_content")
        } catch (error: ApiException) {
            assertEquals(ApiErrorCode.BLOCKED_CONTENT, error.code)
        }

        val stats = api.stats()
        assertTrue(stats.connections >= 1 && stats.messagesSent >= 1)

        api.hideChats()
        assertTrue(api.deactivateAccount().purgeAt.isNotBlank())
        try {
            api.deck()
            fail("esperava account_deactivated")
        } catch (error: ApiException) {
            assertEquals(ApiErrorCode.ACCOUNT_DEACTIVATED, error.code)
        }
        assertEquals(1, deactivatedNotices)
        api.reactivateAccount()
        api.deck()
    }

    private companion object {
        const val MAX_ASSEMBLES = 10
        const val SUGGESTIONS = 3
    }
}
