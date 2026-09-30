package dev.assemble.app.feature.profile

import dev.assemble.app.core.data.mock.CHARACTERS_ASSET_PATH
import dev.assemble.app.core.data.mock.parseMockCharacters
import dev.assemble.app.core.model.Connection
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Style
import dev.assemble.app.core.model.Team
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

class ProfileLogicTest {

    private val characters = parseMockCharacters(File("src/main/assets/$CHARACTERS_ASSET_PATH").readText())
        .associateBy { it.id }

    private fun connection(characterId: String, score: Int) =
        Connection("c-$characterId", characterId, score, threshold = 70, createdAt = Instant.EPOCH)

    @Test
    fun stats_countSeenAndConnectedOnce_andAverageScores() {
        val stats = profileStats(
            seenIds = setOf("jean-grey", "rocket"),
            connections = listOf(connection("jean-grey", 77), connection("storm", 66)),
        )
        assertEquals(3, stats.charactersSeen)
        assertEquals(2, stats.connections)
        assertEquals(72, stats.averageMatch) // 71.5 → 72
    }

    @Test
    fun stats_withoutConnections_haveNoAverage() {
        assertNull(profileStats(emptySet(), emptyList()).averageMatch)
    }

    @Test
    fun topTraits_rankByFrequencyAcrossConnections() {
        val connected = listOf("storm", "jean-grey", "iron-man").map(characters::getValue)
        val top = topTraits(connected)
        // Storm e Jean Grey: Mutant, XMen, Idealist; Storm e Iron Man: Flight, Leadership.
        assertEquals(5, top.size)
        assertTrue(top.containsAll(listOf(Origin.Mutant, Team.XMen, PowerFamily.Flight, Style.Leadership, Style.Idealist)))
    }

    @Test
    fun topTraits_emptyWithoutConnections() {
        assertTrue(topTraits(emptyList()).isEmpty())
    }
}
