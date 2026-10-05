package dev.assemble.app.feature.achievements

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.AssembleLargeTopBar
import dev.assemble.app.core.designsystem.component.HexagonShape
import dev.assemble.app.core.designsystem.component.StateView
import dev.assemble.app.core.designsystem.component.StateViewType
import dev.assemble.app.core.designsystem.component.energyGradientBrush
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import dev.assemble.app.core.feedback.Cue
import dev.assemble.app.core.feedback.LocalFeedback
import dev.assemble.app.core.domain.Achievement
import dev.assemble.app.core.domain.AchievementProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

/** Só repassa o progresso do [AchievementTracker], que vive no AppContainer. */
class AchievementsViewModel(tracker: AchievementTracker) : ViewModel() {
    val progress: StateFlow<List<AchievementProgress>?> = tracker.progress
}

/** Textos e ícone de cada conquista. */
internal data class AchievementInfo(@StringRes val title: Int, @StringRes val description: Int, val icon: ImageVector)

internal fun Achievement.info(): AchievementInfo = when (this) {
    Achievement.FirstConnection -> AchievementInfo(R.string.achievement_first_connection, R.string.achievement_first_connection_desc, AssembleIcons.HeartOutline)
    Achievement.TeamUp -> AchievementInfo(R.string.achievement_team_up, R.string.achievement_team_up_desc, AssembleIcons.Users)
    Achievement.IceBreaker -> AchievementInfo(R.string.achievement_ice_breaker, R.string.achievement_ice_breaker_desc, AssembleIcons.Chat)
    Achievement.Storyteller -> AchievementInfo(R.string.achievement_storyteller, R.string.achievement_storyteller_desc, AssembleIcons.Send)
    Achievement.Explorer -> AchievementInfo(R.string.achievement_explorer, R.string.achievement_explorer_desc, AssembleIcons.Compass)
    Achievement.Crossover -> AchievementInfo(R.string.achievement_crossover, R.string.achievement_crossover_desc, AssembleIcons.Shield)
}

private const val GridColumns = 2
private const val FlipStaggerMillis = 90
private const val FlipMillis = 600
private const val ShineMillis = 900
private const val FlipStartDegrees = 180f
private const val ShineAlpha = 0.6f
private const val ShineBandStart = 0.35f
private const val ShineBandEnd = 0.65f
private const val LockedIconAlpha = 0.6f
private val BadgeWidth = 72.dp
private val BadgeHeight = 80.dp
private const val BadgeIconFraction = 0.42f
private const val CameraDistanceFactor = 12f
private val ProgressBarHeight = 6.dp
private val FlipEasing = CubicBezierEasing(0.34f, 1.4f, 0.5f, 1f)

@Composable
fun AchievementsRoute(viewModel: AchievementsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    AchievementsScreen(progress = progress, onBack = onBack, modifier = modifier)
}

/** Conquistas em insígnias hexagonais; as desbloqueadas viram e brilham ao abrir a tela. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(progress: List<AchievementProgress>?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = AssembleTheme.spacing
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AssembleTheme.colors.bg,
        topBar = { AssembleLargeTopBar(title = stringResource(R.string.achievements_title), onBack = onBack, scrollBehavior = scrollBehavior) },
    ) { padding ->
        if (progress == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                StateView(StateViewType.Loading(), modifier = Modifier.fillMaxWidth())
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space5),
        ) {
            Summary(progress)
            var unlockedIndex = 0
            progress.chunked(GridColumns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    row.forEach { item ->
                        val order = if (item.unlocked) unlockedIndex++ else 0
                        BadgeCard(item, revealOrder = order, modifier = Modifier.weight(1f))
                    }
                    if (row.size < GridColumns) Box(Modifier.weight(1f))
                }
            }
            if (progress.any { it.current == null }) {
                Text(stringResource(R.string.achievements_teams_unknown), style = AssembleTheme.typography.caption, color = AssembleTheme.colors.textMuted)
            }
        }
    }
}

@Composable
private fun Summary(progress: List<AchievementProgress>) {
    val colors = AssembleTheme.colors
    val unlocked = progress.count { it.unlocked }
    Column(verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2)) {
        Text(
            text = stringResource(R.string.achievements_summary, unlocked, progress.size),
            style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.text,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(ProgressBarHeight)
                .clip(AssembleTheme.shapes.pill)
                .background(colors.border),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(unlocked.toFloat() / progress.size)
                    .height(ProgressBarHeight)
                    .background(colors.actionAssemble, AssembleTheme.shapes.pill),
            )
        }
    }
}

@Composable
private fun BadgeCard(item: AchievementProgress, revealOrder: Int, modifier: Modifier = Modifier) {
    val colors = AssembleTheme.colors
    val info = item.achievement.info()
    val title = stringResource(info.title)
    val description = stringResource(info.description)
    val status = when {
        item.unlocked -> stringResource(R.string.achievement_unlocked_state)
        item.current == null -> stringResource(R.string.achievement_unknown_state)
        else -> stringResource(R.string.achievement_progress, item.current, item.achievement.target)
    }
    Column(
        modifier = modifier
            .clip(AssembleTheme.shapes.md)
            .background(colors.surface)
            .padding(AssembleTheme.spacing.space4)
            .clearAndSetSemantics { contentDescription = "$title. $description. $status" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2),
    ) {
        HexBadge(icon = info.icon, unlocked = item.unlocked, revealOrder = revealOrder, tickOnFlip = true)
        Text(title, style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.text, textAlign = TextAlign.Center)
        Text(description, style = AssembleTheme.typography.small, color = colors.textMuted, textAlign = TextAlign.Center)
        Text(
            text = status,
            style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.SemiBold),
            color = if (item.unlocked) colors.accentText else colors.textMuted,
        )
    }
}

/** Insígnia: desbloqueada vira (eixo Y) e uma faixa de brilho atravessa; bloqueada fica parada e apagada. */
@Composable
internal fun HexBadge(
    icon: ImageVector,
    unlocked: Boolean,
    revealOrder: Int,
    modifier: Modifier = Modifier,
    badgeSize: Pair<Dp, Dp> = BadgeWidth to BadgeHeight,
    tickOnFlip: Boolean = false,
) {
    val colors = AssembleTheme.colors
    val feedback = LocalFeedback.current
    val animate = unlocked && rememberAnimationsEnabled()
    val flip = remember { Animatable(if (animate) FlipStartDegrees else 0f) }
    val shine = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        delay((revealOrder * FlipStaggerMillis).toLong())
        if (tickOnFlip) feedback.play(Cue.Tick)
        flip.animateTo(0f, tween(FlipMillis, easing = FlipEasing))
        shine.animateTo(1f, tween(ShineMillis, easing = EaseInOut))
    }
    val fill: Brush = if (unlocked) energyGradientBrush(colors) else Brush.linearGradient(listOf(colors.border, colors.border))
    Box(
        modifier = modifier
            .size(badgeSize.first, badgeSize.second)
            .graphicsLayer {
                rotationY = flip.value
                cameraDistance = CameraDistanceFactor * density
            }
            .clip(HexagonShape)
            .background(fill)
            .drawWithContent {
                drawContent()
                val p = shine.value
                if (p in 0.001f..0.999f) {
                    val band = size.width * 2.5f
                    val startX = size.width * (1.4f - 2f * p) - band / 2
                    drawRect(
                        Brush.linearGradient(
                            ShineBandStart to Color.Transparent,
                            0.5f to Color.White.copy(alpha = ShineAlpha),
                            ShineBandEnd to Color.Transparent,
                            start = Offset(startX, 0f),
                            end = Offset(startX + band, band * 0.47f),
                        ),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (unlocked) Color.White else colors.textMuted.copy(alpha = LockedIconAlpha),
            modifier = Modifier.size(badgeSize.first * BadgeIconFraction),
        )
    }
}

@PreviewLightDark
@Composable
private fun AchievementsPreview() {
    AssembleTheme {
        AchievementsScreen(
            progress = listOf(
                AchievementProgress(Achievement.FirstConnection, 1),
                AchievementProgress(Achievement.TeamUp, 3),
                AchievementProgress(Achievement.IceBreaker, 1),
                AchievementProgress(Achievement.Storyteller, 12),
                AchievementProgress(Achievement.Explorer, 30),
                AchievementProgress(Achievement.Crossover, null),
            ),
            onBack = {},
        )
    }
}
