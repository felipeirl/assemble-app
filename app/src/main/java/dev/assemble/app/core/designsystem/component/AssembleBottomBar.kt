package dev.assemble.app.core.designsystem.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
private val TabIconSize = 28.dp
private val MinTouchTarget = 48.dp
private val HairlineWidth = 1.dp
private val BadgeMinSize = 18.dp
private val BadgeOffsetX = 10.dp
private val BadgeOffsetY = (-6).dp

/** Barra inferior plana: Discover, Chat, Profile. Linha de 1dp no topo, sem elevação. */
@Composable
fun AssembleBottomBar(
    selectedTab: AssembleTab,
    onTabSelected: (AssembleTab) -> Unit,
    modifier: Modifier = Modifier,
    unreadChats: Int = 0,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
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
                .padding(start = spacing.space4, end = spacing.space4, top = spacing.space3, bottom = spacing.space4),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AssembleTab.entries.forEach { tab ->
                BottomBarItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    badgeCount = if (tab == AssembleTab.Chat) unreadChats else 0,
                    onClick = { onTabSelected(tab) },
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
    Column(
        modifier = modifier
            .clip(AssembleTheme.shapes.sm)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .defaultMinSize(minHeight = MinTouchTarget)
            .padding(horizontal = AssembleTheme.spacing.space2, vertical = AssembleTheme.spacing.space1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1),
    ) {
        Box {
            Crossfade(targetState = selected, animationSpec = tween(IconCrossfadeMillis), label = "tabIcon") { active ->
                Icon(
                    imageVector = tab.icon(active),
                    contentDescription = null,
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
        Text(
            text = label,
            style = AssembleTheme.typography.caption.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = tint,
        )
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
    AssembleTab.Discover -> if (active) AssembleIcons.CompassFilled else AssembleIcons.Compass
    AssembleTab.Chat -> if (active) AssembleIcons.ChatFilled else AssembleIcons.Chat
    AssembleTab.Profile -> if (active) AssembleIcons.ProfileFilled else AssembleIcons.Profile
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
