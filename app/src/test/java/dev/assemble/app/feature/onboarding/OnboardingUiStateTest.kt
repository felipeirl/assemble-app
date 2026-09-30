package dev.assemble.app.feature.onboarding

import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.Team
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingUiStateTest {

    @Test
    fun anyEverywhere_cannotFinish() {
        val state = OnboardingUiState(Preferences.Any)
        assertEquals(0, state.totalChoices)
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
        assertEquals(3, state.totalChoices)
        assertTrue(state.canFinish)
    }

    @Test
    fun fameDoesNotCountAsAChoice() {
        val state = OnboardingUiState(Preferences.Any.copy(origins = setOf(Origin.Human, Origin.Alien), fame = 1f))
        assertFalse(state.canFinish)
    }

    @Test
    fun anyIsSelectedOnlyWhenCategoryIsEmpty() {
        val state = OnboardingUiState(Preferences.Any.copy(origins = setOf(Origin.Human)))
        assertFalse(state.isAny(OnboardingStep.Origin))
        assertTrue(state.isAny(OnboardingStep.Powers))
        assertTrue(state.optionsFor(OnboardingStep.Origin).single { it.first == Origin.Human }.second)
    }
}
