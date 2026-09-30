package dev.assemble.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.data.mock.MockSeed
import dev.assemble.app.core.designsystem.component.AssembleTopBar
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.model.PreferenceCategory
import dev.assemble.app.core.ui.PreferenceEditor
import dev.assemble.app.core.ui.title

@Composable
fun OnboardingRoute(
    step: PreferenceCategory,
    viewModel: OnboardingViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(
        step = step,
        state = state,
        onToggle = viewModel::toggle,
        onSelectAny = { viewModel.selectAny(step) },
        onFameChange = viewModel::setFame,
        onNext = onNext,
        onBack = onBack,
        onFinish = viewModel::finish,
        modifier = modifier,
    )
}

@Composable
fun OnboardingScreen(
    step: PreferenceCategory,
    state: OnboardingUiState,
    onToggle: (Enum<*>) -> Unit,
    onSelectAny: () -> Unit,
    onFameChange: (Float) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            AssembleTopBar(
                title = TopBarTitle.None,
                navigation = if (step.ordinal > 0) TopBarNavigation.Back(onBack) else TopBarNavigation.None,
                actions = {
                    if (!step.isLastStep) {
                        TextButton(onClick = onNext) {
                            Text(
                                text = stringResource(R.string.onboarding_skip),
                                style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.accentText,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            OnboardingFooter(step = step, state = state, onNext = onNext, onFinish = onFinish)
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
                progress = { (step.ordinal + 1f) / PreferenceCategory.entries.size },
                modifier = Modifier.fillMaxWidth(),
                color = colors.actionAssemble,
                trackColor = colors.border,
                drawStopIndicator = {},
            )
            Text(
                text = stringResource(step.title).uppercase(),
                style = AssembleTheme.typography.displayMd,
                color = colors.text,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.onboarding_helper),
                style = AssembleTheme.typography.body,
                color = colors.textMuted,
            )
            PreferenceEditor(
                category = step,
                preferences = state.preferences,
                onToggle = onToggle,
                onSelectAny = onSelectAny,
                onFameChange = onFameChange,
            )
        }
    }
}

@Composable
private fun OnboardingFooter(
    step: PreferenceCategory,
    state: OnboardingUiState,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        if (step.isLastStep) {
            if (!state.canFinish) {
                Text(
                    text = pluralStringResource(
                        R.plurals.onboarding_min_choices,
                        OnboardingUiState.MIN_CHOICES,
                        OnboardingUiState.MIN_CHOICES,
                    ),
                    style = AssembleTheme.typography.caption,
                    color = colors.textMuted,
                )
            }
            if (state.showError) {
                Text(
                    text = stringResource(R.string.onboarding_error),
                    style = AssembleTheme.typography.caption,
                    color = colors.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            PrimaryButton(
                text = stringResource(R.string.onboarding_start),
                onClick = onFinish,
                enabled = state.canFinish,
                loading = state.submitting,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.onboarding_next),
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun OnboardingChipsPreview() {
    AssembleTheme {
        OnboardingScreen(
            step = PreferenceCategory.Powers,
            state = OnboardingUiState(MockSeed.initialPreferences),
            onToggle = {}, onSelectAny = {}, onFameChange = {}, onNext = {}, onBack = {}, onFinish = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun OnboardingFamePreview() {
    AssembleTheme {
        OnboardingScreen(
            step = PreferenceCategory.Fame,
            state = OnboardingUiState(MockSeed.initialPreferences.copy(origins = emptySet(), powers = emptySet())),
            onToggle = {}, onSelectAny = {}, onFameChange = {}, onNext = {}, onBack = {}, onFinish = {},
        )
    }
}
