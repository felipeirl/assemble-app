package dev.assemble.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme

private const val TileTintAlpha = 0.12f
private val TileSize = 32.dp
private val TileIconSize = 18.dp

/** Ícone de linha num quadrado arredondado tingido (menu lateral, configurações). Decorativo: o rótulo vem ao lado. */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = AssembleTheme.colors.accentText,
) {
    Box(
        modifier = modifier
            .size(TileSize)
            .background(tint.copy(alpha = TileTintAlpha), AssembleTheme.shapes.sm),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(TileIconSize))
    }
}

@PreviewLightDark
@Composable
private fun IconTilePreview() {
    PreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconTile(AssembleIcons.Settings)
            IconTile(AssembleIcons.Help)
            IconTile(AssembleIcons.Achievements)
            IconTile(AssembleIcons.LogOut, tint = AssembleTheme.colors.error)
        }
    }
}
