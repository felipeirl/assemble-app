package dev.assemble.app.core.model

/**
 * Preferências usadas no cálculo de compatibilidade.
 * Conjunto vazio = "Any" (peso cheio na categoria).
 */
data class Preferences(
    val origins: Set<Origin>,
    val powers: Set<PowerFamily>,
    val teams: Set<Team>,
    val styles: Set<Style>,
    /** 0 = Icons, 1 = Hidden gems. */
    val fame: Float,
) {
    companion object {
        const val FAME_ICONS = 0f
        const val FAME_HIDDEN_GEMS = 1f
        const val FAME_DEFAULT = 0.5f

        val Any = Preferences(emptySet(), emptySet(), emptySet(), emptySet(), FAME_DEFAULT)
    }
}
