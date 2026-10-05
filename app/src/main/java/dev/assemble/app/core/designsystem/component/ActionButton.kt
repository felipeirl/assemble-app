package dev.assemble.app.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleShadow
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.assembleShadow
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import dev.assemble.app.core.feedback.Cue
import dev.assemble.app.core.feedback.LocalFeedback
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ActionButtonType { Pass, Assemble }

enum class ActionButtonSize(val diameter: Dp, val iconSize: Dp) {
    Large(diameter = 72.dp, iconSize = 32.dp),
    Small(diameter = 52.dp, iconSize = 24.dp),
}

private const val PressedScale = 0.92f
private val PassBorderWidth = 1.dp

// Explosão do Assemble: pontos de meio-tom + anel saindo do botão, coração pulsando.
private const val BurstMillis = 650
private const val BurstRingMillis = 600
private const val BurstDotCount = 18
private const val BurstAngleJitter = 0.3f
private const val BurstDotEndScale = 0.2f
private const val BurstRingStartScale = 0.3f
private const val BurstRingEndScale = 3.4f
private const val BurstRingStartAlpha = 0.9f
private const val HeartPopMillis = 500
private const val HeartPopPeakMillis = 275
private const val HeartPopStartScale = 0.7f
private const val HeartPopPeakScale = 1.35f
private val BurstDotRadius = 3.5.dp
private val BurstMinReach = 48.dp
private val BurstReachJitter = 44.dp
private val BurstRingStroke = 3.dp
private val BurstDotEasing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)

/** Um ponto da explosão: direção (radianos), alcance e cor. */
private class BurstDot(val angle: Float, val reach: Float, val color: Color)

/**
 * Botão circular de Pass (neutro) ou Assemble (única ação colorida).
 * No Assemble, o toque vibra e solta a explosão (pontos, anel e coração pulsando).
 */
@Composable
fun ActionButton(
    type: ActionButtonType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ActionButtonSize = ActionButtonSize.Large,
    enabled: Boolean = true,
) {
    val colors = AssembleTheme.colors
    val shape = AssembleTheme.shapes.pill
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) PressedScale else 1f, label = "actionButtonScale")
    val feedback = LocalFeedback.current
    val animationsEnabled = rememberAnimationsEnabled()
    val scope = rememberCoroutineScope()
    val burst = remember { Animatable(1f) }
    val heartScale = remember { Animatable(1f) }
    var burstDots by remember { mutableStateOf(emptyList<BurstDot>()) }
    val burstColors = listOf(colors.actionAssemble, colors.logoPink, colors.accentText, colors.score)
    val ringColor = colors.logoPink
    val reachMinPx: Float
    val reachJitterPx: Float
    with(LocalDensity.current) {
        reachMinPx = BurstMinReach.toPx()
        reachJitterPx = BurstReachJitter.toPx()
    }
    val handleClick: () -> Unit = {
        feedback.play(if (type == ActionButtonType.Assemble) Cue.Assemble else Cue.Pass)
        if (type == ActionButtonType.Assemble) {
            if (animationsEnabled) {
                burstDots = List(BurstDotCount) { i ->
                    BurstDot(
                        angle = (2 * PI * i / BurstDotCount).toFloat() + Random.nextFloat() * BurstAngleJitter,
                        reach = reachMinPx + Random.nextFloat() * reachJitterPx,
                        color = burstColors[i % burstColors.size],
                    )
                }
                scope.launch {
                    burst.snapTo(0f)
                    burst.animateTo(1f, tween(BurstMillis, easing = LinearEasing))
                }
                scope.launch {
                    heartScale.snapTo(HeartPopStartScale)
                    heartScale.animateTo(
                        1f,
                        keyframes {
                            durationMillis = HeartPopMillis
                            HeartPopPeakScale at HeartPopPeakMillis
                        },
                    )
                }
            }
        }
        onClick()
    }

    val (container, content) = when (type) {
        ActionButtonType.Pass -> colors.actionPass to colors.passInk
        ActionButtonType.Assemble -> colors.actionAssemble to colors.onActionAssemble
    }
    val label = stringResource(
        when (type) {
            ActionButtonType.Pass -> R.string.action_pass
            ActionButtonType.Assemble -> R.string.action_assemble
        },
    )

    Box(
        modifier = modifier
            .size(size.diameter)
            .drawWithContent {
                drawContent()
                if (burst.value < 1f) drawBurst(burst.value, burstDots, ringColor)
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .assembleShadow(AssembleShadow.Action, shape, colors)
            .clip(shape)
            .background(container)
            .then(if (type == ActionButtonType.Pass) Modifier.border(PassBorderWidth, colors.border, shape) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = label,
                onClick = handleClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = when (type) {
                ActionButtonType.Pass -> AssembleIcons.Close
                ActionButtonType.Assemble -> AssembleIcons.Heart
            },
            contentDescription = label,
            tint = content,
            modifier = Modifier
                .size(size.iconSize)
                .graphicsLayer {
                    scaleX = heartScale.value
                    scaleY = heartScale.value
                },
        )
    }
}

/** [t] vai de 0 a 1 em [BurstMillis]; anel e pontos têm curvas próprias. Desenha fora dos limites do botão. */
private fun DrawScope.drawBurst(t: Float, dots: List<BurstDot>, ringColor: Color) {
    val ringT = EaseOut.transform((t * BurstMillis / BurstRingMillis).coerceAtMost(1f))
    drawCircle(
        color = ringColor,
        radius = size.minDimension / 2 * (BurstRingStartScale + (BurstRingEndScale - BurstRingStartScale) * ringT),
        alpha = BurstRingStartAlpha * (1f - ringT),
        style = Stroke(BurstRingStroke.toPx()),
    )
    val dotT = BurstDotEasing.transform(t)
    val dotRadius = BurstDotRadius.toPx() * (1f - (1f - BurstDotEndScale) * dotT)
    dots.forEach { dot ->
        drawCircle(
            color = dot.color,
            radius = dotRadius,
            center = center + Offset(cos(dot.angle), sin(dot.angle)) * (dot.reach * dotT),
            alpha = 1f - dotT,
        )
    }
}

@PreviewLightDark
@Composable
private fun ActionButtonPreview() {
    PreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            ActionButton(ActionButtonType.Pass, onClick = {})
            ActionButton(ActionButtonType.Assemble, onClick = {})
            ActionButton(ActionButtonType.Pass, onClick = {}, size = ActionButtonSize.Small)
            ActionButton(ActionButtonType.Assemble, onClick = {}, size = ActionButtonSize.Small)
        }
    }
}
