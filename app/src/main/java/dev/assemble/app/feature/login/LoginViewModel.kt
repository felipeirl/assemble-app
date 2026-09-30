package dev.assemble.app.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.core.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Login mock: Google e e-mail entram do mesmo jeito. A troca de fluxo
 * (onboarding ou app) acontece pela sessão do [UserRepository].
 */
class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val state = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = state.asStateFlow()

    fun signIn() {
        if (state.value.signingIn) return
        state.value = LoginUiState(signingIn = true)
        viewModelScope.launch {
            try {
                userRepository.logIn()
                state.update { it.copy(signingIn = false) }
            } catch (_: IOException) {
                state.value = LoginUiState(signingIn = false, showError = true)
            }
        }
    }
}
