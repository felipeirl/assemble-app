package dev.assemble.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.data.mock.MockSeed
import dev.assemble.app.core.data.remote.LOOKING_FOR_MAX
import dev.assemble.app.core.designsystem.component.AssembleTopBar
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.domain.ProfileRules
import dev.assemble.app.feature.profile.archetypeText

private const val LOOKING_FOR_LINES = 3

@Composable
fun RevealRoute(viewModel: OnboardingViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RevealScreen(
        state = state,
        onLookingForChange = viewModel::setLookingFor,
        onFinish = viewModel::finish,
        onBack = onBack,
        modifier = modifier,
    )
}

/** Fim do cadastro: o arquétipo das preferências, a frase opcional e o aceite do aviso de IA. */
@Composable
fun RevealScreen(
    state: OnboardingUiState,
    onLookingForChange: (String) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val archetype = ProfileRules.archetype(state.preferences)
    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = { AssembleTopBar(title = TopBarTitle.None, navigation = TopBarNavigation.Back(onBack)) },
        bottomBar = {
            OnboardingFooter {
                if (state.showError) {
                    Text(
                        text = stringResource(R.string.onboarding_error),
                        style = AssembleTheme.typography.caption,
                        color = colors.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                // Aviso de IA: tocar em "Start discovering" é o aceite gravado no perfil.
                Text(
                    text = stringResource(R.string.onboarding_ai_notice),
                    style = AssembleTheme.typography.caption,
                    color = colors.textMuted,
                )
                PrimaryButton(
                    text = stringResource(R.string.onboarding_start),
                    onClick = onFinish,
                    loading = state.submitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxWidth(),
                color = colors.actionAssemble,
                trackColor = colors.border,
                drawStopIndicator = {},
            )
            Text(
                text = stringResource(R.string.reveal_title).uppercase(),
                style = AssembleTheme.typography.caption,
                color = colors.textMuted,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = archetype?.let { archetypeText(it) } ?: stringResource(R.string.reveal_open_to_all),
                style = AssembleTheme.typography.displayMd,
                color = colors.accentText,
            )
            val reaction = state.reaction
            if (reaction is ReactionState.Playing && reaction.index > 0) {
                Text(
                    text = stringResource(R.string.reveal_learned, reaction.liked, reaction.index),
                    style = AssembleTheme.typography.body,
                    color = colors.textMuted,
                )
            }
            OutlinedTextField(
                value = state.lookingFor,
                onValueChange = onLookingForChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.reveal_looking_for_label)) },
                placeholder = { Text(stringResource(R.string.reveal_looking_for_placeholder)) },
                supportingText = { Text("${state.lookingFor.length}/$LOOKING_FOR_MAX") },
                maxLines = LOOKING_FOR_LINES,
                shape = AssembleTheme.shapes.md,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.actionAssemble,
                    unfocusedBorderColor = colors.border,
                    cursorColor = colors.actionAssemble,
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RevealPreview() {
    AssembleTheme {
        RevealScreen(
            state = OnboardingUiState(MockSeed.initialPreferences, lookingFor = "Trocar ideia sobre ciência"),
            onLookingForChange = {}, onFinish = {}, onBack = {},
        )
    }
}
