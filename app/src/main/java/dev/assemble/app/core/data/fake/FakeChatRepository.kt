package dev.assemble.app.core.data.fake

import dev.assemble.app.core.data.ChatRepository
import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.data.mock.MockReplies
import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import dev.assemble.app.core.model.MessageStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Conversas em memória. Falha 1 a cada [FAIL_EVERY_NTH_SEND] envios (a mensagem fica Failed).
 * A resposta do personagem chega após ~1,2 s de "digitando".
 */
class FakeChatRepository(
    private val network: FakeNetwork,
    private val clock: Clock,
    private val connections: ConnectionRepository,
    initialMessages: List<Message>,
) : ChatRepository {
    private val messages = MutableStateFlow(initialMessages)
    private val typing = MutableStateFlow<Set<String>>(emptySet())
    private val sendAttempts = AtomicInteger(0)

    override val allMessages: StateFlow<List<Message>> = messages.asStateFlow()
    override val typingConnectionIds: StateFlow<Set<String>> = typing.asStateFlow()

    override fun observeMessages(connectionId: String): Flow<List<Message>> = flow {
        network.call()
        emitAll(messages.map { list -> list.filter { it.connectionId == connectionId }.sortedBy { it.sentAt } })
    }

    override suspend fun send(connectionId: String, text: String) {
        val message = Message(
            id = UUID.randomUUID().toString(),
            connectionId = connectionId,
            author = MessageAuthor.User,
            text = text,
            sentAt = Instant.now(clock),
            status = MessageStatus.Sending,
        )
        messages.update { it + message }
        deliver(message)
    }

    override suspend fun retry(messageId: String) {
        val message = messages.value.firstOrNull { it.id == messageId && it.status == MessageStatus.Failed } ?: return
        setStatus(messageId, MessageStatus.Sending)
        deliver(message)
    }

    override suspend fun startConversation(connectionId: String) {
        val characterId = connections.getConnection(connectionId)?.characterId ?: return
        if (messages.value.any { it.connectionId == connectionId }) return
        addCharacterMessage(connectionId, MockReplies.openerFor(characterId))
    }

    override suspend fun markRead(connectionId: String) {
        messages.update { list ->
            list.map { if (it.connectionId == connectionId && !it.read) it.copy(read = true) else it }
        }
    }

    override suspend fun deleteAll() {
        network.call()
        messages.value = emptyList()
    }

    private suspend fun deliver(message: Message) {
        network.call()
        if (sendAttempts.incrementAndGet() % FAIL_EVERY_NTH_SEND == 0) {
            setStatus(message.id, MessageStatus.Failed)
            return
        }
        setStatus(message.id, MessageStatus.Sent)
        replyTo(message.connectionId)
    }

    private suspend fun replyTo(connectionId: String) {
        val characterId = connections.getConnection(connectionId)?.characterId ?: return
        typing.update { it + connectionId }
        try {
            delay(TYPING_MILLIS)
            val turn = messages.value.count { it.connectionId == connectionId && it.author == MessageAuthor.Character }
            addCharacterMessage(connectionId, MockReplies.replyFor(characterId, turn))
        } finally {
            typing.update { it - connectionId }
        }
    }

    private fun addCharacterMessage(connectionId: String, text: String) {
        val reply = Message(
            id = UUID.randomUUID().toString(),
            connectionId = connectionId,
            author = MessageAuthor.Character,
            text = text,
            sentAt = Instant.now(clock),
            read = false,
        )
        messages.update { it + reply }
    }

    private fun setStatus(messageId: String, status: MessageStatus) {
        messages.update { list -> list.map { if (it.id == messageId) it.copy(status = status) else it } }
    }

    companion object {
        const val FAIL_EVERY_NTH_SEND = 5
        const val TYPING_MILLIS = 1_200L
    }
}
