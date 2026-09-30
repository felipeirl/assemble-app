package dev.assemble.app.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.component.SecondaryButton
import dev.assemble.app.core.designsystem.theme.AssembleTheme

private val LogoHeight = 72.dp

@Composable
fun LoginRoute(viewModel: LoginViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreen(state = state, onContinueWithGoogle = viewModel::signIn, onContinueWithEmail = viewModel::signIn, modifier = modifier)
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onContinueWithGoogle: () -> Unit,
    onContinueWithEmail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.space4, vertical = spacing.space6),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        Spacer(Modifier.height(spacing.space8))
        Image(
            painter = painterResource(R.drawable.ic_assemble_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.height(LogoHeight),
        )
        Text(
            text = stringResource(R.string.login_headline),
            style = AssembleTheme.typography.displayXl,
            color = colors.text,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.login_subtitle),
            style = AssembleTheme.typography.body,
            color = colors.textMuted,
        )
        Spacer(Modifier.height(spacing.space6))
        PrimaryButton(
            text = stringResource(R.string.login_continue_google),
            onClick = onContinueWithGoogle,
            loading = state.signingIn,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = stringResource(R.string.login_continue_email),
            onClick = onContinueWithEmail,
            enabled = !state.signingIn,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.showError) {
            Text(
                text = stringResource(R.string.login_error),
                style = AssembleTheme.typography.caption,
                color = colors.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun LoginScreenPreview() {
    AssembleTheme { LoginScreen(LoginUiState(), onContinueWithGoogle = {}, onContinueWithEmail = {}) }
}

@PreviewLightDark
@Composable
private fun LoginScreenErrorPreview() {
    AssembleTheme { LoginScreen(LoginUiState(showError = true), onContinueWithGoogle = {}, onContinueWithEmail = {}) }
}
