package dev.assemble.app.core.data

import dev.assemble.app.core.model.AppSettings
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.SessionState
import dev.assemble.app.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val session: StateFlow<SessionState>
    val settings: StateFlow<AppSettings>
    val preferences: StateFlow<Preferences>
    val seenCharacterIds: StateFlow<Set<String>>

    /** Perfil em memória, sem latência (drawer, avatar no match). Telas de perfil usam [observeProfile]. */
    val currentProfile: StateFlow<UserProfile>

    /** Perfil do usuário. O primeiro valor pode falhar com IOException. */
    fun observeProfile(): Flow<UserProfile>

    suspend fun logIn()
    suspend fun logOut()
    suspend fun completeOnboarding(preferences: Preferences)

    suspend fun updateProfile(profile: UserProfile)
    suspend fun updatePreferences(preferences: Preferences)
    suspend fun resetPreferences()
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)

    suspend fun markSeen(characterId: String)
    suspend fun unmarkSeen(characterId: String)
    suspend fun clearSeen()

    /** Apaga os dados locais do usuário e encerra a sessão. */
    suspend fun deleteAccount()
}
