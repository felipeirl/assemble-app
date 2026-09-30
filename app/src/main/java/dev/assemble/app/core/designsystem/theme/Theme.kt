package dev.assemble.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

enum class ThemeMode { Light, Dark, System }

@Composable
fun AssembleTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val colors = if (darkTheme) DarkAssembleColors else LightAssembleColors
    val typography = DefaultAssembleTypography
    val shapes = AssembleShapes()

    CompositionLocalProvider(
        LocalAssembleColors provides colors,
        LocalAssembleTypography provides typography,
        LocalAssembleSpacing provides AssembleSpacing(),
        LocalAssembleShapes provides shapes,
    ) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = typography.toMaterialTypography(),
            shapes = AssembleMaterialShapes,
            content = content,
        )
    }
}

/** Acesso aos tokens: `AssembleTheme.colors.surface`, `AssembleTheme.typography.body`… */
object AssembleTheme {
    val colors: AssembleColors
        @Composable @ReadOnlyComposable get() = LocalAssembleColors.current
    val typography: AssembleTypography
        @Composable @ReadOnlyComposable get() = LocalAssembleTypography.current
    val spacing: AssembleSpacing
        @Composable @ReadOnlyComposable get() = LocalAssembleSpacing.current
    val shapes: AssembleShapes
        @Composable @ReadOnlyComposable get() = LocalAssembleShapes.current
}

private fun AssembleColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = actionAssemble,
        onPrimary = onActionAssemble,
        primaryContainer = actionAssemble,
        onPrimaryContainer = onActionAssemble,
        secondary = accentText,
        onSecondary = surface,
        tertiary = score,
        onTertiary = onScore,
        tertiaryContainer = aiSurface,
        onTertiaryContainer = text,
        background = bg,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = surface,
        onSurfaceVariant = textMuted,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface,
        surfaceBright = surface,
        surfaceDim = bg,
        inverseSurface = text,
        inverseOnSurface = surface,
        outline = textMuted,
        outlineVariant = border,
        error = error,
        onError = surface,
        errorContainer = surface,
        onErrorContainer = error,
        scrim = midnight,
    )
}
