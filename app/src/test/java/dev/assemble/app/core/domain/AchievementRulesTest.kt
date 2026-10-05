package dev.assemble.app.core.domain

import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.Team
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementRulesTest {

    private fun progressOf(stats: AchievementStats, achievement: Achievement) =
        AchievementRules.evaluate(stats).first { it.achievement == achievement }

    @Test
    fun unlocksWhenMetricReachesTarget() {
        val stats = AchievementStats(connections = 5, messagesSent = 0, charactersSeen = 29, distinctTeams = 3)
        assertTrue(progressOf(stats, Achievement.FirstConnection).unlocked)
        assertTrue(progressOf(stats, Achievement.TeamUp).unlocked)
        assertFalse(progressOf(stats, Achievement.IceBreaker).unlocked)
        assertFalse(progressOf(stats, Achievement.Explorer).unlocked)
        assertTrue(progressOf(stats, Achievement.Crossover).unlocked)
    }

    @Test
    fun progressIsCappedAtTarget() {
        val stats = AchievementStats(connections = 12, messagesSent = 80, charactersSeen = 0, distinctTeams = 0)
        assertEquals(5, progressOf(stats, Achievement.TeamUp).current)
        assertEquals(50, progressOf(stats, Achievement.Storyteller).current)
    }

    @Test
    fun unknownTeamsStayLockedWithoutProgress() {
        val stats = AchievementStats(connections = 1, messagesSent = 1, charactersSeen = 1, distinctTeams = null)
        val crossover = progressOf(stats, Achievement.Crossover)
        assertNull(crossover.current)
        assertFalse(crossover.unlocked)
    }

    @Test
    fun distinctTeamsIgnoresSoloAndRepeats() {
        fun character(id: String, vararg teams: Team) = Character(
            id = id, name = id, realName = null, imageUrl = null, origin = null,
            powers = emptyList(), teams = teams.toList(), styles = emptyList(),
            firstAppearance = null, issueAppearances = null, bio = null,
        )
        val connected = listOf(
            character("a", Team.XMen, Team.Avengers),
            character("b", Team.XMen),
            character("c", Team.Solo),
            character("d", Team.Guardians),
        )
        assertEquals(3, AchievementRules.distinctTeams(connected))
    }
}
