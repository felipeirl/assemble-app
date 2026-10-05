package dev.assemble.app.core.domain

import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.Team

/** Conquistas do app. Recompensam hábitos de uso, nunca volume de mensagens a um personagem só. */
enum class Achievement(val target: Int) {
    FirstConnection(1),
    TeamUp(5),
    IceBreaker(1),
    Storyteller(50),
    Explorer(30),
    Crossover(3),
}

/** Números de uso que alimentam as conquistas. [distinctTeams] é null quando o catálogo não pôde ser lido. */
data class AchievementStats(
    val connections: Int,
    val messagesSent: Int,
    val charactersSeen: Int,
    val distinctTeams: Int?,
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

    private fun metricFor(achievement: Achievement, stats: AchievementStats): Int? = when (achievement) {
        Achievement.FirstConnection, Achievement.TeamUp -> stats.connections
        Achievement.IceBreaker, Achievement.Storyteller -> stats.messagesSent
        Achievement.Explorer -> stats.charactersSeen
        Achievement.Crossover -> stats.distinctTeams
    }
}
