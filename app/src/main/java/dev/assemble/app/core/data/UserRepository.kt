package dev.assemble.app.core.data

import dev.assemble.app.core.model.AppSettings
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.SessionState
import dev.assemble.app.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface UserRepository {
    /** true depois que a sessão e as configurações salvas foram lidas; antes disso a splash nativa segura a tela. */
    val ready: StateFlow<Boolean>
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

    /** true quando o botão de e-mail pede e-mail e senha (login real). Sem backend, ele entra direto. */
    val passwordLogin: Boolean get() = false

    /** Login com Google disponível nesta configuração. */
    val googleLogin: Boolean get() = true

    /** Login real por e-mail e senha; [createAccount] cria a conta antes. */
    suspend fun logInWithEmail(email: String, password: String, createAccount: Boolean) = logIn()

    /** Conta em carência depois de "Delete account": o app oferece reativar. */
    val accountDeactivated: StateFlow<Boolean> get() = NeverDeactivated

    suspend fun reactivateAccount() = Unit

    /** "Clear seen characters" contradiz "Pass nunca volta" quando o baralho vem do backend. */
    val canClearSeen: Boolean get() = true

    /**
     * true quando [deleteAccount] só desativa a conta e o servidor apaga os dados depois da carência.
     * Nesse caso as conversas não podem sumir antes: a reativação as devolve.
     */
    val accountDeletionHandledByServer: Boolean get() = false
}

private val NeverDeactivated: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
