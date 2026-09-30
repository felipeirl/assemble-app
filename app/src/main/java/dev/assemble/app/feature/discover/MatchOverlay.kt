package dev.assemble.app.feature.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.CharacterAvatar
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.component.ScoreRing
import dev.assemble.app.core.designsystem.component.SecondaryButton
import dev.assemble.app.core.designsystem.component.TraitChip
import dev.assemble.app.core.designsystem.component.energyGradient
import dev.assemble.app.core.designsystem.component.halftone
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.ThemeMode
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import dev.assemble.app.core.ui.traitLabel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AvatarSlideMillis = 400
private const val BurstMillis = 600
private const val BurstRings = 3
private const val BurstRingDelay = 0.15f
private const val BurstMaxAlpha = 0.5f
private const val ChipStaggerMillis = 60L
private const val GradientAlpha = 0.4f
private const val StartTitleScale = 0.8f
private val AvatarSize = 96.dp
private val AvatarBorder = 3.dp
private val AvatarTravel = 360.dp
private val BurstMaxRadius = 320.dp
private val ArenaHeight = 200.dp
private val ScoreRingSize = 120.dp

/**
 * Pop-up de match em tela cheia (~1,6 s de animação). Sempre no tema escuro: fundo midnight.
 * Não é uma rota: o estado vem do DiscoverViewModel.
 */
@Composable
fun MatchOverlay(
    match: DiscoverMatch,
    onStartChat: () -> Unit,
    onKeepDiscovering: () -> Unit,
) {
    Dialog(
        onDismissRequest = onKeepDiscovering,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        AssembleTheme(ThemeMode.Dark) {
            MatchOverlayContent(match = match, onStartChat = onStartChat, onKeepDiscovering = onKeepDiscovering)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MatchOverlayContent(
    match: DiscoverMatch,
    onStartChat: () -> Unit,
    onKeepDiscovering: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val animate = rememberAnimationsEnabled()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val traitCount = match.traitsInCommon.size

    val slide = remember { Animatable(if (animate) 1f else 0f) }
    val burst = remember { Animatable(0f) }
    val titleScale = remember { Animatable(if (animate) StartTitleScale else 1f) }
    var scoreVisible by remember { mutableStateOf(!animate) }
    var visibleChips by remember { mutableIntStateOf(if (animate) 0 else traitCount) }

    LaunchedEffect(match) {
        if (!animate) return@LaunchedEffect
        // Avatares se encontram → explosão + título + vibração → score e chips em sequência.
        coroutineScope {
            slide.animateTo(0f, tween(AvatarSlideMillis, easing = FastOutSlowInEasing))
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            launch { burst.animateTo(1f, tween(BurstMillis)) }
            launch { titleScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
            scoreVisible = true
            repeat(traitCount) {
                delay(ChipStaggerMillis)
                visibleChips = it + 1
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.midnight),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = GradientAlpha }
                .energyGradient(colors)
                .halftone(color = colors.midnight),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(spacing.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space5, Alignment.CenterVertically),
        ) {
            Arena(match = match, slide = slide.value, burst = burst.value, travelPx = with(density) { AvatarTravel.toPx() })
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
                modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    text = stringResource(R.string.match_assembled),
                    style = AssembleTheme.typography.displayXl,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        scaleX = titleScale.value
                        scaleY = titleScale.value
                    },
                )
                Text(
                    text = stringResource(R.string.match_subtitle, match.name),
                    style = AssembleTheme.typography.body,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                )
            }
            if (scoreVisible) {
                ScoreRing(percent = match.score, size = ScoreRingSize)
            } else {
                Box(Modifier.size(ScoreRingSize))
            }
            if (traitCount > 0) {
                Text(
                    text = stringResource(R.string.why_you_match).uppercase(),
                    style = AssembleTheme.typography.eyebrow,
                    color = colors.textMuted,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    match.traitsInCommon.forEachIndexed { index, trait ->
                        AnimatedVisibility(
                            visible = index < visibleChips,
                            enter = fadeIn() + scaleIn(initialScale = StartTitleScale),
                        ) {
                            TraitChip(label = stringResource(traitLabel(trait)))
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.match_start_chat),
                    onClick = onStartChat,
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = stringResource(R.string.match_keep_discovering),
                    onClick = onKeepDiscovering,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Os dois avatares entram pelos lados e se encontram; no encontro, círculos expandem e somem. */
@Composable
private fun Arena(match: DiscoverMatch, slide: Float, burst: Float, travelPx: Float) {
    val colors = AssembleTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ArenaHeight),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val maxRadius = BurstMaxRadius.toPx()
            repeat(BurstRings) { ring ->
                val progress = ((burst - ring * BurstRingDelay) / (1f - ring * BurstRingDelay)).coerceIn(0f, 1f)
                if (progress > 0f && progress < 1f) {
                    drawCircle(
                        color = colors.logoPink,
                        radius = maxRadius * progress,
                        center = Offset(size.width / 2, size.height / 2),
                        alpha = (1f - progress) * BurstMaxAlpha,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2)) {
            Box(Modifier.graphicsLayer { translationX = -slide * travelPx }) {
                UserAvatar()
            }
            Box(Modifier.graphicsLayer { translationX = slide * travelPx }) {
                CharacterAvatar(
                    name = match.name,
                    imageUrl = match.imageUrl,
                    size = AvatarSize,
                    modifier = Modifier.border(AvatarBorder, colors.actionAssemble, AssembleTheme.shapes.pill),
                )
            }
        }
    }
}

@Composable
private fun UserAvatar() {
    val colors = AssembleTheme.colors
    Box(
        modifier = Modifier
            .size(AvatarSize)
            .energyGradient(colors, AssembleTheme.shapes.pill)
            .border(AvatarBorder, colors.actionAssemble, AssembleTheme.shapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AssembleIcons.ProfileFilled,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(AvatarSize / 2),
        )
    }
}
