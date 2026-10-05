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
    fun reactionCountsLikesAndFinishesAfterTheLastCard() {
        val cards = listOf(
            DiscoverCard("storm", "Storm", null, emptyList()),
            DiscoverCard("rocket", "Rocket", null, emptyList()),
        )
        val start = ReactionState.Playing(cards)
        assertEquals("storm", start.current?.characterId)

        val end = start.after(liked = true).after(liked = false)

        assertTrue(end.finished)
        assertNull(end.current)
        assertEquals(1, end.liked)
    }
}
