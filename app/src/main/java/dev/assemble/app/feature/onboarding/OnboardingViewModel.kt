package dev.assemble.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.model.PreferenceCategory
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.selectAny
import dev.assemble.app.core.model.toggle
import dev.assemble.app.core.model.withFame
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

    fun toggle(trait: Enum<*>) = updatePreferences { it.toggle(trait) }

    /** "Any": sem preferência na categoria (conjunto vazio). */
    fun selectAny(category: PreferenceCategory) = updatePreferences { it.selectAny(category) }

    fun setFame(value: Float) = updatePreferences { it.withFame(value) }

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
}
