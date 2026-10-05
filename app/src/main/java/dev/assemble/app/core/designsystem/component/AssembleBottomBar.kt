package dev.assemble.app.core.designsystem.component

import dev.assemble.app.core.feedback.LocalFeedback
import dev.assemble.app.core.feedback.Cue
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled

enum class AssembleTab { Discover, Chat, Profile }

private const val IconCrossfadeMillis = 150
private const val BadgeOvershootScale = 1.2f
private const val MaxBadgeCount = 99
private const val PillAlpha = 0.14f
private const val PillDampingRatio = 0.6f
private const val IconBumpMillis = 400
private const val IconBumpPeakMillis = 240
private const val IconBumpStartScale = 0.8f
private const val IconBumpPeakScale = 1.25f
private val PillRadius = 16.dp
private val PillInsetX = 4.dp
private val TabIconSize = 28.dp
private val MinTouchTarget = 48.dp
private val HairlineWidth = 1.dp
private val BadgeMinSize = 18.dp
private val BadgeOffsetX = 10.dp
private val BadgeOffsetY = (-6).dp

/**
 * Barra inferior plana: Discover, Chat, Profile, só com ícones (traço fino; o nome fica para o leitor de tela).
 * Linha de 1dp no topo, sem elevação.
 * Uma pílula desliza com leve excesso até a aba ativa, e o ícone ativo dá um salto.
 */
@Composable
fun AssembleBottomBar(
    selectedTab: AssembleTab,
    onTabSelected: (AssembleTab) -> Unit,
    modifier: Modifier = Modifier,
    unreadChats: Int = 0,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val animationsEnabled = rememberAnimationsEnabled()
    val tabCount = AssembleTab.entries.size
    val pillIndex by animateFloatAsState(
        targetValue = AssembleTab.entries.indexOf(selectedTab).toFloat(),
        animationSpec = if (animationsEnabled) {
            spring(dampingRatio = PillDampingRatio, stiffness = Spring.StiffnessMediumLow)
        } else {
            snap()
        },
        label = "tabPill",
    )
    val pillColor = colors.accentText.copy(alpha = PillAlpha)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Box(Modifier.fillMaxWidth().height(HairlineWidth).background(colors.border))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(start = spacing.space4, end = spacing.space4, top = spacing.space2, bottom = spacing.space4)
                .drawBehind {
                    val cellWidth = size.width / tabCount
                    val inset = PillInsetX.toPx()
                    drawRoundRect(
                        color = pillColor,
                        topLeft = Offset(pillIndex * cellWidth + inset, 0f),
                        size = Size(cellWidth - 2 * inset, size.height),
                        cornerRadius = CornerRadius(PillRadius.toPx()),
                    )
                },
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val feedback = LocalFeedback.current
            AssembleTab.entries.forEach { tab ->
                BottomBarItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    badgeCount = if (tab == AssembleTab.Chat) unreadChats else 0,
                    onClick = {
                        if (tab != selectedTab) feedback.play(Cue.Tick)
                        onTabSelected(tab)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    tab: AssembleTab,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val tint = if (selected) colors.accentText else colors.textMuted
    val label = stringResource(tab.labelRes())
    val animationsEnabled = rememberAnimationsEnabled()
    val crossfadeMillis = if (animationsEnabled) IconCrossfadeMillis else 0
    val iconScale = remember { Animatable(1f) }
    // Só salta ao ser escolhida: a aba já ativa na primeira composição fica parada.
    val firstRun = remember { booleanArrayOf(true) }
    LaunchedEffect(selected) {
        val isFirstRun = firstRun[0]
        firstRun[0] = false
        if (selected && animationsEnabled && !isFirstRun) {
            iconScale.snapTo(IconBumpStartScale)
            iconScale.animateTo(
                1f,
                keyframes {
                    durationMillis = IconBumpMillis
                    IconBumpPeakScale at IconBumpPeakMillis
                },
            )
        }
    }
    Column(
        modifier = modifier
            .clip(AssembleTheme.shapes.sm)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .defaultMinSize(minHeight = MinTouchTarget)
            // Topo maior que o deslocamento do selo (6dp): o clip da aba não corta o selo nem o pulso dele.
            .padding(
                start = AssembleTheme.spacing.space2,
                end = AssembleTheme.spacing.space2,
                top = AssembleTheme.spacing.space2,
                bottom = AssembleTheme.spacing.space2,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.graphicsLayer {
                scaleX = iconScale.value
                scaleY = iconScale.value
            },
        ) {
            Crossfade(targetState = selected, animationSpec = tween(crossfadeMillis), label = "tabIcon") { active ->
                // Sem rótulo visível: o nome da aba fica para o leitor de tela.
                Icon(
                    imageVector = tab.icon(active),
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(TabIconSize),
                )
            }
            if (badgeCount > 0) {
                CountBadge(
                    count = badgeCount,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = BadgeOffsetX, y = BadgeOffsetY),
                )
            }
        }
    }
}

/** Badge numérico (action-assemble, texto branco). Pulsa 0 → 1.2 → 1 quando o número muda. */
@Composable
private fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    val animationsEnabled = rememberAnimationsEnabled()
    val scale = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(count, animationsEnabled) {
        if (animationsEnabled) {
            scale.snapTo(0f)
            scale.animateTo(BadgeOvershootScale, spring(stiffness = Spring.StiffnessMedium))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        } else {
            scale.snapTo(1f)
        }
    }
    val description = pluralStringResource(R.plurals.chat_unread_count, count, count)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .defaultMinSize(minWidth = BadgeMinSize, minHeight = BadgeMinSize)
            .background(AssembleTheme.colors.actionAssemble, AssembleTheme.shapes.pill)
            .padding(horizontal = AssembleTheme.spacing.space1)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > MaxBadgeCount) stringResource(R.string.badge_overflow) else count.toString(),
            style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.Bold),
            color = AssembleTheme.colors.onActionAssemble,
        )
    }
}

private fun AssembleTab.labelRes(): Int = when (this) {
    AssembleTab.Discover -> R.string.nav_discover
    AssembleTab.Chat -> R.string.nav_chat
    AssembleTab.Profile -> R.string.nav_profile
}

private fun AssembleTab.icon(active: Boolean): ImageVector = when (this) {
    AssembleTab.Discover -> if (active) AssembleIcons.CompassFilled else AssembleIcons.TabCompass
    AssembleTab.Chat -> if (active) AssembleIcons.ChatFilled else AssembleIcons.TabChat
    AssembleTab.Profile -> if (active) AssembleIcons.ProfileFilled else AssembleIcons.TabProfile
}

@PreviewLightDark
@Composable
private fun AssembleBottomBarPreview() {
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AssembleBottomBar(selectedTab = AssembleTab.Discover, onTabSelected = {}, unreadChats = 1)
            AssembleBottomBar(selectedTab = AssembleTab.Chat, onTabSelected = {}, unreadChats = 12)
        }
    }
}
