package dev.assemble.app.core.domain

import dev.assemble.app.core.model.AvatarFrame
import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import dev.assemble.app.core.model.ProfileAccent
import dev.assemble.app.core.model.ProfileCover
import dev.assemble.app.core.model.ProfileStyle
import dev.assemble.app.core.model.ProfileTitle
import dev.assemble.app.core.model.Team

/** Seção da tela de conquistas. */
enum class AchievementCategory { Connections, Conversations, Discovery, Profile }

/** Raridade: muda só a cor do anel da insígnia. */
enum class AchievementTier { Bronze, Silver, Gold }

/** Cosmético que uma conquista libera. Cada recompensa pertence a uma conquista só. */
sealed interface Reward {
    data class Frame(val frame: AvatarFrame) : Reward
    data class Cover(val cover: ProfileCover) : Reward
    data class Accent(val accent: ProfileAccent) : Reward
    data class Title(val title: ProfileTitle) : Reward
}

/** Equipes do catálogo sem contar Solo (Avengers, X-Men, Fantastic Four, Guardians, S.H.I.E.L.D., Defenders). */
private const val NON_SOLO_TEAMS = 6

/**
 * Conquistas do app, na ordem de exibição dentro de cada categoria. Recompensam hábitos de uso,
 * nunca volume de mensagens a um personagem só. [reward] é a única tabela de recompensas do app.
 */
enum class Achievement(val target: Int, val category: AchievementCategory, val tier: AchievementTier, val reward: Reward) {
    FirstConnection(1, AchievementCategory.Connections, AchievementTier.Bronze, Reward.Title(ProfileTitle.Recruit)),
    TeamUp(5, AchievementCategory.Connections, AchievementTier.Silver, Reward.Frame(AvatarFrame.Hexagon)),
    FullRoster(15, AchievementCategory.Connections, AchievementTier.Silver, Reward.Accent(ProfileAccent.Emerald)),
    Legion(30, AchievementCategory.Connections, AchievementTier.Gold, Reward.Frame(AvatarFrame.Shield)),
    IceBreaker(1, AchievementCategory.Conversations, AchievementTier.Bronze, Reward.Title(ProfileTitle.IceBreaker)),
    RoundOfIntros(5, AchievementCategory.Conversations, AchievementTier.Silver, Reward.Accent(ProfileAccent.Silver)),
    Storyteller(50, AchievementCategory.Conversations, AchievementTier.Silver, Reward.Cover(ProfileCover.Headline)),
    Diplomat(15, AchievementCategory.Conversations, AchievementTier.Gold, Reward.Title(ProfileTitle.Diplomat)),
    Veteran(200, AchievementCategory.Conversations, AchievementTier.Gold, Reward.Title(ProfileTitle.LivingLegend)),
    Explorer(30, AchievementCategory.Discovery, AchievementTier.Bronze, Reward.Title(ProfileTitle.Explorer)),
    Scout(100, AchievementCategory.Discovery, AchievementTier.Silver, Reward.Cover(ProfileCover.Blueprint)),
    Cartographer(300, AchievementCategory.Discovery, AchievementTier.Gold, Reward.Cover(ProfileCover.Cosmos)),
    Crossover(3, AchievementCategory.Discovery, AchievementTier.Silver, Reward.Frame(AvatarFrame.Burst)),
    Multiverse(NON_SOLO_TEAMS, AchievementCategory.Discovery, AchievementTier.Gold, Reward.Frame(AvatarFrame.Cosmic)),
    SecretIdentity(1, AchievementCategory.Profile, AchievementTier.Bronze, Reward.Title(ProfileTitle.AlterEgo)),
    Sentinel(7, AchievementCategory.Profile, AchievementTier.Silver, Reward.Frame(AvatarFrame.Lightning)),
}

/**
 * Números de uso que alimentam as conquistas. [distinctTeams] é null quando o catálogo não pôde ser lido.
 * [charactersChatted]: conversas em que o usuário escreveu. [activeDays]: dias distintos com o app aberto.
 */
data class AchievementStats(
    val connections: Int,
    val messagesSent: Int,
    val charactersSeen: Int,
    val distinctTeams: Int?,
    val charactersChatted: Int = 0,
    val activeDays: Int = 0,
    val profileComplete: Boolean = false,
)

/** Progresso de uma conquista; [current] é null quando o dado não está disponível agora. */
data class AchievementProgress(val achievement: Achievement, val current: Int?) {
    val unlocked: Boolean get() = current != null && current >= achievement.target
}

object AchievementRules {

    fun evaluate(stats: AchievementStats): List<AchievementProgress> =
        Achievement.entries.map { achievement ->
            AchievementProgress(achievement, metricFor(achievement, stats)?.coerceAtMost(achievement.target))
        }

    /** Equipes diferentes entre os personagens conectados. "Solo" não é equipe. */
    fun distinctTeams(connectedCharacters: List<Character>): Int =
        connectedCharacters.flatMap { it.teams }.filter { it != Team.Solo }.toSet().size

    /** Conversas em que o usuário mandou ao menos uma mensagem. */
    fun charactersChatted(messages: List<Message>): Int =
        messages.filter { it.author == MessageAuthor.User }.mapTo(mutableSetOf()) { it.connectionId }.size

    /** "Identidade secreta": frase de apresentação respondida e ao menos uma conexão em destaque. */
    fun isProfileComplete(style: ProfileStyle): Boolean =
        style.promptAnswer.isNotBlank() && style.featuredConnections.isNotEmpty()

    private fun metricFor(achievement: Achievement, stats: AchievementStats): Int? = when (achievement) {
        Achievement.FirstConnection, Achievement.TeamUp, Achievement.FullRoster, Achievement.Legion -> stats.connections
        Achievement.IceBreaker, Achievement.Storyteller, Achievement.Veteran -> stats.messagesSent
        Achievement.RoundOfIntros, Achievement.Diplomat -> stats.charactersChatted
        Achievement.Explorer, Achievement.Scout, Achievement.Cartographer -> stats.charactersSeen
        Achievement.Crossover, Achievement.Multiverse -> stats.distinctTeams
        Achievement.SecretIdentity -> if (stats.profileComplete) 1 else 0
        Achievement.Sentinel -> stats.activeDays
    }
}
