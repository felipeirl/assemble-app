package dev.assemble.app.feature.discover

import dev.assemble.app.core.model.MatchBand

/** Card do Discover: só a faixa (nunca a %) e os traços em comum. */
data class DiscoverCard(
    val characterId: String,
    val name: String,
    val imageUrl: String?,
    val band: MatchBand,
    val traitsInCommon: List<Enum<*>>,
)

/** Resultado de um Assemble que virou conexão (pop-up de match, etapa 8). */
data class DiscoverMatch(
    val characterId: String,
    val name: String,
    val imageUrl: String?,
    val score: Int,
    val connectionId: String,
    val traitsInCommon: List<Enum<*>>,
)

enum class DiscoverMessage { NotEnoughInCommon, AssembleFailed }

/** Pilha de cards: os quatro estados obrigatórios. */
sealed interface DeckState {
    data object Loading : DeckState
    data class Content(val cards: List<DiscoverCard>) : DeckState
    data object Empty : DeckState
    data object Error : DeckState
}

data class DiscoverUiState(
    val deck: DeckState = DeckState.Loading,
    val canUndo: Boolean = false,
    val refreshing: Boolean = false,
    val match: DiscoverMatch? = null,
    val message: DiscoverMessage? = null,
)
