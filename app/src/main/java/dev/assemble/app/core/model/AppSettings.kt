package dev.assemble.app.core.model

enum class ThemePreference { Light, Dark, System }

/** Configurações locais (Settings). Persistência em DataStore na etapa 11. */
data class AppSettings(
    val theme: ThemePreference = ThemePreference.System,
    val notifyNewConnections: Boolean = true,
    val notifyNewMessages: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
)

/** Estado da sessão mock: login e onboarding. */
data class SessionState(
    val isLoggedIn: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    /** Login por e-mail e senha com o e-mail ainda por confirmar: a conta fica na tela de verificação. */
    val emailVerificationPending: Boolean = false,
)
