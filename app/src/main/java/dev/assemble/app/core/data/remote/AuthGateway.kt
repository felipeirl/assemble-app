package dev.assemble.app.core.data.remote

import dev.assemble.app.core.network.IdTokenProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

/** Por que o login falhou, para a tela escolher a mensagem. */
enum class AuthFailure { InvalidCredentials, EmailInUse, WeakPassword, InvalidEmail, Network, Cancelled, Other }

class AuthException(val reason: AuthFailure, cause: Throwable? = null) : IOException(reason.name, cause)

/** Login do Firebase Auth. O token vai em toda chamada ao backend ([IdTokenProvider]). */
interface AuthGateway : IdTokenProvider {
    /** uid do usuário logado; null sem login. */
    val uid: StateFlow<String?>

    /** true depois que o Firebase informou o estado inicial do login. */
    val ready: StateFlow<Boolean>

    /** E-mail do usuário logado, para o nome inicial do perfil. */
    val email: String?

    suspend fun signIn(email: String, password: String)

    suspend fun createAccount(email: String, password: String)

    /** Entra com o ID token do Google (Credential Manager). */
    suspend fun signInWithGoogle(idToken: String)

    /** Envia o link de redefinição de senha. Não revela se o e-mail tem conta. */
    suspend fun sendPasswordReset(email: String)

    suspend fun signOut()

    /** true com login por e-mail e senha e o e-mail ainda não confirmado (o Google já vem confirmado). */
    val emailVerificationPending: StateFlow<Boolean> get() = NeverPending

    /** Relê o usuário no Firebase e renova o token; devolve true se o e-mail já está confirmado. */
    suspend fun reloadEmailVerified(): Boolean = true

    /** E-mail padrão do Firebase, usado quando o backend não consegue mandar o dele. */
    suspend fun sendFirebaseVerificationEmail() = Unit
}

private val NeverPending: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
