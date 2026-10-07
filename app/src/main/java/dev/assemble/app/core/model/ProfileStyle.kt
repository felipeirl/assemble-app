package dev.assemble.app.core.model

/** Capa do perfil do usuário. As quatro primeiras são livres; as demais vêm de conquistas. */
enum class ProfileCover { Energy, Halftone, Comic, Night, Headline, Blueprint, Cosmos }

/** Cor de destaque do perfil: moldura, arquétipo, números e capa meio-tom. Esmeralda e Prata vêm de conquistas. */
enum class ProfileAccent { Pink, Red, Blue, Violet, Gold, Emerald, Silver }

/** Moldura do avatar. Simples e Anel são livres; as demais vêm de conquistas. */
enum class AvatarFrame { Simple, Ring, Hexagon, Burst, Shield, Cosmic, Lightning }

/** Título exibido abaixo do nome. Todos vêm de conquistas; sem título é o padrão. */
enum class ProfileTitle { Recruit, IceBreaker, Explorer, Diplomat, LivingLegend, AlterEgo }

/** Pergunta da frase de apresentação. */
enum class ProfilePrompt { DreamPower, IdealTeam, FirstRecruit }

/**
 * Como a pessoa personalizou o perfil. [featuredConnections] guarda ids de personagens conectados
 * e [featuredBadges] nomes de conquistas, até [FEATURED_MAX] cada, na ordem escolhida.
 */
data class ProfileStyle(
    val cover: ProfileCover = ProfileCover.Energy,
    val accent: ProfileAccent = ProfileAccent.Pink,
    val frame: AvatarFrame = AvatarFrame.Ring,
    val prompt: ProfilePrompt = ProfilePrompt.DreamPower,
    val promptAnswer: String = "",
    val featuredConnections: List<String> = emptyList(),
    val featuredBadges: List<String> = emptyList(),
    val title: ProfileTitle? = null,
) {
    companion object {
        const val FEATURED_MAX = 3
        const val PROMPT_ANSWER_MAX = 80
    }
}
