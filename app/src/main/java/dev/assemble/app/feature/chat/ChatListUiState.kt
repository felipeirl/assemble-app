package dev.assemble.app.feature.chat

import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.Connection
import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import java.time.Instant

/** Linha da lista de conversas. */
data class ConversationSummary(
    val connectionId: String,
    val characterId: String,
    val name: String,
    val imageUrl: String?,
    val score: Int,
    val lastMessage: String?,
    /** Hora da última mensagem (ou da conexão, se ainda não houver mensagem). */
    val lastActivity: Instant,
    val unread: Boolean,
    /** true depois que você mandou a primeira mensagem; antes disso a conexão aparece na fileira de novas. */
    val replied: Boolean,
)

sealed interface ChatListUiState {
    data object Loading : ChatListUiState
    data class Content(
        val newConnections: List<ConversationSummary>,
        val conversations: List<ConversationSummary>,
    ) : ChatListUiState
    data object Empty : ChatListUiState
    data object Error : ChatListUiState
}

/**
 * Uma linha por conexão, mais recente primeiro. Não lida = há mensagem do personagem não lida.
 * Conexões cujo personagem não está no catálogo ficam de fora (nada é inventado).
 */
internal fun buildConversationSummaries(
    connections: List<Connection>,
    messages: List<Message>,
    charactersById: Map<String, Character>,
): List<ConversationSummary> {
    val byConnection = messages.groupBy { it.connectionId }
    return connections.mapNotNull { connection ->
        val character = charactersById[connection.characterId] ?: return@mapNotNull null
        val thread = byConnection[connection.id].orEmpty().sortedBy { it.sentAt }
        val last = thread.lastOrNull()
        ConversationSummary(
            connectionId = connection.id,
            characterId = character.id,
            name = character.name,
            imageUrl = character.imageUrl,
            score = connection.score,
            lastMessage = last?.text,
            lastActivity = last?.sentAt ?: connection.createdAt,
            unread = thread.any { it.author == MessageAuthor.Character && !it.read },
            replied = thread.any { it.author == MessageAuthor.User },
        )
    }.sortedByDescending { it.lastActivity }
}
