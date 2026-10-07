package dev.assemble.app.core.domain

import dev.assemble.app.core.model.AvatarFrame
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.ProfileAccent
import dev.assemble.app.core.model.ProfileCover
import dev.assemble.app.core.model.ProfileStyle
import dev.assemble.app.core.model.Style

/** Arquétipo exibido no perfil ("Estrategista · Mutante"). Uma das partes pode faltar, nunca as duas. */
data class Archetype(val style: Style?, val origin: Origin?)

/** Regras da personalização do perfil. Puras: valem igual no app e no backend. */
object ProfileRules {

    /** Conquista que libera a recompensa; null = livre desde o início. Vem de [Achievement.reward], a única tabela. */
    fun requiredAchievement(reward: Reward): Achievement? = Achievement.entries.firstOrNull { it.reward == reward }

    fun isUnlocked(reward: Reward, unlocked: Set<Achievement>): Boolean =
        requiredAchievement(reward)?.let { it in unlocked } ?: true

    /** Primeiro estilo e primeira origem das preferências (ordem do enum). "Any" nas duas = sem arquétipo. */
    fun archetype(preferences: Preferences): Archetype? {
        val style = preferences.styles.minByOrNull { it.ordinal }
        val origin = preferences.origins.minByOrNull { it.ordinal }
        return if (style == null && origin == null) null else Archetype(style, origin)
    }

    /**
     * O que pode ser mostrado agora: capa, cor, moldura e título bloqueados voltam ao padrão, destaques que deixaram
     * de valer (conexão desfeita, conquista zerada) saem, e a resposta respeita o limite.
     */
    fun sanitize(style: ProfileStyle, unlocked: Set<Achievement>, connectedIds: Set<String>): ProfileStyle = style.copy(
        cover = if (isUnlocked(Reward.Cover(style.cover), unlocked)) style.cover else ProfileCover.Energy,
        accent = if (isUnlocked(Reward.Accent(style.accent), unlocked)) style.accent else ProfileAccent.Pink,
        frame = if (isUnlocked(Reward.Frame(style.frame), unlocked)) style.frame else AvatarFrame.Ring,
        title = style.title?.takeIf { isUnlocked(Reward.Title(it), unlocked) },
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
