package dev.assemble.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.theme.AssembleShadow
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.assembleShadow
import dev.assemble.app.core.model.MatchBand

private const val MaxTraitsInCommon = 3
private val ArtHeight = 220.dp

/**
 * Card do Discover: arte, faixa de compatibilidade, nome, até 3 traços em comum e a fonte.
 * O logo nunca aparece aqui.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterCardTeaser(
    name: String,
    imageUrl: String?,
    band: MatchBand,
    traitsInCommon: List<String>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val shape = AssembleTheme.shapes.lg

    Column(
        modifier = modifier
            .assembleShadow(AssembleShadow.Card, shape, colors)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .background(colors.surface),
    ) {
        Box(Modifier.fillMaxWidth().height(ArtHeight)) {
            CharacterArt(name = name, imageUrl = imageUrl, modifier = Modifier.fillMaxWidth().height(ArtHeight))
            MatchBandChip(
                band = band,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(spacing.space3),
            )
        }
        Column(
            modifier = Modifier.padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Text(text = name.uppercase(), style = AssembleTheme.typography.displayMd, color = colors.text)
            val traits = traitsInCommon.take(MaxTraitsInCommon)
            if (traits.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.card_in_common).uppercase(),
                    style = AssembleTheme.typography.eyebrow,
                    color = colors.textMuted,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                    verticalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    traits.forEach { TraitChip(label = it) }
                }
            }
            SourceLabel()
        }
    }
}

/** "Source: Comic Vine" (12sp, text-muted). Obrigatório em todo card e perfil. */
@Composable
fun SourceLabel(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.source_comic_vine),
        style = AssembleTheme.typography.small,
        color = AssembleTheme.colors.textMuted,
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun CharacterCardTeaserPreview() {
    PreviewSurface {
        CharacterCardTeaser(
            name = "Spider-Man",
            imageUrl = null,
            band = MatchBand.High,
            traitsInCommon = listOf("Science", "Humor", "Avengers"),
            modifier = Modifier.width(320.dp),
            onClick = {},
        )
    }
}
