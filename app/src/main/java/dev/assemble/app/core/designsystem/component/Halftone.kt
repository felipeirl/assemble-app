package dev.assemble.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.ThemeMode

enum class HalftoneFade { None, Diagonal }

/** Opacidade padrão do meio-tom sobre arte (components.css: .as-card__art::after). */
const val HalftoneDefaultAlpha = 0.22f

/** No fade diagonal, os pontos somem a 55% do caminho do canto superior esquerdo ao inferior direito. */
private const val DiagonalFadeEnd = 0.55f

/**
 * Motivo meio-tom: grade de pontos desenhada atrás do conteúdo, sem imagens.
 * Só em arte e estados vazios.
 */
fun Modifier.halftone(
    color: Color,
    dotRadius: Dp = 1.2.dp,
    spacing: Dp = 8.dp,
    alpha: Float = HalftoneDefaultAlpha,
    fade: HalftoneFade = HalftoneFade.Diagonal,
): Modifier = drawBehind {
    val radiusPx = dotRadius.toPx()
    val stepPx = spacing.toPx()
    if (stepPx <= 0f || size.minDimension <= 0f) return@drawBehind
    val diagonalLength = size.width + size.height
    var y = stepPx / 2
    while (y < size.height) {
        var x = stepPx / 2
        while (x < size.width) {
            val fadeFactor = when (fade) {
                HalftoneFade.None -> 1f
                HalftoneFade.Diagonal -> (1f - ((x + y) / diagonalLength) / DiagonalFadeEnd).coerceIn(0f, 1f)
            }
            if (fadeFactor > 0f) {
                drawCircle(color = color, radius = radiusPx, center = Offset(x, y), alpha = alpha * fadeFactor)
            }
            x += stepPx
        }
        y += stepPx
    }
}

@Preview(name = "Light")
@Composable
private fun HalftoneLightPreview() {
    AssembleTheme(ThemeMode.Light) {
        Box(
            Modifier.fillMaxWidth().height(160.dp)
                .energyGradient(AssembleTheme.colors)
                .halftone(color = AssembleTheme.colors.midnight),
        )
    }
}

@Preview(name = "Dark")
@Composable
private fun HalftoneDarkPreview() {
    AssembleTheme(ThemeMode.Dark) {
        Box(
            Modifier.fillMaxWidth().height(160.dp)
                .energyGradient(AssembleTheme.colors)
                .halftone(color = AssembleTheme.colors.midnight),
        )
    }
}
