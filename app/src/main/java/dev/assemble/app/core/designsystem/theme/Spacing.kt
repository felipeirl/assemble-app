package dev.assemble.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Escala de espaçamento (tokens space-1 … space-8). */
@Immutable
data class AssembleSpacing(
    /** 4dp: ícone ↔ rótulo. */
    val space1: Dp = 4.dp,
    /** 8dp: grupos compactos. */
    val space2: Dp = 8.dp,
    /** 12dp: padding de item da nav. */
    val space3: Dp = 12.dp,
    /** 16dp: padding de card, margem da tela. */
    val space4: Dp = 16.dp,
    /** 24dp: entre botões de ação. */
    val space5: Dp = 24.dp,
    /** 32dp: entre seções. */
    val space6: Dp = 32.dp,
    /** 48dp: espaço de hero. */
    val space8: Dp = 48.dp,
)

val LocalAssembleSpacing = staticCompositionLocalOf { AssembleSpacing() }
