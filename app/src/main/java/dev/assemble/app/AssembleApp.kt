package dev.assemble.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.assemble.app.core.designsystem.theme.AssembleTheme

/** Raiz da UI. Drawer, bottom bar e NavDisplay entram na etapa 5. */
@Composable
fun AssembleApp() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    )
}

@Preview(name = "Light")
@Composable
private fun AssembleAppLightPreview() {
    AssembleTheme(darkTheme = false) { AssembleApp() }
}

@Preview(name = "Dark")
@Composable
private fun AssembleAppDarkPreview() {
    AssembleTheme(darkTheme = true) { AssembleApp() }
}
