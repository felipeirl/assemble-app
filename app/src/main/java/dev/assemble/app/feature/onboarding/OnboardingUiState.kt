package dev.assemble.app.feature.onboarding

import dev.assemble.app.core.model.PreferenceCategory
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.feature.discover.DiscoverCard

/** Passos do cadastro: as 5 categorias, a rodada "este ou aquele" e a revelação do perfil. */
val REACTION_STEP: Int = PreferenceCategory.entries.size
val REVEAL_STEP: Int = REACTION_STEP + 1
val ONBOARDING_STEP_COUNT: Int = REVEAL_STEP + 1

/** Passos de categoria, na mesma ordem das preferências. */
fun onboardingStepAt(index: Int): PreferenceCategory =
    PreferenceCategory.entries[index.coerceIn(PreferenceCategory.entries.indices)]

/** Quantos personagens aparecem juntos em cada duelo. */
const val DUEL_SIZE = 2

/**
 * Rodada "este ou aquele": um par por vez. Escolher um ensina o gosto (o escolhido conta como
 * curtido, o outro como recusado); "nenhum dos dois" recusa os dois. Nada disso é decisão.
 */
sealed interface ReactionState {
    data object Loading : ReactionState
    data object Failed : ReactionState
    data class Playing(val cards: List<DiscoverCard>, val pair: Int = 0, val picks: Int = 0) : ReactionState {
        val pairs: List<List<DiscoverCard>> get() = cards.chunked(DUEL_SIZE)
        val current: List<DiscoverCard> get() = pairs.getOrElse(pair) { emptyList() }
        val finished: Boolean get() = pair >= pairs.size
        val isLastPair: Boolean get() = pair == pairs.lastIndex

        /** [picked] = índice do escolhido no par, ou null para "nenhum dos dois". */
        fun after(picked: Int?): Playing = copy(pair = pair + 1, picks = picks + if (picked != null) 1 else 0)
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
