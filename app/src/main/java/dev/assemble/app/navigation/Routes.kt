package dev.assemble.app.navigation

import androidx.navigation3.runtime.NavKey
import dev.assemble.app.feature.profile.ProfileSection
import kotlinx.serialization.Serializable

// Fluxo de entrada
@Serializable data object Login : NavKey
@Serializable data class Onboarding(val step: Int) : NavKey

// Abas (raiz de cada back stack)
@Serializable data object Discover : NavKey
@Serializable data object ChatList : NavKey
@Serializable data object Profile : NavKey

// Telas secundárias
/** [name] e [imageUrl] vêm do card, para a arte compartilhada existir antes de os detalhes carregarem. */
@Serializable data class CharacterPreview(val characterId: String, val name: String, val imageUrl: String?) : NavKey
@Serializable data class CharacterProfile(val characterId: String) : NavKey
@Serializable data class Conversation(val connectionId: String) : NavKey
/** [focus]: seção a mostrar ao abrir (recompensa tocada nas conquistas); null abre no topo. */
@Serializable data class EditProfile(val focus: ProfileSection? = null) : NavKey
@Serializable data object Settings : NavKey
@Serializable data object About : NavKey
@Serializable data object Help : NavKey
@Serializable data object Achievements : NavKey

/** Abas da bottom bar, na ordem de exibição. */
val TopLevelRoutes: Set<NavKey> = linkedSetOf(Discover, ChatList, Profile)
