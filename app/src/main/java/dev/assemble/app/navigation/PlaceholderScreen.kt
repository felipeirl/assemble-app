package dev.assemble.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.assemble.app.core.designsystem.component.AssembleTopBar
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.core.designsystem.theme.AssembleTheme

data class PlaceholderLink(val label: String, val onClick: () -> Unit)

/**
 * Tela provisória da etapa 5: top bar, um texto de contexto e links de navegação.
 * Cada feature substitui a sua na etapa correspondente.
 */
@Composable
fun PlaceholderScreen(
    title: TopBarTitle,
    navigation: TopBarNavigation,
    modifier: Modifier = Modifier,
    detail: String? = null,
    links: List<PlaceholderLink> = emptyList(),
) {
    Scaffold(
        modifier = modifier,
        containerColor = AssembleTheme.colors.bg,
        topBar = { AssembleTopBar(title = title, navigation = navigation) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AssembleTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space3, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (detail != null) {
                Text(text = detail, style = AssembleTheme.typography.caption, color = AssembleTheme.colors.textMuted)
            }
            links.forEach { link ->
                OutlinedButton(onClick = link.onClick) { Text(link.label) }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PlaceholderScreenPreview() {
    AssembleTheme {
        PlaceholderScreen(
            title = TopBarTitle.Text("Settings"),
            navigation = TopBarNavigation.Back {},
            detail = "placeholder",
            links = listOf(PlaceholderLink("Help") {}),
        )
    }
}
