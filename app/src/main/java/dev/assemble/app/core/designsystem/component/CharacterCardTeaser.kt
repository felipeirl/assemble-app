package dev.assemble.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleShadow
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.assembleShadow

private const val MaxTraitsInCommon = 3
private const val MaxNameLines = 2
private const val MaxTaglineLines = 3
private const val ScrimHeightFraction = 0.55f
private const val ScrimMaxAlpha = 0.92f
private const val SecondaryTextAlpha = 0.75f
private const val OverlayChipAlpha = 0.2f
private val InfoButtonSize = 48.dp
private val PreviewCardHeight = 480.dp

/**
 * Card do Discover: a arte ocupa o card inteiro (recorte a partir do topo) e um degradê midnight
 * embaixo segura nome, até 3 traços em comum, a fonte e o botão de informação. O texto é sempre branco.
 * Sem faixa de compatibilidade: o card não antecipa o match.
 * O tamanho vem do [modifier]; o logo nunca aparece aqui.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterCardTeaser(
    name: String,
    imageUrl: String?,
    traitsInCommon: List<String>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    artModifier: Modifier = Modifier,
    tagline: String? = null,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val shape = AssembleTheme.shapes.lg

    Box(
        modifier = modifier
            .assembleShadow(AssembleShadow.Card, shape, colors)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        CharacterArt(
            name = name,
            imageUrl = imageUrl,
            imageAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .then(artModifier)
                .clip(shape),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(ScrimHeightFraction)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, colors.midnight.copy(alpha = ScrimMaxAlpha))),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name.uppercase(),
                    style = AssembleTheme.typography.displayMd,
                    color = Color.White,
                    maxLines = MaxNameLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (onClick != null) InfoButton(onClick = onClick)
            }
            if (!tagline.isNullOrBlank()) {
                Text(
                    text = tagline,
                    style = AssembleTheme.typography.body,
                    color = Color.White.copy(alpha = SecondaryTextAlpha),
                    maxLines = MaxTaglineLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val traits = traitsInCommon.take(MaxTraitsInCommon)
            if (traits.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.card_in_common).uppercase(),
                    style = AssembleTheme.typography.eyebrow,
                    color = Color.White.copy(alpha = SecondaryTextAlpha),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                    verticalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    traits.forEach { OverlayTraitChip(label = it) }
                }
            }
            SourceLabel(color = Color.White.copy(alpha = SecondaryTextAlpha))
        }
    }
}

/** Abre a pré-visualização, como tocar no card. Alvo de 48dp. */
@Composable
private fun InfoButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(InfoButtonSize)
            .clip(AssembleTheme.shapes.pill)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AssembleIcons.Info,
            contentDescription = stringResource(R.string.card_more_info),
            tint = Color.White,
        )
    }
}

/** Traço sobre a arte: pill branca translúcida, texto branco. Só leitura. */
@Composable
private fun OverlayTraitChip(label: String) {
    Text(
        text = label,
        style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        modifier = Modifier
            .background(Color.White.copy(alpha = OverlayChipAlpha), AssembleTheme.shapes.pill)
            .padding(horizontal = AssembleTheme.spacing.space3, vertical = AssembleTheme.spacing.space1),
    )
}

/** "Source: Comic Vine" (12sp). Obrigatório em todo card e perfil. */
@Composable
fun SourceLabel(modifier: Modifier = Modifier, color: Color = AssembleTheme.colors.textMuted) {
    Text(
        text = stringResource(R.string.source_comic_vine),
        style = AssembleTheme.typography.small,
        color = color,
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
            traitsInCommon = listOf("Science", "Humor", "Avengers"),
            modifier = Modifier.width(320.dp).height(PreviewCardHeight),
            onClick = {},
        )
    }
}
