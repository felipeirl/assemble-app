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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
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

/** Conteúdo central: texto em display, só o logo (Discover; 44dp de altura) ou nada. */
sealed interface TopBarTitle {
    data class Text(val value: String) : TopBarTitle
    data object Logo : TopBarTitle
    data object None : TopBarTitle
}

private val LogoSize = 44.dp
private const val MaxTopBarScale = 1.75f
private val LargeTitleExpandedSize = 40.sp
private val LargeTitleCollapsedSize = 24.sp

/** Altura da top bar acompanha a fonte do sistema (até 175%) para o título não ser cortado a 200%. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun scaledTopBarHeight(): Dp =
    TopAppBarDefaults.TopAppBarExpandedHeight * LocalDensity.current.fontScale.coerceIn(1f, MaxTopBarScale)

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
        expandedHeight = scaledTopBarHeight(),
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
                    modifier = Modifier
                        .size(LogoSize)
                        .logoAnchor(LocalLogoAnchor.current),
                )
                TopBarTitle.None -> Unit
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

/**
 * Top bar das telas secundárias: título grande em display que encolhe até a barra ao rolar.
 * Ligue o [scrollBehavior] ao conteúdo com `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssembleLargeTopBar(
    title: String,
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val fontSize = lerp(LargeTitleExpandedSize, LargeTitleCollapsedSize, scrollBehavior.state.collapsedFraction)
    LargeTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title.uppercase(),
                style = AssembleTheme.typography.displayMd.copy(fontSize = fontSize, lineHeight = fontSize),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(AssembleIcons.Back, contentDescription = stringResource(R.string.top_bar_back))
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.bg,
            scrolledContainerColor = colors.bg,
            navigationIconContentColor = colors.text,
            titleContentColor = colors.text,
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
