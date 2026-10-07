package dev.assemble.app.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.AvatarPreset
import dev.assemble.app.core.designsystem.component.ProfileCoverArt
import dev.assemble.app.core.designsystem.component.onColor
import dev.assemble.app.core.model.ProfileCover
import dev.assemble.app.core.model.ProfileStyle
import dev.assemble.app.core.designsystem.component.UserAvatar
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import kotlinx.coroutines.delay

enum class DrawerItem(@StringRes val label: Int, val icon: ImageVector) {
    Achievements(R.string.achievements_title, AssembleIcons.Achievements),
    Settings(R.string.settings_title, AssembleIcons.Settings),
    Help(R.string.help_title, AssembleIcons.Help),
    About(R.string.about_title, AssembleIcons.About),
    LogOut(R.string.drawer_log_out, AssembleIcons.LogOut),
}

/** Números da pessoa no cabeçalho do menu. */
data class DrawerStats(val connections: Int, val seen: Int)

private const val ItemStaggerMillis = 50
private const val ItemsStartDelayMillis = 120
private const val ItemEnterMillis = 400
private const val ResetAfterCloseMillis = 350L
private const val SecondaryOnGradientAlpha = 0.85f
private const val PressedTintAlpha = 0.09f
private val ItemSlide = 16.dp
private val HeaderAvatarSize = 56.dp
private val ItemHeight = 52.dp
private val ItemIconSize = 22.dp
private val ItemIconGap = 16.dp
private val ItemEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/**
 * Menu lateral: cabeçalho com o gradiente de energia, os números da pessoa e itens só com ícone
 * de linha (sem caixinha) entrando em cascata ao abrir. "Sair da conta" fica depois de uma linha.
 * As abas principais ficam só na bottom bar.
 */
@Composable
fun AppDrawer(
    userName: String?,
    avatarPreset: AvatarPreset,
    stats: DrawerStats,
    revealItems: Boolean,
    onItemClick: (DrawerItem) -> Unit,
    modifier: Modifier = Modifier,
    archetype: String? = null,
    style: ProfileStyle = ProfileStyle(),
    photo: String? = null,
    onHeaderClick: () -> Unit = {},
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    ModalDrawerSheet(
        modifier = modifier,
        drawerContainerColor = colors.surface,
        drawerContentColor = colors.text,
        // O gradiente do cabeçalho passa por baixo da barra de status; o resto respeita as barras.
        windowInsets = WindowInsets(0),
    ) {
        DrawerHeader(
            userName = userName,
            avatarPreset = avatarPreset,
            archetype = archetype,
            stats = stats,
            style = style,
            photo = photo,
            onClick = onHeaderClick,
        )
        Column(
            modifier = Modifier.padding(horizontal = spacing.space3, vertical = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            val mainItems = DrawerItem.entries - DrawerItem.LogOut
            mainItems.forEachIndexed { index, item ->
                DrawerRow(item = item, index = index, reveal = revealItems, onClick = { onItemClick(item) })
            }
            HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = spacing.space2))
            DrawerRow(
                item = DrawerItem.LogOut,
                index = mainItems.size,
                reveal = revealItems,
                destructive = true,
                onClick = { onItemClick(DrawerItem.LogOut) },
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.drawer_footer),
            style = AssembleTheme.typography.small,
            color = colors.textMuted,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = spacing.space5, vertical = spacing.space4),
        )
    }
}

/** Cabeçalho com a capa e a foto escolhidas no perfil. Tocar nele abre o perfil. */
@Composable
private fun DrawerHeader(
    userName: String?,
    avatarPreset: AvatarPreset,
    archetype: String?,
    stats: DrawerStats,
    style: ProfileStyle,
    photo: String?,
    onClick: () -> Unit,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    // Capas de cor lisa (Halftone, Comic) seguem o destaque: dourado e azul pedem texto escuro. Manchete é papel claro.
    val onCover = when (style.cover) {
        ProfileCover.Halftone, ProfileCover.Comic -> style.accent.onColor(colors)
        ProfileCover.Headline -> colors.midnight
        ProfileCover.Energy, ProfileCover.Night, ProfileCover.Blueprint, ProfileCover.Cosmos -> Color.White
    }
    Box(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        ProfileCoverArt(style.cover, style.accent, Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            UserAvatar(preset = avatarPreset, size = HeaderAvatarSize, photo = photo)
            Column {
                if (userName != null) {
                    Text(text = userName.uppercase(), style = AssembleTheme.typography.displayMd, color = onCover)
                }
                if (archetype != null) {
                    Text(
                        text = archetype,
                        style = AssembleTheme.typography.caption,
                        color = onCover.copy(alpha = SecondaryOnGradientAlpha),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.space5)) {
                HeaderStat(value = stats.connections, label = stringResource(R.string.drawer_stat_connections), color = onCover)
                HeaderStat(value = stats.seen, label = stringResource(R.string.drawer_stat_seen), color = onCover)
            }
        }
    }
}

@Composable
private fun HeaderStat(value: Int, label: String, color: Color) {
    Column {
        Text(text = value.toString(), style = AssembleTheme.typography.h2, color = color)
        Text(text = label, style = AssembleTheme.typography.small, color = color.copy(alpha = SecondaryOnGradientAlpha))
    }
}

/** Ícone de linha e rótulo. Entra deslizando, em cascata, quando o menu abre. */
@Composable
private fun DrawerRow(
    item: DrawerItem,
    index: Int,
    reveal: Boolean,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val colors = AssembleTheme.colors
    val animationsEnabled = rememberAnimationsEnabled()
    val progress = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(reveal, animationsEnabled) {
        when {
            !animationsEnabled -> progress.snapTo(1f)
            reveal -> {
                delay((ItemsStartDelayMillis + index * ItemStaggerMillis).toLong())
                progress.animateTo(1f, tween(ItemEnterMillis, easing = ItemEasing))
            }
            else -> {
                // Espera o menu terminar de fechar antes de esconder, para a próxima abertura animar.
                delay(ResetAfterCloseMillis)
                progress.snapTo(0f)
            }
        }
    }
    val slidePx = with(LocalDensity.current) { ItemSlide.toPx() }
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val iconTint by animateColorAsState(
        targetValue = when {
            destructive -> colors.error
            pressed -> colors.accentText
            else -> colors.textMuted
        },
        label = "drawerIconTint",
    )
    val shape = AssembleTheme.shapes.md
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = progress.value
                translationX = (1f - progress.value) * -slidePx
            }
            .height(ItemHeight)
            .clip(shape)
            .background(if (pressed) colors.accentText.copy(alpha = PressedTintAlpha) else Color.Transparent, shape)
            .clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = AssembleTheme.spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(ItemIconGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = item.icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(ItemIconSize))
        Text(
            text = stringResource(item.label),
            style = AssembleTheme.typography.body,
            color = if (destructive) colors.error else colors.text,
        )
    }
}

@PreviewLightDark
@Composable
private fun AppDrawerPreview() {
    AssembleTheme {
        Box {
            AppDrawer(
                userName = "Felipe",
                avatarPreset = AvatarPreset.Energy,
                stats = DrawerStats(connections = 7, seen = 12),
                archetype = "Estrategista · Mutante",
                revealItems = true,
                onItemClick = {},
            )
        }
    }
}
