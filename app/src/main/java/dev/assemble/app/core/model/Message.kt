package dev.assemble.app.core.model

import java.time.Instant

enum class MessageAuthor { User, Character }

enum class MessageStatus { Sending, Sent, Failed }

/** Mensagem de uma conversa. Respostas do personagem são ficção gerada por IA. */
data class Message(
    val id: String,
    val connectionId: String,
    val author: MessageAuthor,
    val text: String,
    val sentAt: Instant,
    val status: MessageStatus = MessageStatus.Sent,
    /** Só relevante para mensagens do personagem. */
    val read: Boolean = true,
)
