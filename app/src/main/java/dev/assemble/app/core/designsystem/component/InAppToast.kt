package dev.assemble.app.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.theme.AssembleShadow
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.assembleShadow
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import kotlinx.coroutines.delay

private const val ToastVisibleMillis = 4_000L
private val ToastAvatarSize = 40.dp

/**
 * Aviso de nova mensagem dentro do app. Entra pelo topo, some em 4 s e navega ao ser tocado.
 * Posicione no topo da tela (respeitando os insets da status bar).
 */
@Composable
fun InAppToast(
    visible: Boolean,
    characterName: String,
    imageUrl: String?,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes message: Int = R.string.toast_new_message,
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(visible, characterName) {
        if (visible) {
            delay(ToastVisibleMillis)
            currentOnDismiss()
        }
    }
    val animationsEnabled = rememberAnimationsEnabled()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (animationsEnabled) slideInVertically { -it } + fadeIn() else fadeIn(),
        exit = if (animationsEnabled) slideOutVertically { -it } + fadeOut() else fadeOut(),
    ) {
        ToastContent(characterName = characterName, imageUrl = imageUrl, message = message, onClick = onClick)
    }
}

@Composable
private fun ToastContent(characterName: String, imageUrl: String?, @StringRes message: Int, onClick: () -> Unit) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val shape = AssembleTheme.shapes.md
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space4)
            .assembleShadow(AssembleShadow.Card, shape, colors)
            .clip(shape)
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CharacterAvatar(name = characterName, imageUrl = imageUrl, size = ToastAvatarSize)
        Text(
            text = stringResource(message, characterName),
            style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.text,
        )
    }
}

@PreviewLightDark
@Composable
private fun InAppToastPreview() {
    PreviewSurface {
        ToastContent(characterName = "Spider-Man", imageUrl = null, message = R.string.toast_new_message, onClick = {})
    }
}
