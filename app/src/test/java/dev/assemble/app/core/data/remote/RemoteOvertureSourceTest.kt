package dev.assemble.app.core.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class RemoteOvertureSourceTest {
    private val store = InMemoryUserDataStore()

    private fun overture(id: String, status: String, at: Instant) =
        Document(id, mapOf("status" to status, "createdAt" to at))

    @Test
    fun pending_keepsOnlyPendingOnesOldestFirst() = runRemoteTest {
        store.overtures.value = listOf(
            overture("storm", "pending", TestNow.plusSeconds(60)),
            overture("rogue", "skipped", TestNow),
            overture("thor", "accepted", TestNow),
            overture("wasp", "pending", TestNow),
            Document("broken", mapOf("status" to "pending")),
        )

        val pending = RemoteOvertureSource(MutableStateFlow("uid-1"), store).pending.first()

        assertEquals(listOf("wasp", "storm"), pending.map { it.characterId })
    }

    @Test
    fun pending_isEmptyWithoutAUser() = runRemoteTest {
        store.overtures.value = listOf(overture("storm", "pending", TestNow))

        assertEquals(emptyList<Overture>(), RemoteOvertureSource(MutableStateFlow(null), store).pending.first())
    }
}
