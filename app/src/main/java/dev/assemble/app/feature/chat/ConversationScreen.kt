package dev.assemble.app.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.navigation.PlaceholderLink
import dev.assemble.app.navigation.PlaceholderScreen

// Placeholder da etapa 5; tela real na etapa 9.
@Composable
fun ConversationScreen(
    connectionId: String,
    onBack: () -> Unit,
    onOpenCharacter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = TopBarTitle.Text(connectionId),
        navigation = TopBarNavigation.Back(onBack),
        modifier = modifier,
        links = listOf(PlaceholderLink(connectionId, onOpenCharacter)),
    )
}
