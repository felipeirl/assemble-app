package dev.assemble.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.assemble.app.core.designsystem.theme.AssembleColors
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.ThemeMode

/**
 * Gradiente de energia (hero-red → logo-pink, diagonal 135°).
 * Só em placeholders, banners e no pop-up de match; nunca atrás de texto corrido.
 */
fun energyGradientBrush(colors: AssembleColors): Brush = Brush.linearGradient(
    colors = listOf(colors.heroRed, colors.logoPink),
    start = Offset.Zero,
    end = Offset.Infinite,
)

fun Modifier.energyGradient(colors: AssembleColors, shape: Shape = RectangleShape): Modifier =
    background(brush = energyGradientBrush(colors), shape = shape)

@Preview(name = "Light")
@Composable
private fun EnergyGradientLightPreview() {
    AssembleTheme(ThemeMode.Light) {
        Box(Modifier.fillMaxWidth().height(120.dp).energyGradient(AssembleTheme.colors, AssembleTheme.shapes.lg))
    }
}

@Preview(name = "Dark")
@Composable
private fun EnergyGradientDarkPreview() {
    AssembleTheme(ThemeMode.Dark) {
        Box(Modifier.fillMaxWidth().height(120.dp).energyGradient(AssembleTheme.colors, AssembleTheme.shapes.lg))
    }
}
