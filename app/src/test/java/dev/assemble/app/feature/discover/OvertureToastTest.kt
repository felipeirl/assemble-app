package dev.assemble.app.feature.discover

import dev.assemble.app.core.data.remote.Overture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class OvertureToastTest {
    private val now = Instant.parse("2026-10-07T12:00:00Z")

    @Test
    fun newOverture_isTheOldestNotAnnouncedYet() {
        val pending = listOf(Overture("wasp", now), Overture("storm", now.plusSeconds(60)))

        assertEquals("wasp", newOverture(pending, emptySet())?.characterId)
        assertEquals("storm", newOverture(pending, setOf("wasp"))?.characterId)
    }

    @Test
    fun newOverture_isNullWhenEverythingWasAnnounced() {
        assertNull(newOverture(listOf(Overture("wasp", now)), setOf("wasp")))
        assertNull(newOverture(emptyList(), emptySet()))
    }
}
