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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.LocalLogoAnchor
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.component.SecondaryButton
import dev.assemble.app.core.designsystem.component.logoAnchor
import dev.assemble.app.core.designsystem.theme.AssembleTheme

private val LogoHeight = 72.dp

@Composable
fun LoginRoute(viewModel: LoginViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreen(
        state = state,
        actions = LoginActions(
            onContinueWithGoogle = viewModel::signIn,
            onContinueWithEmail = viewModel::continueWithEmail,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onToggleCreateAccount = viewModel::toggleCreateAccount,
            onSubmitEmail = viewModel::submitEmail,
        ),
        modifier = modifier,
    )
}

/** Callbacks da tela agrupados. */
data class LoginActions(
    val onContinueWithGoogle: () -> Unit,
    val onContinueWithEmail: () -> Unit,
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onToggleCreateAccount: () -> Unit = {},
    val onSubmitEmail: () -> Unit = {},
)

@Composable
fun LoginScreen(state: LoginUiState, actions: LoginActions, modifier: Modifier = Modifier) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.space4, vertical = spacing.space6),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        Spacer(Modifier.height(spacing.space8))
        Image(
            painter = painterResource(R.drawable.ic_assemble_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .height(LogoHeight)
                .logoAnchor(LocalLogoAnchor.current),
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
        if (state.emailFormOpen) {
            EmailForm(state, actions)
        } else {
            if (state.showGoogle) {
                PrimaryButton(
                    text = stringResource(R.string.login_continue_google),
                    onClick = actions.onContinueWithGoogle,
                    loading = state.signingIn,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SecondaryButton(
                text = stringResource(R.string.login_continue_email),
                onClick = actions.onContinueWithEmail,
                enabled = !state.signingIn,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.error?.let { error ->
            Text(
                text = stringResource(error.message()),
                style = AssembleTheme.typography.caption,
                color = colors.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun EmailForm(state: LoginUiState, actions: LoginActions) {
    val colors = AssembleTheme.colors
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.actionAssemble,
        unfocusedBorderColor = colors.border,
        cursorColor = colors.actionAssemble,
        focusedLabelColor = colors.accentText,
    )
    OutlinedTextField(
        value = state.email,
        onValueChange = actions.onEmailChange,
        label = { Text(stringResource(R.string.login_email)) },
        singleLine = true,
        enabled = !state.signingIn,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        shape = AssembleTheme.shapes.md,
        colors = fieldColors,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.password,
        onValueChange = actions.onPasswordChange,
        label = { Text(stringResource(R.string.login_password)) },
        supportingText = if (state.createAccount) {
            {
                Text(
                    pluralStringResource(R.plurals.login_password_hint, LoginUiState.MIN_PASSWORD_LENGTH, LoginUiState.MIN_PASSWORD_LENGTH),
                )
            }
        } else {
            null
        },
        singleLine = true,
        enabled = !state.signingIn,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { actions.onSubmitEmail() }),
        shape = AssembleTheme.shapes.md,
        colors = fieldColors,
        modifier = Modifier.fillMaxWidth(),
    )
    PrimaryButton(
        text = stringResource(if (state.createAccount) R.string.login_create_account else R.string.login_sign_in),
        onClick = actions.onSubmitEmail,
        enabled = state.canSubmitEmail,
        loading = state.signingIn,
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(onClick = actions.onToggleCreateAccount, enabled = !state.signingIn, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(if (state.createAccount) R.string.login_have_account else R.string.login_new_account),
            color = colors.accentText,
        )
    }
}

private fun LoginError.message(): Int = when (this) {
    LoginError.Generic -> R.string.login_error
    LoginError.InvalidCredentials -> R.string.login_error_credentials
    LoginError.EmailInUse -> R.string.login_error_email_in_use
    LoginError.WeakPassword -> R.string.login_error_weak_password
    LoginError.InvalidEmail -> R.string.login_error_invalid_email
}

private val PreviewActions = LoginActions(onContinueWithGoogle = {}, onContinueWithEmail = {})

@PreviewLightDark
@Composable
private fun LoginScreenPreview() {
    AssembleTheme { LoginScreen(LoginUiState(), PreviewActions) }
}

@PreviewLightDark
@Composable
private fun LoginScreenErrorPreview() {
    AssembleTheme { LoginScreen(LoginUiState(error = LoginError.Generic), PreviewActions) }
}

@PreviewLightDark
@Composable
private fun LoginScreenEmailPreview() {
    AssembleTheme {
        LoginScreen(LoginUiState(showGoogle = false, emailFormOpen = true, email = "tony@stark.com"), PreviewActions)
    }
}
