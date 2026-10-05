package dev.assemble.app.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.TraitChip
import dev.assemble.app.core.designsystem.theme.AssemblePalette
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.model.PreferenceCategory
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.isAny
import dev.assemble.app.core.model.options
import dev.assemble.app.core.model.selected

private const val FameIconsMax = 1f / 3
private const val FameHiddenGemsMin = 2f / 3
private val FameTrackHeight = 8.dp

@get:StringRes
val PreferenceCategory.title: Int
    get() = when (this) {
        PreferenceCategory.Origin -> R.string.onboarding_step_origin
        PreferenceCategory.Powers -> R.string.onboarding_step_powers
        PreferenceCategory.Teams -> R.string.onboarding_step_teams
        PreferenceCategory.Style -> R.string.onboarding_step_style
        PreferenceCategory.Fame -> R.string.onboarding_step_fame
    }

/** Editor de uma categoria: slider para Fame, chips com "Any" para as demais. */
@Composable
fun PreferenceEditor(
    category: PreferenceCategory,
    preferences: Preferences,
    onToggle: (Enum<*>) -> Unit,
    onSelectAny: () -> Unit,
    onFameChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    largeChips: Boolean = false,
) {
    if (category == PreferenceCategory.Fame) {
        FameSlider(value = preferences.fame, onValueChange = onFameChange, modifier = modifier)
    } else {
        TraitSelector(
            options = preferences.options(category),
            isAny = preferences.isAny(category),
            onToggle = onToggle,
            onSelectAny = onSelectAny,
            modifier = modifier,
            large = largeChips,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TraitSelector(
    options: List<Pair<Enum<*>, Boolean>>,
    isAny: Boolean,
    onToggle: (Enum<*>) -> Unit,
    onSelectAny: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2),
        verticalArrangement = Arrangement.spacedBy(if (large) AssembleTheme.spacing.space1 else 0.dp),
    ) {
        TraitChip(
            label = stringResource(R.string.onboarding_any),
            selected = isAny,
            onSelectedChange = { onSelectAny() },
            large = large,
        )
        options.forEach { (trait, selected) ->
            TraitChip(
                label = stringResource(traitLabel(trait)),
                selected = selected,
                onSelectedChange = { onToggle(trait) },
                large = large,
            )
        }
    }
}

/** Slider "Icons ↔ Hidden gems" (0 = Icons, 1 = Hidden gems). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FameSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val colors = AssembleTheme.colors
    val description = stringResource(R.string.onboarding_step_fame)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2)) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.semantics { contentDescription = description },
            colors = SliderDefaults.colors(
                thumbColor = colors.actionAssemble,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
            // Trilho com o gradiente da marca; a bolinha marca onde a pessoa está.
            track = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(FameTrackHeight)
                        .clip(AssembleTheme.shapes.pill)
                        .background(Brush.horizontalGradient(listOf(colors.logoRed, AssemblePalette.AccentViolet))),
                )
            },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.fame_icons), style = AssembleTheme.typography.caption, color = colors.text)
            Text(
                stringResource(R.string.fame_hidden_gems),
                style = AssembleTheme.typography.caption,
                color = colors.text,
                textAlign = TextAlign.End,
            )
        }
    }
}

/** Resumo de uma categoria para a lista "My preferences". */
@Composable
fun preferenceSummary(category: PreferenceCategory, preferences: Preferences): String = when {
    category == PreferenceCategory.Fame -> stringResource(
        when {
            preferences.fame <= FameIconsMax -> R.string.fame_icons
            preferences.fame >= FameHiddenGemsMin -> R.string.fame_hidden_gems
            else -> R.string.fame_range
        },
    )
    preferences.isAny(category) -> stringResource(R.string.onboarding_any)
    else -> preferences.selected(category).map { stringResource(traitLabel(it)) }.joinToString()
}
