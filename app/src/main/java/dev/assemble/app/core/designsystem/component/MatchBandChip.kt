package dev.assemble.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.model.MatchBand

/** Pill em score com texto midnight. Mostra só a faixa, nunca a porcentagem. */
@Composable
fun MatchBandChip(band: MatchBand, modifier: Modifier = Modifier) {
    val label = stringResource(
        when (band) {
            MatchBand.High -> R.string.match_band_high
            MatchBand.Possible -> R.string.match_band_possible
            MatchBand.Low -> R.string.match_band_low
        },
    )
    Text(
        text = label,
        style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
        color = AssembleTheme.colors.onScore,
        modifier = modifier
            .background(AssembleTheme.colors.score, AssembleTheme.shapes.pill)
            .padding(horizontal = AssembleTheme.spacing.space3, vertical = AssembleTheme.spacing.space1),
    )
}

@PreviewLightDark
@Composable
private fun MatchBandChipPreview() {
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MatchBand.entries.forEach { MatchBandChip(it) }
        }
    }
}
