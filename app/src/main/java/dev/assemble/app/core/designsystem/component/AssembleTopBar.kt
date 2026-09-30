package dev.assemble.app.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme

/** Ação à esquerda da top bar. */
sealed interface TopBarNavigation {
    data class Menu(val onClick: () -> Unit) : TopBarNavigation
    data class Back(val onClick: () -> Unit) : TopBarNavigation
    data object None : TopBarNavigation
}

/** Conteúdo central: texto em display ou só o logo (Discover; mínimo de 32dp). */
sealed interface TopBarTitle {
    data class Text(val value: String) : TopBarTitle
    data object Logo : TopBarTitle
}

private val LogoSize = 32.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssembleTopBar(
    title: TopBarTitle,
    navigation: TopBarNavigation,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = AssembleTheme.colors
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            when (title) {
                is TopBarTitle.Text -> Text(
                    text = title.value.uppercase(),
                    style = AssembleTheme.typography.displayMd,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TopBarTitle.Logo -> Image(
                    painter = painterResource(R.drawable.ic_assemble_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(LogoSize),
                )
            }
        },
        navigationIcon = {
            when (navigation) {
                is TopBarNavigation.Menu -> IconButton(onClick = navigation.onClick) {
                    Icon(AssembleIcons.Menu, contentDescription = stringResource(R.string.top_bar_open_menu))
                }
                is TopBarNavigation.Back -> IconButton(onClick = navigation.onClick) {
                    Icon(AssembleIcons.Back, contentDescription = stringResource(R.string.top_bar_back))
                }
                TopBarNavigation.None -> Unit
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.bg,
            scrolledContainerColor = colors.bg,
            navigationIconContentColor = colors.text,
            titleContentColor = colors.text,
            actionIconContentColor = colors.text,
        ),
    )
}

@PreviewLightDark
@Composable
private fun AssembleTopBarPreview() {
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AssembleTopBar(title = TopBarTitle.Logo, navigation = TopBarNavigation.Menu {})
            AssembleTopBar(title = TopBarTitle.Text("Chat"), navigation = TopBarNavigation.Menu {})
            AssembleTopBar(title = TopBarTitle.Text("Settings"), navigation = TopBarNavigation.Back {})
        }
    }
}
