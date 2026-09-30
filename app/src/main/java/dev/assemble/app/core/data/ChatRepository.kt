package dev.assemble.app.core.data

import dev.assemble.app.core.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    /** Mensagens de uma conversa, em ordem cronológica. O primeiro valor pode falhar com IOException. */
    fun observeMessages(connectionId: String): Flow<List<Message>>

    /** Todas as mensagens (lista de conversas e badge). Sem latência simulada. */
    val allMessages: StateFlow<List<Message>>

    /** Conversas em que o personagem está "digitando". */
    val typingConnectionIds: StateFlow<Set<String>>

    /**
     * Envia a mensagem do usuário. Em sucesso, a resposta ficcional do personagem chega depois
     * do indicador de digitação. Em falha, a mensagem fica com status Failed (use [retry]).
     */
    suspend fun send(connectionId: String, text: String)

    suspend fun retry(messageId: String)

    /** Primeira mensagem do personagem, logo após a conexão. */
    suspend fun startConversation(connectionId: String)

    suspend fun markRead(connectionId: String)

    suspend fun deleteAll()
}
