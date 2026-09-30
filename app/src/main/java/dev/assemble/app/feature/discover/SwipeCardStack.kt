package dev.assemble.app.feature.discover

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.CharacterCardTeaser
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.ui.traitLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

enum class SwipeDirection { Pass, Assemble }

private const val SwipeThresholdFraction = 0.3f
private const val MaxRotationDegrees = 12f
private const val ExitDistanceFactor = 1.5f
private const val ExitMillis = 250
private const val ReturnDampingRatio = 0.6f
private const val BackCardMinScale = 0.95f
private const val StampRotationDegrees = 12f
private val StampBorderWidth = 3.dp

/**
 * Estado do card do topo: deslocamento animável e largura medida.
 * Os botões Pass/Assemble usam o mesmo [swipeOut] do gesto.
 */
@Stable
class SwipeCardState(private val animationsEnabled: Boolean) {
    val offset = Animatable(Offset.Zero, Offset.VectorConverter)
    var width by mutableFloatStateOf(0f)
    var isLeaving by mutableStateOf(false)
        private set

    /** 0 → 1 conforme o card se aproxima do limiar de 30% da largura. */
    val progress: Float
        get() = if (width == 0f) 0f else (abs(offset.value.x) / (width * SwipeThresholdFraction)).coerceIn(0f, 1f)

    val rotation: Float
        get() = if (width == 0f) 0f else (offset.value.x / width * MaxRotationDegrees).coerceIn(-MaxRotationDegrees, MaxRotationDegrees)

    val direction: SwipeDirection?
        get() = when {
            offset.value.x > 0f -> SwipeDirection.Assemble
            offset.value.x < 0f -> SwipeDirection.Pass
            else -> null
        }

    val passedThreshold: Boolean
        get() = width > 0f && abs(offset.value.x) > width * SwipeThresholdFraction

    suspend fun swipeOut(direction: SwipeDirection) {
        isLeaving = true
        val sign = if (direction == SwipeDirection.Assemble) 1f else -1f
        val target = Offset(sign * width * ExitDistanceFactor, offset.value.y)
        if (animationsEnabled) offset.animateTo(target, tween(ExitMillis)) else offset.snapTo(target)
    }

    suspend fun reset() {
        if (animationsEnabled) {
            offset.animateTo(Offset.Zero, spring(dampingRatio = ReturnDampingRatio))
        } else {
            offset.snapTo(Offset.Zero)
        }
    }
}

/** Dois cards visíveis: o de cima arrasta; o de trás cresce de 0.95 a 1 conforme o arrasto. */
@Composable
fun SwipeCardStack(
    cards: List<DiscoverCard>,
    swipeState: SwipeCardState,
    onSwiped: (DiscoverCard, SwipeDirection) -> Unit,
    onCardClick: (DiscoverCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        cards.getOrNull(1)?.let { back ->
            key(back.characterId) {
                DiscoverCardContent(
                    card = back,
                    onClick = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val scale = BackCardMinScale + (1f - BackCardMinScale) * swipeState.progress
                            scaleX = scale
                            scaleY = scale
                        },
                )
            }
        }
        cards.firstOrNull()?.let { top ->
            key(top.characterId) {
                TopCard(
                    card = top,
                    state = swipeState,
                    scope = scope,
                    onSwiped = onSwiped,
                    onClick = { onCardClick(top) },
                )
            }
        }
    }
}

@Composable
private fun TopCard(
    card: DiscoverCard,
    state: SwipeCardState,
    scope: CoroutineScope,
    onSwiped: (DiscoverCard, SwipeDirection) -> Unit,
    onClick: () -> Unit,
) {
    val passLabel = stringResource(R.string.action_pass)
    val assembleLabel = stringResource(R.string.action_assemble)
    fun swipe(direction: SwipeDirection) {
        if (state.isLeaving) return
        scope.launch {
            state.swipeOut(direction)
            onSwiped(card, direction)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { state.width = it.width.toFloat() }
            .graphicsLayer {
                translationX = state.offset.value.x
                translationY = state.offset.value.y
                rotationZ = state.rotation
            }
            .pointerInput(card.characterId) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        if (state.isLeaving) return@detectDragGestures
                        change.consume()
                        scope.launch { state.offset.snapTo(state.offset.value + dragAmount) }
                    },
                    onDragEnd = {
                        if (state.passedThreshold) {
                            val direction = if (state.offset.value.x.sign > 0) SwipeDirection.Assemble else SwipeDirection.Pass
                            swipe(direction)
                        } else {
                            scope.launch { state.reset() }
                        }
                    },
                    onDragCancel = { scope.launch { state.reset() } },
                )
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(passLabel) { swipe(SwipeDirection.Pass); true },
                    CustomAccessibilityAction(assembleLabel) { swipe(SwipeDirection.Assemble); true },
                )
            },
    ) {
        DiscoverCardContent(card = card, onClick = onClick, modifier = Modifier.fillMaxWidth())
        SwipeStamp(state = state, passLabel = passLabel, assembleLabel = assembleLabel)
    }
}

@Composable
private fun DiscoverCardContent(card: DiscoverCard, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    CharacterCardTeaser(
        name = card.name,
        imageUrl = card.imageUrl,
        band = card.band,
        traitsInCommon = card.traitsInCommon.map { stringResource(traitLabel(it)) },
        onClick = onClick,
        modifier = modifier,
    )
}

/** Selo "PASS" (arrasto à esquerda, text-muted) ou "ASSEMBLE" (à direita, action-assemble). */
@Composable
private fun SwipeStamp(state: SwipeCardState, passLabel: String, assembleLabel: String) {
    val direction = state.direction ?: return
    val colors = AssembleTheme.colors
    val (label, color: Color, rotation) = when (direction) {
        SwipeDirection.Pass -> Triple(passLabel, colors.textMuted, StampRotationDegrees)
        SwipeDirection.Assemble -> Triple(assembleLabel, colors.actionAssemble, -StampRotationDegrees)
    }
    Box(Modifier.fillMaxWidth().padding(AssembleTheme.spacing.space5)) {
        Text(
            text = label.uppercase(),
            style = AssembleTheme.typography.displayMd,
            color = color,
            modifier = Modifier
                .align(if (direction == SwipeDirection.Assemble) Alignment.TopStart else Alignment.TopEnd)
                .graphicsLayer {
                    alpha = state.progress
                    rotationZ = rotation
                }
                .border(StampBorderWidth, color, AssembleTheme.shapes.sm)
                .padding(horizontal = AssembleTheme.spacing.space3, vertical = AssembleTheme.spacing.space1),
        )
    }
}
