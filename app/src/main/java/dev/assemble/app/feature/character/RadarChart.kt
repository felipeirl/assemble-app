package dev.assemble.app.feature.character

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val MaxStat = 100f
private const val GridRings = 4
private const val GrowMillis = 700
private const val CompareMillis = 450
private const val FillAlpha = 0.25f
private val LabelSpace = 44.dp
private val PolygonStroke = 2.dp
private val GridStroke = 1.dp
private const val LabelRadiusFactor = 1.14f

/**
 * Radar de atributos (0–100). Cresce do centro ao aparecer; [compareValues] desenha um segundo
 * polígono que cresce/encolhe ao ligar/desligar e muda de forma entre personagens.
 */
@Composable
internal fun RadarChart(
    values: List<Int>,
    labels: List<String>,
    color: Color,
    compareValues: List<Int>?,
    compareColor: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val animate = rememberAnimationsEnabled()
    val grow = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animate) grow.animateTo(1f, tween(GrowMillis, easing = EaseOutCubic))
    }
    // Guarda a última comparação para o polígono encolher em vez de sumir de uma vez.
    var lastCompare by remember { mutableStateOf(compareValues ?: values) }
    if (compareValues != null) lastCompare = compareValues
    val duration = if (animate) CompareMillis else 0
    val compareShown by animateFloatAsState(if (compareValues != null) 1f else 0f, tween(duration), label = "compareShown")
    val compareAnimated = lastCompare.mapIndexed { index, value ->
        animateFloatAsState(value.toFloat(), tween(duration), label = "compare$index").value
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = AssembleTheme.typography.small.copy(color = colors.textMuted)
    Canvas(modifier.semantics { contentDescription = description }) {
        val radius = size.minDimension / 2 - LabelSpace.toPx()
        if (radius <= 0f) return@Canvas
        drawGrid(values.size, radius, colors.border)
        drawLabels(labels, radius * LabelRadiusFactor, textMeasurer, labelStyle)
        if (compareShown > 0f) {
            drawPolygon(compareAnimated.map { it * compareShown }, radius, compareColor)
        }
        drawPolygon(values.map { it * grow.value }, radius, color)
    }
}

private fun DrawScope.vertex(index: Int, count: Int, distance: Float): Offset {
    val angle = 2 * PI * index / count - PI / 2
    return Offset(center.x + (distance * cos(angle)).toFloat(), center.y + (distance * sin(angle)).toFloat())
}

private fun DrawScope.drawGrid(count: Int, radius: Float, color: Color) {
    for (ring in 1..GridRings) {
        val distance = radius * ring / GridRings
        val path = Path()
        for (i in 0 until count) {
            val point = vertex(i, count, distance)
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(path, color, style = Stroke(GridStroke.toPx()))
    }
    for (i in 0 until count) drawLine(color, center, vertex(i, count, radius), GridStroke.toPx())
}

private fun DrawScope.drawPolygon(values: List<Float>, radius: Float, color: Color) {
    val path = Path()
    values.forEachIndexed { i, value ->
        val point = vertex(i, values.size, radius * (value / MaxStat).coerceIn(0f, 1f))
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(path, color.copy(alpha = FillAlpha))
    drawPath(path, color, style = Stroke(PolygonStroke.toPx()))
}

private fun DrawScope.drawLabels(
    labels: List<String>,
    distance: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle,
) {
    labels.forEachIndexed { i, label ->
        val layout = textMeasurer.measure(label, style)
        val anchor = vertex(i, labels.size, distance)
        drawText(layout, topLeft = Offset(anchor.x - layout.size.width / 2f, anchor.y - layout.size.height / 2f))
    }
}
