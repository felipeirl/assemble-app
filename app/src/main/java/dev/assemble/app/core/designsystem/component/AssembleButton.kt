package dev.assemble.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.core.designsystem.theme.AssembleTheme

private val MinButtonHeight = 48.dp
private val BorderWidth = 1.dp
private val ProgressSize = 20.dp
private val ProgressStroke = 2.dp

/** Botão pill em connection-pink, texto branco (components.css .as-banner__cta). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = AssembleTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.defaultMinSize(minHeight = MinButtonHeight),
        shape = AssembleTheme.shapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.actionAssemble,
            contentColor = colors.onActionAssemble,
            disabledContainerColor = colors.border,
            disabledContentColor = colors.textMuted,
        ),
        contentPadding = PaddingValues(horizontal = AssembleTheme.spacing.space4),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(ProgressSize),
                color = colors.onActionAssemble,
                strokeWidth = ProgressStroke,
            )
        } else {
            ButtonLabel(text, FontWeight.Bold)
        }
    }
}

/** Botão pill neutro: surface com borda (components.css .as-state__btn). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AssembleTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = MinButtonHeight),
        shape = AssembleTheme.shapes.pill,
        border = BorderStroke(BorderWidth, colors.border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = colors.text,
            disabledContentColor = colors.textMuted,
        ),
        contentPadding = PaddingValues(horizontal = AssembleTheme.spacing.space4),
    ) {
        ButtonLabel(text, FontWeight.SemiBold)
    }
}

@Composable
private fun ButtonLabel(text: String, weight: FontWeight) {
    Text(
        text = text,
        style = AssembleTheme.typography.caption.copy(fontWeight = weight),
        textAlign = TextAlign.Center,
    )
}

@PreviewLightDark
@Composable
private fun AssembleButtonPreview() {
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Start chat", onClick = {})
            PrimaryButton("Start discovering", onClick = {}, enabled = false)
            PrimaryButton("Loading", onClick = {}, loading = true)
            SecondaryButton("Keep discovering", onClick = {})
        }
    }
}
