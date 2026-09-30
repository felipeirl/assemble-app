package dev.assemble.app.feature.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.navigation.PlaceholderLink
import dev.assemble.app.navigation.PlaceholderScreen

// Placeholder da etapa 5; tela real na etapa 6.
@Composable
fun LoginScreen(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = TopBarTitle.Logo,
        navigation = TopBarNavigation.None,
        modifier = modifier,
        links = listOf(
            PlaceholderLink(stringResource(R.string.login_continue_google), onContinue),
            PlaceholderLink(stringResource(R.string.login_continue_email), onContinue),
        ),
    )
}
