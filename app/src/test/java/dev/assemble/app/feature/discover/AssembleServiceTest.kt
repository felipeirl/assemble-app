package dev.assemble.app.feature.discover

import dev.assemble.app.core.data.remote.runRemoteTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration

class AssembleServiceTest {

    private class RecordingDeckSource(private val outcome: () -> AssembleOutcome) : DeckSource {
        val events = mutableListOf<String>()
        override val deck: Flow<DeckState> = MutableStateFlow(DeckState.Loading)
        override val canUndo: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun reload() = Unit
        override suspend fun pass(characterId: String) {
            events += "pass:$characterId"
        }
        override suspend fun undo() = Unit
        override suspend fun dismiss(characterId: String) {
            events += "dismiss:$characterId"
        }
        override suspend fun assemble(characterId: String): AssembleOutcome {
            events += "assemble:$characterId"
            return outcome()
        }
        override suspend fun restore(characterId: String) {
            events += "restore:$characterId"
        }
    }

    private val match = DiscoverMatch("storm", "Storm", null, 90, "storm", emptyList())

    @Test
    fun assemble_matched_queuesMatch() = runRemoteTest { scope ->
        val source = RecordingDeckSource { AssembleOutcome.Matched(match) }
        val service = AssembleService(source, scope, replyDelay = Duration.ZERO)

        service.assemble("storm")

        assertEquals(listOf("dismiss:storm", "assemble:storm"), source.events)
        assertEquals(match, service.match.first())
        service.dismissMatch()
        assertNull(service.match.first())
    }

    @Test
    fun assembling_isTrueWhileWaitingForTheCharacter() = runRemoteTest { scope ->
        val gate = CompletableDeferred<AssembleOutcome>()
        val source = object : DeckSource by RecordingDeckSource({ AssembleOutcome.NotMatched }) {
            override suspend fun assemble(characterId: String): AssembleOutcome = gate.await()
        }
        val service = AssembleService(source, scope, replyDelay = Duration.ZERO)

        service.assemble("storm")
        assertEquals(true, service.assembling.first())

        gate.complete(AssembleOutcome.NotMatched)
        assertEquals(false, service.assembling.first())
    }

    @Test
    fun assemble_notMatched_isSilent() = runRemoteTest { scope ->
        val service = AssembleService(RecordingDeckSource { AssembleOutcome.NotMatched }, scope, replyDelay = Duration.ZERO)
        service.assemble("storm")
        assertNull(service.message.value)
        assertNull(service.match.first())
    }

    @Test
    fun assemble_failure_restoresCardAndWarns() = runRemoteTest { scope ->
        val source = RecordingDeckSource { throw IOException("offline") }
        val service = AssembleService(source, scope, replyDelay = Duration.ZERO)

        service.assemble("storm")

        assertEquals(listOf("dismiss:storm", "assemble:storm", "restore:storm"), source.events)
        assertEquals(DiscoverMessage.AssembleFailed, service.message.value)
    }
}
