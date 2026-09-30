package dev.assemble.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.assemble.app.core.designsystem.theme.AssembleTheme

/** Fundo e margem para @PreviewLightDark (o tema segue o uiMode do preview). */
@Composable
internal fun PreviewSurface(content: @Composable () -> Unit) {
    AssembleTheme {
        Box(
            Modifier
                .background(AssembleTheme.colors.bg)
                .padding(AssembleTheme.spacing.space4),
        ) { content() }
    }
}
