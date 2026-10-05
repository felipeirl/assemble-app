package dev.assemble.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme

private const val HeroAspectRatio = 0.85f
private const val ScrimHeightFraction = 0.45f
private const val ScrimMaxAlpha = 0.92f
private const val BackButtonAlpha = 0.85f
private const val MaxNameLines = 2
private val BackButtonSize = 40.dp

/**
 * Arte do personagem de ponta a ponta no topo da pré-visualização e do perfil, como o card do Discover:
 * recorte a partir do topo e degradê midnight embaixo com o nome em branco.
 * [artModifier] recebe o elemento compartilhado com o card.
 */
@Composable
fun CharacterHero(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    artModifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(HeroAspectRatio),
    ) {
        CharacterArt(
            name = name,
            imageUrl = imageUrl,
            imageAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .then(artModifier),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(ScrimHeightFraction)
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.midnight.copy(alpha = ScrimMaxAlpha)))),
        )
        Text(
            text = name.uppercase(),
            style = AssembleTheme.typography.displayXl,
            color = Color.White,
            maxLines = MaxNameLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(AssembleTheme.spacing.space4)
                .semantics { heading() },
        )
    }
}

/** Voltar por cima da arte: círculo translúcido para ficar legível sobre qualquer imagem. */
@Composable
fun HeroBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AssembleTheme.colors
    IconButton(onClick = onBack, modifier = modifier) {
        Box(
            Modifier
                .size(BackButtonSize)
                .background(colors.surface.copy(alpha = BackButtonAlpha), AssembleTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AssembleIcons.Back, contentDescription = stringResource(R.string.top_bar_back), tint = colors.text)
        }
    }
}

@PreviewLightDark
@Composable
private fun CharacterHeroPreview() {
    PreviewSurface {
        Box {
            CharacterHero(name = "Jean Grey", imageUrl = null)
            HeroBackButton(onBack = {}, modifier = Modifier.padding(8.dp))
        }
    }
}
