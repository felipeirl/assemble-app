package dev.assemble.app.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.LocalLogoAnchor
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.component.SecondaryButton
import dev.assemble.app.core.designsystem.component.logoAnchor
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.firebase.requestGoogleIdToken

private val LogoHeight = 72.dp
private val CompactLogoHeight = 40.dp
private val TabMinHeight = 40.dp

@Composable
fun LoginRoute(viewModel: LoginViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val webClientId = viewModel.googleWebClientId
    LoginScreen(
        state = state,
        actions = LoginActions(
            onContinueWithGoogle = {
                if (webClientId == null) {
                    viewModel.signIn()
                } else {
                    viewModel.signInWithGoogle { requestGoogleIdToken(context, webClientId) }
                }
            },
            onContinueWithEmail = viewModel::signIn,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
            onCreateAccountChange = viewModel::setCreateAccount,
            onSubmitEmail = viewModel::submitEmail,
            onResetPassword = viewModel::resetPassword,
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
    val onTogglePasswordVisibility: () -> Unit = {},
    val onCreateAccountChange: (Boolean) -> Unit = {},
    val onSubmitEmail: () -> Unit = {},
    val onResetPassword: () -> Unit = {},
)

/**
 * Em repouso, não rola: o hero ocupa a sobra acima das ações e encolhe (ou some) conforme ela.
 * Com o teclado aberto, o hero sai e a coluna rola, para o campo focado não ficar coberto em telas baixas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(state: LoginUiState, actions: LoginActions, modifier: Modifier = Modifier) {
    val spacing = AssembleTheme.spacing
    val keyboardOpen = WindowInsets.isImeVisible
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AssembleTheme.colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .then(if (keyboardOpen) Modifier.verticalScroll(scrollState) else Modifier)
            .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space4, Alignment.Bottom),
    ) {
        if (!keyboardOpen) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.TopStart,
            ) {
                LoginHero(heroVariantFor(maxHeight))
            }
        }
        LoginActionsBlock(state, actions)
    }
}

/**
 * Logo no topo e título com subtítulo embaixo, ocupando toda a sobra acima das ações: o hero é o
 * mesmo em Entrar e em Criar conta. A versão compacta (telas muito baixas) usa o título menor e
 * mantém o subtítulo junto dele.
 */
@Composable
private fun LoginHero(variant: HeroVariant) {
    if (variant == HeroVariant.Hidden) return
    val colors = AssembleTheme.colors
    val typography = AssembleTheme.typography
    val full = variant == HeroVariant.Full
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_assemble_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .height(if (full) LogoHeight else CompactLogoHeight)
                .logoAnchor(LocalLogoAnchor.current),
        )
        Column(verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space3)) {
            Text(
                text = stringResource(R.string.login_headline),
                style = if (full) typography.displayXl else typography.displayMd,
                color = colors.text,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.login_subtitle),
                style = typography.body,
                color = colors.textMuted,
            )
        }
    }
}

/** Google, e-mail e o erro do último envio. */
@Composable
private fun LoginActionsBlock(state: LoginUiState, actions: LoginActions) {
    val googleLabel = stringResource(R.string.login_continue_google)
    Column(verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space3)) {
        if (state.inlineEmailForm) {
            if (state.showGoogle) {
                GoogleButton(
                    text = googleLabel,
                    onClick = actions.onContinueWithGoogle,
                    enabled = !state.signingIn,
                    modifier = Modifier.fillMaxWidth(),
                )
                OrDivider()
            }
            EmailCard(state, actions)
        } else {
            GoogleButton(
                text = googleLabel,
                onClick = actions.onContinueWithGoogle,
                loading = state.signingIn,
                modifier = Modifier.fillMaxWidth(),
            )
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
                color = AssembleTheme.colors.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun OrDivider() {
    val colors = AssembleTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space3)) {
        HorizontalDivider(Modifier.weight(1f), color = colors.border)
        Text(stringResource(R.string.login_or).uppercase(), style = AssembleTheme.typography.eyebrow, color = colors.textMuted)
        HorizontalDivider(Modifier.weight(1f), color = colors.border)
    }
}

/** Entrar ou criar conta com e-mail e senha, num cartão só. */
@Composable
private fun EmailCard(state: LoginUiState, actions: LoginActions) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val shape = AssembleTheme.shapes.md
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.actionAssemble,
        unfocusedBorderColor = colors.border,
        cursorColor = colors.actionAssemble,
        focusedLabelColor = colors.accentText,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        AuthModeTabs(createAccount = state.createAccount, enabled = !state.signingIn, onChange = actions.onCreateAccountChange)
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
            trailingIcon = {
                IconButton(onClick = actions.onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (state.showPassword) AssembleIcons.EyeOff else AssembleIcons.Eye,
                        contentDescription = stringResource(
                            if (state.showPassword) R.string.login_hide_password else R.string.login_show_password,
                        ),
                        tint = colors.textMuted,
                    )
                }
            },
            singleLine = true,
            enabled = !state.signingIn,
            visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { actions.onSubmitEmail() }),
            shape = AssembleTheme.shapes.md,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!state.createAccount) {
            ForgotPassword(resetSent = state.resetSent, enabled = !state.signingIn, onReset = actions.onResetPassword)
        }
        PrimaryButton(
            text = stringResource(if (state.createAccount) R.string.login_create_account else R.string.login_sign_in),
            onClick = actions.onSubmitEmail,
            enabled = state.canSubmitEmail,
            loading = state.signingIn,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Logo abaixo da senha, à direita; depois de pedido, vira a confirmação de envio. */
@Composable
private fun ForgotPassword(resetSent: Boolean, enabled: Boolean, onReset: () -> Unit) {
    val colors = AssembleTheme.colors
    if (resetSent) {
        Text(
            text = stringResource(R.string.login_reset_sent),
            style = AssembleTheme.typography.caption,
            color = colors.textMuted,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    } else {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(
                onClick = onReset,
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = AssembleTheme.spacing.space2),
            ) {
                Text(
                    text = stringResource(R.string.login_forgot_password),
                    style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accentText,
                )
            }
        }
    }
}

/** Abas em pílula, no mesmo estilo das do perfil do personagem. */
@Composable
private fun AuthModeTabs(createAccount: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val colors = AssembleTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(AssembleTheme.shapes.pill)
            .background(colors.bg)
            .selectableGroup(),
    ) {
        listOf(false to R.string.login_sign_in, true to R.string.login_create_account).forEach { (create, label) ->
            val selected = createAccount == create
            Box(
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = TabMinHeight)
                    .clip(AssembleTheme.shapes.pill)
                    .background(if (selected) colors.actionAssemble else Color.Transparent)
                    .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = { onChange(create) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(label),
                    style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selected) colors.onActionAssemble else colors.textMuted,
                )
            }
        }
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
private fun LoginScreenMockPreview() {
    AssembleTheme { LoginScreen(LoginUiState(), PreviewActions) }
}

@PreviewLightDark
@Composable
private fun LoginScreenSignInPreview() {
    AssembleTheme {
        LoginScreen(LoginUiState(inlineEmailForm = true, email = "tony@stark.com", password = "friday"), PreviewActions)
    }
}

@PreviewLightDark
@Composable
private fun LoginScreenCreateAccountPreview() {
    AssembleTheme {
        LoginScreen(
            LoginUiState(inlineEmailForm = true, createAccount = true, error = LoginError.EmailInUse),
            PreviewActions,
        )
    }
}

/** Pior caso da spec: tela baixa, Criar conta, com erro. Tudo deve caber sem rolar. */
@Preview(name = "360x640 criar conta com erro", widthDp = 360, heightDp = 640, showSystemUi = true)
@Composable
private fun LoginScreenSmallCreateAccountPreview() {
    AssembleTheme {
        LoginScreen(
            LoginUiState(inlineEmailForm = true, createAccount = true, error = LoginError.EmailInUse),
            PreviewActions,
        )
    }
}
