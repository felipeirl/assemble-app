package dev.assemble.app.feature.onboarding

import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.PreferenceCategory
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.Team
import dev.assemble.app.core.model.isAny
import dev.assemble.app.core.model.options
import dev.assemble.app.core.model.selectAny
import dev.assemble.app.core.model.toggle
import dev.assemble.app.core.model.totalChoices
import dev.assemble.app.core.model.withFame
import dev.assemble.app.feature.discover.DiscoverCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingUiStateTest {

    @Test
    fun anyEverywhere_hasNoChoices() {
        assertEquals(0, OnboardingUiState(Preferences.Any).preferences.totalChoices)
    }

    @Test
    fun choicesCountAcrossCategories_butNotFame() {
        val prefs = Preferences.Any.copy(
            origins = setOf(Origin.Mutant),
            powers = setOf(PowerFamily.Mind),
            teams = setOf(Team.XMen),
            fame = 1f,
        )
        assertEquals(3, prefs.totalChoices)
    }

    @Test
    fun anyIsSelectedOnlyWhenCategoryIsEmpty() {
        val prefs = Preferences.Any.copy(origins = setOf(Origin.Human))
        assertFalse(prefs.isAny(PreferenceCategory.Origin))
        assertTrue(prefs.isAny(PreferenceCategory.Powers))
        assertFalse(prefs.isAny(PreferenceCategory.Fame))
        assertTrue(prefs.options(PreferenceCategory.Origin).single { it.first == Origin.Human }.second)
    }

    @Test
    fun toggleAndSelectAny_editOneCategory() {
        val prefs = Preferences.Any.toggle(PowerFamily.Mind).toggle(PowerFamily.Flight).toggle(PowerFamily.Mind)
        assertEquals(setOf(PowerFamily.Flight), prefs.powers)
        assertTrue(prefs.selectAny(PreferenceCategory.Powers).isAny(PreferenceCategory.Powers))
        assertEquals(1f, prefs.withFame(5f).fame, 0f)
    }

    @Test
    fun stepsAreCategoriesThenReactionThenReveal() {
        assertEquals(PreferenceCategory.Origin, onboardingStepAt(0))
        assertEquals(PreferenceCategory.Fame, onboardingStepAt(PreferenceCategory.entries.lastIndex))
        assertEquals(PreferenceCategory.entries.size, REACTION_STEP)
        assertEquals(REACTION_STEP + 1, REVEAL_STEP)
        assertEquals(REVEAL_STEP + 1, ONBOARDING_STEP_COUNT)
    }

    @Test
    fun reactionPlaysCardsInPairsAndCountsPicks() {
        val cards = listOf("storm", "iron-man", "rocket", "jean-grey").map { DiscoverCard(it, it, null, emptyList()) }
        val start = ReactionState.Playing(cards)
        assertEquals(listOf("storm", "iron-man"), start.current.map { it.characterId })
        assertFalse(start.isLastPair)

        val second = start.after(picked = 1)
        assertEquals(listOf("rocket", "jean-grey"), second.current.map { it.characterId })
        assertTrue(second.isLastPair)

        val end = second.after(picked = null)
        assertTrue(end.finished)
        assertEquals(1, end.picks)
    }

    @Test
    fun reactionWithAnOddCardEndsWithASingleCard() {
        val cards = listOf("a", "b", "c").map { DiscoverCard(it, it, null, emptyList()) }

        assertEquals(listOf("c"), ReactionState.Playing(cards, pair = 1).current.map { it.characterId })
    }
}
