package dev.assemble.app.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.data.mock.MockSeed
import dev.assemble.app.core.data.remote.LOOKING_FOR_MAX
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.component.energyGradientBrush
import dev.assemble.app.core.designsystem.component.halftone
import dev.assemble.app.core.designsystem.theme.AssemblePalette
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import dev.assemble.app.core.domain.ProfileRules
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.PreferenceCategory
import dev.assemble.app.core.model.selected
import dev.assemble.app.core.ui.traitLabel
import dev.assemble.app.feature.profile.archetypeText
import kotlinx.coroutines.delay

private const val LOOKING_FOR_LINES = 3
private const val CountMillis = 900
private const val CountStartMillis = 500L
private const val TileStaggerMillis = 150L
private const val CardAlpha = 0.22f
private const val PercentMax = 100
private const val MAX_ORIGIN_TAGS = 3
private const val FameLegendsMax = 1f / 3
private const val FameGemsMin = 2f / 3

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

/** Fim do cadastro: o perfil de herói em cartão, a frase opcional e o aceite do aviso de IA. */
@OptIn(ExperimentalLayoutApi::class)
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
    val preferences = state.preferences
    val picks = (state.reaction as? ReactionState.Playing)?.picks ?: 0
    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = { OnboardingTopBar(showBack = true, onBack = onBack, onSkip = null) },
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
            StepProgress(current = REVEAL_STEP, total = ONBOARDING_STEP_COUNT)
            HeroCard(preferences)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                StatTile(picks, R.string.reveal_stat_picks, index = 0, Modifier.weight(1f))
                StatTile(preferences.selected(PreferenceCategory.Origin).size, R.string.reveal_stat_origins, index = 1, Modifier.weight(1f))
                StatTile(Math.round(preferences.fame * PercentMax), R.string.reveal_stat_gems, index = 2, Modifier.weight(1f), suffix = "%")
            }
            OutlinedTextField(
                value = state.lookingFor,
                onValueChange = onLookingForChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.reveal_looking_for_label)) },
                placeholder = { Text(stringResource(R.string.reveal_looking_for_placeholder)) },
                supportingText = {
                    Row {
                        Text(stringResource(R.string.reveal_looking_for_hint), Modifier.weight(1f))
                        Text("${state.lookingFor.length}/$LOOKING_FOR_MAX")
                    }
                },
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

/** Cartão com o gradiente da marca: o arquétipo em destaque e o que a pessoa escolheu. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroCard(preferences: Preferences) {
    val colors = AssembleTheme.colors
    val animationsEnabled = rememberAnimationsEnabled()
    val appear = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled) appear.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }
    val title = ProfileRules.archetype(preferences)?.let { archetypeText(it) } ?: stringResource(R.string.reveal_open_to_all)
    val origins = preferences.selected(PreferenceCategory.Origin).take(MAX_ORIGIN_TAGS).map { stringResource(traitLabel(it)) }
    val fameLabel = stringResource(
        when {
            preferences.fame <= FameLegendsMax -> R.string.onboarding_fame_icons_title
            preferences.fame >= FameGemsMin -> R.string.onboarding_fame_gems_title
            else -> R.string.onboarding_fame_mid_title
        },
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = 0.8f + 0.2f * appear.value
                scaleY = 0.8f + 0.2f * appear.value
                alpha = appear.value.coerceIn(0f, 1f)
            }
            .clip(AssembleTheme.shapes.lg)
            .background(energyGradientBrush(colors))
            .halftone(color = colors.midnight)
            .padding(AssembleTheme.spacing.space5),
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2),
    ) {
        Text(
            text = stringResource(R.string.reveal_title).uppercase(),
            style = AssembleTheme.typography.eyebrow,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.semantics { heading() },
        )
        Text(text = title, style = AssembleTheme.typography.displayMd, color = Color.White)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2),
            verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2),
        ) {
            (origins + fameLabel).forEach { tag ->
                Text(
                    text = tag,
                    style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier
                        .clip(AssembleTheme.shapes.pill)
                        .background(Color.White.copy(alpha = CardAlpha))
                        .padding(horizontal = AssembleTheme.spacing.space3, vertical = AssembleTheme.spacing.space1),
                )
            }
        }
    }
}

/** Número que conta até o valor, uma peça de cada vez. */
@Composable
private fun StatTile(value: Int, @androidx.annotation.StringRes label: Int, index: Int, modifier: Modifier = Modifier, suffix: String = "") {
    val colors = AssembleTheme.colors
    val animationsEnabled = rememberAnimationsEnabled()
    var shown by remember { mutableIntStateOf(if (animationsEnabled) 0 else value) }
    val appear = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(value, animationsEnabled) {
        if (!animationsEnabled) {
            shown = value
            return@LaunchedEffect
        }
        delay(CountStartMillis + index * TileStaggerMillis)
        appear.snapTo(1f)
        Animatable(0f).animateTo(1f, tween(CountMillis)) { shown = Math.round(this.value * value) }
        shown = value
    }
    Column(
        modifier = modifier
            .graphicsLayer { alpha = appear.value }
            .clip(AssembleTheme.shapes.md)
            .background(colors.surface)
            .border(1.dp, colors.border, AssembleTheme.shapes.md)
            .padding(AssembleTheme.spacing.space3),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(text = "$shown$suffix", style = AssembleTheme.typography.h1, color = colors.text)
        Text(text = stringResource(label), style = AssembleTheme.typography.small, color = colors.textMuted)
    }
}

@PreviewLightDark
@Composable
private fun RevealPreview() {
    AssembleTheme {
        RevealScreen(
            state = OnboardingUiState(
                MockSeed.initialPreferences,
                reaction = ReactionState.Playing(emptyList(), pair = 6, picks = 5),
                lookingFor = "Trocar ideia sobre ciência",
            ),
            onLookingForChange = {}, onFinish = {}, onBack = {},
        )
    }
}
