package dev.assemble.app.core.network

import dev.assemble.app.feature.character.remoteProfileContent
import kotlinx.coroutines.delay
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

        // O Assemble é decidido numa fila: repetir com a mesma chave devolve o estado atual.
        val connectionId = deck.cards.drop(2).take(MAX_ASSEMBLES).firstNotNullOfOrNull { card ->
            val key = UUID.randomUUID().toString()
            val status = settle { api.decide(card.characterId, DecisionChoice.ASSEMBLE, key)!!.status }
            card.characterId.takeIf { status == ApiStatus.MATCHED }
        }
        assertNotNull("nenhum match nos primeiros cards", connectionId)

        val profile = api.character(connectionId!!)
        assertTrue(profile.connected)
        assertNotNull(remoteProfileContent(profile, unlockPending = true).score)

        val key = UUID.randomUUID().toString()
        val accepted = api.sendMessage(connectionId, "Oi! Qual é a sua maior qualidade?", key)
        assertEquals("USER", accepted.userMessage.author)
        assertEquals(ApiStatus.SENT, settle { api.sendMessage(connectionId, "Oi! Qual é a sua maior qualidade?", key).userMessage.status })

        val regenerated = api.regenerate(connectionId)
        assertEquals(ApiStatus.PENDING, regenerated.reply.status)

        val blockedKey = UUID.randomUUID().toString()
        val blocked = settle {
            api.sendMessage(connectionId, "meu email é fulano@exemplo.com", blockedKey).userMessage.status
        }
        assertEquals(ApiStatus.BLOCKED, blocked)

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

    /** Repete [read] até o estado sair de `pending` (a fila terminou), por até um minuto. */
    private suspend fun settle(read: suspend () -> String): String {
        repeat(SETTLE_ATTEMPTS) {
            val status = try {
                read()
            } catch (error: ApiException) {
                if (error.code != ApiErrorCode.REPLY_PENDING) throw error
                ApiStatus.PENDING
            }
            if (status != ApiStatus.PENDING) return status
            delay(SETTLE_INTERVAL_MILLIS)
        }
        fail("a fila do backend não terminou")
        error("inalcançável")
    }

    private companion object {
        const val MAX_ASSEMBLES = 10
        const val SETTLE_ATTEMPTS = 60
        const val SETTLE_INTERVAL_MILLIS = 1_000L
    }
}
