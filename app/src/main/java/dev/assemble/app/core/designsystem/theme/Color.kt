package dev.assemble.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Paleta base (docs/design-system/tokens.json). Use os tokens semânticos de AssembleColors na UI.
internal object AssemblePalette {
    val HeroRed = Color(0xFFE62429)
    val DeepRed = Color(0xFFB71C1C)
    val Midnight = Color(0xFF0B1020)
    val White = Color(0xFFFFFFFF)
    val OffWhite = Color(0xFFF5F1E8)
    val Ink = Color(0xFF111827)
    val Slate = Color(0xFF596273)
    val Mist = Color(0xFFD9DDE5)
    val ConnectionPink = Color(0xFFD6133F)
    val ArcReactorBlue = Color(0xFF38BDF8)
    val LogoRed = Color(0xFFFF114B)
    val LogoPink = Color(0xFFFF3475)

    val SurfaceDark = Color(0xFF151B2E)
    val TextMutedDark = Color(0xFFA9B2C5)
    val BorderDark = Color(0xFF2A3247)
    val AccentTextDark = Color(0xFFFF5C7A)
    val AiSurfaceLight = Color(0xFFEAF7FE)
    val AiSurfaceDark = Color(0xFF12304A)
    val ErrorDark = Color(0xFFFF8A80)

    // shadow-card / shadow-action: rgba(11,16,32,a) no claro, rgba(0,0,0,a) no escuro.
    val ShadowCardLight = Midnight.copy(alpha = 0.10f)
    val ShadowActionLight = Midnight.copy(alpha = 0.20f)
    val ShadowCardDark = Color.Black.copy(alpha = 0.45f)
    val ShadowActionDark = Color.Black.copy(alpha = 0.50f)
}

/** Tokens semânticos de cor. Uma instância por tema. */
@Immutable
data class AssembleColors(
    val bg: Color,
    val surface: Color,
    val text: Color,
    val textMuted: Color,
    val border: Color,
    val actionPass: Color,
    val passInk: Color,
    val actionAssemble: Color,
    val onActionAssemble: Color,
    val accentText: Color,
    val score: Color,
    val onScore: Color,
    val aiSurface: Color,
    val error: Color,
    val heroRed: Color,
    val deepRed: Color,
    val logoRed: Color,
    val logoPink: Color,
    val midnight: Color,
    val shadowCard: Color,
    val shadowAction: Color,
    val isDark: Boolean,
)

val LightAssembleColors = AssembleColors(
    bg = AssemblePalette.OffWhite,
    surface = AssemblePalette.White,
    text = AssemblePalette.Ink,
    textMuted = AssemblePalette.Slate,
    border = AssemblePalette.Mist,
    actionPass = AssemblePalette.White,
    passInk = AssemblePalette.Ink,
    actionAssemble = AssemblePalette.ConnectionPink,
    onActionAssemble = AssemblePalette.White,
    accentText = AssemblePalette.ConnectionPink,
    score = AssemblePalette.ArcReactorBlue,
    onScore = AssemblePalette.Midnight,
    aiSurface = AssemblePalette.AiSurfaceLight,
    error = AssemblePalette.DeepRed,
    heroRed = AssemblePalette.HeroRed,
    deepRed = AssemblePalette.DeepRed,
    logoRed = AssemblePalette.LogoRed,
    logoPink = AssemblePalette.LogoPink,
    midnight = AssemblePalette.Midnight,
    shadowCard = AssemblePalette.ShadowCardLight,
    shadowAction = AssemblePalette.ShadowActionLight,
    isDark = false,
)

val DarkAssembleColors = AssembleColors(
    bg = AssemblePalette.Midnight,
    surface = AssemblePalette.SurfaceDark,
    text = AssemblePalette.OffWhite,
    textMuted = AssemblePalette.TextMutedDark,
    border = AssemblePalette.BorderDark,
    actionPass = AssemblePalette.SurfaceDark,
    passInk = AssemblePalette.OffWhite,
    actionAssemble = AssemblePalette.ConnectionPink,
    onActionAssemble = AssemblePalette.White,
    accentText = AssemblePalette.AccentTextDark,
    score = AssemblePalette.ArcReactorBlue,
    onScore = AssemblePalette.Midnight,
    aiSurface = AssemblePalette.AiSurfaceDark,
    error = AssemblePalette.ErrorDark,
    heroRed = AssemblePalette.HeroRed,
    deepRed = AssemblePalette.DeepRed,
    logoRed = AssemblePalette.LogoRed,
    logoPink = AssemblePalette.LogoPink,
    midnight = AssemblePalette.Midnight,
    shadowCard = AssemblePalette.ShadowCardDark,
    shadowAction = AssemblePalette.ShadowActionDark,
    isDark = true,
)

val LocalAssembleColors = staticCompositionLocalOf { LightAssembleColors }
