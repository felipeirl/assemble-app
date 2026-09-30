package dev.assemble.app.core.data.mock

import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.Style
import dev.assemble.app.core.model.Team
import dev.assemble.app.core.model.UserProfile
import java.time.Duration
import java.time.Instant

/** Estado inicial do app mockado. */
object MockSeed {
    val initialPreferences = Preferences(
        origins = setOf(Origin.Mutant, Origin.Human),
        powers = setOf(PowerFamily.Mind, PowerFamily.TechGadgets),
        teams = setOf(Team.XMen),
        styles = setOf(Style.Leadership),
        fame = Preferences.FAME_DEFAULT,
    )

    val initialProfile = UserProfile(name = "Felipe", bio = "", avatarPreset = 0)

    const val SPIDER_MAN_CONNECTION_ID = "connection-spider-man"
    const val STORM_CONNECTION_ID = "connection-storm"

    /** Conexões iniciais: (id da conexão, id do personagem, há quanto tempo). */
    val initialConnections = listOf(
        SeedConnection(SPIDER_MAN_CONNECTION_ID, "spider-man", Duration.ofDays(1)),
        SeedConnection(STORM_CONNECTION_ID, "storm", Duration.ofHours(2)),
    )

    /** Spider-Man com 3 mensagens lidas; Storm com 1 mensagem não lida (badge = 1). */
    fun initialMessages(now: Instant): List<Message> = listOf(
        Message(
            id = "seed-1",
            connectionId = SPIDER_MAN_CONNECTION_ID,
            author = MessageAuthor.Character,
            text = MockReplies.openerFor("spider-man"),
            sentAt = now - Duration.ofDays(1),
        ),
        Message(
            id = "seed-2",
            connectionId = SPIDER_MAN_CONNECTION_ID,
            author = MessageAuthor.User,
            text = "What got you into science?",
            sentAt = now - Duration.ofHours(20),
        ),
        Message(
            id = "seed-3",
            connectionId = SPIDER_MAN_CONNECTION_ID,
            author = MessageAuthor.Character,
            text = "Honestly? Curiosity and a very radioactive field trip. After that, chemistry felt personal.",
            sentAt = now - Duration.ofHours(20) + Duration.ofMinutes(1),
        ),
        Message(
            id = "seed-4",
            connectionId = STORM_CONNECTION_ID,
            author = MessageAuthor.Character,
            text = MockReplies.openerFor("storm"),
            sentAt = now - Duration.ofHours(2),
            read = false,
        ),
    )
}

data class SeedConnection(val id: String, val characterId: String, val age: Duration)
