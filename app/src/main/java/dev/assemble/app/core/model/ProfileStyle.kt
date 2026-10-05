package dev.assemble.app.core.model

/** Capa do perfil do usuário. */
enum class ProfileCover { Energy, Halftone, Comic, Night }

/** Cor de destaque do perfil: moldura, arquétipo, números e capa meio-tom. */
enum class ProfileAccent { Pink, Red, Blue, Violet, Gold }

/** Moldura do avatar. Hexágono e Explosão são liberadas por conquistas. */
enum class AvatarFrame { Simple, Ring, Hexagon, Burst }

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
) {
    companion object {
        const val FEATURED_MAX = 3
        const val PROMPT_ANSWER_MAX = 80
    }
}
