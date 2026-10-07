package dev.assemble.app.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.data.mock.MockSeed
import dev.assemble.app.core.designsystem.component.AssembleTopBar
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.model.PreferenceCategory
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.isAny
import dev.assemble.app.core.model.selected
import dev.assemble.app.core.ui.PreferenceEditor
import dev.assemble.app.core.ui.title
import dev.assemble.app.core.ui.traitLabel

private const val ProgressFillMillis = 500
private val ProgressSegmentHeight = 5.dp
private const val FameLegendsMax = 1f / 3
private const val FameGemsMin = 2f / 3

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
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = { OnboardingTopBar(showBack = step.ordinal > 0, onBack = onBack, onSkip = onNext) },
        bottomBar = {
            OnboardingFooter { PrimaryButton(stringResource(R.string.onboarding_next), onNext, Modifier.fillMaxWidth()) }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            StepProgress(current = step.ordinal, total = ONBOARDING_STEP_COUNT)
            Text(
                text = stringResource(
                    R.string.onboarding_kicker,
                    step.ordinal + 1,
                    PreferenceCategory.entries.size,
                    stringResource(step.title),
                ).uppercase(),
                style = AssembleTheme.typography.eyebrow,
                color = colors.textMuted,
            )
            Text(
                text = stringResource(step.question),
                style = AssembleTheme.typography.displayMd,
                color = colors.text,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(step.subtitle),
                style = AssembleTheme.typography.body,
                color = colors.textMuted,
            )
            PreferenceEditor(
                category = step,
                preferences = state.preferences,
                onToggle = onToggle,
                onSelectAny = onSelectAny,
                onFameChange = onFameChange,
                largeChips = true,
            )
            if (step == PreferenceCategory.Fame) {
                FameDescription(state.preferences.fame)
            } else {
                SelectionHint(step, state.preferences)
            }
        }
    }
}

/** Voltar e Pular, no alto de todos os passos. */
@Composable
internal fun OnboardingTopBar(showBack: Boolean, onBack: () -> Unit, onSkip: (() -> Unit)?) {
    val colors = AssembleTheme.colors
    AssembleTopBar(
        title = TopBarTitle.None,
        navigation = if (showBack) TopBarNavigation.Back(onBack) else TopBarNavigation.None,
        actions = {
            if (onSkip != null) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accentText,
                    )
                }
            }
        },
    )
}

/** Barra em segmentos: cada passo concluído enche de vermelho. */
@Composable
internal fun StepProgress(current: Int, total: Int, modifier: Modifier = Modifier) {
    val colors = AssembleTheme.colors
    val fill = colors.logoRed
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1)) {
        repeat(total) { index ->
            val fraction by animateFloatAsState(
                targetValue = if (index <= current) 1f else 0f,
                animationSpec = tween(ProgressFillMillis),
                label = "stepProgress",
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(ProgressSegmentHeight)
                    .clip(AssembleTheme.shapes.pill)
                    .background(colors.border),
            ) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(fill))
            }
        }
    }
}

@Composable
private fun SelectionHint(step: PreferenceCategory, preferences: Preferences) {
    val text = if (preferences.isAny(step)) {
        stringResource(R.string.onboarding_hint_none)
    } else {
        val chosen = preferences.selected(step)
        stringResource(
            R.string.onboarding_hint_selected,
            chosen.size,
            chosen.map { stringResource(traitLabel(it)) }.joinToString(),
        )
    }
    Text(text = text, style = AssembleTheme.typography.caption, color = AssembleTheme.colors.textMuted)
}

/** Explica, com exemplos, o que o ponto escolhido no slider de fama traz para o baralho. */
@Composable
private fun FameDescription(fame: Float) {
    val colors = AssembleTheme.colors
    val (title, text) = when {
        fame <= FameLegendsMax -> R.string.onboarding_fame_icons_title to R.string.onboarding_fame_icons_text
        fame >= FameGemsMin -> R.string.onboarding_fame_gems_title to R.string.onboarding_fame_gems_text
        else -> R.string.onboarding_fame_mid_title to R.string.onboarding_fame_mid_text
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AssembleTheme.shapes.md)
            .background(colors.surface)
            .border(1.dp, colors.border, AssembleTheme.shapes.md)
            .padding(AssembleTheme.spacing.space4),
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1),
    ) {
        Text(stringResource(title), style = AssembleTheme.typography.h2, color = colors.text)
        Text(stringResource(text), style = AssembleTheme.typography.caption, color = colors.textMuted)
    }
}

/** Rodapé dos passos do cadastro, acima da barra de navegação. */
@Composable
internal fun OnboardingFooter(content: @Composable ColumnScope.() -> Unit) {
    val spacing = AssembleTheme.spacing
    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
        content = content,
    )
}

@get:StringRes
private val PreferenceCategory.question: Int
    get() = when (this) {
        PreferenceCategory.Origin -> R.string.onboarding_title_origin
        PreferenceCategory.Powers -> R.string.onboarding_title_powers
        PreferenceCategory.Teams -> R.string.onboarding_title_teams
        PreferenceCategory.Style -> R.string.onboarding_title_style
        PreferenceCategory.Fame -> R.string.onboarding_title_fame
    }

@get:StringRes
private val PreferenceCategory.subtitle: Int
    get() = when (this) {
        PreferenceCategory.Origin -> R.string.onboarding_sub_origin
        PreferenceCategory.Powers -> R.string.onboarding_sub_powers
        PreferenceCategory.Teams -> R.string.onboarding_sub_teams
        PreferenceCategory.Style -> R.string.onboarding_sub_style
        PreferenceCategory.Fame -> R.string.onboarding_sub_fame
    }

@PreviewLightDark
@Composable
private fun OnboardingChipsPreview() {
    AssembleTheme {
        OnboardingScreen(
            step = PreferenceCategory.Origin,
            state = OnboardingUiState(MockSeed.initialPreferences),
            onToggle = {}, onSelectAny = {}, onFameChange = {}, onNext = {}, onBack = {},
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
            onToggle = {}, onSelectAny = {}, onFameChange = {}, onNext = {}, onBack = {},
        )
    }
}
