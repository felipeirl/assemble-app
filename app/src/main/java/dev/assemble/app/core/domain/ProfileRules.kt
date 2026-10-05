package dev.assemble.app.core.domain

import dev.assemble.app.core.model.AvatarFrame
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.ProfileStyle
import dev.assemble.app.core.model.Style

/** Arquétipo exibido no perfil ("Estrategista · Mutante"). Uma das partes pode faltar, nunca as duas. */
data class Archetype(val style: Style?, val origin: Origin?)

/** Regras da personalização do perfil. Puras: valem igual no app e no backend. */
object ProfileRules {

    /** Conquista que libera a moldura; null = livre desde o início. */
    fun requiredAchievement(frame: AvatarFrame): Achievement? = when (frame) {
        AvatarFrame.Simple, AvatarFrame.Ring -> null
        AvatarFrame.Hexagon -> Achievement.TeamUp
        AvatarFrame.Burst -> Achievement.Crossover
    }

    fun isFrameUnlocked(frame: AvatarFrame, unlocked: Set<Achievement>): Boolean =
        requiredAchievement(frame)?.let { it in unlocked } ?: true

    /** Primeiro estilo e primeira origem das preferências (ordem do enum). "Any" nas duas = sem arquétipo. */
    fun archetype(preferences: Preferences): Archetype? {
        val style = preferences.styles.minByOrNull { it.ordinal }
        val origin = preferences.origins.minByOrNull { it.ordinal }
        return if (style == null && origin == null) null else Archetype(style, origin)
    }

    /**
     * O que pode ser mostrado agora: moldura bloqueada volta para o anel, destaques que deixaram de valer
     * (conexão desfeita, conquista zerada) saem, e a resposta respeita o limite.
     */
    fun sanitize(style: ProfileStyle, unlocked: Set<Achievement>, connectedIds: Set<String>): ProfileStyle = style.copy(
        frame = if (isFrameUnlocked(style.frame, unlocked)) style.frame else AvatarFrame.Ring,
        promptAnswer = style.promptAnswer.trim().take(ProfileStyle.PROMPT_ANSWER_MAX),
        featuredConnections = style.featuredConnections.filter { it in connectedIds }.distinct().take(ProfileStyle.FEATURED_MAX),
        featuredBadges = style.featuredBadges
            .filter { name -> unlocked.any { it.name == name } }
            .distinct()
            .take(ProfileStyle.FEATURED_MAX),
    )

    /** Liga/desliga um destaque mantendo a ordem de escolha; cheio, ignora novas escolhas. */
    fun toggleFeatured(current: List<String>, id: String): List<String> = when {
        id in current -> current - id
        current.size >= ProfileStyle.FEATURED_MAX -> current
        else -> current + id
    }
}
