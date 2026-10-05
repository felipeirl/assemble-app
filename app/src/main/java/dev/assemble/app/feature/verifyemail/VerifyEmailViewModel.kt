package dev.assemble.app.feature.verifyemail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.network.ApiErrorCode
import dev.assemble.app.core.network.ApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/** Intervalo mínimo entre dois e-mails, igual ao do backend (`VERIFICATION_RESEND_SECONDS`). */
const val RESEND_COOLDOWN_SECONDS = 60
private const val ONE_SECOND_MILLIS = 1_000L

enum class VerifyEmailMessage { Sent, StillPending, Failed, RateLimited }

data class VerifyEmailUiState(
    val email: String? = null,
    val sending: Boolean = false,
    val checking: Boolean = false,
    /** Segundos até poder reenviar; 0 = pode. */
    val cooldownSeconds: Int = 0,
    val message: VerifyEmailMessage? = null,
)

/**
 * Tela "Confirme o seu e-mail". Ao confirmar, o próprio repositório muda a sessão e o app
 * segue para o cadastro; aqui só se reenvia o e-mail e se verifica de novo.
 */
class VerifyEmailViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val state = MutableStateFlow(VerifyEmailUiState(email = userRepository.accountEmail))
    val uiState: StateFlow<VerifyEmailUiState> = state.asStateFlow()
    private var cooldownJob: Job? = null

    /** "Já confirmei" ou a volta ao app. Em [silent], não avisa que ainda falta confirmar. */
    fun check(silent: Boolean = false) {
        if (state.value.checking) return
        state.update { it.copy(checking = true, message = if (silent) it.message else null) }
        viewModelScope.launch {
            val message = try {
                if (userRepository.refreshEmailVerified()) null else VerifyEmailMessage.StillPending
            } catch (_: IOException) {
                VerifyEmailMessage.Failed
            }
            state.update { it.copy(checking = false, message = if (silent && message != null) it.message else message) }
        }
    }

    fun resend() {
        val current = state.value
        if (current.sending || current.cooldownSeconds > 0) return
        state.update { it.copy(sending = true, message = null) }
        viewModelScope.launch {
            try {
                userRepository.sendVerificationEmail()
                state.update { it.copy(sending = false, message = VerifyEmailMessage.Sent) }
                startCooldown(RESEND_COOLDOWN_SECONDS)
            } catch (error: ApiException) {
                if (error.code == ApiErrorCode.RATE_LIMITED) {
                    state.update { it.copy(sending = false, message = VerifyEmailMessage.RateLimited) }
                    startCooldown((error.retryAfterSeconds ?: RESEND_COOLDOWN_SECONDS.toLong()).toInt())
                } else {
                    state.update { it.copy(sending = false, message = VerifyEmailMessage.Failed) }
                }
            } catch (_: IOException) {
                state.update { it.copy(sending = false, message = VerifyEmailMessage.Failed) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { userRepository.logOut() }
    }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        state.update { it.copy(cooldownSeconds = seconds) }
        cooldownJob = viewModelScope.launch {
            while (state.value.cooldownSeconds > 0) {
                delay(ONE_SECOND_MILLIS)
                state.update { it.copy(cooldownSeconds = (it.cooldownSeconds - 1).coerceAtLeast(0)) }
            }
        }
    }
}
