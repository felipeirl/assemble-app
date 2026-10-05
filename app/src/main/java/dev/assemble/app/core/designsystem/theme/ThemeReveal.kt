package dev.assemble.app.core.designsystem.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.hypot

private const val RevealMillis = 650
private val RevealEasing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

/**
 * Troca de tema em círculo: o tema novo se espalha a partir do último toque (o controle de tema).
 * Tira uma foto da tela no tema antigo, aplica o novo por baixo e abre um buraco crescente na foto.
 * Sem animações do sistema, a troca é imediata.
 */
@Composable
fun ThemeRevealHost(darkTheme: Boolean, content: @Composable (darkTheme: Boolean) -> Unit) {
    val animationsEnabled = rememberAnimationsEnabled()
    val screenLayer = rememberGraphicsLayer()
    var shownDark by remember { mutableStateOf(darkTheme) }
    var snapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var lastTouch by remember { mutableStateOf(Offset.Unspecified) }
    var revealCenter by remember { mutableStateOf(Offset.Zero) }
    var screenSize by remember { mutableStateOf(IntSize.Zero) }
    val radius = remember { Animatable(0f) }
    // Holder simples (não é estado): marcado no primeiro desenho, antes dele não há foto para tirar.
    val hasDrawn = remember { booleanArrayOf(false) }

    LaunchedEffect(darkTheme) {
        if (darkTheme == shownDark) return@LaunchedEffect
        if (!animationsEnabled || !hasDrawn[0]) {
            shownDark = darkTheme
            return@LaunchedEffect
        }
        try {
            snapshot = screenLayer.toImageBitmap()
            shownDark = darkTheme
            revealCenter = if (lastTouch.isSpecified) lastTouch else Offset(screenSize.width / 2f, screenSize.height / 2f)
            radius.snapTo(0f)
            radius.animateTo(farthestCornerDistance(revealCenter, screenSize), tween(RevealMillis, easing = RevealEasing))
        } finally {
            shownDark = darkTheme
            snapshot = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { screenSize = it }
            .pointerInput(Unit) {
                // Só observa (passagem Initial, sem consumir): o toque continua indo para o app.
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.firstOrNull { it.changedToDown() }?.let { lastTouch = it.position }
                    }
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    screenLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(screenLayer)
                    hasDrawn[0] = true
                },
        ) {
            content(shownDark)
        }
        snapshot?.let { oldScreen ->
            Canvas(Modifier.fillMaxSize()) {
                val hole = Path().apply { addOval(Rect(center = revealCenter, radius = radius.value)) }
                clipPath(hole, ClipOp.Difference) { drawImage(oldScreen) }
            }
        }
    }
}

private fun farthestCornerDistance(center: Offset, size: IntSize): Float {
    val dx = maxOf(center.x, size.width - center.x)
    val dy = maxOf(center.y, size.height - center.y)
    return hypot(dx, dy)
}
