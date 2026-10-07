package dev.assemble.app.core.domain

import dev.assemble.app.core.model.AvatarFrame
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.ProfileAccent
import dev.assemble.app.core.model.ProfileCover
import dev.assemble.app.core.model.ProfileStyle
import dev.assemble.app.core.model.ProfileTitle
import dev.assemble.app.core.model.Style
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRulesTest {

    @Test
    fun freeItems_needNoAchievement() {
        listOf(AvatarFrame.Simple, AvatarFrame.Ring).forEach { assertTrue(ProfileRules.isUnlocked(Reward.Frame(it), emptySet())) }
        listOf(ProfileCover.Energy, ProfileCover.Halftone, ProfileCover.Comic, ProfileCover.Night)
            .forEach { assertTrue(ProfileRules.isUnlocked(Reward.Cover(it), emptySet())) }
        listOf(ProfileAccent.Pink, ProfileAccent.Red, ProfileAccent.Blue, ProfileAccent.Violet, ProfileAccent.Gold)
            .forEach { assertTrue(ProfileRules.isUnlocked(Reward.Accent(it), emptySet())) }
    }

    @Test
    fun rewards_followTheirAchievement() {
        assertFalse(ProfileRules.isUnlocked(Reward.Frame(AvatarFrame.Hexagon), emptySet()))
        assertTrue(ProfileRules.isUnlocked(Reward.Frame(AvatarFrame.Hexagon), setOf(Achievement.TeamUp)))
        assertFalse(ProfileRules.isUnlocked(Reward.Frame(AvatarFrame.Burst), setOf(Achievement.TeamUp)))
        assertTrue(ProfileRules.isUnlocked(Reward.Frame(AvatarFrame.Burst), setOf(Achievement.Crossover)))
        assertEquals(Achievement.Legion, ProfileRules.requiredAchievement(Reward.Frame(AvatarFrame.Shield)))
        assertEquals(Achievement.Cartographer, ProfileRules.requiredAchievement(Reward.Cover(ProfileCover.Cosmos)))
        assertEquals(Achievement.FullRoster, ProfileRules.requiredAchievement(Reward.Accent(ProfileAccent.Emerald)))
        assertEquals(Achievement.FirstConnection, ProfileRules.requiredAchievement(Reward.Title(ProfileTitle.Recruit)))
    }

    @Test
    fun everyRewardBelongsToExactlyOneAchievement() {
        val rewards = Achievement.entries.map { it.reward }
        assertEquals(rewards.size, rewards.toSet().size)
        ProfileTitle.entries.forEach { assertTrue(Reward.Title(it) in rewards) }
    }

    @Test
    fun archetype_usesFirstStyleAndOriginInEnumOrder() {
        val preferences = Preferences.Any.copy(
            styles = setOf(Style.Strategist, Style.Humor),
            origins = setOf(Origin.Cosmic, Origin.Mutant),
        )
        assertEquals(Archetype(Style.Humor, Origin.Mutant), ProfileRules.archetype(preferences))
    }

    @Test
    fun archetype_keepsTheHalfThatExists() {
        assertEquals(Archetype(null, Origin.Alien), ProfileRules.archetype(Preferences.Any.copy(origins = setOf(Origin.Alien))))
    }

    @Test
    fun archetype_isNullWhenBothAreAny() {
        assertNull(ProfileRules.archetype(Preferences.Any))
    }

    @Test
    fun sanitize_dropsWhatNoLongerApplies() {
        val style = ProfileStyle(
            cover = ProfileCover.Cosmos,
            accent = ProfileAccent.Emerald,
            frame = AvatarFrame.Burst,
            title = ProfileTitle.Diplomat,
            promptAnswer = "  " + "x".repeat(ProfileStyle.PROMPT_ANSWER_MAX + 5),
            featuredConnections = listOf("storm", "gone", "storm", "rocket"),
            featuredBadges = listOf("Crossover", "FirstConnection", "Removed"),
        )
        val clean = ProfileRules.sanitize(style, unlocked = setOf(Achievement.FirstConnection), connectedIds = setOf("storm", "rocket"))
        assertEquals(ProfileCover.Energy, clean.cover)
        assertEquals(ProfileAccent.Pink, clean.accent)
        assertEquals(AvatarFrame.Ring, clean.frame)
        assertNull(clean.title)
        assertEquals(ProfileStyle.PROMPT_ANSWER_MAX, clean.promptAnswer.length)
        assertEquals(listOf("storm", "rocket"), clean.featuredConnections)
        assertEquals(listOf("FirstConnection"), clean.featuredBadges)
    }

    @Test
    fun sanitize_keepsUnlockedTitle() {
        val clean = ProfileRules.sanitize(ProfileStyle(title = ProfileTitle.Recruit), setOf(Achievement.FirstConnection), emptySet())
        assertEquals(ProfileTitle.Recruit, clean.title)
    }

    @Test
    fun toggleFeatured_keepsOrderAndLimit() {
        val full = listOf("a", "b", "c")
        assertEquals(full, ProfileRules.toggleFeatured(full, "d"))
        assertEquals(listOf("a", "c"), ProfileRules.toggleFeatured(full, "b"))
        assertEquals(listOf("a", "c", "b"), ProfileRules.toggleFeatured(listOf("a", "c"), "b"))
    }
}
