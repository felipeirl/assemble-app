package dev.assemble.app.feature.character

import androidx.annotation.StringRes

/** Dado da fonte (Comic Vine). Vazio/nulo nunca chega aqui: o campo é omitido. */
data class ProfileFact(
    @StringRes val label: Int,
    val text: String? = null,
    val traits: List<Enum<*>> = emptyList(),
)

/** Traços que bateram numa categoria (Origin, Powers, Teams, Style). */
data class WhyYouMatchItem(
    @StringRes val category: Int,
    val traits: List<Enum<*>>,
)

sealed interface CharacterProfileUiState {
    data object Loading : CharacterProfileUiState

    data class Content(
        val connectionId: String,
        val name: String,
        val imageUrl: String?,
        /** Score gravado na conexão. */
        val score: Int,
        val whyYouMatch: List<WhyYouMatchItem>,
        val facts: List<ProfileFact>,
        /** true na primeira abertura: roda a animação de desbloqueio. */
        val unlockPending: Boolean,
    ) : CharacterProfileUiState

    /** Sem conexão com o personagem, ou a fonte não o devolveu. */
    data object Unavailable : CharacterProfileUiState

    data object Error : CharacterProfileUiState
}
