package dev.assemble.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Fluxo de entrada
@Serializable data object Splash : NavKey
@Serializable data object Login : NavKey
@Serializable data class Onboarding(val step: Int) : NavKey

// Abas (raiz de cada back stack)
@Serializable data object Discover : NavKey
@Serializable data object ChatList : NavKey
@Serializable data object Profile : NavKey

// Telas secundárias
@Serializable data class CharacterPreview(val characterId: String) : NavKey
@Serializable data class CharacterProfile(val characterId: String) : NavKey
@Serializable data class Conversation(val connectionId: String) : NavKey
@Serializable data object EditProfile : NavKey
@Serializable data object Settings : NavKey
@Serializable data object About : NavKey
@Serializable data object Help : NavKey

/** Abas da bottom bar, na ordem de exibição. */
val TopLevelRoutes: Set<NavKey> = linkedSetOf(Discover, ChatList, Profile)

const val ONBOARDING_STEP_COUNT = 5
