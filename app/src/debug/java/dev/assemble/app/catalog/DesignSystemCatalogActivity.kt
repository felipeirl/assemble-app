package dev.assemble.app.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.ThemeMode

/** Catálogo interno do design system. Existe só no build de debug. */
class DesignSystemCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.System) }
            AssembleTheme(themeMode) {
                DesignSystemCatalog(themeMode = themeMode, onThemeModeChange = { themeMode = it })
            }
        }
    }
}
