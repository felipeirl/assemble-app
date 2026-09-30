package dev.assemble.app.feature.discover

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.navigation.PlaceholderLink
import dev.assemble.app.navigation.PlaceholderScreen

// Placeholder da etapa 5; tela real na etapa 7. Os links usam ids do mock.
@Composable
fun DiscoverScreen(
    onOpenMenu: () -> Unit,
    onOpenPreview: (characterId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = TopBarTitle.Logo,
        navigation = TopBarNavigation.Menu(onOpenMenu),
        modifier = modifier,
        links = listOf("jean-grey", "storm", "rocket").map { id -> PlaceholderLink(id) { onOpenPreview(id) } },
    )
}
