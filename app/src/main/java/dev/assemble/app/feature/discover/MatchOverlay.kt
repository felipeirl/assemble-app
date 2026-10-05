package dev.assemble.app.feature.discover

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.AvatarPreset
import dev.assemble.app.core.designsystem.component.CharacterArt
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.component.ScoreRing
import dev.assemble.app.core.designsystem.component.UserAvatar
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.ThemeMode
import dev.assemble.app.core.feedback.Cue
import dev.assemble.app.core.feedback.LocalFeedback
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Linha do tempo (ms): o card vira avatar, o seu sobe, explosão no encontro, título, anel e botões.
private const val TotalMillis = 3_900
private const val SkipGuardMillis = 500
private const val BackdropMillis = 700
private const val GlowStart = 600
private const val GlowMillis = 1_200
private const val CardMorphMillis = 1_200
private const val UserRiseStart = 400
private const val UserRiseMillis = 1_100
private const val BurstStart = 1_450
private const val BurstGlowMillis = 1_200
private const val BurstDotsMillis = 1_100
private const val EyebrowStart = 1_800
private const val TitleStart = 1_950
private const val TitleMillis = 900
private const val SubtitleStart = 2_200
private const val RingStart = 2_450
private const val ButtonsStart = 3_200
private const val FadeUpMillis = 700

private const val AvatarTiltDegrees = 8f
private const val UserStartTiltDegrees = -20f
private const val UserStartScale = 0.6f
private const val BurstDotCount = 24
private const val BurstGlowEndScale = 16f
private const val BurstGlowStartAlpha = 0.8f
private const val LabelAlpha = 0.7f
private const val PairGlowPinkAlpha = 0.42f
private const val PairGlowBlueAlpha = 0.16f
private const val BurstAngleJitter = 0.3f
private const val BurstDotEndScale = 0.3f
private const val BurstCoreAlpha = 0.9f
private const val BurstEdgeAlpha = 0.4f
private const val SubtitleAlpha = 0.85f
private const val FallbackCardWidthFraction = 0.84f
private const val FallbackCardHeightFraction = 0.6f
private val AvatarSize = 96.dp
private val AvatarOverlap = 14.dp
private val AvatarRing = 3.dp
private val CardCornerRadius = 20.dp
private val PairHeight = 144.dp
private val ScoreRingSize = 104.dp
private val TitleSlide = 40.dp
private val FadeUpLift = 14.dp
private val BurstGlowSize = 32.dp
private val BurstDotRadius = 3.dp
private val BurstMinReach = 70.dp
private val BurstReachJitter = 60.dp
private val GlowSize = 360.dp
private val SecondaryMinHeight = 48.dp

private val MorphEasing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
private val ArriveEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/** Fração de 0 a 1 de uma etapa que começa em [start] e dura [duration]. */
private fun stage(t: Float, start: Int, duration: Int): Float = ((t - start) / duration).coerceIn(0f, 1f)

/** Um ponto da explosão: direção (radianos), alcance (px) e cor. */
private class BurstDot(val angle: Float, val reach: Float, val color: Color)

/**
 * Pop-up de match (forma F). O card do personagem volta de onde saiu no Discover ([cardOffset], [cardRotation]) e vira o avatar,
 * o seu avatar sobe do botão Assemble, e no encontro acontece a explosão. Depois "It's an ASSEMBLE!",
 * o nome, o anel de compatibilidade e os botões. Cerca de 4 s; um toque adianta para o fim.
 * Sempre no tema escuro. [cardBounds] e [assembleBounds] vêm da janela do app (null usa uma posição padrão).
 */
@Composable
fun MatchOverlay(
    match: DiscoverMatch,
    userAvatarPreset: AvatarPreset,
    onStartChat: () -> Unit,
    onKeepDiscovering: () -> Unit,
    cardBounds: Rect? = null,
    assembleBounds: Rect? = null,
    cardOffset: Offset = Offset.Zero,
    cardRotation: Float = 0f,
) {
    Dialog(
        onDismissRequest = onKeepDiscovering,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // Sem escurecimento do sistema: o fundo midnight entra pela própria animação, por cima do Discover.
        (LocalView.current.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
        AssembleTheme(ThemeMode.Dark) {
            MatchOverlayContent(
                match = match,
                userAvatarPreset = userAvatarPreset,
                onStartChat = onStartChat,
                onKeepDiscovering = onKeepDiscovering,
                cardBounds = cardBounds,
                assembleBounds = assembleBounds,
                cardOffset = cardOffset,
                cardRotation = cardRotation,
            )
        }
    }
}

@Composable
internal fun MatchOverlayContent(
    match: DiscoverMatch,
    userAvatarPreset: AvatarPreset,
    onStartChat: () -> Unit,
    onKeepDiscovering: () -> Unit,
    modifier: Modifier = Modifier,
    cardBounds: Rect? = null,
    assembleBounds: Rect? = null,
    cardOffset: Offset = Offset.Zero,
    cardRotation: Float = 0f,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val density = LocalDensity.current
    val feedback = LocalFeedback.current
    val animate = rememberAnimationsEnabled()
    val scope = rememberCoroutineScope()

    val time = remember { Animatable(if (animate) 0f else TotalMillis.toFloat()) }
    var skipped by remember { mutableStateOf(!animate) }
    var rootOrigin by remember { mutableStateOf<Offset?>(null) }
    var rootSize by remember { mutableStateOf(Size.Zero) }
    var userSlot by remember { mutableStateOf<Rect?>(null) }
    var characterSlot by remember { mutableStateOf<Rect?>(null) }
    val burstColors = listOf(colors.actionAssemble, colors.logoPink, colors.accentText, colors.score)
    val dots = remember {
        val minReach = with(density) { BurstMinReach.toPx() }
        val jitter = with(density) { BurstReachJitter.toPx() }
        List(BurstDotCount) { i ->
            BurstDot(
                angle = (2 * PI * i / BurstDotCount).toFloat() + Random.nextFloat() * BurstAngleJitter,
                reach = minReach + Random.nextFloat() * jitter,
                color = burstColors[i % burstColors.size],
            )
        }
    }

    val ready = userSlot != null && characterSlot != null && rootOrigin != null
    // Quem esperou a resposta costuma estar tocando na tela: o primeiro toque não pode pular tudo.
    var tapsEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        delay(SkipGuardMillis.toLong())
        tapsEnabled = true
    }
    LaunchedEffect(ready) {
        if (!ready || skipped) return@LaunchedEffect
        time.animateTo(BurstStart.toFloat(), tween(BurstStart, easing = LinearEasing))
        feedback.play(Cue.Match)
        time.animateTo(TotalMillis.toFloat(), tween(TotalMillis - BurstStart, easing = LinearEasing))
    }
    val t = time.value
    // Antes de medir, nada aparece: evita um quadro com o fundo sem os avatares.
    val visible = ready || skipped

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                val bounds = it.boundsInWindow()
                rootOrigin = bounds.topLeft
                rootSize = bounds.size
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    if (tapsEnabled && time.value < TotalMillis) {
                        skipped = true
                        scope.launch { time.snapTo(TotalMillis.toFloat()) }
                    }
                }
            },
    ) {
        val now = if (skipped) TotalMillis.toFloat() else t
        // Fundo midnight e o brilho rosa e azul da forma B.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (visible) stage(now, 0, BackdropMillis) else 0f }
                .background(colors.midnight),
        )
        val glow = EaseOut.transform(stage(now, GlowStart, GlowMillis))
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = glow
                    val scale = 0.5f + 0.5f * glow
                    scaleX = scale
                    scaleY = scale
                }
                .drawPairGlow(characterSlot, userSlot, rootOrigin, colors.logoPink, colors.score),
        )

        // Rolável só quando não cabe (tela pequena ou fonte grande); centralizado quando cabe.
        BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(spacing.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space6, Alignment.CenterVertically),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
                modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            ) {
                FadeUp(now, EyebrowStart) {
                    Text(
                        text = stringResource(R.string.match_its_an).uppercase(),
                        style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.em),
                        color = colors.text,
                    )
                }
                val title = ArriveEasing.transform(stage(now, TitleStart, TitleMillis))
                Text(
                    text = stringResource(R.string.match_assembled).uppercase(),
                    style = AssembleTheme.typography.displayXl.copy(
                        brush = Brush.horizontalGradient(listOf(colors.logoPink, colors.heroRed)),
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        alpha = title
                        translationX = (title - 1f) * TitleSlide.toPx()
                    },
                )
                FadeUp(now, SubtitleStart) {
                    Text(
                        text = stringResource(R.string.match_subtitle, match.name),
                        style = AssembleTheme.typography.body,
                        color = colors.text.copy(alpha = SubtitleAlpha),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            // Lugar dos dois avatares; eles são desenhados por cima, voando até aqui.
            Box(Modifier.size(width = AvatarSize * 2 - AvatarOverlap, height = PairHeight), contentAlignment = Alignment.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy(-AvatarOverlap)) {
                    Spacer(Modifier.size(AvatarSize).onGloballyPositioned { userSlot = it.boundsInWindow() })
                    Spacer(Modifier.size(AvatarSize).onGloballyPositioned { characterSlot = it.boundsInWindow() })
                }
            }
            FadeUp(now, RingStart) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    Text(
                        text = stringResource(R.string.match_compatibility).uppercase(),
                        style = AssembleTheme.typography.eyebrow,
                        color = colors.text.copy(alpha = LabelAlpha),
                    )
                    if (now >= RingStart) {
                        ScoreRing(percent = match.score, size = ScoreRingSize, animate = !skipped)
                    } else {
                        Spacer(Modifier.size(ScoreRingSize))
                    }
                }
            }
            FadeUp(now, ButtonsStart) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.match_start_chat),
                        onClick = onStartChat,
                        enabled = now >= ButtonsStart,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.match_keep_discovering),
                        style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.text.copy(alpha = SubtitleAlpha),
                        modifier = Modifier
                            .defaultMinSize(minHeight = SecondaryMinHeight)
                            .clip(AssembleTheme.shapes.pill)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = now >= ButtonsStart,
                                role = Role.Button,
                                onClick = onKeepDiscovering,
                            )
                            .padding(horizontal = spacing.space4, vertical = spacing.space3),
                    )
                }
            }
        }

        }
        // Camada dos avatares e da explosão, em coordenadas da raiz.
        val origin = rootOrigin
        val userTarget = userSlot
        val characterTarget = characterSlot
        if (visible && origin != null && userTarget != null && characterTarget != null) {
            val screen = Rect(Offset.Zero, rootSize)
            // Parte de onde o card saiu no Discover (fora da tela, à direita, inclinado).
            val cardStart = (cardBounds ?: fallbackCardBounds(screen, origin)).translate(cardOffset - origin)
            val characterEnd = characterTarget.translate(-origin)
            val userEnd = userTarget.translate(-origin)
            val junction = Offset((userEnd.right + characterEnd.left) / 2, userEnd.center.y)

            BurstLayer(now, junction, dots, colors.logoPink)

            val rise = ArriveEasing.transform(stage(now, UserRiseStart, UserRiseMillis))
            val userStartCenter = (assembleBounds?.center ?: Offset(screen.width / 2, screen.height)) - origin
            val userCenter = lerp(userStartCenter, userEnd.center, rise)
            UserAvatar(
                preset = userAvatarPreset,
                size = AvatarSize,
                modifier = Modifier
                    .absolutePosition(userCenter, userEnd.size)
                    .graphicsLayer {
                        alpha = rise
                        val scale = UserStartScale + (1f - UserStartScale) * rise
                        scaleX = scale
                        scaleY = scale
                        rotationZ = UserStartTiltDegrees + (-AvatarTiltDegrees - UserStartTiltDegrees) * rise
                    }
                    .border(AvatarRing, Color.White.copy(alpha = rise), AssembleTheme.shapes.pill),
            )

            val morph = MorphEasing.transform(stage(now, 0, CardMorphMillis))
            val cardRect = lerp(cardStart, characterEnd, morph)
            val cornerPx = with(density) { CardCornerRadius.toPx() } + (cardRect.minDimension / 2 - with(density) { CardCornerRadius.toPx() }) * morph
            val shape = RoundedCornerShape(with(density) { cornerPx.toDp() })
            Box(
                Modifier
                    .absolutePosition(cardRect.center, cardRect.size)
                    .graphicsLayer { rotationZ = cardRotation + (AvatarTiltDegrees - cardRotation) * morph }
                    .clip(shape)
                    .border(AvatarRing, Color.White.copy(alpha = morph), shape),
            ) {
                CharacterArt(
                    name = match.name,
                    imageUrl = match.imageUrl,
                    imageAlignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** Card de tamanho típico no centro da tela, quando o Discover não informou onde o card estava. */
private fun fallbackCardBounds(screen: Rect, origin: Offset): Rect {
    val size = Size(screen.width * FallbackCardWidthFraction, screen.height * FallbackCardHeightFraction)
    return Rect(Offset((screen.width - size.width) / 2, (screen.height - size.height) / 2), size).translate(origin)
}

/** Posiciona pelo centro, em px da raiz, com o tamanho dado. */
@Composable
private fun Modifier.absolutePosition(center: Offset, size: Size): Modifier {
    val density = LocalDensity.current
    return this
        .offset { IntOffset((center.x - size.width / 2).roundToInt(), (center.y - size.height / 2).roundToInt()) }
        .size(with(density) { size.width.toDp() }, with(density) { size.height.toDp() })
}

/** Entra subindo 14dp e aparecendo, a partir de [start] ms. */
@Composable
private fun FadeUp(now: Float, start: Int, content: @Composable () -> Unit) {
    val p = ArriveEasing.transform(stage(now, start, FadeUpMillis))
    Box(
        Modifier.graphicsLayer {
            alpha = p
            translationY = (1f - p) * FadeUpLift.toPx()
        },
    ) {
        content()
    }
}

/** Brilho do encontro e os pontos de meio-tom saindo dele. */
@Composable
private fun BurstLayer(now: Float, junction: Offset, dots: List<BurstDot>, glowColor: Color) {
    val glowP = stage(now, BurstStart, BurstGlowMillis)
    val dotsP = ArriveEasing.transform(stage(now, BurstStart, BurstDotsMillis))
    if (glowP <= 0f || glowP >= 1f) return
    Canvas(Modifier.fillMaxSize()) {
        val glowRadius = BurstGlowSize.toPx() / 2 * (1f + (BurstGlowEndScale - 1f) * EaseOut.transform(glowP))
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = BurstCoreAlpha), glowColor.copy(alpha = BurstEdgeAlpha), Color.Transparent),
                center = junction,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = junction,
            alpha = BurstGlowStartAlpha * (1f - glowP),
        )
        if (dotsP < 1f) {
            val radius = BurstDotRadius.toPx() * (1f - (1f - BurstDotEndScale) * dotsP)
            dots.forEach { dot ->
                drawCircle(
                    color = dot.color,
                    radius = radius,
                    center = junction + Offset(cos(dot.angle), sin(dot.angle)) * (dot.reach * dotsP),
                    alpha = 1f - dotsP,
                )
            }
        }
    }
}

/** Brilho rosa e azul atrás do par de avatares (fundo da forma B). */
private fun Modifier.drawPairGlow(characterSlot: Rect?, userSlot: Rect?, origin: Offset?, pink: Color, blue: Color): Modifier =
    drawBehind {
        if (characterSlot == null || userSlot == null || origin == null) return@drawBehind
        val center = Offset((userSlot.right + characterSlot.left) / 2, userSlot.center.y) - origin
        val radius = GlowSize.toPx() / 2
        drawCircle(
            brush = Brush.radialGradient(
                0f to pink.copy(alpha = PairGlowPinkAlpha),
                0.45f to blue.copy(alpha = PairGlowBlueAlpha),
                1f to Color.Transparent,
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
