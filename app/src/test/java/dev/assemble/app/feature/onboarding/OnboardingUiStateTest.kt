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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingUiStateTest {

    @Test
    fun anyEverywhere_cannotFinish() {
        val state = OnboardingUiState(Preferences.Any)
        assertEquals(0, state.preferences.totalChoices)
        assertFalse(state.canFinish)
    }

    @Test
    fun threeChoicesAcrossCategories_canFinish() {
        val state = OnboardingUiState(
            Preferences.Any.copy(
                origins = setOf(Origin.Mutant),
                powers = setOf(PowerFamily.Mind),
                teams = setOf(Team.XMen),
            ),
        )
        assertEquals(3, state.preferences.totalChoices)
        assertTrue(state.canFinish)
    }

    @Test
    fun fameDoesNotCountAsAChoice() {
        val state = OnboardingUiState(Preferences.Any.copy(origins = setOf(Origin.Human, Origin.Alien), fame = 1f))
        assertFalse(state.canFinish)
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
    fun stepsFollowCategoryOrder() {
        assertEquals(PreferenceCategory.Origin, onboardingStepAt(0))
        assertEquals(PreferenceCategory.Fame, onboardingStepAt(99))
        assertTrue(PreferenceCategory.Fame.isLastStep)
    }
}
