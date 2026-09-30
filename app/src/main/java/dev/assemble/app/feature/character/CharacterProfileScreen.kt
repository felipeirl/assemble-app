package dev.assemble.app.feature.character

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.navigation.PlaceholderLink
import dev.assemble.app.navigation.PlaceholderScreen

// Placeholder da etapa 5; tela real na etapa 8.
@Composable
fun CharacterProfileScreen(
    characterId: String,
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = TopBarTitle.Text(characterId),
        navigation = TopBarNavigation.Back(onBack),
        modifier = modifier,
        links = listOf(PlaceholderLink(stringResource(R.string.nav_chat), onOpenChat)),
    )
}
