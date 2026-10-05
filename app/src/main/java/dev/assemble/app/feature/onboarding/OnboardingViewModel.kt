package dev.assemble.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.data.remote.LOOKING_FOR_MAX
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
 * Estado compartilhado por todos os passos. Começa com as preferências atuais do usuário
 * (no mock, as iniciais) e só grava ao tocar em "Start discovering", na revelação.
 */
class OnboardingViewModel(
    private val userRepository: UserRepository,
    private val tasteSource: TasteSource,
) : ViewModel() {
    private val state = MutableStateFlow(OnboardingUiState(userRepository.preferences.value))
    val uiState: StateFlow<OnboardingUiState> = state.asStateFlow()

    fun toggle(trait: Enum<*>) = updatePreferences { it.toggle(trait) }

    /** "Any": sem preferência na categoria (conjunto vazio). */
    fun selectAny(category: PreferenceCategory) = updatePreferences { it.selectAny(category) }

    fun setFame(value: Float) = updatePreferences { it.withFame(value) }

    /** Busca os cards uma vez; voltar ao passo não reinicia a rodada. */
    fun loadReactionCards() {
        if (state.value.reaction is ReactionState.Playing) return
        state.update { it.copy(reaction = ReactionState.Loading) }
        viewModelScope.launch {
            val reaction = try {
                ReactionState.Playing(tasteSource.reactionCards())
            } catch (_: IOException) {
                ReactionState.Failed
            }
            state.update { it.copy(reaction = reaction) }
        }
    }

    fun react(liked: Boolean) {
        val playing = state.value.reaction as? ReactionState.Playing ?: return
        val card = playing.current ?: return
        state.update { it.copy(reaction = playing.after(liked)) }
        viewModelScope.launch {
            try {
                tasteSource.react(card.characterId, liked)
            } catch (_: IOException) {
                // Sinal perdido: o gosto aprende um pouco menos, e o cadastro não para por isso.
            }
        }
    }

    fun setLookingFor(text: String) {
        state.update { it.copy(lookingFor = text.take(LOOKING_FOR_MAX)) }
    }

    fun finish() {
        val current = state.value
        if (current.submitting) return
        state.update { it.copy(submitting = true, showError = false) }
        viewModelScope.launch {
            try {
                userRepository.completeOnboarding(current.preferences, current.lookingFor)
            } catch (_: IOException) {
                state.update { it.copy(submitting = false, showError = true) }
            }
        }
    }

    private fun updatePreferences(transform: (Preferences) -> Preferences) {
        state.update { it.copy(preferences = transform(it.preferences), showError = false) }
    }
}
