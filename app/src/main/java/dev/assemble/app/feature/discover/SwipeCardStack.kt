package dev.assemble.app.feature.discover

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
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
import dev.assemble.app.core.feedback.Cue
import dev.assemble.app.core.feedback.LocalFeedback
import dev.assemble.app.core.ui.sharedCharacterArt
import dev.assemble.app.core.ui.traitLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

enum class SwipeDirection { Pass, Assemble }

private const val SwipeThresholdFraction = 0.3f
/** Arrastar para cima além de 22% da altura abre a pré-visualização. */
private const val SwipeUpThresholdFraction = 0.22f
private const val SettleMillis = 120
internal const val MaxRotationDegrees = 12f

/** Até onde o card voa ao sair (fração da largura); o pop-up de match o traz de volta dali. */
internal const val ExitDistanceFactor = 1.5f
private const val ExitMillis = 400
/** Sai acelerando, como um card arremessado. */
private val ExitEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
private const val ReturnDampingRatio = 0.6f
private const val BackCardMinScale = 0.95f
private const val StampRotationDegrees = 12f
private val StampBorderWidth = 3.dp
private const val AssembleGlowAlpha = 0.55f
private const val PassGlowAlpha = 0.45f
private const val GlowReachFraction = 0.7f

/** Até onde o brilho passa da borda do baralho: o padding lateral do Discover, para acender a borda da tela. */
private val GlowOverflow = 16.dp

/**
 * Estado do card do topo: deslocamento animável e largura medida.
 * Gesto e botões passam pela mesma decisão (DeckContent): o card sai nas duas direções.
 */
@Stable
class SwipeCardState(private val animationsEnabled: Boolean) {
    val offset = Animatable(Offset.Zero, Offset.VectorConverter)
    var width by mutableFloatStateOf(0f)
    var height by mutableFloatStateOf(0f)
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

    /** Arrasto para cima, sem ter ido de lado o bastante para virar Pass/Assemble. */
    val passedUpThreshold: Boolean
        get() = height > 0f && -offset.value.y > height * SwipeUpThresholdFraction && !passedThreshold

    /** Trava o card enquanto a decisão anda (gesto e botões ignorados). */
    fun markLeaving() {
        isLeaving = true
    }

    suspend fun swipeOut(direction: SwipeDirection) {
        isLeaving = true
        val sign = if (direction == SwipeDirection.Assemble) 1f else -1f
        val target = Offset(sign * width * ExitDistanceFactor, offset.value.y)
        if (animationsEnabled) offset.animateTo(target, tween(ExitMillis, easing = ExitEasing)) else offset.snapTo(target)
    }

    /** Volta rápido ao lugar antes de abrir a pré-visualização: a arte sai da posição de repouso. */
    suspend fun settle() {
        if (animationsEnabled) offset.animateTo(Offset.Zero, tween(SettleMillis)) else offset.snapTo(Offset.Zero)
    }

    suspend fun reset() {
        isLeaving = false
        if (animationsEnabled) {
            offset.animateTo(Offset.Zero, spring(dampingRatio = ReturnDampingRatio))
        } else {
            offset.snapTo(Offset.Zero)
        }
    }
}

/**
 * Dois cards visíveis: o de cima arrasta; o de trás cresce de 0.95 a 1 conforme o arrasto.
 * Entre os dois, a borda da tela acende (rosa à direita, cinza à esquerda) conforme a distância.
 */
@Composable
fun SwipeCardStack(
    cards: List<DiscoverCard>,
    swipeState: SwipeCardState,
    onDecide: (DiscoverCard, SwipeDirection) -> Unit,
    onCardClick: (DiscoverCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val colors = AssembleTheme.colors
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        cards.getOrNull(1)?.let { back ->
            key(back.characterId) {
                DiscoverCardContent(
                    card = back,
                    onClick = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val scale = BackCardMinScale + (1f - BackCardMinScale) * swipeState.progress
                            scaleX = scale
                            scaleY = scale
                        },
                )
            }
        }
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    when (swipeState.direction) {
                        SwipeDirection.Assemble -> drawEdgeGlow(colors.actionAssemble, AssembleGlowAlpha * swipeState.progress, rightSide = true)
                        SwipeDirection.Pass -> drawEdgeGlow(colors.textMuted, PassGlowAlpha * swipeState.progress, rightSide = false)
                        null -> Unit
                    }
                },
        )
        cards.firstOrNull()?.let { top ->
            key(top.characterId) {
                TopCard(
                    card = top,
                    state = swipeState,
                    scope = scope,
                    onDecide = onDecide,
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
    onDecide: (DiscoverCard, SwipeDirection) -> Unit,
    onClick: () -> Unit,
) {
    val passLabel = stringResource(R.string.action_pass)
    val assembleLabel = stringResource(R.string.action_assemble)
    val feedback = LocalFeedback.current
    fun swipe(direction: SwipeDirection) {
        if (state.isLeaving) return
        feedback.play(if (direction == SwipeDirection.Assemble) Cue.Assemble else Cue.Pass)
        onDecide(card, direction)
    }
    // Um tique quando o card passa do ponto em que soltar decide.
    LaunchedEffect(state) {
        snapshotFlow { state.passedThreshold && !state.isLeaving }.collect { if (it) feedback.play(Cue.Tick) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                state.width = it.width.toFloat()
                state.height = it.height.toFloat()
            }
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
                        if (state.passedUpThreshold) {
                            scope.launch {
                                state.settle()
                                onClick()
                            }
                        } else if (state.passedThreshold) {
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
        DiscoverCardContent(
            card = card,
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            artModifier = Modifier.sharedCharacterArt(card.characterId),
        )
        SwipeStamp(state = state, passLabel = passLabel, assembleLabel = assembleLabel)
    }
}

@Composable
private fun DiscoverCardContent(
    card: DiscoverCard,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    artModifier: Modifier = Modifier,
) {
    CharacterCardTeaser(
        name = card.name,
        imageUrl = card.imageUrl,
        traitsInCommon = card.traitsInCommon.map { stringResource(traitLabel(it)) },
        tagline = card.tagline,
        onClick = onClick,
        modifier = modifier,
        artModifier = artModifier,
    )
}

/** Brilho radial alongado na vertical, centrado logo depois da borda do baralho. */
private fun DrawScope.drawEdgeGlow(color: Color, alpha: Float, rightSide: Boolean) {
    if (alpha <= 0f) return
    val overflow = GlowOverflow.toPx()
    val center = Offset(if (rightSide) size.width + overflow else -overflow, size.height / 2)
    val radius = size.width * GlowReachFraction
    val stretch = (size.height / (2 * radius)).coerceAtLeast(1f)
    scale(scaleX = 1f, scaleY = stretch, pivot = center) {
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center, radius),
            radius = radius,
            center = center,
        )
    }
}

/** Selo branco sobre a arte: "PASS" (arrasto à esquerda) ou "ASSEMBLE" (à direita). */
@Composable
private fun SwipeStamp(state: SwipeCardState, passLabel: String, assembleLabel: String) {
    val direction = state.direction ?: return
    val color = Color.White
    val (label, rotation) = when (direction) {
        SwipeDirection.Pass -> passLabel to StampRotationDegrees
        SwipeDirection.Assemble -> assembleLabel to -StampRotationDegrees
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
