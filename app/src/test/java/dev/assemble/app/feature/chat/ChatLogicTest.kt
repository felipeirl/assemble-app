package dev.assemble.app.feature.chat

import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.Connection
import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

class ChatLogicTest {

    private val now = Instant.parse("2026-09-30T15:00:00Z")

    private fun character(id: String) = Character(
        id = id, name = id.uppercase(), realName = null, imageUrl = null, origin = null,
        powers = emptyList(), teams = emptyList(), styles = emptyList(),
        firstAppearance = null, issueAppearances = null, bio = null,
    )

    private fun connection(characterId: String, createdAt: Instant) =
        Connection("c-$characterId", characterId, score = 80, threshold = 70, createdAt = createdAt)

    private fun message(id: String, connectionId: String, author: MessageAuthor, at: Instant, read: Boolean = true) =
        Message(id, connectionId, author, "text $id", at, read = read)

    @Test
    fun summaries_sortByLastActivity_andFlagUnread() {
        val connections = listOf(
            connection("storm", now - Duration.ofDays(3)),
            connection("rocket", now - Duration.ofDays(1)),
        )
        val messages = listOf(
            message("1", "c-storm", MessageAuthor.Character, now - Duration.ofHours(1), read = false),
            message("2", "c-rocket", MessageAuthor.User, now - Duration.ofHours(5)),
        )
        val summaries = buildConversationSummaries(
            connections, messages, mapOf("storm" to character("storm"), "rocket" to character("rocket")),
        )
        assertEquals(listOf("c-storm", "c-rocket"), summaries.map { it.connectionId })
        assertTrue(summaries[0].unread)
        assertFalse(summaries[1].unread)
        assertEquals("text 1", summaries[0].lastMessage)
    }

    @Test
    fun summaries_withoutMessages_useConnectionTime_andSkipUnknownCharacters() {
        val created = now - Duration.ofMinutes(10)
        val summaries = buildConversationSummaries(
            listOf(connection("storm", created), connection("ghost", now)),
            emptyList(),
            mapOf("storm" to character("storm")),
        )
        assertEquals(1, summaries.size)
        assertNull(summaries.single().lastMessage)
        assertEquals(created, summaries.single().lastActivity)
    }

    @Test
    fun conversationTime_showsTimeToday_andDateBefore() {
        val today = formatConversationTime(now - Duration.ofHours(2), now, ZoneOffset.UTC, Locale.US)
        val yesterday = formatConversationTime(now - Duration.ofDays(1), now, ZoneOffset.UTC, Locale.US)
        assertTrue(today, today.contains(":"))
        assertFalse(yesterday, yesterday.contains(":"))
    }

    @Test
    fun incoming_ignoresOpenConversationOpenerUserAndKnownMessages() {
        val opener = message("o", "c-a", MessageAuthor.Character, now, read = false)
        assertNull("opener", newIncomingMessage(listOf(opener), emptySet(), openConnectionId = null))

        val thread = listOf(
            message("1", "c-a", MessageAuthor.Character, now - Duration.ofMinutes(5)),
            message("2", "c-a", MessageAuthor.User, now - Duration.ofMinutes(1)),
            message("3", "c-a", MessageAuthor.Character, now, read = false),
        )
        assertEquals("3", newIncomingMessage(thread, setOf("1", "2"), openConnectionId = null)?.id)
        assertNull("open conversation", newIncomingMessage(thread, setOf("1", "2"), openConnectionId = "c-a"))
        assertNull("already known", newIncomingMessage(thread, setOf("1", "2", "3"), openConnectionId = null))
    }
}
