package dev.assemble.app.core.designsystem.component

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled

/** Os quatro estados obrigatórios de toda tela que carrega dados. */
sealed interface StateViewType {
    data class Loading(val message: String? = null) : StateViewType
    data class Empty(
        val icon: ImageVector,
        val title: String,
        val message: String? = null,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null,
    ) : StateViewType
    data class Unavailable(val title: String, val message: String) : StateViewType
    data class Error(val title: String, val message: String, val onRetry: () -> Unit) : StateViewType
}

private const val ShimmerMillis = 1_500
private const val ShimmerBandFactor = 2.5f
private const val ShimmerSlope = 0.47f // ≈ tan(25°): faixa a 115°, como no resto da marca
private const val ShimmerStartCenter = 1.3f
private const val ShimmerEndCenter = -0.3f
private const val ShimmerAlpha = 0.32f
private const val SkeletonDotsAlpha = 0.35f
private val SkeletonDotRadius = 1.1.dp
private val SkeletonDotSpacing = 9.dp
private const val SkeletonShortFraction = 0.6f
private val StateMaxWidth = 280.dp
private val StateIconSize = 40.dp
private val SkeletonLargeHeight = 120.dp
private val SkeletonLineHeight = 16.dp

@Composable
fun StateView(type: StateViewType, modifier: Modifier = Modifier) {
    val spacing = AssembleTheme.spacing
    Column(
        modifier = modifier
            .widthIn(max = StateMaxWidth)
            .padding(horizontal = spacing.space4, vertical = spacing.space5)
            .then(if (type is StateViewType.Error) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        when (type) {
            is StateViewType.Loading -> LoadingContent(type.message)
            is StateViewType.Empty -> MessageContent(
                icon = type.icon,
                title = type.title,
                message = type.message,
                isError = false,
                actionLabel = type.actionLabel,
                onAction = type.onAction,
            )
            is StateViewType.Unavailable -> MessageContent(
                icon = AssembleIcons.Unavailable,
                title = type.title,
                message = type.message,
                isError = false,
            )
            is StateViewType.Error -> MessageContent(
                icon = AssembleIcons.Error,
                title = type.title,
                message = type.message,
                isError = true,
                actionLabel = stringResource(R.string.state_try_again),
                onAction = type.onRetry,
            )
        }
    }
}

@Composable
private fun LoadingContent(message: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { message?.let { contentDescription = it } },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2),
    ) {
        SkeletonBlock(Modifier.fillMaxWidth().height(SkeletonLargeHeight))
        SkeletonBlock(Modifier.fillMaxWidth(SkeletonShortFraction).height(SkeletonLineHeight))
        if (message != null) {
            Text(
                text = message,
                style = AssembleTheme.typography.caption,
                color = AssembleTheme.colors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MessageContent(
    icon: ImageVector,
    title: String,
    message: String?,
    isError: Boolean,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = AssembleTheme.colors
    val accent = if (isError) colors.error else colors.textMuted
    Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(StateIconSize))
    Text(
        text = title,
        style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
        color = if (isError) colors.error else colors.text,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
    if (message != null) {
        Text(
            text = message,
            style = AssembleTheme.typography.caption,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
        )
    }
    if (actionLabel != null && onAction != null) {
        SecondaryButton(
            text = actionLabel,
            onClick = onAction,
            modifier = Modifier.padding(top = AssembleTheme.spacing.space2),
        )
    }
}

/**
 * Bloco de skeleton da marca: base border, uma faixa diagonal rosa que atravessa o bloco
 * e o meio-tom por cima. Estático (sem faixa) se as animações estiverem desligadas.
 */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier) {
    val colors = AssembleTheme.colors
    val animationsEnabled = rememberAnimationsEnabled()
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(ShimmerMillis, easing = EaseInOut), RepeatMode.Restart),
        label = "shimmerProgress",
    )
    Box(
        modifier
            .clip(AssembleTheme.shapes.md)
            .drawWithCache {
                val bandWidth = size.width * ShimmerBandFactor
                val highlight = colors.accentText.copy(alpha = ShimmerAlpha)
                onDrawBehind {
                    drawRect(colors.border)
                    if (animationsEnabled) {
                        val center = size.width * (ShimmerStartCenter + (ShimmerEndCenter - ShimmerStartCenter) * progress)
                        val startX = center - bandWidth / 2
                        drawRect(
                            brush = Brush.linearGradient(
                                0.35f to Color.Transparent,
                                0.5f to highlight,
                                0.65f to Color.Transparent,
                                start = Offset(startX, 0f),
                                end = Offset(startX + bandWidth, bandWidth * ShimmerSlope),
                            ),
                        )
                    }
                }
            }
            .halftone(
                color = colors.bg,
                dotRadius = SkeletonDotRadius,
                spacing = SkeletonDotSpacing,
                alpha = SkeletonDotsAlpha,
                fade = HalftoneFade.None,
            ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@PreviewLightDark
@Composable
private fun StateViewPreview() {
    PreviewSurface {
        FlowRow {
            StateView(StateViewType.Loading("Loading characters…"))
            StateView(
                StateViewType.Empty(
                    icon = AssembleIcons.Chat,
                    title = "No connections yet",
                    message = "Assemble with characters in Discover to start a chat.",
                    actionLabel = "Go to Discover",
                    onAction = {},
                ),
            )
            StateView(
                StateViewType.Unavailable(
                    title = "Character unavailable",
                    message = "Comic Vine didn't return this character. It may have been removed.",
                ),
            )
            StateView(
                StateViewType.Error(
                    title = "Couldn't load characters",
                    message = "Check your connection and try again.",
                    onRetry = {},
                ),
            )
        }
    }
}
