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
 * e-mail e senha. A troca de fluxo (onboarding ou app) acontece pela sessão do [UserRepository].
 */
class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val state = MutableStateFlow(LoginUiState(showGoogle = userRepository.googleLogin))
    val uiState: StateFlow<LoginUiState> = state.asStateFlow()

    fun signIn() {
        submit { userRepository.logIn() }
    }

    /** Botão "Continuar com e-mail": abre o formulário ou, sem backend, entra direto. */
    fun continueWithEmail() {
        if (userRepository.passwordLogin) {
            state.update { it.copy(emailFormOpen = true, error = null) }
        } else {
            signIn()
        }
    }

    fun onEmailChange(email: String) = state.update { it.copy(email = email, error = null) }

    fun onPasswordChange(password: String) = state.update { it.copy(password = password, error = null) }

    fun toggleCreateAccount() = state.update { it.copy(createAccount = !it.createAccount, error = null) }

    fun submitEmail() {
        val current = state.value
        if (!current.canSubmitEmail) return
        submit { userRepository.logInWithEmail(current.email.trim(), current.password, current.createAccount) }
    }

    private fun submit(action: suspend () -> Unit) {
        if (state.value.signingIn) return
        state.update { it.copy(signingIn = true, error = null) }
        viewModelScope.launch {
            try {
                action()
                state.update { it.copy(signingIn = false, password = "") }
            } catch (error: IOException) {
                state.update { it.copy(signingIn = false, error = loginError(error)) }
            }
        }
    }
}

internal fun loginError(error: IOException): LoginError = when ((error as? AuthException)?.reason) {
    AuthFailure.InvalidCredentials -> LoginError.InvalidCredentials
    AuthFailure.EmailInUse -> LoginError.EmailInUse
    AuthFailure.WeakPassword -> LoginError.WeakPassword
    AuthFailure.InvalidEmail -> LoginError.InvalidEmail
    AuthFailure.Network, AuthFailure.Other, null -> LoginError.Generic
}
