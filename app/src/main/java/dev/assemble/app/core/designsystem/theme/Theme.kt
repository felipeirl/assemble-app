package dev.assemble.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tema mínimo da migração. Tokens completos, tipografia e shapes chegam na etapa 2.
private val OffWhite = Color(0xFFF5F1E8)
private val Midnight = Color(0xFF0B1020)
private val White = Color(0xFFFFFFFF)
private val SurfaceDark = Color(0xFF151B2E)
private val Ink = Color(0xFF111827)
private val ConnectionPink = Color(0xFFD6133F)

private val LightColorScheme = lightColorScheme(
    primary = ConnectionPink,
    onPrimary = White,
    background = OffWhite,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
)

private val DarkColorScheme = darkColorScheme(
    primary = ConnectionPink,
    onPrimary = White,
    background = Midnight,
    onBackground = OffWhite,
    surface = SurfaceDark,
    onSurface = OffWhite,
)

@Composable
fun AssembleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content,
    )
}
