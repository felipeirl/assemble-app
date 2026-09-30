package dev.assemble.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.Style
import dev.assemble.app.core.model.Team
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Estado compartilhado pelos 5 passos. Começa com as preferências atuais do usuário
 * (no mock, as iniciais) e só grava ao tocar em "Start discovering".
 */
class OnboardingViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val state = MutableStateFlow(OnboardingUiState(userRepository.preferences.value))
    val uiState: StateFlow<OnboardingUiState> = state.asStateFlow()

    fun toggle(trait: Enum<*>) = updatePreferences { prefs ->
        when (trait) {
            is Origin -> prefs.copy(origins = prefs.origins.toggle(trait))
            is PowerFamily -> prefs.copy(powers = prefs.powers.toggle(trait))
            is Team -> prefs.copy(teams = prefs.teams.toggle(trait))
            is Style -> prefs.copy(styles = prefs.styles.toggle(trait))
            else -> prefs
        }
    }

    /** "Any": sem preferência na categoria (conjunto vazio). */
    fun selectAny(step: OnboardingStep) = updatePreferences { prefs ->
        when (step) {
            OnboardingStep.Origin -> prefs.copy(origins = emptySet())
            OnboardingStep.Powers -> prefs.copy(powers = emptySet())
            OnboardingStep.Teams -> prefs.copy(teams = emptySet())
            OnboardingStep.Style -> prefs.copy(styles = emptySet())
            OnboardingStep.Fame -> prefs
        }
    }

    fun setFame(value: Float) = updatePreferences {
        it.copy(fame = value.coerceIn(Preferences.FAME_ICONS, Preferences.FAME_HIDDEN_GEMS))
    }

    fun finish() {
        val current = state.value
        if (!current.canFinish || current.submitting) return
        state.update { it.copy(submitting = true, showError = false) }
        viewModelScope.launch {
            try {
                userRepository.completeOnboarding(current.preferences)
            } catch (_: IOException) {
                state.update { it.copy(submitting = false, showError = true) }
            }
        }
    }

    private fun updatePreferences(transform: (Preferences) -> Preferences) {
        state.update { it.copy(preferences = transform(it.preferences), showError = false) }
    }

    private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
}
