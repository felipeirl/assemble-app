package dev.assemble.app.core.domain

import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import dev.assemble.app.core.model.ProfileStyle
import dev.assemble.app.core.model.Team
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

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

    @Test
    fun catalogHas16AchievementsSplitByTier() {
        assertEquals(16, Achievement.entries.size)
        assertEquals(4, Achievement.entries.count { it.tier == AchievementTier.Bronze })
        assertEquals(7, Achievement.entries.count { it.tier == AchievementTier.Silver })
        assertEquals(5, Achievement.entries.count { it.tier == AchievementTier.Gold })
    }

    @Test
    fun newMetricsFeedTheirAchievements() {
        val stats = AchievementStats(
            connections = 30, messagesSent = 199, charactersSeen = 300, distinctTeams = 6,
            charactersChatted = 15, activeDays = 6, profileComplete = true,
        )
        assertTrue(progressOf(stats, Achievement.Legion).unlocked)
        assertFalse(progressOf(stats, Achievement.Veteran).unlocked)
        assertTrue(progressOf(stats, Achievement.Cartographer).unlocked)
        assertTrue(progressOf(stats, Achievement.Multiverse).unlocked)
        assertTrue(progressOf(stats, Achievement.Diplomat).unlocked)
        assertFalse(progressOf(stats, Achievement.Sentinel).unlocked)
        assertEquals(6, progressOf(stats, Achievement.Sentinel).current)
        assertTrue(progressOf(stats, Achievement.SecretIdentity).unlocked)
    }

    @Test
    fun incompleteProfileHasZeroProgress() {
        val stats = AchievementStats(connections = 0, messagesSent = 0, charactersSeen = 0, distinctTeams = 0)
        val secret = progressOf(stats, Achievement.SecretIdentity)
        assertEquals(0, secret.current)
        assertFalse(secret.unlocked)
    }

    @Test
    fun unknownTeamsAlsoLeaveMultiverseUnknown() {
        val stats = AchievementStats(connections = 1, messagesSent = 1, charactersSeen = 1, distinctTeams = null)
        assertNull(progressOf(stats, Achievement.Multiverse).current)
    }

    @Test
    fun charactersChatted_countsConversationsTheUserWroteIn() {
        fun message(connectionId: String, author: MessageAuthor) =
            Message(id = "$connectionId-$author", connectionId = connectionId, author = author, text = "oi", sentAt = Instant.EPOCH)
        val messages = listOf(
            message("a", MessageAuthor.User),
            message("a", MessageAuthor.User),
            message("b", MessageAuthor.User),
            message("c", MessageAuthor.Character),
        )
        assertEquals(2, AchievementRules.charactersChatted(messages))
    }

    @Test
    fun isProfileComplete_needsAnswerAndFeaturedConnection() {
        assertFalse(AchievementRules.isProfileComplete(ProfileStyle(promptAnswer = "Voar")))
        assertFalse(AchievementRules.isProfileComplete(ProfileStyle(promptAnswer = "  ", featuredConnections = listOf("storm"))))
        assertTrue(AchievementRules.isProfileComplete(ProfileStyle(promptAnswer = "Voar", featuredConnections = listOf("storm"))))
    }
}
