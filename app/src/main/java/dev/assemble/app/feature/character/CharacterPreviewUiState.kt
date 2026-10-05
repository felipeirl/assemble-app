package dev.assemble.app.feature.character


/** Preview de personagem sem conexão: os quatro estados obrigatórios. */
sealed interface CharacterPreviewUiState {
    data object Loading : CharacterPreviewUiState

    data class Content(
        val name: String,
        val imageUrl: String?,
        val traitsInCommon: List<Enum<*>>,
        /** true enquanto Pass/Assemble está em andamento (evita toque duplo). */
        val acting: Boolean = false,
    ) : CharacterPreviewUiState

    /** A fonte não devolveu o personagem. */
    data object Unavailable : CharacterPreviewUiState

    data object Error : CharacterPreviewUiState
}
