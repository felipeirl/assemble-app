package dev.assemble.app.feature.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.PreviewSurface
import dev.assemble.app.core.designsystem.theme.AssembleTheme

/** Cores do botão "Sign in with Google" (diretrizes de marca do Google). São da marca Google, não tokens do Assemble. */
private object GoogleButtonColors {
    val LightContainer = Color(0xFFFFFFFF)
    val LightBorder = Color(0xFF747775)
    val LightContent = Color(0xFF1F1F1F)
    val DarkContainer = Color(0xFF131314)
    val DarkBorder = Color(0xFF8E918F)
    val DarkContent = Color(0xFFE3E3E3)
}

private val MinButtonHeight = 48.dp
private val GoogleIconSize = 20.dp
private val BorderWidth = 1.dp
private val ProgressStroke = 2.dp
private const val DisabledAlpha = 0.38f

/** "Continuar com Google" no padrão de marca do Google: o "G" fica sempre colorido, nos dois temas. */
@Composable
internal fun GoogleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val dark = AssembleTheme.colors.isDark
    val container = if (dark) GoogleButtonColors.DarkContainer else GoogleButtonColors.LightContainer
    val border = if (dark) GoogleButtonColors.DarkBorder else GoogleButtonColors.LightBorder
    val content = if (dark) GoogleButtonColors.DarkContent else GoogleButtonColors.LightContent
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        // Desabilitado (não carregando) apaga o botão inteiro, "G" incluso, como pede a diretriz.
        modifier = modifier
            .defaultMinSize(minHeight = MinButtonHeight)
            .alpha(if (enabled || loading) 1f else DisabledAlpha),
        shape = AssembleTheme.shapes.pill,
        border = BorderStroke(BorderWidth, border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container,
            disabledContentColor = content,
        ),
        contentPadding = PaddingValues(horizontal = AssembleTheme.spacing.space4),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(GoogleIconSize), color = content, strokeWidth = ProgressStroke)
        } else {
            Image(
                painter = painterResource(R.drawable.ic_google_g),
                contentDescription = null,
                modifier = Modifier.size(GoogleIconSize),
            )
        }
        Spacer(Modifier.width(AssembleTheme.spacing.space3))
        Text(
            text = text,
            style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewLightDark
@Composable
private fun GoogleButtonPreview() {
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
            GoogleButton("Continuar com Google", onClick = {}, modifier = Modifier.fillMaxWidth())
            GoogleButton("Continuar com Google", onClick = {}, modifier = Modifier.fillMaxWidth(), loading = true)
            GoogleButton("Continuar com Google", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
        }
    }
}
