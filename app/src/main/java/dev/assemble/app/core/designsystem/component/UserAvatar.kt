package dev.assemble.app.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleColors
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.media.AvatarImage

private const val IconSizeFraction = 0.55f
private val DefaultUserAvatarSize = 48.dp

/**
 * Avatares do usuário: só cores da paleta + ícone de pessoa (nada de foto nesta fase).
 * Ícone branco, exceto sobre arc-reactor-blue (midnight, como todo conteúdo sobre score).
 */
enum class AvatarPreset {
    Energy,
    Pink,
    Blue,
    Midnight,
    HeroRed,
    DeepRed,
    ;

    companion object {
        fun fromIndex(index: Int): AvatarPreset = entries[index.coerceIn(entries.indices)]
    }
}

/** Foto do usuário (JPEG em Base64) que todo [UserAvatar] mostra no lugar do preset; null = só o preset. */
val LocalUserPhoto = compositionLocalOf<String?> { null }

@Composable
fun UserAvatar(
    preset: AvatarPreset,
    modifier: Modifier = Modifier,
    size: Dp = DefaultUserAvatarSize,
    shape: Shape = AssembleTheme.shapes.pill,
    photo: String? = LocalUserPhoto.current,
) {
    val bitmap = remember(photo) { photo?.let(AvatarImage::decode) }
    val colors = AssembleTheme.colors
    val background = when (preset) {
        AvatarPreset.Energy -> Modifier.energyGradient(colors, shape)
        else -> Modifier.background(preset.fill(colors), shape)
    }
    Box(
        modifier = modifier.size(size).clip(shape).then(background),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(
                imageVector = AssembleIcons.ProfileFilled,
                contentDescription = null,
                tint = if (preset == AvatarPreset.Blue) colors.onScore else Color.White,
                modifier = Modifier.size(size * IconSizeFraction),
            )
        }
    }
}

private fun AvatarPreset.fill(colors: AssembleColors): Color = when (this) {
    AvatarPreset.Energy -> colors.heroRed
    AvatarPreset.Pink -> colors.actionAssemble
    AvatarPreset.Blue -> colors.score
    AvatarPreset.Midnight -> colors.midnight
    AvatarPreset.HeroRed -> colors.heroRed
    AvatarPreset.DeepRed -> colors.deepRed
}

@PreviewLightDark
@Composable
private fun UserAvatarPreview() {
    PreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AvatarPreset.entries.forEach { UserAvatar(it) }
        }
    }
}
