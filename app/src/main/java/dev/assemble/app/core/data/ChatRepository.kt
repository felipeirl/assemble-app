package dev.assemble.app.core.data

import dev.assemble.app.core.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

interface ChatRepository {
    /** Mensagens de uma conversa, em ordem cronológica. O primeiro valor pode falhar com IOException. */
    fun observeMessages(connectionId: String): Flow<List<Message>>

    /** Todas as mensagens (lista de conversas e badge). Sem latência simulada. */
    val allMessages: StateFlow<List<Message>>

    /** Conversas em que o personagem está "digitando". */
    val typingConnectionIds: StateFlow<Set<String>>

    /** Conversas em que a última resposta do personagem está sendo gerada de novo. */
    val regeneratingConnectionIds: StateFlow<Set<String>>

    /**
     * Envia a mensagem do usuário. Em sucesso, a resposta ficcional do personagem chega depois
     * do indicador de digitação. Em falha, a mensagem fica com status Failed (use [retry]).
     */
    suspend fun send(connectionId: String, text: String)

    suspend fun retry(messageId: String)

    /** Primeira mensagem do personagem, logo após a conexão. */
    suspend fun startConversation(connectionId: String)

    /** Gera outra resposta no lugar da última do personagem (mesma mensagem, texto novo). */
    suspend fun regenerateLast(connectionId: String)

    /** Volta a conversa até [messageId] (uma resposta do personagem): o que veio depois é apagado. */
    suspend fun rewindTo(connectionId: String, messageId: String)

    suspend fun markRead(connectionId: String)

    suspend fun deleteAll()

    /** Respostas sugeridas pelo backend para a conversa; null = o app monta as suas. */
    fun observeSuggestions(connectionId: String): Flow<List<String>?> = flowOf(null)
}
