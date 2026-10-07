package dev.assemble.app

import android.app.UiModeManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dev.assemble.app.core.designsystem.component.LocalLogoAnchor
import dev.assemble.app.core.feedback.LocalFeedback
import dev.assemble.app.core.designsystem.component.LogoAnchor
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.ThemeMode
import dev.assemble.app.core.designsystem.theme.ThemeRevealHost
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import dev.assemble.app.core.model.ThemePreference
import dev.assemble.app.feature.splash.BrandSplash
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = (application as MyApplication).container
        // A splash nativa segura a tela até a sessão e o tema salvos serem lidos (sem piscar Login nem tema).
        splashScreen.setKeepOnScreenCondition { !container.userRepository.ready.value }
        enableEdgeToEdge()
        // Splash da marca só na abertura a frio; ao recriar a Activity (rotação, volta do segundo plano), não.
        val isColdStart = savedInstanceState == null
        setContent {
            val settings by container.userRepository.settings.collectAsStateWithLifecycle()
            // A splash nativa é desenhada pelo sistema antes do app: no Android 12+ ela segue o tema
            // escolhido aqui na próxima abertura. Antes disso, segue o tema do aparelho.
            LaunchedEffect(settings.theme) { applyNightModeToSystemSplash(settings.theme) }
            val darkTheme = when (settings.theme.toThemeMode()) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> isSystemInDarkTheme()
            }
            // Ícones das barras do sistema seguem o tema do app, não só o do aparelho.
            DisposableEffect(darkTheme) {
                val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose {}
            }
            val logoAnchor = remember { LogoAnchor() }
            val animationsEnabled = rememberAnimationsEnabled()
            var showBrandSplash by rememberSaveable { mutableStateOf(isColdStart && animationsEnabled) }
            ThemeRevealHost(darkTheme) { shownDark ->
                AssembleTheme(if (shownDark) ThemeMode.Dark else ThemeMode.Light) {
                    CompositionLocalProvider(LocalLogoAnchor provides logoAnchor, LocalFeedback provides container.feedback) {
                        Box(Modifier.fillMaxSize()) {
                            AssembleApp(container)
                            if (showBrandSplash) {
                                BrandSplash(anchor = logoAnchor, onFinished = { showBrandSplash = false })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun applyNightModeToSystemSplash(theme: ThemePreference) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val uiModeManager = getSystemService(UiModeManager::class.java) ?: return
        uiModeManager.setApplicationNightMode(
            when (theme) {
                ThemePreference.Light -> UiModeManager.MODE_NIGHT_NO
                ThemePreference.Dark -> UiModeManager.MODE_NIGHT_YES
                ThemePreference.System -> UiModeManager.MODE_NIGHT_AUTO
            },
        )
    }

    /** Cada vez que o app aparece conta o dia de hoje (uma vez por dia) para a conquista "Sentinela". */
    override fun onStart() {
        super.onStart()
        val container = (application as MyApplication).container
        lifecycleScope.launch { container.activeDays.markToday() }
    }
}

private fun ThemePreference.toThemeMode(): ThemeMode = when (this) {
    ThemePreference.Light -> ThemeMode.Light
    ThemePreference.Dark -> ThemeMode.Dark
    ThemePreference.System -> ThemeMode.System
}
