package dev.assemble.app.feature.onboarding

import dev.assemble.app.core.model.PreferenceCategory
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.feature.discover.DiscoverCard

/** Passos do cadastro: as 5 categorias, a rodada de reação e a revelação do perfil. */
val REACTION_STEP: Int = PreferenceCategory.entries.size
val REVEAL_STEP: Int = REACTION_STEP + 1
val ONBOARDING_STEP_COUNT: Int = REVEAL_STEP + 1

/** Passos de categoria, na mesma ordem das preferências. */
fun onboardingStepAt(index: Int): PreferenceCategory =
    PreferenceCategory.entries[index.coerceIn(PreferenceCategory.entries.indices)]

/** Rodada de reação: um card por vez; cada Curti/Pular só ensina o gosto. */
sealed interface ReactionState {
    data object Loading : ReactionState
    data object Failed : ReactionState
    data class Playing(val cards: List<DiscoverCard>, val index: Int = 0, val liked: Int = 0) : ReactionState {
        val current: DiscoverCard? get() = cards.getOrNull(index)
        val finished: Boolean get() = index >= cards.size

        fun after(liked: Boolean): Playing = copy(index = index + 1, liked = this.liked + if (liked) 1 else 0)
    }
}

data class OnboardingUiState(
    val preferences: Preferences,
    val submitting: Boolean = false,
    val showError: Boolean = false,
    val reaction: ReactionState = ReactionState.Loading,
    /** "O que você procura numa conversa?", opcional. */
    val lookingFor: String = "",
)
