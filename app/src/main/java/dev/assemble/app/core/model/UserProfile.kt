package dev.assemble.app.core.model

/** Perfil local do usuário. O avatar é um dos presets do app (índice). */
data class UserProfile(
    val name: String,
    val bio: String,
    val avatarPreset: Int,
)
