package dev.assemble.app.feature.profile

import dev.assemble.app.core.domain.Reward
import kotlinx.serialization.Serializable

/** Seção do Editar perfil que pode receber o foco ao abrir (vindo de uma recompensa nas conquistas). */
@Serializable
enum class ProfileSection { Title, Cover, Accent, Frame }

fun Reward.section(): ProfileSection = when (this) {
    is Reward.Title -> ProfileSection.Title
    is Reward.Cover -> ProfileSection.Cover
    is Reward.Accent -> ProfileSection.Accent
    is Reward.Frame -> ProfileSection.Frame
}
