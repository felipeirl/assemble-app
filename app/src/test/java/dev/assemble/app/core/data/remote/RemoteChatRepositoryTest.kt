package dev.assemble.app.core.data.remote

import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import dev.assemble.app.core.model.MessageStatus
import dev.assemble.app.core.network.ApiCharacterReply
import dev.assemble.app.core.network.ApiErrorCode
import dev.assemble.app.core.network.ApiException
import dev.assemble.app.core.network.ApiMessage
import dev.assemble.app.core.network.ApiRegenerated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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

    private fun apiMessage(id: String, author: String, text: String, at: String) =
        ApiMessage(id, "thor", author, text, at, fictional = author == "CHARACTER")

    @Test
    fun regenerateLast_callsTheApiAndClearsTyping() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        api.onRegenerate = { ApiRegenerated(apiMessage("c1", "CHARACTER", "Outra resposta", "2026-10-04T12:00:05Z")) }
        val chat = repository(scope)

        chat.regenerateLast("thor")

        assertEquals(listOf("thor"), api.regenerateCalls)
        assertEquals(emptySet<String>(), chat.typingConnectionIds.value)
        assertEquals(listOf("Outra resposta"), chat.observeMessages("thor").first().map { it.text })
    }

    @Test
    fun regenerateLast_failureClearsTypingAndPropagates() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        val chat = repository(scope)

        try {
            chat.regenerateLast("thor")
            fail("esperava IOException")
        } catch (_: IOException) {
            assertEquals(emptySet<String>(), chat.typingConnectionIds.value)
        }
    }

    @Test
    fun rewindTo_dropsLocalCopiesOfTheDeletedMessages() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        var turn = 0
        api.onSend = { _, text, _ ->
            turn++
            val minute = "2026-10-04T12:0$turn"
            ApiCharacterReply(
                userMessage = apiMessage("u$turn", "USER", text, "$minute:00Z"),
                reply = apiMessage("c$turn", "CHARACTER", "Resposta $turn", "$minute:05Z"),
            )
        }
        val chat = repository(scope)
        chat.send("thor", "Primeira")
        chat.send("thor", "Segunda")
        assertEquals(listOf("u1", "c1", "u2", "c2"), chat.observeMessages("thor").first().map { it.id })

        chat.rewindTo("thor", "c1")

        assertEquals(listOf("thor" to "c1"), api.rewinds)
        assertEquals(listOf("u1", "c1"), chat.observeMessages("thor").first().map { it.id })
    }

    @Test
    fun rewindTo_failureKeepsTheLocalMessages() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        api.onSend = { _, text, _ -> reply("thor", text) }
        api.onRewind = { _, _ -> throw IOException("sem rede") }
        val chat = repository(scope)
        chat.send("thor", "Oi")

        try {
            chat.rewindTo("thor", "c1")
            fail("esperava IOException")
        } catch (_: IOException) {
            assertEquals(listOf("u1", "c1"), chat.observeMessages("thor").first().map { it.id })
        }
    }

    @Test
    fun send_thatTimedOutButWasProcessed_isNotShownTwice() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        api.onSend = { _, _, _ -> throw IOException("tempo esgotado") }
        val chat = repository(scope)

        chat.send("thor", "Oi")
        assertEquals(listOf(MessageStatus.Failed), chat.observeMessages("thor").first().map { it.status })

        // O servidor terminou o pedido depois do tempo esgotado: o Firestore traz a mensagem com a mesma chave.
        store.messages.value = mapOf(
            "thor" to listOf(
                Document("m_1", mapOf("createdAt" to TestNow, "author" to "USER", "text" to "Oi", "idempotencyKey" to "key-1")),
                Document("m_2", mapOf("createdAt" to TestNow.plusSeconds(1), "author" to "CHARACTER", "text" to "Olá")),
            ),
        )

        assertEquals(listOf("m_1", "m_2"), chat.observeMessages("thor").first().map { it.id })
    }

    @Test
    fun regenerateLast_marksTheConversationWhileWaitingAndShowsTheNewTextAtOnce() = runRemoteTest { scope ->
        store.matches.value = listOf(matchDoc("thor"))
        store.messages.value = mapOf(
            "thor" to listOf(Document("c1", mapOf("createdAt" to TestNow, "author" to "CHARACTER", "text" to "Resposta antiga"))),
        )
        lateinit var chat: RemoteChatRepository
        var markedWhileWaiting = false
        api.onRegenerate = {
            markedWhileWaiting = "thor" in chat.regeneratingConnectionIds.value
            ApiRegenerated(apiMessage("c1", "CHARACTER", "Resposta nova", "2026-10-04T12:00:00Z"))
        }
        chat = repository(scope)

        chat.regenerateLast("thor")

        assertTrue(markedWhileWaiting)
        assertEquals(emptySet<String>(), chat.regeneratingConnectionIds.value)
        // O Firestore ainda tem o texto antigo; o texto novo vale já.
        assertEquals(listOf("Resposta nova"), chat.observeMessages("thor").first().map { it.text })
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
