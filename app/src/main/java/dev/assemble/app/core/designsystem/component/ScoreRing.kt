package dev.assemble.app.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import kotlin.math.roundToInt

private const val MaxPercent = 100
private const val StartAngle = -90f
private const val FullSweep = 360f
private const val CountUpMillis = 800
private val DefaultRingSize = 112.dp
private val DefaultStrokeWidth = 8.dp

/** Anel de compatibilidade (score) com a porcentagem exata no centro. Só depois da conexão. */
@Composable
fun ScoreRing(
    percent: Int,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    size: Dp = DefaultRingSize,
    strokeWidth: Dp = DefaultStrokeWidth,
) {
    val target = percent.coerceIn(0, MaxPercent)
    val shouldAnimate = animate && rememberAnimationsEnabled()
    val progress = remember { Animatable(if (shouldAnimate) 0f else target.toFloat()) }
    LaunchedEffect(target, shouldAnimate) {
        if (shouldAnimate) {
            progress.animateTo(target.toFloat(), tween(CountUpMillis))
        } else {
            progress.snapTo(target.toFloat())
        }
    }

    val colors = AssembleTheme.colors
    val description = stringResource(R.string.score_ring_description, target)
    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = colors.border,
                startAngle = 0f,
                sweepAngle = FullSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke),
            )
            drawArc(
                color = colors.score,
                startAngle = StartAngle,
                sweepAngle = FullSweep * progress.value / MaxPercent,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = stringResource(R.string.score_ring_value, progress.value.roundToInt()),
            style = AssembleTheme.typography.displayMd,
            color = colors.text,
        )
    }
}

@PreviewLightDark
@Composable
private fun ScoreRingPreview() {
    PreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ScoreRing(percent = 82, animate = false)
            ScoreRing(percent = 45, animate = false, size = 72.dp, strokeWidth = 6.dp)
        }
    }
}
