package dev.assemble.app.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.data.remote.AuthException
import dev.assemble.app.core.data.remote.AuthFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Sem backend, Google e e-mail entram do mesmo jeito (login simulado). Com backend, o e-mail pede
 * e-mail e senha e o Google devolve um ID token. A troca de fluxo (onboarding ou app) acontece
 * pela sessão do [UserRepository].
 */
class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val state = MutableStateFlow(
        LoginUiState(showGoogle = userRepository.googleLogin, inlineEmailForm = userRepository.passwordLogin),
    )
    val uiState: StateFlow<LoginUiState> = state.asStateFlow()

    /** Não nulo quando o login com Google precisa de um token real; a tela abre o seletor de contas. */
    val googleWebClientId: String? = userRepository.googleWebClientId

    /** Login simulado (sem backend). */
    fun signIn() {
        submit { userRepository.logIn() }
    }

    fun signInWithGoogle(tokenSource: suspend () -> String) {
        submit { userRepository.logInWithGoogleToken(tokenSource()) }
    }

    fun onEmailChange(email: String) = state.update { it.copy(email = email, error = null, resetSent = false) }

    fun onPasswordChange(password: String) = state.update { it.copy(password = password, error = null) }

    fun togglePasswordVisibility() = state.update { it.copy(showPassword = !it.showPassword) }

    fun setCreateAccount(create: Boolean) =
        state.update { it.copy(createAccount = create, error = null, resetSent = false) }

    fun submitEmail() {
        val current = state.value
        if (!current.canSubmitEmail) return
        submit { userRepository.logInWithEmail(current.email.trim(), current.password, current.createAccount) }
    }

    fun resetPassword() {
        val email = state.value.email.trim()
        if (!email.contains('@')) {
            state.update { it.copy(error = LoginError.InvalidEmail) }
            return
        }
        submit(onSuccess = { it.copy(resetSent = true) }) { userRepository.sendPasswordReset(email) }
    }

    private fun submit(onSuccess: (LoginUiState) -> LoginUiState = { it }, action: suspend () -> Unit) {
        if (state.value.signingIn) return
        state.update { it.copy(signingIn = true, error = null, resetSent = false) }
        viewModelScope.launch {
            try {
                action()
                state.update { onSuccess(it.copy(signingIn = false, password = "")) }
            } catch (error: IOException) {
                val cancelled = (error as? AuthException)?.reason == AuthFailure.Cancelled
                state.update { it.copy(signingIn = false, error = if (cancelled) null else loginError(error)) }
            }
        }
    }
}

internal fun loginError(error: IOException): LoginError = when ((error as? AuthException)?.reason) {
    AuthFailure.InvalidCredentials -> LoginError.InvalidCredentials
    AuthFailure.EmailInUse -> LoginError.EmailInUse
    AuthFailure.WeakPassword -> LoginError.WeakPassword
    AuthFailure.InvalidEmail -> LoginError.InvalidEmail
    AuthFailure.Network, AuthFailure.Cancelled, AuthFailure.Other, null -> LoginError.Generic
}
