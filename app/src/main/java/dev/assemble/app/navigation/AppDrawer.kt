package dev.assemble.app.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
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
import dev.assemble.app.core.designsystem.component.HalftoneFade
import dev.assemble.app.core.designsystem.component.IconTile
import dev.assemble.app.core.designsystem.component.UserAvatar
import dev.assemble.app.core.designsystem.component.energyGradient
import dev.assemble.app.core.designsystem.component.halftone
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import kotlinx.coroutines.delay

enum class DrawerItem(@StringRes val label: Int, val icon: ImageVector) {
    Achievements(R.string.achievements_title, AssembleIcons.Achievements),
    Settings(R.string.settings_title, AssembleIcons.Settings),
    Help(R.string.help_title, AssembleIcons.Help),
    About(R.string.about_title, AssembleIcons.Info),
    LogOut(R.string.drawer_log_out, AssembleIcons.LogOut),
}

/** Números da pessoa no cabeçalho do menu. */
data class DrawerStats(val connections: Int, val seen: Int)

private const val ItemStaggerMillis = 50
private const val ItemsStartDelayMillis = 120
private const val ItemEnterMillis = 400
private const val ResetAfterCloseMillis = 350L
private const val SecondaryOnGradientAlpha = 0.85f
private val ItemSlide = 16.dp
private val HeaderAvatarSize = 56.dp
private val MinItemHeight = 56.dp
private val ItemEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/**
 * Menu lateral: cabeçalho com o gradiente de energia, os números da pessoa, itens com ícone
 * entrando em cascata ao abrir, e Sair separado no fim. As abas principais ficam só na bottom bar.
 */
@Composable
fun AppDrawer(
    userName: String?,
    avatarPreset: AvatarPreset,
    stats: DrawerStats,
    revealItems: Boolean,
    onItemClick: (DrawerItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    ModalDrawerSheet(
        modifier = modifier,
        drawerContainerColor = colors.surface,
        drawerContentColor = colors.text,
        // O gradiente do cabeçalho passa por baixo da barra de status; o resto respeita as barras.
        windowInsets = WindowInsets(0),
    ) {
        DrawerHeader(userName = userName, avatarPreset = avatarPreset, stats = stats)
        val mainItems = DrawerItem.entries - DrawerItem.LogOut
        mainItems.forEachIndexed { index, item ->
            DrawerRow(item = item, index = index, reveal = revealItems, onClick = { onItemClick(item) })
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = colors.border)
        DrawerRow(
            item = DrawerItem.LogOut,
            index = mainItems.size,
            reveal = revealItems,
            destructive = true,
            onClick = { onItemClick(DrawerItem.LogOut) },
        )
        Text(
            text = stringResource(R.string.drawer_footer),
            style = AssembleTheme.typography.small,
            color = colors.textMuted,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = AssembleTheme.spacing.space4, end = AssembleTheme.spacing.space4, bottom = AssembleTheme.spacing.space4),
        )
    }
}

@Composable
private fun DrawerHeader(userName: String?, avatarPreset: AvatarPreset, stats: DrawerStats) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .energyGradient(colors)
            .halftone(color = colors.midnight, fade = HalftoneFade.Diagonal)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        UserAvatar(preset = avatarPreset, size = HeaderAvatarSize)
        if (userName != null) {
            Text(text = userName.uppercase(), style = AssembleTheme.typography.displayMd, color = Color.White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.space5)) {
            HeaderStat(value = stats.connections, label = stringResource(R.string.drawer_stat_connections))
            HeaderStat(value = stats.seen, label = stringResource(R.string.drawer_stat_seen))
        }
    }
}

@Composable
private fun HeaderStat(value: Int, label: String) {
    Column {
        Text(text = value.toString(), style = AssembleTheme.typography.h2, color = Color.White)
        Text(text = label, style = AssembleTheme.typography.small, color = Color.White.copy(alpha = SecondaryOnGradientAlpha))
    }
}

/** Item com ícone. Entra deslizando, em cascata, quando o menu abre. */
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
    val tint = if (destructive) colors.error else colors.accentText
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = progress.value
                translationX = (1f - progress.value) * -slidePx
            }
            .defaultMinSize(minHeight = MinItemHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AssembleTheme.spacing.space4, vertical = AssembleTheme.spacing.space2),
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(item.icon, tint = tint)
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
                revealItems = true,
                onItemClick = {},
            )
        }
    }
}
