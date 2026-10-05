package dev.assemble.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.assemble.app.core.designsystem.theme.AssembleTheme

private const val MaxInitials = 2
private val DefaultAvatarSize = 48.dp

/** Até duas iniciais do nome: "Spider-Man" → "SM", "Storm" → "S". */
internal fun initialsOf(name: String): String =
    name.split(' ', '-')
        .filter { it.isNotBlank() }
        .take(MaxInitials)
        .joinToString("") { it.first().uppercase() }

/**
 * Arte do personagem: imagem (Coil) sobre o placeholder de gradiente + meio-tom + iniciais.
 * Se a imagem faltar ou falhar, o placeholder fica visível.
 */
@Composable
fun CharacterArt(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    initialsStyle: TextStyle = AssembleTheme.typography.displayMd,
    imageAlignment: Alignment = Alignment.Center,
) {
    val colors = AssembleTheme.colors
    Box(
        modifier = modifier
            .energyGradient(colors)
            .halftone(color = colors.midnight),
        contentAlignment = Alignment.Center,
    ) {
        // Iniciais são decorativas: o nome aparece ao lado (evita o TalkBack ler "SM, Spider-Man").
        Text(
            text = initialsOf(name),
            style = initialsStyle,
            color = Color.White,
            modifier = Modifier.clearAndSetSemantics {},
        )
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = imageAlignment,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Avatar circular do personagem (lista de chat, toast, top bar da conversa). Decorativo: o nome vem ao lado. */
@Composable
fun CharacterAvatar(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = DefaultAvatarSize,
) {
    CharacterArt(
        name = name,
        imageUrl = imageUrl,
        initialsStyle = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
        modifier = modifier
            .size(size)
            .clip(AssembleTheme.shapes.pill),
    )
}

@PreviewLightDark
@Composable
private fun CharacterArtPreview() {
    PreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CharacterArt(name = "Spider-Man", imageUrl = null, modifier = Modifier.size(120.dp).clip(AssembleTheme.shapes.lg))
            CharacterAvatar(name = "Storm", imageUrl = null)
            CharacterAvatar(name = "Rocket Raccoon", imageUrl = null, size = 32.dp)
        }
    }
}
