package dev.assemble.app.core.domain

import dev.assemble.app.core.model.AvatarFrame
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.ProfileStyle
import dev.assemble.app.core.model.Style
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRulesTest {

    @Test
    fun freeFrames_needNoAchievement() {
        assertTrue(ProfileRules.isFrameUnlocked(AvatarFrame.Simple, emptySet()))
        assertTrue(ProfileRules.isFrameUnlocked(AvatarFrame.Ring, emptySet()))
    }

    @Test
    fun specialFrames_followTheirAchievement() {
        assertFalse(ProfileRules.isFrameUnlocked(AvatarFrame.Hexagon, emptySet()))
        assertTrue(ProfileRules.isFrameUnlocked(AvatarFrame.Hexagon, setOf(Achievement.TeamUp)))
        assertFalse(ProfileRules.isFrameUnlocked(AvatarFrame.Burst, setOf(Achievement.TeamUp)))
        assertTrue(ProfileRules.isFrameUnlocked(AvatarFrame.Burst, setOf(Achievement.Crossover)))
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
            frame = AvatarFrame.Burst,
            promptAnswer = "  " + "x".repeat(ProfileStyle.PROMPT_ANSWER_MAX + 5),
            featuredConnections = listOf("storm", "gone", "storm", "rocket"),
            featuredBadges = listOf("Crossover", "FirstConnection", "Removed"),
        )
        val clean = ProfileRules.sanitize(style, unlocked = setOf(Achievement.FirstConnection), connectedIds = setOf("storm", "rocket"))
        assertEquals(AvatarFrame.Ring, clean.frame)
        assertEquals(ProfileStyle.PROMPT_ANSWER_MAX, clean.promptAnswer.length)
        assertEquals(listOf("storm", "rocket"), clean.featuredConnections)
        assertEquals(listOf("FirstConnection"), clean.featuredBadges)
    }

    @Test
    fun toggleFeatured_keepsOrderAndLimit() {
        val full = listOf("a", "b", "c")
        assertEquals(full, ProfileRules.toggleFeatured(full, "d"))
        assertEquals(listOf("a", "c"), ProfileRules.toggleFeatured(full, "b"))
        assertEquals(listOf("a", "c", "b"), ProfileRules.toggleFeatured(listOf("a", "c"), "b"))
    }
}
