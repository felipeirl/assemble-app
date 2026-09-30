package dev.assemble.app.core.data.fake

import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.model.AppSettings
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.SessionState
import dev.assemble.app.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/** Usuário, sessão, preferências e configurações em memória. Settings vão para DataStore na etapa 11. */
class FakeUserRepository(
    private val network: FakeNetwork,
    private val initialProfile: UserProfile,
    initialPreferences: Preferences,
) : UserRepository {
    private val sessionState = MutableStateFlow(SessionState())
    private val settingsState = MutableStateFlow(AppSettings())
    private val preferencesState = MutableStateFlow(initialPreferences)
    private val seen = MutableStateFlow<Set<String>>(emptySet())
    private val profile = MutableStateFlow(initialProfile)

    override val session: StateFlow<SessionState> = sessionState.asStateFlow()
    override val settings: StateFlow<AppSettings> = settingsState.asStateFlow()
    override val preferences: StateFlow<Preferences> = preferencesState.asStateFlow()
    override val seenCharacterIds: StateFlow<Set<String>> = seen.asStateFlow()
    override val currentProfile: StateFlow<UserProfile> = profile.asStateFlow()

    override fun observeProfile(): Flow<UserProfile> = flow {
        network.call()
        emitAll(profile)
    }

    override suspend fun logIn() {
        network.call()
        sessionState.update { it.copy(isLoggedIn = true) }
    }

    override suspend fun logOut() {
        sessionState.update { it.copy(isLoggedIn = false) }
    }

    override suspend fun completeOnboarding(preferences: Preferences) {
        network.call()
        preferencesState.value = preferences
        sessionState.update { it.copy(hasCompletedOnboarding = true) }
    }

    override suspend fun updateProfile(profile: UserProfile) {
        network.call()
        this.profile.value = profile
    }

    override suspend fun updatePreferences(preferences: Preferences) {
        network.call()
        preferencesState.value = preferences
    }

    override suspend fun resetPreferences() {
        network.call()
        preferencesState.value = Preferences.Any
    }

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsState.update(transform)
    }

    override suspend fun markSeen(characterId: String) {
        seen.update { it + characterId }
    }

    override suspend fun unmarkSeen(characterId: String) {
        seen.update { it - characterId }
    }

    override suspend fun clearSeen() {
        seen.value = emptySet()
    }

    override suspend fun deleteAccount() {
        network.call()
        profile.value = initialProfile
        preferencesState.value = Preferences.Any
        settingsState.value = AppSettings()
        seen.value = emptySet()
        sessionState.value = SessionState()
    }
}
