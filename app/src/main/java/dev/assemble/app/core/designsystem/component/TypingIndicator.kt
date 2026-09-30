package dev.assemble.app.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled

private const val DotCount = 3
private const val DotPhaseMillis = 150
private const val WaveMillis = 450
private val DotSize = 8.dp
private val DotLift = 4.dp

/** Bolha da IA com 3 pontos em onda enquanto a resposta é gerada. */
@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.chat_typing)
    ChatBubbleContainer(
        author = ChatAuthor.Ai,
        showAiLabel = false,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = description
            liveRegion = LiveRegionMode.Polite
        },
    ) {
        TypingDots()
    }
}

@Composable
private fun TypingDots() {
    val animationsEnabled = rememberAnimationsEnabled()
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = Modifier.padding(vertical = AssembleTheme.spacing.space1),
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(DotCount) { index ->
            val lift by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(WaveMillis),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * DotPhaseMillis),
                ),
                label = "dot$index",
            )
            Box(
                Modifier
                    .graphicsLayer { translationY = if (animationsEnabled) -lift * DotLift.toPx() else 0f }
                    .size(DotSize)
                    .background(AssembleTheme.colors.textMuted, AssembleTheme.shapes.pill),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TypingIndicatorPreview() {
    PreviewSurface { TypingIndicator() }
}
