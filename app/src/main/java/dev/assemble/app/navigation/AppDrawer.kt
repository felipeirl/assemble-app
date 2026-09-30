package dev.assemble.app.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.AvatarPreset
import dev.assemble.app.core.designsystem.component.UserAvatar
import dev.assemble.app.core.designsystem.theme.AssembleTheme

enum class DrawerItem(@StringRes val label: Int) {
    Settings(R.string.settings_title),
    About(R.string.about_title),
    Help(R.string.help_title),
    LogOut(R.string.drawer_log_out),
}

/** Drawer com itens secundários. As abas principais ficam só na bottom bar. */
@Composable
fun AppDrawer(
    userName: String?,
    avatarPreset: AvatarPreset,
    onItemClick: (DrawerItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    ModalDrawerSheet(
        modifier = modifier,
        drawerContainerColor = colors.surface,
        drawerContentColor = colors.text,
    ) {
        Row(
            modifier = Modifier.padding(spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (userName != null) {
                UserAvatar(preset = avatarPreset)
                Text(text = userName, style = AssembleTheme.typography.h2, color = colors.text)
            }
        }
        HorizontalDivider(color = colors.border)
        DrawerItem.entries.forEach { item ->
            NavigationDrawerItem(
                label = { Text(stringResource(item.label), style = AssembleTheme.typography.body) },
                selected = false,
                onClick = { onItemClick(item) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = colors.surface,
                    unselectedTextColor = colors.text,
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun AppDrawerPreview() {
    AssembleTheme { AppDrawer(userName = "Felipe", avatarPreset = AvatarPreset.Energy, onItemClick = {}) }
}
