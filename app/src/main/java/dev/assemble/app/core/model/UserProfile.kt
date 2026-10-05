package dev.assemble.app.core.model

/**
 * Perfil local do usuário. O avatar é um dos presets do app (índice) ou, quando há [photo], a foto
 * escolhida (JPEG pequeno em Base64). [style] guarda a personalização.
 */
data class UserProfile(
    val name: String,
    val bio: String,
    val avatarPreset: Int,
    val style: ProfileStyle = ProfileStyle(),
    val photo: String? = null,
)
