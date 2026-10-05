package dev.assemble.app.feature.discover

/** Card do Discover: nome, arte e traços em comum. Nem faixa nem % de compatibilidade. */
data class DiscoverCard(
    val characterId: String,
    val name: String,
    val imageUrl: String?,
    val traitsInCommon: List<Enum<*>>,
    /** Mini descrição (primeira frase da bio); só vem do backend. */
    val tagline: String? = null,
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

/** Aviso do Discover. Só falha vira aviso: um Assemble sem match passa em silêncio. */
enum class DiscoverMessage { AssembleFailed }

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
    val match: DiscoverMatch? = null,
    val message: DiscoverMessage? = null,
    /** Um Assemble espera a resposta do personagem. */
    val assembling: Boolean = false,
)
