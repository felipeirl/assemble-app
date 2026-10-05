package dev.assemble.app.feature.chat

import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class RegeneratingVisibilityTest {
    private val now = Instant.parse("2026-10-04T12:00:00Z")
    private val user = Message("u1", "c", MessageAuthor.User, "Oi", now)
    private val reply = Message("c1", "c", MessageAuthor.Character, "Olá", now.plusSeconds(1))

    @Test
    fun theOldReplyDisappearsWhileANewOneIsGenerated() {
        assertEquals(listOf(user), withoutReplyBeingRegenerated(listOf(user, reply), regenerating = true))
    }

    @Test
    fun nothingChangesOtherwise() {
        assertEquals(listOf(user, reply), withoutReplyBeingRegenerated(listOf(user, reply), regenerating = false))
        assertEquals(listOf(user), withoutReplyBeingRegenerated(listOf(user), regenerating = true))
    }
}
