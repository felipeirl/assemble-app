package dev.assemble.app.core.data.remote

import dev.assemble.app.core.data.ChatRepository
import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import dev.assemble.app.core.model.MessageStatus
import dev.assemble.app.core.network.ApiErrorCode
import dev.assemble.app.core.network.ApiException
import dev.assemble.app.core.network.ApiMessage
import dev.assemble.app.core.network.AssembleApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

// Uma mensagem bloqueada localmente some quando a cópia do servidor (sem texto) chega.
private val BLOCKED_ECHO_WINDOW: Duration = Duration.ofMinutes(2)

/**
 * Conversas: histórico em `users/{uid}/matches/{id}/messages` (Firestore, tempo real) e envio
 * pelo backend, que grava a mensagem do usuário e a resposta do personagem. Enquanto o envio
 * não volta, a mensagem aparece como "enviando"; em falha, fica para "tentar de novo" com a
 * mesma Idempotency-Key (repetir não duplica a mensagem nem a resposta).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteChatRepository(
    private val uid: StateFlow<String?>,
    private val store: UserDataStore,
    private val api: AssembleApi,
    private val connections: RemoteConnectionRepository,
    scope: CoroutineScope,
    private val clock: Clock,
    private val newKey: () -> String = { UUID.randomUUID().toString() },
) : ChatRepository {

    /** Mensagem do usuário ainda sem confirmação do servidor; [key] é a Idempotency-Key. */
    private data class Pending(val key: String, val message: Message)

    private val pending = MutableStateFlow<List<Pending>>(emptyList())

    /** Mensagens já confirmadas pelo backend, até o listener do Firestore trazê-las. */
    private val echoes = MutableStateFlow<List<Message>>(emptyList())

    private val typing = MutableStateFlow<Set<String>>(emptySet())
    override val typingConnectionIds: StateFlow<Set<String>> = typing.asStateFlow()

    override fun observeMessages(connectionId: String): Flow<List<Message>> {
        val lastReadAt = connections.matchDocuments
            .map { list -> list.firstOrNull { it.connection.id == connectionId }?.lastReadAt }
            .distinctUntilChanged()
        val server = uid.flatMapLatest { current ->
            if (current == null) flowOf(emptyList()) else store.observeMessages(current, connectionId)
        }
        return combine(server, lastReadAt, pending, echoes) { documents, readAt, local, confirmed ->
            val fromServer = documents.mapNotNull { messageFrom(it, connectionId, readAt) }
            mergeMessages(
                server = fromServer,
                confirmed = confirmed.filter { it.connectionId == connectionId },
                local = local.map { it.message }.filter { it.connectionId == connectionId },
            )
        }
    }

    override val allMessages: StateFlow<List<Message>> = connections.matchDocuments
        .map { list -> list.map { it.connection.id } }
        .distinctUntilChanged()
        .flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyList()) else combine(ids.map(::observeMessages)) { threads -> threads.flatMap { it } }
        }
        .catch { emit(emptyList()) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun observeSuggestions(connectionId: String): Flow<List<String>?> = connections.matchDocuments
        .map { list -> list.firstOrNull { it.connection.id == connectionId }?.suggestions?.takeIf { it.isNotEmpty() } }
        .distinctUntilChanged()

    override suspend fun send(connectionId: String, text: String) {
        val key = newKey()
        val message = Message(
            id = "pending-$key",
            connectionId = connectionId,
            author = MessageAuthor.User,
            text = text,
            sentAt = Instant.now(clock),
            status = MessageStatus.Sending,
        )
        pending.update { it + Pending(key, message) }
        deliver(key, message)
    }

    override suspend fun retry(messageId: String) {
        val item = pending.value.firstOrNull { it.message.id == messageId && it.message.status == MessageStatus.Failed } ?: return
        setPendingStatus(item.key, MessageStatus.Sending)
        deliver(item.key, item.message)
    }

    /** O backend grava a fala de abertura junto com o match; aqui não há nada a fazer. */
    override suspend fun startConversation(connectionId: String) = Unit

    override suspend fun markRead(connectionId: String) {
        val current = uid.value ?: return
        val hasUnread = allMessages.value.any { it.connectionId == connectionId && !it.read }
        if (!hasUnread) return
        try {
            store.updateMatch(current, connectionId, mapOf("lastReadAt" to ServerTime))
        } catch (_: IOException) {
            // Marcar como lida é melhor esforço: a próxima abertura da conversa tenta de novo.
        }
    }

    override suspend fun deleteAll() {
        api.hideChats()
        pending.value = emptyList()
        echoes.value = emptyList()
    }

    private suspend fun deliver(key: String, message: Message) {
        typing.update { it + message.connectionId }
        try {
            val reply = api.sendMessage(message.connectionId, message.text, key)
            echoes.update { it + reply.userMessage.toMessage(read = true) + reply.reply.toMessage(read = false) }
            pending.update { list -> list.filterNot { it.key == key } }
        } catch (error: ApiException) {
            val status = if (error.code == ApiErrorCode.BLOCKED_CONTENT) MessageStatus.Blocked else MessageStatus.Failed
            setPendingStatus(key, status)
        } catch (_: IOException) {
            setPendingStatus(key, MessageStatus.Failed)
        } finally {
            typing.update { it - message.connectionId }
        }
    }

    private fun setPendingStatus(key: String, status: MessageStatus) {
        pending.update { list ->
            list.map { if (it.key == key) it.copy(message = it.message.copy(status = status)) else it }
        }
    }
}

/**
 * Junta o histórico do servidor com o que ainda não chegou por ele. O servidor vence quando a
 * mesma mensagem aparece nos dois (mesmo id). Uma recusa local some quando a do servidor chega.
 */
internal fun mergeMessages(server: List<Message>, confirmed: List<Message>, local: List<Message>): List<Message> {
    val serverIds = server.mapTo(mutableSetOf()) { it.id }
    val serverBlocked = server.filter { it.status == MessageStatus.Blocked }
    val localVisible = local.filterNot { mine ->
        mine.status == MessageStatus.Blocked && serverBlocked.any {
            Duration.between(mine.sentAt, it.sentAt).abs() <= BLOCKED_ECHO_WINDOW
        }
    }
    return (server + confirmed.filter { it.id !in serverIds } + localVisible).sortedBy { it.sentAt }
}

internal fun ApiMessage.toMessage(read: Boolean): Message {
    val fromUser = author == "USER"
    return Message(
        id = id,
        connectionId = connectionId,
        author = if (fromUser) MessageAuthor.User else MessageAuthor.Character,
        text = text,
        sentAt = Instant.parse(createdAt),
        // Pedido de ajuda (autoagressão): a fala é respondida com apoio, mas o texto não é guardado.
        status = if (fromUser && blocked) MessageStatus.Blocked else MessageStatus.Sent,
        read = read,
    )
}
