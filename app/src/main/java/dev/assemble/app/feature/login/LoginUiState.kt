package dev.assemble.app.feature.login

/** Login não carrega dados: só o envio e o erro de envio. */
data class LoginUiState(
    val signingIn: Boolean = false,
    val showError: Boolean = false,
)
