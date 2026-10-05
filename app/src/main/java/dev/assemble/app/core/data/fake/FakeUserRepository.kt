package dev.assemble.app.core.data.fake

import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.data.local.MockSessionStore
import dev.assemble.app.core.data.local.SettingsDataStore
import dev.assemble.app.core.data.local.StoredSession
import dev.assemble.app.core.model.AppSettings
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.SessionState
import dev.assemble.app.core.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Usuário sem backend. Sessão, preferências e perfil ficam guardados no aparelho ([MockSessionStore]);
 * Settings (tema, notificações) em DataStore; personagens vistos só em memória.
 * [ready] vira true depois que os dois foram lidos, e só então (a splash nativa espera por isso).
 */
class FakeUserRepository(
    private val network: FakeNetwork,
    private val initialProfile: UserProfile,
    initialPreferences: Preferences,
    private val settingsStore: SettingsDataStore,
    private val sessionStore: MockSessionStore,
    scope: CoroutineScope,
) : UserRepository {
    private val sessionState = MutableStateFlow(SessionState())
    private val preferencesState = MutableStateFlow(initialPreferences)
    private val seen = MutableStateFlow<Set<String>>(emptySet())
    private val profile = MutableStateFlow(initialProfile)
    private val settingsState = MutableStateFlow(AppSettings())
    private val readyState = MutableStateFlow(false)

    override val ready: StateFlow<Boolean> = readyState.asStateFlow()
    override val session: StateFlow<SessionState> = sessionState.asStateFlow()
    override val settings: StateFlow<AppSettings> = settingsState.asStateFlow()
    override val preferences: StateFlow<Preferences> = preferencesState.asStateFlow()
    override val seenCharacterIds: StateFlow<Set<String>> = seen.asStateFlow()
    override val currentProfile: StateFlow<UserProfile> = profile.asStateFlow()

    init {
        scope.launch {
            // Uma ordem só: sessão restaurada, depois cada valor das configurações; "pronto" no primeiro deles.
            val stored = sessionStore.read()
            sessionState.value = stored.session
            preferencesState.value = stored.preferences
            profile.value = stored.profile
            settingsStore.settings.collect { current ->
                settingsState.value = current
                readyState.value = true
            }
        }
    }

    override fun observeProfile(): Flow<UserProfile> = flow {
        network.call()
        emitAll(profile)
    }

    override suspend fun logIn() {
        network.call()
        sessionState.update { it.copy(isLoggedIn = true) }
        persist()
    }

    override suspend fun logOut() {
        sessionState.update { it.copy(isLoggedIn = false) }
        persist()
    }

    override suspend fun completeOnboarding(preferences: Preferences) {
        network.call()
        preferencesState.value = preferences
        sessionState.update { it.copy(hasCompletedOnboarding = true) }
        persist()
    }

    override suspend fun updateProfile(profile: UserProfile) {
        network.call()
        this.profile.value = profile
        persist()
    }

    override suspend fun updatePreferences(preferences: Preferences) {
        network.call()
        preferencesState.value = preferences
        persist()
    }

    override suspend fun resetPreferences() {
        network.call()
        preferencesState.value = Preferences.Any
        persist()
    }

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsStore.update(transform)
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
        settingsStore.reset()
        sessionStore.clear()
        seen.value = emptySet()
        sessionState.value = SessionState()
    }

    private suspend fun persist() {
        sessionStore.save(StoredSession(sessionState.value, preferencesState.value, profile.value))
    }
}
