package dev.assemble.app.core.data.remote

import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import dev.assemble.app.core.model.MessageStatus
import dev.assemble.app.core.network.ApiCharacterReply
import dev.assemble.app.core.network.ApiErrorCode
import dev.assemble.app.core.network.ApiException
import dev.assemble.app.core.network.ApiMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.ZoneOffset

class RemoteChatRepositoryTest {
    private val store = InMemoryUserDataStore()
    private val api = FakeAssembleApi()
    private val uid = MutableStateFlow<String?>("uid-1")
    private var keyCounter = 0

    private fun repository(scope: CoroutineScope): RemoteChatRepository {
        val connections = RemoteConnectionRepository(uid, store, api, scope)
        return RemoteChatRepository(
            uid = uid,
            store = store,
            api = api,
            connections = connections,
            scope = scope,
            clock = Clock.fixed(TestNow, ZoneOffset.UTC),
            newKey = { "key-${++keyCounter}" },
        )
    }

    private fun reply(connectionId: String, text: String, blocked: Boolean = false) = ApiCharacterReply(
        userMessage = ApiMessage("u1", connectionId, "USER", if (blocked) "" else text, "2026-10-04T12:00:00Z", blocked = blocked),
        reply = ApiMessage("c1", connectionId, "CHARACTER", "Hello, mortal.", "2026-10-04T12:00:05Z", fictional = true),
    )

    @Test
    fun send_success_showsConfirmedMessagesBeforeFirestore() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        api.onSend = { connectionId, text, _ -> reply(connectionId, text) }
        val chat = repository(scope)

        chat.send("thor", "Hi")

        val messages = chat.observeMessages("thor").first()
        assertEquals(listOf("u1", "c1"), messages.map { it.id })
        assertTrue(messages.all { it.status == MessageStatus.Sent })
        assertEquals(emptySet<String>(), chat.typingConnectionIds.value)
    }

    @Test
    fun send_blockedByGuardrail_marksBlocked() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        api.onSend = { _, _, _ -> throw ApiException(ApiErrorCode.BLOCKED_CONTENT, 422) }
        val chat = repository(scope)

        chat.send("thor", "bad words")

        val message = chat.observeMessages("thor").first().single()
        assertEquals(MessageStatus.Blocked, message.status)
        assertEquals("bad words", message.text)
    }

    @Test
    fun send_selfHarmReply_keepsUserMessageBlockedWithoutText() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        api.onSend = { connectionId, text, _ -> reply(connectionId, text, blocked = true) }
        val chat = repository(scope)

        chat.send("thor", "help")

        val (mine, theirs) = chat.observeMessages("thor").first()
        assertEquals(MessageStatus.Blocked, mine.status)
        assertEquals("", mine.text)
        assertEquals(MessageAuthor.Character, theirs.author)
    }

    @Test
    fun retry_afterFailure_reusesIdempotencyKey() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        api.onSend = { _, _, _ -> throw IOException("offline") }
        val chat = repository(scope)

        chat.send("thor", "Hi")
        val failed = chat.observeMessages("thor").first().single()
        assertEquals(MessageStatus.Failed, failed.status)

        api.onSend = { connectionId, text, _ -> reply(connectionId, text) }
        chat.retry(failed.id)

        assertEquals(listOf("key-1", "key-1"), api.sentKeys)
        assertEquals(listOf("u1", "c1"), chat.observeMessages("thor").first().map { it.id })
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

        store.messages.value = mapOf(
            "thor" to listOf(Document("m1", mapOf("createdAt" to TestNow, "author" to "CHARACTER", "text" to "Greetings"))),
        )
        chat.markRead("thor")
        assertEquals(listOf("thor" to mapOf<String, Any?>("lastReadAt" to ServerTime)), store.matchUpdates)
    }

    @Test
    fun deleteAll_hidesChatsOnServer() = runRemoteTest { scope ->
        repository(scope).deleteAll()
        assertEquals(1, api.hideChatsCalls)
    }

    @Test
    fun mergeMessages_serverWinsAndBlockedEchoReplacesLocalCopy() {
        val server = listOf(
            message("u1", MessageAuthor.User, seconds = 0),
            message("s-blocked", MessageAuthor.User, seconds = 30, text = "", status = MessageStatus.Blocked),
        )
        val confirmed = listOf(message("u1", MessageAuthor.User, seconds = 0), message("c1", MessageAuthor.Character, seconds = 5))
        val local = listOf(message("pending-x", MessageAuthor.User, seconds = 29, status = MessageStatus.Blocked))

        val merged = mergeMessages(server, confirmed, local)

        assertEquals(listOf("u1", "c1", "s-blocked"), merged.map { it.id })
    }

    private fun message(
        id: String,
        author: MessageAuthor,
        seconds: Long,
        text: String = "text",
        status: MessageStatus = MessageStatus.Sent,
    ) = Message(id, "thor", author, text, TestNow.plusSeconds(seconds), status)
}
