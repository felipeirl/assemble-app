package dev.assemble.app.feature.splash

import kotlinx.coroutines.delay
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.LogoAnchor
import dev.assemble.app.core.feedback.Cue
import dev.assemble.app.core.feedback.LocalFeedback
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import kotlin.math.hypot
import kotlin.math.roundToInt

// Linha do tempo (ms, 2,35 s). Começo da proposta A (encontro), final da B (encaixe no logo da tela).
private const val TotalMillis = 2_350
private const val BodyMillis = 550
private const val EyesStart = 200
private const val EyesMillis = 500
private const val BurstStart = 550
private const val BurstMillis = 950
private const val Ring1Start = 550
private const val Ring2Start = 680
private const val RingMillis = 700
private const val LettersStart = 780
private const val LetterStaggerMillis = 45
private const val LetterMillis = 500
private const val WordOutStart = 1_700
private const val WordOutMillis = 250
private const val DockStart = 1_750
private const val SoundStart = 1_500
private const val DockMillis = 600
private const val BgFadeStart = 1_850
private const val BgFadeMillis = 500

// Composição (frações da tela) e movimento das peças, em unidades do viewBox do logo (742 × 932).
private const val LogoWidthFraction = 0.36f
private const val LogoCenterY = 0.42f
private const val BurstCenterY = 0.46f
private const val WordTopY = 0.61f
private const val BodyStartScale = 0.72f
private const val BodyRise = 24f
private const val EyeTravel = 260f
private const val EyeTurnDegrees = 30f
private const val BurstReach = 0.6f
private const val BurstStartAlpha = 0.6f
private const val RingRadiusFraction = 0.45f
private const val RingStartScale = 0.3f
private const val RingEndScale = 3.4f
private const val RingStartAlpha = 0.9f
private const val LetterRise = 1.1f
private val MaxLogoWidth = 140.dp
private val BurstDotRadius = 2.5.dp
private val BurstDotSpacing = 17.dp
private val RingStroke = 2.dp
private val FallbackLogoHeight = 44.dp
private val FallbackLogoCenterBelowStatusBar = 32.dp
private val WordFontSize = 52.sp
private val WordLineHeight = 56.sp
private val WordLetterSpacing = 0.07.em

private val BodyEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private val SnapOutEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val DockEasing = CubicBezierEasing(0.65f, 0f, 0.25f, 1f)

/**
 * Splash da marca, por cima do app no primeiro quadro após a splash nativa (fundo liso no tema escolhido).
 * A chama sobe, os dois olhos entram pelos lados, um pulso de meio-tom e anéis saem do encontro e
 * "ASSEMBLE" sobe letra por letra. Depois o logo encolhe até o logo da tela de destino ([anchor]) enquanto
 * o fundo se dissolve. Um toque encerra. Só é usada com as animações do sistema ligadas.
 */
@Composable
fun BrandSplash(anchor: LogoAnchor, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val currentOnFinished by rememberUpdatedState(onFinished)
    val elapsed = remember { Animatable(0f) }
    val art = remember { LogoArt() }
    val colors = AssembleTheme.colors
    val statusBarTop = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()

    DisposableEffect(anchor) {
        anchor.covered = true
        onDispose { anchor.covered = false }
    }
    val feedback = LocalFeedback.current
    LaunchedEffect(Unit) {
        // O arquivo de som tem o sino aos 0,6 s: começa antes do encaixe para o sino cair nele.
        delay(SoundStart.toLong())
        feedback.play(Cue.Splash)
    }
    LaunchedEffect(Unit) {
        elapsed.animateTo(TotalMillis.toFloat(), tween(TotalMillis, easing = LinearEasing))
        currentOnFinished()
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .clearAndSetSemantics {}
            .pointerInput(Unit) { detectTapGestures { currentOnFinished() } },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val t = elapsed.value
            drawRect(colors.bg, alpha = 1f - EaseInOut.transform(progress(t, BgFadeStart, BgFadeMillis)))
            drawHalftoneBurst(t, Offset(size.width / 2, size.height * BurstCenterY), colors.logoPink)

            val start = startLogoRect(size, MaxLogoWidth.toPx())
            drawRing(t, Ring1Start, start, colors.logoPink)
            drawRing(t, Ring2Start, start, colors.logoPink)

            val dock = anchor.bounds?.let(::fitLogo) ?: fallbackDockRect(size, statusBarTop)
            val dockProgress = DockEasing.transform(progress(t, DockStart, DockMillis))
            drawLogo(art, lerp(start, dock, dockProgress), t)
        }
        SplashWord(
            elapsed = elapsed,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, (constraints.maxHeight * WordTopY).roundToInt()) },
        )
    }
}

/** "ASSEMBLE" letra por letra, cada uma subindo de dentro de uma faixa recortada. */
@Composable
private fun SplashWord(elapsed: Animatable<Float, *>, modifier: Modifier = Modifier) {
    val word = stringResource(R.string.app_name).uppercase()
    val style = AssembleTheme.typography.displayXl.copy(
        fontSize = WordFontSize,
        lineHeight = WordLineHeight,
        letterSpacing = WordLetterSpacing,
        color = AssembleTheme.colors.text,
    )
    Row(
        modifier
            .graphicsLayer { alpha = 1f - progress(elapsed.value, WordOutStart, WordOutMillis) }
            .clipToBounds(),
    ) {
        word.forEachIndexed { index, letter ->
            Text(
                text = letter.toString(),
                style = style,
                modifier = Modifier.graphicsLayer {
                    val p = SnapOutEasing.transform(
                        progress(elapsed.value, LettersStart + index * LetterStaggerMillis, LetterMillis),
                    )
                    alpha = p
                    translationY = (1f - p) * size.height * LetterRise
                },
            )
        }
    }
}

/** Fração de 0 a 1 de uma etapa que começa em [start] e dura [duration]. */
private fun progress(elapsed: Float, start: Int, duration: Int): Float =
    ((elapsed - start) / duration).coerceIn(0f, 1f)

private fun startLogoRect(screen: Size, maxWidth: Float): Rect {
    val width = minOf(screen.width * LogoWidthFraction, maxWidth)
    val height = width * LogoArt.ViewportHeight / LogoArt.ViewportWidth
    return Rect(Offset(screen.width / 2 - width / 2, screen.height * LogoCenterY - height / 2), Size(width, height))
}

/** O logo desenhado dentro de [bounds] (ContentScale.Fit, centralizado), como o Image faz na top bar e no Login. */
private fun fitLogo(bounds: Rect): Rect {
    val scale = minOf(bounds.width / LogoArt.ViewportWidth, bounds.height / LogoArt.ViewportHeight)
    val size = Size(LogoArt.ViewportWidth * scale, LogoArt.ViewportHeight * scale)
    return Rect(Offset(bounds.center.x - size.width / 2, bounds.center.y - size.height / 2), size)
}

/** Sem logo na tela de destino: pousa onde fica o logo da top bar. */
private fun DrawScope.fallbackDockRect(screen: Size, statusBarTop: Float): Rect {
    val height = FallbackLogoHeight.toPx()
    val width = height * LogoArt.ViewportWidth / LogoArt.ViewportHeight
    val centerY = statusBarTop + FallbackLogoCenterBelowStatusBar.toPx()
    return Rect(Offset(screen.width / 2 - width / 2, centerY - height / 2), Size(width, height))
}

/** Pontos de meio-tom revelados por um círculo que cresce a partir do encontro, esmaecendo. */
private fun DrawScope.drawHalftoneBurst(elapsed: Float, center: Offset, color: Color) {
    val p = EaseOut.transform(progress(elapsed, BurstStart, BurstMillis))
    if (p <= 0f || p >= 1f) return
    val radius = p * hypot(size.width, size.height) * BurstReach
    val alpha = BurstStartAlpha * (1f - p)
    val step = BurstDotSpacing.toPx()
    val dotRadius = BurstDotRadius.toPx()
    val firstColumn = ((center.x - radius) / step).toInt().coerceAtLeast(0)
    val lastColumn = ((center.x + radius) / step).toInt()
    val firstRow = ((center.y - radius) / step).toInt().coerceAtLeast(0)
    val lastRow = ((center.y + radius) / step).toInt()
    for (row in firstRow..lastRow) {
        for (column in firstColumn..lastColumn) {
            val dot = Offset(column * step + step / 2, row * step + step / 2)
            if ((dot - center).getDistance() <= radius) drawCircle(color, dotRadius, dot, alpha)
        }
    }
}

private fun DrawScope.drawRing(elapsed: Float, start: Int, logo: Rect, color: Color) {
    val p = EaseOut.transform(progress(elapsed, start, RingMillis))
    if (p <= 0f || p >= 1f) return
    drawCircle(
        color = color,
        radius = logo.width * RingRadiusFraction * (RingStartScale + (RingEndScale - RingStartScale) * p),
        center = logo.center,
        alpha = RingStartAlpha * (1f - p),
        style = Stroke(RingStroke.toPx()),
    )
}

/** Desenha as três peças em [rect], cada uma no seu ponto da animação de montagem. */
private fun DrawScope.drawLogo(art: LogoArt, rect: Rect, elapsed: Float) {
    withTransform({
        translate(rect.left, rect.top)
        scale(rect.width / LogoArt.ViewportWidth, rect.height / LogoArt.ViewportHeight, pivot = Offset.Zero)
    }) {
        val bodyProgress = BodyEasing.transform(progress(elapsed, 0, BodyMillis))
        if (bodyProgress > 0f) {
            val scale = BodyStartScale + (1f - BodyStartScale) * bodyProgress
            withTransform({
                translate(0f, (1f - bodyProgress) * BodyRise)
                scale(scale, scale, pivot = art.bodyCenter)
            }) {
                drawPath(art.body, art.bodyBrush, alpha = bodyProgress)
            }
        }
        val eyesProgress = SnapOutEasing.transform(progress(elapsed, EyesStart, EyesMillis))
        drawEye(art.eyeLeft, art.slitLeft, art.eyeLeftCenter, eyesProgress, side = -1f)
        drawEye(art.eyeRight, art.slitRight, art.eyeRightCenter, eyesProgress, side = 1f)
    }
}

/** O olho entra do lado dele ([side] = -1 esquerda, 1 direita), girando até o lugar. */
private fun DrawScope.drawEye(eye: Path, slit: Path, center: Offset, p: Float, side: Float) {
    if (p <= 0f) return
    withTransform({
        translate(side * EyeTravel * (1f - p), 0f)
        rotate(side * EyeTurnDegrees * (1f - p), pivot = center)
    }) {
        drawPath(eye, LogoArt.EyeInk, alpha = p)
        drawPath(slit, LogoArt.SlitWhite, alpha = p)
    }
}

/** O logo (res/drawable/ic_assemble_logo.xml) separado em corpo e olhos, com os mesmos traços e cores. */
private class LogoArt {
    val body = path(BODY)
    val eyeLeft = path(EYE_LEFT)
    val slitLeft = path(SLIT_LEFT)
    val eyeRight = path(EYE_RIGHT)
    val slitRight = path(SLIT_RIGHT)
    val bodyCenter = body.getBounds().center
    val eyeLeftCenter = eyeLeft.getBounds().center
    val eyeRightCenter = eyeRight.getBounds().center
    val bodyBrush = Brush.radialGradient(
        0.09f to LogoPink,
        0.59f to LogoRed,
        center = Offset(BodyGradientCenterX, BodyGradientCenterY),
        radius = BodyGradientRadius,
    )

    companion object {
        const val ViewportWidth = 742f
        const val ViewportHeight = 932f

        // Cores da arte do logo (iguais ao vetor); não são tokens de UI.
        val LogoPink = Color(0xFFFF3475)
        val LogoRed = Color(0xFFFF114B)
        val EyeInk = Color(0xFF1A1819)
        val SlitWhite = Color(0xFFFDFDFD)
        private const val BodyGradientCenterX = 351.986f
        private const val BodyGradientCenterY = 906.861f
        private const val BodyGradientRadius = 800f

        private fun path(data: String): Path = PathParser().parsePathString(data).toPath()

        private const val BODY = "M211.192 381.619C209.985 381.619 209.18 381.619 208.376 380.744C181.021 341.334 174.183 273.462 172.574 247.627C172.172 242.591 166.942 239.964 162.919 242.372C78.4427 293.824 0 415.993 0 533.784C0 736.743 129.33 906.861 351.986 906.861C560.563 906.861 703.972 731.708 703.972 534.003C703.972 275.433 534.416 104.22 383.363 26.7146C379.341 24.5252 374.916 28.2472 375.318 32.845C395.029 172.311 368.077 323.6 210.991 382.057L211.192 381.619Z"
        private const val EYE_LEFT = "M48.1471 469C69.7334 410.256 103.742 374.052 150.699 396.644C206.753 424.093 264.199 487.144 305.699 588.644C330.231 648.644 343.5 737.5 320.195 809.109C302.844 851.1 263 860 224.647 846C201.743 837.639 125.647 798.5 71.647 696.5C21.6471 623.5 29.4298 519.356 48.1471 469Z"
        private const val SLIT_LEFT = "M89.6475 577C119.921 593.484 209.087 642.789 271.147 684C246.929 690.594 184.394 692.242 157.147 684C134.442 675.758 98.7295 626.453 89.6475 577Z"
        private const val EYE_RIGHT = "M656.883 452.588C635.297 393.844 595.095 357.486 548.137 380.078C492.083 407.527 435.137 473.578 393.637 575.078C369.105 635.078 355.729 737.536 384.637 808.078C401.988 850.07 430 863.5 467.137 847.078C489.5 836 574.637 795.078 628.637 693.078C678.637 620.078 675.6 502.944 656.883 452.588Z"
        private const val SLIT_RIGHT = "M609.637 576.078C579.364 592.563 490.198 641.867 428.137 683.078C452.356 689.672 514.891 691.321 542.137 683.078C564.842 674.836 600.555 625.531 609.637 576.078Z"
    }
}
