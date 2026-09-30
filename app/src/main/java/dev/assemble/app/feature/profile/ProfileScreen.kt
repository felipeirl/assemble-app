package dev.assemble.app.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.navigation.PlaceholderLink
import dev.assemble.app.navigation.PlaceholderScreen

// Placeholder da etapa 5; tela real na etapa 10.
@Composable
fun ProfileScreen(
    onOpenMenu: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenCharacter: (characterId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = TopBarTitle.Text(stringResource(R.string.nav_profile)),
        navigation = TopBarNavigation.Menu(onOpenMenu),
        modifier = modifier,
        links = listOf(
            PlaceholderLink(stringResource(R.string.profile_edit), onEditProfile),
            PlaceholderLink("spider-man") { onOpenCharacter("spider-man") },
        ),
    )
}
