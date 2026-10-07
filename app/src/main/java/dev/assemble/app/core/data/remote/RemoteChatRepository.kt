package dev.assemble.app.core.data.remote

import dev.assemble.app.core.data.ChatRepository
import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import dev.assemble.app.core.model.MessageStatus
import dev.assemble.app.core.network.ApiStatus
import dev.assemble.app.core.network.AssembleApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Pendente no servidor por mais que isto: o servidor reiniciou e a fila em memória se perdeu.
 * A mensagem vira falha e o "tentar de novo" (mesma chave) a põe de volta na fila.
 */
internal val PENDING_STALE_AFTER: Duration = Duration.ofMinutes(3)

/** Quanto esperar o Firestore mostrar "gerar outra resposta" antes de soltar o aviso local. */
private const val REGENERATE_ECHO_TIMEOUT_MILLIS = 10_000L

/** O listener de uma conversa fica ligado um pouco depois da última tela, para trocar de aba sem religar. */
private const val THREAD_LISTENER_KEEP_MILLIS = 30_000L

private const val FIELD_AUTHOR = "author"
private const val FIELD_STATUS = "status"
private const val FIELD_KEY = "idempotencyKey"
private const val FIELD_CREATED_AT = "createdAt"
private const val FIELD_HIDDEN = "hidden"
private const val FIELD_REGENERATED_AT = "regeneratedAt"
private const val FIELD_REGENERATE_REQUESTED_AT = "regenerateRequestedAt"
private const val AUTHOR_USER_VALUE = "USER"
private const val AUTHOR_CHARACTER_VALUE = "CHARACTER"

/** Mensagem do usuário que o app acompanha até o servidor responder; [key] é a Idempotency-Key. */
internal data class PendingMessage(
    val key: String,
    val message: Message,
    /** Quando o backend aceitou (202); null enquanto o envio não foi aceito. */
    val acceptedAt: Instant? = null,
)

/**
 * Conversas: histórico em `users/{uid}/matches/{id}/messages` (Firestore, tempo real) e envio
 * pelo backend, que aceita a mensagem na hora (202) e gera a resposta numa fila. O documento da
 * mensagem do usuário diz o estado (`pending`, `sent`, `blocked`, `failed`); enquanto ele está
 * pendente, o app mostra a cópia local (o servidor só grava o texto depois do filtro) e o
 * "digitando". Em falha, "tentar de novo" repete com a mesma Idempotency-Key.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteChatRepository(
    private val uid: StateFlow<String?>,
    private val store: UserDataStore,
    private val api: AssembleApi,
    private val connections: RemoteConnectionRepository,
    private val scope: CoroutineScope,
    private val clock: Clock,
    private val newKey: () -> String = { UUID.randomUUID().toString() },
    private val regenerateEchoTimeoutMillis: Long = REGENERATE_ECHO_TIMEOUT_MILLIS,
) : ChatRepository {

    private val pending = MutableStateFlow<List<PendingMessage>>(emptyList())

    /** Um listener do Firestore por conversa, dividido entre a conversa, a lista, o badge e o "digitando". */
    private val sharedThreads = ConcurrentHashMap<String, Flow<Result<List<Document>>>>()

    /** "Gerar outra resposta" pedido, até o Firestore mostrar a mensagem sendo gerada. */
    private val regenerating = MutableStateFlow<Set<String>>(emptySet())

    /** Documentos de todas as conversas, por conexão: base do "digitando" e do "gerando outra". */
    private val threads: StateFlow<Map<String, List<Document>>> = connections.matchDocuments
        .map { list -> list.map { it.connection.id } }
        .distinctUntilChanged()
        .flatMapLatest { ids ->
            if (ids.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(ids.map { id -> serverDocuments(id).map { id to it } }) { pairs -> pairs.toMap() }
            }
        }
        .catch { emit(emptyMap()) }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    override val typingConnectionIds: StateFlow<Set<String>> = combine(threads, pending) { docs, local ->
        awaitingReply(docs, local, Instant.now(clock))
    }.stateIn(scope, SharingStarted.Eagerly, emptySet())

    override val regeneratingConnectionIds: StateFlow<Set<String>> = combine(threads, regenerating) { docs, local ->
        val now = Instant.now(clock)
        local + docs.filterValues { regeneratingNow(it, now) }.keys
    }.stateIn(scope, SharingStarted.Eagerly, emptySet())

    override fun observeMessages(connectionId: String): Flow<List<Message>> {
        val lastReadAt = connections.matchDocuments
            .map { list -> list.firstOrNull { it.connection.id == connectionId }?.lastReadAt }
            .distinctUntilChanged()
        return combine(serverDocuments(connectionId), lastReadAt, pending) { documents, readAt, local ->
            mergeThread(
                documents = documents,
                connectionId = connectionId,
                readAt = readAt,
                local = local.filter { it.message.connectionId == connectionId },
                now = Instant.now(clock),
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
        pending.update { list -> list.filterNot { settled(it) } + PendingMessage(key, message) }
        deliver(key)
    }

    /** Repete o envio com a mesma chave: o backend devolve o estado gravado ou retoma a fila. */
    override suspend fun retry(messageId: String) {
        val item = pending.value.firstOrNull { it.message.id == messageId } ?: return
        if (item.message.status == MessageStatus.Sending) return
        setStatus(item.key, MessageStatus.Sending)
        deliver(item.key)
    }

    /** O backend grava a fala de abertura junto com o match; aqui não há nada a fazer. */
    override suspend fun startConversation(connectionId: String) = Unit

    /**
     * Pede outra resposta. O texto novo chega pelo Firestore, na mesma mensagem; enquanto isso, a
     * conversa fica marcada como "gerando outra" (primeiro por aqui, depois pelo documento).
     */
    override suspend fun regenerateLast(connectionId: String) {
        val before = threads.value[connectionId].orEmpty().associate { it.id to it.data[FIELD_REGENERATED_AT] }
        regenerating.update { it + connectionId }
        try {
            val accepted = api.regenerate(connectionId)
            withTimeoutOrNull(regenerateEchoTimeoutMillis) {
                threads.first { docs ->
                    val doc = docs[connectionId]?.firstOrNull { it.id == accepted.reply.id }
                    doc != null && (doc.data[FIELD_STATUS] != ApiStatus.SENT || doc.data[FIELD_REGENERATED_AT] != before[doc.id])
                }
            }
        } finally {
            regenerating.update { it - connectionId }
        }
    }

    override suspend fun rewindTo(connectionId: String, messageId: String) {
        val cutoff = allMessages.value.firstOrNull { it.id == messageId }?.sentAt
        api.rewind(connectionId, messageId)
        // As cópias locais do que foi apagado não podem ressuscitar a mensagem na lista.
        if (cutoff != null) {
            pending.update { list -> list.filterNot { it.message.connectionId == connectionId && it.message.sentAt.isAfter(cutoff) } }
        }
    }

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
    }

    private fun serverDocuments(connectionId: String): Flow<List<Document>> = sharedThreads
        .getOrPut(connectionId) {
            uid.flatMapLatest { current ->
                if (current == null) flowOf(emptyList()) else store.observeMessages(current, connectionId)
            }
                .map { Result.success(it) }
                // A falha vira valor para quem escuta (a conversa mostra o erro) sem derrubar o escopo.
                .catch { emit(Result.failure(it)) }
                .shareIn(scope, SharingStarted.WhileSubscribed(THREAD_LISTENER_KEEP_MILLIS, replayExpirationMillis = 0), replay = 1)
        }
        .map { it.getOrThrow() }

    private suspend fun deliver(key: String) {
        val item = pending.value.firstOrNull { it.key == key } ?: return
        try {
            api.sendMessage(item.message.connectionId, item.message.text, key)
            val acceptedAt = Instant.now(clock)
            pending.update { list ->
                list.map { if (it.key == key) it.copy(message = it.message.copy(status = MessageStatus.Sent), acceptedAt = acceptedAt) else it }
            }
        } catch (_: IOException) {
            setStatus(key, MessageStatus.Failed)
        }
    }

    private fun setStatus(key: String, status: MessageStatus) {
        pending.update { list ->
            list.map { if (it.key == key) it.copy(message = it.message.copy(status = status)) else it }
        }
    }

    /** O servidor já resolveu a mensagem (respondida ou recusada): a cópia local não serve mais. */
    private fun settled(item: PendingMessage): Boolean {
        val doc = threads.value[item.message.connectionId]?.firstOrNull { it.data[FIELD_KEY] == item.key } ?: return false
        return doc.data[FIELD_STATUS] != ApiStatus.PENDING && doc.data[FIELD_STATUS] != ApiStatus.FAILED
    }
}

/**
 * Junta o histórico do servidor com as cópias locais. Mensagem do usuário pendente ou com falha no
 * servidor não tem texto (ele só é gravado depois do filtro de entrada): quem aparece é a cópia
 * local, com o estado do servidor; sem a cópia (o app foi reaberto), ela não aparece. Respondida ou
 * recusada, vale a do servidor.
 */
internal fun mergeThread(
    documents: List<Document>,
    connectionId: String,
    readAt: Instant?,
    local: List<PendingMessage>,
    now: Instant,
): List<Message> {
    val byKey = documents.filter { it.data[FIELD_KEY] is String }.associateBy { it.data[FIELD_KEY] as String }
    val fromServer = documents.mapNotNull { doc ->
        if (doc.isUnresolvedUserMessage()) null else messageFrom(doc, connectionId, readAt)
    }
    val fromLocal = local.mapNotNull { item ->
        val doc = byKey[item.key]
        when {
            item.message.status == MessageStatus.Sending -> item.message
            doc == null -> if (isStale(item.acceptedAt, now)) item.message.copy(status = MessageStatus.Failed) else item.message
            doc.data[FIELD_STATUS] == ApiStatus.PENDING -> {
                val stale = isStale(maxOf(doc.createdAt(), item.acceptedAt ?: Instant.MIN), now)
                item.message.copy(status = if (stale) MessageStatus.Failed else MessageStatus.Sent)
            }
            doc.data[FIELD_STATUS] == ApiStatus.FAILED -> item.message.copy(status = MessageStatus.Failed)
            else -> null
        }
    }
    return (fromServer + fromLocal).sortedBy { it.sentAt }
}

/** Conversas esperando a resposta do personagem: envio em andamento ou pendente no servidor. */
internal fun awaitingReply(threads: Map<String, List<Document>>, local: List<PendingMessage>, now: Instant): Set<String> {
    val localKeys = local.map { it.key }.toSet()
    val fromLocal = local.filter { item ->
        val doc = threads[item.message.connectionId]?.firstOrNull { it.data[FIELD_KEY] == item.key }
        when {
            item.message.status == MessageStatus.Sending -> true
            // O servidor manda: o envio pode ter estourado o tempo no app e mesmo assim ter entrado na fila.
            doc != null -> doc.data[FIELD_STATUS] == ApiStatus.PENDING &&
                !isStale(maxOf(doc.createdAt(), item.acceptedAt ?: Instant.MIN), now)
            else -> item.message.status == MessageStatus.Sent && !isStale(item.acceptedAt, now)
        }
    }.map { it.message.connectionId }
    // App reaberto com a resposta ainda na fila: o servidor diz que está pendente.
    val fromServer = threads.filterValues { docs ->
        docs.any { doc ->
            doc.data[FIELD_AUTHOR] == AUTHOR_USER_VALUE && doc.data[FIELD_STATUS] == ApiStatus.PENDING &&
                doc.data[FIELD_KEY] !in localKeys && !isStale(doc.createdAt(), now)
        }
    }.keys
    return fromLocal.toSet() + fromServer
}

/** A última resposta do personagem está sendo gerada de novo no servidor. */
internal fun regeneratingNow(documents: List<Document>, now: Instant): Boolean {
    val last = documents.lastOrNull { it.data[FIELD_HIDDEN] != true } ?: return false
    if (last.data[FIELD_AUTHOR] != AUTHOR_CHARACTER_VALUE || last.data[FIELD_STATUS] != ApiStatus.PENDING) return false
    val requestedAt = last.data[FIELD_REGENERATE_REQUESTED_AT] as? Instant ?: return false
    return !isStale(requestedAt, now)
}

private fun Document.isUnresolvedUserMessage(): Boolean =
    data[FIELD_AUTHOR] == AUTHOR_USER_VALUE && (data[FIELD_STATUS] == ApiStatus.PENDING || data[FIELD_STATUS] == ApiStatus.FAILED)

private fun Document.createdAt(): Instant = data[FIELD_CREATED_AT] as? Instant ?: Instant.MIN

private fun isStale(since: Instant?, now: Instant): Boolean =
    since != null && since != Instant.MIN && Duration.between(since, now) > PENDING_STALE_AFTER
