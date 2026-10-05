package dev.assemble.app.feature.discover

import dev.assemble.app.core.data.remote.FakeAssembleApi
import dev.assemble.app.core.data.remote.FakeAuthGateway
import dev.assemble.app.core.data.remote.InMemoryUserDataStore
import dev.assemble.app.core.data.remote.RemoteCharacterRepository
import dev.assemble.app.core.data.remote.RemoteConnectionRepository
import dev.assemble.app.core.data.remote.RemoteUserRepository
import dev.assemble.app.core.data.remote.inMemorySettings
import dev.assemble.app.core.data.remote.runRemoteTest
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.Team
import dev.assemble.app.core.network.ApiDeck
import dev.assemble.app.core.network.ApiDeckCard
import dev.assemble.app.core.network.ApiErrorCode
import dev.assemble.app.core.network.ApiException
import dev.assemble.app.core.network.ApiMatchCharacter
import dev.assemble.app.core.network.ApiMatchResult
import dev.assemble.app.core.network.DecisionChoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class RemoteDeckSourceTest {
    private val api = FakeAssembleApi()
    private lateinit var users: RemoteUserRepository
    private var keyCounter = 0

    private suspend fun deck(scope: CoroutineScope, vararg ids: String, canUndo: Boolean = false): RemoteDeckSource {
        val auth = FakeAuthGateway(signedInUid = "uid-1")
        val store = InMemoryUserDataStore()
        users = RemoteUserRepository(auth, store, api, inMemorySettings(), scope)
        val connections = RemoteConnectionRepository(auth.uid, store, api, scope)
        val characters = RemoteCharacterRepository(api, connections) { "en" }
        api.onDeck = {
            ApiDeck(
                date = "2026-10-04",
                cards = ids.map { ApiDeckCard(it, it.replaceFirstChar(Char::uppercase), "https://img/$it", listOf("Mutant", "XMen"), "About $it.") },
                remaining = ids.size,
                total = 30,
                nextDeckAt = "2026-10-05T03:00:00Z",
                canUndo = canUndo,
            )
        }
        return RemoteDeckSource(api, characters, users) { "key-${++keyCounter}" }.also { it.reload() }
    }

    private suspend fun RemoteDeckSource.cardIds(): List<String> =
        when (val state = deck.first()) {
            is DeckState.Content -> state.cards.map { it.characterId }
            DeckState.Empty -> emptyList()
            else -> error("estado inesperado: $state")
        }

    @Test
    fun reload_mapsCardsAndTraits() = runRemoteTest { scope ->
        val source = deck(scope, "storm", "cyclops", canUndo = true)
        val content = source.deck.first() as DeckState.Content
        assertEquals(listOf("storm", "cyclops"), content.cards.map { it.characterId })
        assertEquals(listOf(Origin.Mutant, Team.XMen), content.cards.first().traitsInCommon)
        assertEquals("About storm.", content.cards.first().tagline)
        assertTrue(source.canUndo.value)
    }

    @Test
    fun reload_emptyAndFailure() = runRemoteTest { scope ->
        val source = deck(scope)
        assertEquals(DeckState.Empty, source.deck.first())
        api.onDeck = { throw IOException("offline") }
        source.reload()
        assertEquals(DeckState.Error, source.deck.first())
    }

    @Test
    fun pass_removesCardAndEnablesUndo() = runRemoteTest { scope ->
        val source = deck(scope, "storm", "cyclops")
        source.pass("storm")
        assertEquals(listOf("cyclops"), source.cardIds())
        assertEquals(DecisionChoice.PASS, api.decisions.single().choice)
        assertTrue(source.canUndo.value)
        assertTrue("storm" in users.seenCharacterIds.value)
    }

    @Test
    fun pass_failure_putsCardBackOnTop() = runRemoteTest { scope ->
        val source = deck(scope, "storm", "cyclops")
        api.onDecide = { throw IOException("offline") }
        source.pass("cyclops")
        assertEquals(listOf("cyclops", "storm"), source.cardIds())
        assertFalse("cyclops" in users.seenCharacterIds.value)
    }

    @Test
    fun pass_alreadyDecided_keepsCardOut() = runRemoteTest { scope ->
        val source = deck(scope, "storm")
        api.onDecide = { throw ApiException(ApiErrorCode.ALREADY_DECIDED, 409) }
        source.pass("storm")
        assertEquals(emptyList<String>(), source.cardIds())
    }

    @Test
    fun assemble_matched_returnsMatchWithReasons() = runRemoteTest { scope ->
        val source = deck(scope, "storm")
        api.onDecide = {
            ApiMatchResult(true, "storm", ApiMatchCharacter("storm", "Storm", "https://img/storm"), 87, listOf("Mutant"))
        }
        source.dismiss("storm")
        val outcome = source.assemble("storm") as AssembleOutcome.Matched
        assertEquals(87, outcome.match.score)
        assertEquals("storm", outcome.match.connectionId)
        assertEquals(listOf(Origin.Mutant), outcome.match.traitsInCommon)
    }

    @Test
    fun assemble_notMatched_andAlreadyDecided() = runRemoteTest { scope ->
        val source = deck(scope, "storm", "cyclops")
        api.onDecide = { ApiMatchResult(matched = false) }
        assertEquals(AssembleOutcome.NotMatched, source.assemble("storm"))
        api.onDecide = { throw ApiException(ApiErrorCode.ALREADY_DECIDED, 409) }
        assertEquals(AssembleOutcome.NotMatched, source.assemble("cyclops"))
    }

    @Test
    fun assemble_retryAfterFailure_reusesKey_andRestoreReturnsCard() = runRemoteTest { scope ->
        val source = deck(scope, "storm", "cyclops")
        api.onDecide = { throw IOException("timeout") }
        source.dismiss("cyclops")
        try {
            source.assemble("cyclops")
            fail("esperava IOException")
        } catch (_: IOException) {
            source.restore("cyclops")
        }
        assertEquals(listOf("cyclops", "storm"), source.cardIds())

        api.onDecide = { ApiMatchResult(matched = false) }
        source.assemble("cyclops")
        assertEquals(api.decisions[0].key, api.decisions[1].key)
    }

    @Test
    fun undo_putsCardOnTop_andNothingToUndoIsQuiet() = runRemoteTest { scope ->
        val source = deck(scope, "storm", canUndo = true)
        api.onUndo = { ApiDeckCard("cyclops", "Cyclops") }
        source.undo()
        assertEquals(listOf("cyclops", "storm"), source.cardIds())
        assertFalse(source.canUndo.value)

        api.onUndo = { throw ApiException(ApiErrorCode.NOTHING_TO_UNDO, 409) }
        source.undo()
        assertEquals(listOf("cyclops", "storm"), source.cardIds())
    }
}
