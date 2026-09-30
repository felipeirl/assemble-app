package dev.assemble.app.core.model

enum class ThemePreference { Light, Dark, System }

/** Configurações locais (Settings). Persistência em DataStore na etapa 11. */
data class AppSettings(
    val theme: ThemePreference = ThemePreference.System,
    val minimumCompatibility: Int = DEFAULT_MINIMUM_COMPATIBILITY,
    val notifyNewConnections: Boolean = true,
    val notifyNewMessages: Boolean = true,
) {
    companion object {
        const val DEFAULT_MINIMUM_COMPATIBILITY = 70
        const val MIN_MINIMUM_COMPATIBILITY = 50
        const val MAX_MINIMUM_COMPATIBILITY = 90
    }
}

/** Estado da sessão mock: login e onboarding. */
data class SessionState(
    val isLoggedIn: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
)
