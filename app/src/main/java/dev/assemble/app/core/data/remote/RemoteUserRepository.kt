package dev.assemble.app.core.data.remote

import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.data.local.SettingsDataStore
import dev.assemble.app.core.model.AppSettings
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.SessionState
import dev.assemble.app.core.model.UserProfile
import dev.assemble.app.core.network.AssembleApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

private const val EMAIL_NAME_MAX = 60
private const val FALLBACK_NAME = "Hero"

/**
 * Usuário com login real (Firebase Auth) e perfil em `users/{uid}` (Firestore). O app grava só
 * perfil, preferências e `aiConsent`; status da conta e tudo o mais são do backend.
 * Tema, notificações e personagens vistos ficam no aparelho.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteUserRepository(
    private val auth: AuthGateway,
    private val store: UserDataStore,
    private val api: AssembleApi,
    private val settingsStore: SettingsDataStore,
    scope: CoroutineScope,
    private val webClientId: String? = null,
) : UserRepository {

    private sealed interface UserState {
        data object Unknown : UserState
        data object SignedOut : UserState
        data class SignedIn(val uid: String, val document: UserDocument) : UserState
    }

    private val userState: StateFlow<UserState> = combine(auth.ready, auth.uid) { ready, uid -> ready to uid }
        .flatMapLatest { (ready, uid) ->
            when {
                !ready -> flowOf(UserState.Unknown)
                uid == null -> flowOf(UserState.SignedOut)
                else -> store.observeUser(uid)
                    .map<Map<String, Any?>?, UserState> { UserState.SignedIn(uid, userDocument(it, defaultName())) }
                    // Sem ler o perfil (rede, regra), a sessão segue com os padrões; a tela tenta de novo.
                    .catch { emit(UserState.SignedIn(uid, userDocument(null, defaultName()))) }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, UserState.Unknown)

    private val settingsState = MutableStateFlow(AppSettings())
    private val settingsLoaded = MutableStateFlow(false)
    private val seen = MutableStateFlow<Set<String>>(emptySet())
    private val deactivatedByApi = MutableStateFlow(false)

    override val ready: StateFlow<Boolean> = combine(userState, settingsLoaded) { user, loaded ->
        loaded && user != UserState.Unknown
    }.stateIn(scope, SharingStarted.Eagerly, false)

    override val session: StateFlow<SessionState> = userState.map { user ->
        when (user) {
            is UserState.SignedIn -> SessionState(isLoggedIn = true, hasCompletedOnboarding = user.document.onboardingCompleted)
            else -> SessionState()
        }
    }.stateIn(scope, SharingStarted.Eagerly, SessionState())

    override val settings: StateFlow<AppSettings> = settingsState.asStateFlow()

    override val preferences: StateFlow<Preferences> = userState.map { user ->
        (user as? UserState.SignedIn)?.document?.preferences ?: Preferences.Any
    }.stateIn(scope, SharingStarted.Eagerly, Preferences.Any)

    override val seenCharacterIds: StateFlow<Set<String>> = seen.asStateFlow()

    override val currentProfile: StateFlow<UserProfile> = userState.map { user ->
        (user as? UserState.SignedIn)?.document?.profile ?: UserProfile(defaultName(), "", 0)
    }.stateIn(scope, SharingStarted.Eagerly, UserProfile(FALLBACK_NAME, "", 0))

    override val accountDeactivated: StateFlow<Boolean> = combine(userState, deactivatedByApi) { user, byApi ->
        byApi || (user as? UserState.SignedIn)?.document?.deactivated == true
    }.stateIn(scope, SharingStarted.Eagerly, false)

    override val passwordLogin: Boolean get() = true

    // O login com Google só aparece com o ID do cliente web configurado no local.properties.
    override val googleLogin: Boolean get() = !webClientId.isNullOrBlank()

    override val googleWebClientId: String? get() = webClientId?.takeIf { it.isNotBlank() }

    override val canClearSeen: Boolean get() = false

    override val accountDeletionHandledByServer: Boolean get() = true

    init {
        scope.launch {
            settingsStore.settings.collect { current ->
                settingsState.value = current
                settingsLoaded.value = true
            }
        }
    }

    /** Avisado pela API quando o backend responde 403 account_deactivated. */
    fun onAccountDeactivated() {
        deactivatedByApi.value = true
    }

    override fun observeProfile(): Flow<UserProfile> = userState
        .filterIsInstance<UserState.SignedIn>()
        .map { it.document.profile }
        .onStart { if (auth.uid.value == null) throw IOException("Sem usuário logado") }

    override suspend fun logIn() {
        throw IOException("Login com Google indisponível nesta versão")
    }

    override suspend fun logInWithEmail(email: String, password: String, createAccount: Boolean) {
        if (createAccount) auth.createAccount(email, password) else auth.signIn(email, password)
        deactivatedByApi.value = false
    }

    override suspend fun logInWithGoogleToken(idToken: String) {
        auth.signInWithGoogle(idToken)
        deactivatedByApi.value = false
    }

    override suspend fun sendPasswordReset(email: String) = auth.sendPasswordReset(email)

    override suspend fun logOut() {
        auth.signOut()
        seen.value = emptySet()
        deactivatedByApi.value = false
    }

    override suspend fun completeOnboarding(preferences: Preferences) {
        val uid = requireUid()
        store.mergeUser(
            uid,
            profileFields(currentProfile.value, currentProfile.value.photo) + mapOf(
                "preferences" to preferencesFields(preferences),
                "onboardingCompletedAt" to ServerTime,
                // O passo final do onboarding mostra o aviso de IA; continuar é o aceite.
                "aiConsent" to mapOf("acceptedAt" to ServerTime, "version" to AI_CONSENT_VERSION),
            ) + createdAtIfNew(),
        )
        // O listener do Firestore confirma; esperar evita voltar ao onboarding por um instante.
        session.first { it.hasCompletedOnboarding }
    }

    override suspend fun updateProfile(profile: UserProfile) {
        store.mergeUser(requireUid(), profileFields(profile, previousPhoto = currentProfile.value.photo))
    }

    override suspend fun updatePreferences(preferences: Preferences) {
        store.mergeUser(requireUid(), mapOf("preferences" to preferencesFields(preferences)))
    }

    override suspend fun resetPreferences() = updatePreferences(Preferences.Any)

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

    /** "Delete account": o backend desativa e conta 30 dias de carência; a sessão termina aqui. */
    override suspend fun deleteAccount() {
        api.deactivateAccount()
        settingsStore.reset()
        logOut()
    }

    override suspend fun reactivateAccount() {
        api.reactivateAccount()
        deactivatedByApi.value = false
    }

    private fun requireUid(): String = auth.uid.value ?: throw IOException("Sem usuário logado")

    private fun createdAtIfNew(): Map<String, Any?> {
        val user = userState.value as? UserState.SignedIn ?: return emptyMap()
        return if (user.document.onboardingCompleted) emptyMap() else mapOf("createdAt" to ServerTime)
    }

    private fun defaultName(): String =
        auth.email?.substringBefore('@')?.takeIf { it.isNotBlank() }?.take(EMAIL_NAME_MAX) ?: FALLBACK_NAME

    /** Espera o primeiro estado conhecido do login (usado nos testes). */
    internal suspend fun awaitKnown() {
        userState.first { it != UserState.Unknown }
    }
}
