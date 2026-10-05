package dev.assemble.app.feature.login

/** Por que o último envio falhou; cada um tem sua mensagem. */
enum class LoginError { Generic, InvalidCredentials, EmailInUse, WeakPassword, InvalidEmail }

/**
 * Login não carrega dados: só o envio e o erro de envio. Com backend, o botão de e-mail abre o
 * formulário de e-mail e senha ([emailFormOpen]); sem backend, entra direto.
 */
data class LoginUiState(
    val signingIn: Boolean = false,
    val error: LoginError? = null,
    val showGoogle: Boolean = true,
    val emailFormOpen: Boolean = false,
    val email: String = "",
    val password: String = "",
    val createAccount: Boolean = false,
) {
    val showError: Boolean get() = error != null

    /** E-mail com "@" e senha com o mínimo do Firebase. */
    val canSubmitEmail: Boolean get() = email.trim().contains('@') && password.length >= MIN_PASSWORD_LENGTH

    companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
