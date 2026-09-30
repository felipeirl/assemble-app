package dev.assemble.app.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.assemble.app.R
import dev.assemble.app.core.data.mock.MockSeed
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.navigation.PlaceholderLink
import dev.assemble.app.navigation.PlaceholderScreen

// Placeholder da etapa 5; tela real na etapa 9. Os links usam as conexões do mock.
@Composable
fun ChatListScreen(
    onOpenMenu: () -> Unit,
    onOpenConversation: (connectionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = TopBarTitle.Text(stringResource(R.string.nav_chat)),
        navigation = TopBarNavigation.Menu(onOpenMenu),
        modifier = modifier,
        links = MockSeed.initialConnections.map { seed ->
            PlaceholderLink(seed.characterId) { onOpenConversation(seed.id) }
        },
    )
}
