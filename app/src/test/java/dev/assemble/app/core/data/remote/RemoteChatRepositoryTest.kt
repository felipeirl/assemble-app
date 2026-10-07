package dev.assemble.app.core.data.remote

import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import dev.assemble.app.core.model.MessageStatus
import dev.assemble.app.core.network.ApiAcceptedMessage
import dev.assemble.app.core.network.ApiMessage
import dev.assemble.app.core.network.ApiRegenerationAccepted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RemoteChatRepositoryTest {
    private val store = InMemoryUserDataStore()
    private val api = FakeAssembleApi()
    private val uid = MutableStateFlow<String?>("uid-1")
    private var keyCounter = 0

    private fun repository(scope: CoroutineScope, echoTimeoutMillis: Long = 1_000): RemoteChatRepository {
        val connections = RemoteConnectionRepository(uid, store, api, scope)
        return RemoteChatRepository(
            uid = uid,
            store = store,
            api = api,
            connections = connections,
            scope = scope,
            clock = Clock.fixed(TestNow, ZoneOffset.UTC),
            newKey = { "key-${++keyCounter}" },
            regenerateEchoTimeoutMillis = echoTimeoutMillis,
        )
    }

    private fun accepted(connectionId: String, text: String) = ApiAcceptedMessage(
        ApiMessage("m_user", connectionId, "USER", text, "2026-10-04T12:00:00Z", status = "pending"),
    )

    private fun userDoc(id: String, key: String, status: String, text: String = "", at: Instant = TestNow) =
        Document(id, mapOf("createdAt" to at, "author" to "USER", "text" to text, "idempotencyKey" to key, "status" to status))

    private fun characterDoc(id: String, text: String, at: Instant = TestNow.plusSeconds(5), extra: Map<String, Any?> = emptyMap()) =
        Document(id, mapOf("createdAt" to at, "author" to "CHARACTER", "text" to text, "status" to "sent") + extra)

    private fun serverThread(vararg documents: Document) {
        store.messages.value = mapOf("thor" to documents.toList())
    }

    @Test
    fun send_accepted_showsTheLocalCopyAndTypingUntilTheReplyArrives() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        api.onSend = { connectionId, text, _ -> accepted(connectionId, text) }
        val chat = repository(scope)

        chat.send("thor", "Hi")

        val messages = chat.observeMessages("thor").first()
        assertEquals(listOf("Hi"), messages.map { it.text })
        assertEquals(listOf(MessageStatus.Sent), messages.map { it.status })
        assertEquals(setOf("thor"), chat.typingConnectionIds.value)
    }

    @Test
    fun send_pendingOnTheServer_keepsTheLocalTextInsteadOfTheEmptyDocument() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        api.onSend = { connectionId, text, key ->
            serverThread(userDoc("m_user", key, "pending"))
            accepted(connectionId, text)
        }
        val chat = repository(scope)

        chat.send("thor", "Hi")

        val message = chat.observeMessages("thor").first().single()
        assertEquals("Hi", message.text)
        assertEquals(MessageStatus.Sent, message.status)
        assertEquals(setOf("thor"), chat.typingConnectionIds.value)
    }

    @Test
    fun send_answeredOnTheServer_showsTheServerMessagesAndStopsTyping() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        api.onSend = { connectionId, text, _ -> accepted(connectionId, text) }
        val chat = repository(scope)
        chat.send("thor", "Hi")

        serverThread(userDoc("m_user", "key-1", "sent", text = "Hi"), characterDoc("m_reply", "Hello, mortal."))

        assertEquals(listOf("m_user", "m_reply"), chat.observeMessages("thor").first().map { it.id })
        assertEquals(emptySet<String>(), chat.typingConnectionIds.value)
    }

    @Test
    fun send_blockedOnTheServer_showsTheBlockedMessageWithoutText() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        api.onSend = { connectionId, text, _ -> accepted(connectionId, text) }
        val chat = repository(scope)
        chat.send("thor", "bad words")

        serverThread(userDoc("m_user", "key-1", "blocked"))

        val message = chat.observeMessages("thor").first().single()
        assertEquals(MessageStatus.Blocked, message.status)
        assertEquals("", message.text)
    }

    @Test
    fun send_failedOnTheServer_offersRetryWithTheSameKey() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        api.onSend = { connectionId, text, _ -> accepted(connectionId, text) }
        val chat = repository(scope)
        chat.send("thor", "Hi")
        serverThread(userDoc("m_user", "key-1", "failed"))

        val failed = chat.observeMessages("thor").first().single()
        assertEquals(MessageStatus.Failed, failed.status)
        assertEquals("Hi", failed.text)

        api.onSend = { connectionId, text, key ->
            serverThread(userDoc("m_user", key, "pending"))
            accepted(connectionId, text)
        }
        chat.retry(failed.id)

        assertEquals(listOf("key-1", "key-1"), api.sentKeys)
        assertEquals(listOf(MessageStatus.Sent), chat.observeMessages("thor").first().map { it.status })
    }

    @Test
    fun send_networkFailure_marksFailedAndRetryReusesTheKey() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        api.onSend = { _, _, _ -> throw IOException("offline") }
        val chat = repository(scope)

        chat.send("thor", "Hi")
        val failed = chat.observeMessages("thor").first().single()
        assertEquals(MessageStatus.Failed, failed.status)
        assertEquals(emptySet<String>(), chat.typingConnectionIds.value)

        api.onSend = { connectionId, text, _ -> accepted(connectionId, text) }
        chat.retry(failed.id)

        assertEquals(listOf("key-1", "key-1"), api.sentKeys)
        assertEquals(setOf("thor"), chat.typingConnectionIds.value)
    }

    @Test
    fun send_thatTimedOutButWasProcessed_isNotShownTwice() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        api.onSend = { _, _, _ -> throw IOException("tempo esgotado") }
        val chat = repository(scope)

        chat.send("thor", "Oi")
        assertEquals(listOf(MessageStatus.Failed), chat.observeMessages("thor").first().map { it.status })

        // O servidor terminou o pedido depois do tempo esgotado: o Firestore traz a mensagem com a mesma chave.
        serverThread(userDoc("m_1", "key-1", "sent", text = "Oi"), characterDoc("m_2", "Olá"))

        assertEquals(listOf("m_1", "m_2"), chat.observeMessages("thor").first().map { it.id })
    }

    @Test
    fun pendingOnTheServerAfterTheAppReopens_showsTypingWithoutAnEmptyBubble() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        serverThread(characterDoc("c0", "Olá", at = TestNow.minusSeconds(60)), userDoc("m_user", "old-key", "pending"))
        val chat = repository(scope)

        assertEquals(listOf("c0"), chat.observeMessages("thor").first().map { it.id })
        assertEquals(setOf("thor"), chat.typingConnectionIds.value)
    }

    @Test
    fun mergeThread_aPendingReplyLostInARestartBecomesARetry() {
        val local = listOf(
            PendingMessage(
                key = "k1",
                message = message("pending-k1", MessageAuthor.User, seconds = 0, status = MessageStatus.Sent),
                acceptedAt = TestNow,
            ),
        )
        val documents = listOf(userDoc("m_user", "k1", "pending"))
        val later = TestNow.plus(PENDING_STALE_AFTER).plusSeconds(1)

        val merged = mergeThread(documents, "thor", readAt = null, local = local, now = later)

        assertEquals(listOf(MessageStatus.Failed), merged.map { it.status })
        assertEquals(emptySet<String>(), awaitingReply(mapOf("thor" to documents), local, later))
    }

    @Test
    fun aSendThatTimedOutButEnteredTheQueue_showsTypingAndNoRetry() {
        val local = listOf(
            PendingMessage("k1", message("pending-k1", MessageAuthor.User, seconds = 0, status = MessageStatus.Failed)),
        )
        val documents = listOf(userDoc("m_user", "k1", "pending"))

        val merged = mergeThread(documents, "thor", readAt = null, local = local, now = TestNow)

        assertEquals(listOf(MessageStatus.Sent), merged.map { it.status })
        assertEquals(setOf("thor"), awaitingReply(mapOf("thor" to documents), local, TestNow))
    }

    @Test
    fun regenerateLast_marksTheConversationUntilTheServerShowsTheNewText() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        serverThread(characterDoc("c1", "Resposta antiga"))
        lateinit var chat: RemoteChatRepository
        var markedWhileWaiting = false
        api.onRegenerate = {
            markedWhileWaiting = "thor" in chat.regeneratingConnectionIds.value
            serverThread(characterDoc("c1", "Resposta antiga", extra = mapOf("status" to "pending", "regenerateRequestedAt" to TestNow)))
            ApiRegenerationAccepted(ApiMessage("c1", "thor", "CHARACTER", "Resposta antiga", "2026-10-04T12:00:05Z", status = "pending"))
        }
        chat = repository(scope)

        chat.regenerateLast("thor")

        assertTrue(markedWhileWaiting)
        assertEquals(setOf("thor"), chat.regeneratingConnectionIds.value)

        serverThread(characterDoc("c1", "Resposta nova", extra = mapOf("regeneratedAt" to TestNow)))

        assertEquals(emptySet<String>(), chat.regeneratingConnectionIds.value)
        assertEquals(listOf("Resposta nova"), chat.observeMessages("thor").first().map { it.text })
    }

    @Test
    fun regenerateLast_givesUpWaitingWhenTheServerNeverShowsIt() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        serverThread(characterDoc("c1", "Resposta antiga"))
        api.onRegenerate = {
            ApiRegenerationAccepted(ApiMessage("c1", "thor", "CHARACTER", "Resposta antiga", "2026-10-04T12:00:05Z", status = "pending"))
        }
        val chat = repository(scope, echoTimeoutMillis = 20)

        chat.regenerateLast("thor")

        assertEquals(emptySet<String>(), chat.regeneratingConnectionIds.value)
    }

    @Test
    fun regenerateLast_failureClearsTheMarkAndPropagates() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        val chat = repository(scope)

        try {
            chat.regenerateLast("thor")
            fail("esperava IOException")
        } catch (_: IOException) {
            assertEquals(emptySet<String>(), chat.regeneratingConnectionIds.value)
        }
    }

    @Test
    fun regeneratingNow_ignoresARequestLostInARestart() {
        val stale = characterDoc(
            "c1",
            "Resposta antiga",
            extra = mapOf("status" to "pending", "regenerateRequestedAt" to TestNow.minus(PENDING_STALE_AFTER).minusSeconds(1)),
        )

        assertFalse(regeneratingNow(listOf(stale), TestNow))
    }

    @Test
    fun rewindTo_dropsLocalCopiesOfTheDeletedMessages() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        serverThread(
            userDoc("u1", "old", "sent", text = "Primeira", at = TestNow.minusSeconds(60)),
            characterDoc("c1", "Resposta 1", at = TestNow.minusSeconds(55)),
        )
        api.onSend = { _, _, _ -> throw IOException("sem rede") }
        val chat = repository(scope)
        chat.send("thor", "Segunda")
        assertEquals(3, chat.observeMessages("thor").first().size)

        chat.rewindTo("thor", "c1")

        assertEquals(listOf("thor" to "c1"), api.rewinds)
        assertEquals(listOf("u1", "c1"), chat.observeMessages("thor").first().map { it.id })
    }

    @Test
    fun rewindTo_failureKeepsTheLocalMessages() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        serverThread(characterDoc("c1", "Resposta 1", at = TestNow.minusSeconds(55)))
        api.onSend = { _, _, _ -> throw IOException("sem rede") }
        api.onRewind = { _, _ -> throw IOException("sem rede") }
        val chat = repository(scope)
        chat.send("thor", "Oi")

        try {
            chat.rewindTo("thor", "c1")
            fail("esperava IOException")
        } catch (_: IOException) {
            assertEquals(2, chat.observeMessages("thor").first().size)
        }
    }

    @Test
    fun observeSuggestions_readsMatchDocument() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor", extra = mapOf("suggestions" to listOf("Tell me about Asgard"))))
        val chat = repository(scope)
        assertEquals(listOf("Tell me about Asgard"), chat.observeSuggestions("thor").first())
    }

    @Test
    fun markRead_writesLastReadAtOnlyWhenUnread() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        val chat = repository(scope)
        chat.markRead("thor")
        assertTrue(store.matchUpdates.isEmpty())

        serverThread(Document("m1", mapOf("createdAt" to TestNow, "author" to "CHARACTER", "text" to "Greetings")))
        chat.markRead("thor")
        assertEquals(listOf("thor" to mapOf<String, Any?>("lastReadAt" to ServerTime)), store.matchUpdates)
    }

    @Test
    fun deleteAll_hidesChatsOnServer() = runRemoteTest { scope ->
        repository(scope).deleteAll()
        assertEquals(1, api.hideChatsCalls)
    }

    private fun message(
        id: String,
        author: MessageAuthor,
        seconds: Long,
        text: String = "text",
        status: MessageStatus = MessageStatus.Sent,
    ) = Message(id, "thor", author, text, TestNow.plusSeconds(seconds), status)
}
