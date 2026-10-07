package dev.assemble.app.feature.achievements

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import dev.assemble.app.core.designsystem.theme.AssemblePalette
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import dev.assemble.app.core.domain.Achievement
import dev.assemble.app.core.domain.AchievementCategory
import dev.assemble.app.core.domain.AchievementProgress
import dev.assemble.app.core.domain.AchievementTier
import dev.assemble.app.core.domain.Reward
import dev.assemble.app.core.feedback.Cue
import dev.assemble.app.core.feedback.LocalFeedback
import dev.assemble.app.feature.profile.label
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
    Achievement.FullRoster -> AchievementInfo(R.string.achievement_full_roster, R.string.achievement_full_roster_desc, AssembleIcons.Star)
    Achievement.Legion -> AchievementInfo(R.string.achievement_legion, R.string.achievement_legion_desc, AssembleIcons.Crown)
    Achievement.IceBreaker -> AchievementInfo(R.string.achievement_ice_breaker, R.string.achievement_ice_breaker_desc, AssembleIcons.Chat)
    Achievement.RoundOfIntros -> AchievementInfo(R.string.achievement_round_of_intros, R.string.achievement_round_of_intros_desc, AssembleIcons.ChatPlus)
    Achievement.Storyteller -> AchievementInfo(R.string.achievement_storyteller, R.string.achievement_storyteller_desc, AssembleIcons.Send)
    Achievement.Diplomat -> AchievementInfo(R.string.achievement_diplomat, R.string.achievement_diplomat_desc, AssembleIcons.Chats)
    Achievement.Veteran -> AchievementInfo(R.string.achievement_veteran, R.string.achievement_veteran_desc, AssembleIcons.Book)
    Achievement.Explorer -> AchievementInfo(R.string.achievement_explorer, R.string.achievement_explorer_desc, AssembleIcons.Compass)
    Achievement.Scout -> AchievementInfo(R.string.achievement_scout, R.string.achievement_scout_desc, AssembleIcons.Eye)
    Achievement.Cartographer -> AchievementInfo(R.string.achievement_cartographer, R.string.achievement_cartographer_desc, AssembleIcons.Map)
    Achievement.Crossover -> AchievementInfo(R.string.achievement_crossover, R.string.achievement_crossover_desc, AssembleIcons.Shield)
    Achievement.Multiverse -> AchievementInfo(R.string.achievement_multiverse, R.string.achievement_multiverse_desc, AssembleIcons.Globe)
    Achievement.SecretIdentity -> AchievementInfo(R.string.achievement_secret_identity, R.string.achievement_secret_identity_desc, AssembleIcons.Mask)
    Achievement.Sentinel -> AchievementInfo(R.string.achievement_sentinel, R.string.achievement_sentinel_desc, AssembleIcons.Bolt)
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
private val CardBarHeight = 4.dp
private val FlipEasing = CubicBezierEasing(0.34f, 1.4f, 0.5f, 1f)

/** Fração da insígnia dentro do anel de raridade. */
private const val RingInnerFraction = 0.84f
private const val LockedRingAlpha = 0.35f
private val TierDotSize = 10.dp
private val RewardIconSize = 12.dp
private const val RewardChipAlpha = 0.14f
private val BronzeRing = Color(0xFFB87333)

@Composable
fun AchievementsRoute(
    viewModel: AchievementsViewModel,
    onBack: () -> Unit,
    onOpenReward: (Reward) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    AchievementsScreen(progress = progress, onBack = onBack, onOpenReward = onOpenReward, modifier = modifier)
}

/**
 * Conquistas por categoria, em insígnias hexagonais com o anel da raridade. As desbloqueadas viram e brilham
 * ao abrir a tela; tocar numa delas leva à recompensa no Editar perfil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    progress: List<AchievementProgress>?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenReward: (Reward) -> Unit = {},
) {
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
            AchievementCategory.entries.forEach { category ->
                val items = progress.filter { it.achievement.category == category }
                if (items.isEmpty()) return@forEach
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    Text(
                        text = stringResource(category.label).uppercase(),
                        style = AssembleTheme.typography.eyebrow,
                        color = AssembleTheme.colors.textMuted,
                        modifier = Modifier.semantics { heading() },
                    )
                    items.chunked(GridColumns).forEach { row ->
                        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
                            row.forEach { item ->
                                val order = if (item.unlocked) unlockedIndex++ else 0
                                BadgeCard(
                                    item = item,
                                    revealOrder = order,
                                    onOpenReward = onOpenReward,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
                            if (row.size < GridColumns) Box(Modifier.weight(1f))
                        }
                    }
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
    val spacing = AssembleTheme.spacing
    val unlocked = progress.count { it.unlocked }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            text = stringResource(R.string.achievements_summary, unlocked, progress.size),
            style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.text,
        )
        ProgressBar(fraction = unlocked.toFloat() / progress.size, height = ProgressBarHeight)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
            AchievementTier.entries.forEach { tier -> TierCount(tier, progress, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun TierCount(tier: AchievementTier, progress: List<AchievementProgress>, modifier: Modifier = Modifier) {
    val colors = AssembleTheme.colors
    val ofTier = progress.filter { it.achievement.tier == tier }
    Row(
        modifier = modifier
            .clip(AssembleTheme.shapes.pill)
            .background(colors.surface)
            .padding(horizontal = AssembleTheme.spacing.space3, vertical = AssembleTheme.spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1),
    ) {
        Box(Modifier.size(TierDotSize).clip(AssembleTheme.shapes.pill).background(tier.ringColor()))
        Text(
            text = stringResource(R.string.achievements_tier_count, stringResource(tier.label), ofTier.count { it.unlocked }, ofTier.size),
            style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.SemiBold),
            color = colors.text,
            maxLines = 1,
        )
    }
}

@Composable
private fun ProgressBar(fraction: Float, height: Dp, modifier: Modifier = Modifier) {
    val colors = AssembleTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(AssembleTheme.shapes.pill)
            .background(colors.border),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .background(colors.actionAssemble, AssembleTheme.shapes.pill),
        )
    }
}

@Composable
private fun BadgeCard(
    item: AchievementProgress,
    revealOrder: Int,
    onOpenReward: (Reward) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val achievement = item.achievement
    val info = achievement.info()
    val title = stringResource(info.title)
    val description = stringResource(info.description)
    val current = item.current
    val status = when {
        item.unlocked -> stringResource(R.string.achievement_unlocked_state)
        current == null -> stringResource(R.string.achievement_unknown_state)
        else -> stringResource(R.string.achievement_progress, current, achievement.target)
    }
    val reward = rewardText(achievement.reward)
    val rewardStatus = stringResource(if (item.unlocked) R.string.achievement_reward_unlocked else R.string.achievement_reward, reward)
    val open = { onOpenReward(achievement.reward) }
    Column(
        modifier = modifier
            .clip(AssembleTheme.shapes.md)
            .background(colors.surface)
            .then(if (item.unlocked) Modifier.clickable(role = Role.Button, onClick = open) else Modifier)
            .padding(spacing.space4)
            .clearAndSetSemantics {
                contentDescription = "$title. $description. $status. $rewardStatus"
                if (item.unlocked) {
                    onClick {
                        open()
                        true
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        TierRing(achievement.tier, item.unlocked) {
            HexBadge(
                icon = info.icon,
                unlocked = item.unlocked,
                revealOrder = revealOrder,
                badgeSize = BadgeWidth * RingInnerFraction to BadgeHeight * RingInnerFraction,
                tickOnFlip = true,
            )
        }
        Text(title, style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.text, textAlign = TextAlign.Center)
        Text(description, style = AssembleTheme.typography.small, color = colors.textMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        if (item.unlocked || current == null) {
            Text(
                text = status,
                style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.SemiBold),
                color = if (item.unlocked) colors.accentText else colors.textMuted,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                ProgressBar(fraction = current.toFloat() / achievement.target, height = CardBarHeight, modifier = Modifier.weight(1f))
                Text(status, style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.SemiBold), color = colors.textMuted)
            }
        }
        RewardChip(reward, item.unlocked)
    }
}

/** Anel hexagonal na cor da raridade; apagado enquanto bloqueada. */
@Composable
private fun TierRing(tier: AchievementTier, unlocked: Boolean, badge: @Composable () -> Unit) {
    val ring = tier.ringColor().let { if (unlocked) it else it.copy(alpha = LockedRingAlpha) }
    Box(
        Modifier.size(BadgeWidth, BadgeHeight).clip(HexagonShape).background(ring),
        contentAlignment = Alignment.Center,
    ) { badge() }
}

/** "Moldura · Escudo": cadeado enquanto bloqueada, fundo no destaque quando já liberada. */
@Composable
private fun RewardChip(text: String, unlocked: Boolean) {
    val colors = AssembleTheme.colors
    val pill = AssembleTheme.shapes.pill
    Row(
        modifier = Modifier
            .clip(pill)
            .then(
                if (unlocked) {
                    Modifier.background(colors.actionAssemble.copy(alpha = RewardChipAlpha))
                } else {
                    Modifier.border(1.dp, colors.border, pill)
                },
            )
            .padding(horizontal = AssembleTheme.spacing.space2, vertical = AssembleTheme.spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1),
    ) {
        if (!unlocked) Icon(AssembleIcons.Lock, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(RewardIconSize))
        Text(
            text = text,
            style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.SemiBold),
            color = if (unlocked) colors.accentText else colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Tipo e nome da recompensa ("Moldura · Escudo"). */
@Composable
internal fun rewardText(reward: Reward): String = stringResource(
    R.string.achievement_reward_chip,
    stringResource(reward.typeLabel),
    stringResource(reward.nameLabel),
)

@get:StringRes
private val Reward.typeLabel: Int
    get() = when (this) {
        is Reward.Frame -> R.string.reward_type_frame
        is Reward.Cover -> R.string.reward_type_cover
        is Reward.Accent -> R.string.reward_type_accent
        is Reward.Title -> R.string.reward_type_title
    }

@get:StringRes
private val Reward.nameLabel: Int
    get() = when (this) {
        is Reward.Frame -> frame.label
        is Reward.Cover -> cover.label
        is Reward.Accent -> accent.label
        is Reward.Title -> title.label
    }

internal fun AchievementTier.ringColor(): Color = when (this) {
    AchievementTier.Bronze -> BronzeRing
    AchievementTier.Silver -> AssemblePalette.AccentSilver
    AchievementTier.Gold -> AssemblePalette.AccentGold
}

@get:StringRes
private val AchievementTier.label: Int
    get() = when (this) {
        AchievementTier.Bronze -> R.string.achievements_tier_bronze
        AchievementTier.Silver -> R.string.achievements_tier_silver
        AchievementTier.Gold -> R.string.achievements_tier_gold
    }

@get:StringRes
private val AchievementCategory.label: Int
    get() = when (this) {
        AchievementCategory.Connections -> R.string.achievements_category_connections
        AchievementCategory.Conversations -> R.string.achievements_category_conversations
        AchievementCategory.Discovery -> R.string.achievements_category_discovery
        AchievementCategory.Profile -> R.string.achievements_category_profile
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
            progress = Achievement.entries.mapIndexed { index, achievement ->
                AchievementProgress(achievement, if (index % 3 == 0) achievement.target else achievement.target / 2)
            },
            onBack = {},
        )
    }
}
