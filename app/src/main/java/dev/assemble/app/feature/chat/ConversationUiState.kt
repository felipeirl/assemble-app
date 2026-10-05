package dev.assemble.app.feature.chat

import dev.assemble.app.core.model.Message

/** Aviso de uma ação que não deu certo (regenerar, voltar a conversa). */
enum class ConversationNotice { ActionFailed }

sealed interface ConversationUiState {
    data object Loading : ConversationUiState

    data class Content(
        val characterId: String,
        val name: String,
        val imageUrl: String?,
        val messages: List<Message>,
        val typing: Boolean,
        /** Perguntas prontas acima do campo; giram a cada mensagem sua. */
        val suggestions: List<ReplySuggestion>,
        val notice: ConversationNotice? = null,
    ) : ConversationUiState {
        val hasUnread: Boolean get() = messages.any { !it.read }
    }

    /** A conexão ou o personagem não existem mais. */
    data object Unavailable : ConversationUiState

    data object Error : ConversationUiState
}
