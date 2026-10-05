package dev.assemble.app.feature.verifyemail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.component.SecondaryButton
import dev.assemble.app.core.designsystem.component.energyGradient
import dev.assemble.app.core.designsystem.component.halftone
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme

private val HeroSize = 96.dp
private val HeroIconSize = 44.dp

@Composable
fun VerifyEmailRoute(viewModel: VerifyEmailViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Voltando do app de e-mail, confere sozinho: quem já clicou no link entra sem tocar em nada.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.check(silent = true) }
    VerifyEmailScreen(
        state = state,
        onCheck = { viewModel.check() },
        onResend = viewModel::resend,
        onSignOut = viewModel::signOut,
        modifier = modifier,
    )
}

@Composable
fun VerifyEmailScreen(
    state: VerifyEmailUiState,
    onCheck: () -> Unit,
    onResend: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Box(modifier.fillMaxSize().background(colors.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.space5, vertical = spacing.space6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            Box(
                modifier = Modifier
                    .size(HeroSize)
                    .clip(AssembleTheme.shapes.lg)
                    .energyGradient(colors)
                    .halftone(color = colors.midnight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AssembleIcons.Mail, contentDescription = null, tint = Color.White, modifier = Modifier.size(HeroIconSize))
            }
            Text(
                text = stringResource(R.string.verify_title).uppercase(),
                style = AssembleTheme.typography.displayMd,
                color = colors.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = state.email?.let { stringResource(R.string.verify_body, it) } ?: stringResource(R.string.verify_body_no_email),
                style = AssembleTheme.typography.body,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
            )
            state.message?.let { message ->
                Text(
                    text = stringResource(message.text),
                    style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = if (message == VerifyEmailMessage.Sent) colors.accentText else colors.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            PrimaryButton(
                text = stringResource(R.string.verify_checked),
                onClick = onCheck,
                loading = state.checking,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = if (state.cooldownSeconds > 0) {
                    stringResource(R.string.verify_resend_wait, state.cooldownSeconds)
                } else {
                    stringResource(R.string.verify_resend)
                },
                onClick = onResend,
                enabled = !state.sending && state.cooldownSeconds == 0,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onSignOut) {
                Text(
                    text = stringResource(R.string.verify_other_account),
                    style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accentText,
                )
            }
        }
    }
}

private val VerifyEmailMessage.text: Int
    get() = when (this) {
        VerifyEmailMessage.Sent -> R.string.verify_msg_sent
        VerifyEmailMessage.StillPending -> R.string.verify_msg_pending
        VerifyEmailMessage.Failed -> R.string.verify_msg_failed
        VerifyEmailMessage.RateLimited -> R.string.verify_msg_rate_limited
    }

@PreviewLightDark
@Composable
private fun VerifyEmailPreview() {
    AssembleTheme {
        VerifyEmailScreen(
            state = VerifyEmailUiState(email = "tony@stark.com", cooldownSeconds = 42, message = VerifyEmailMessage.Sent),
            onCheck = {}, onResend = {}, onSignOut = {},
        )
    }
}
