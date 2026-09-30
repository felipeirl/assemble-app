package dev.assemble.app.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import dev.assemble.app.core.designsystem.theme.AssembleTheme

/**
 * A arte do splash é da Splash Screen API (fundo midnight + ícone).
 * Esta rota só decide o próximo destino e segue.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) { currentOnFinished() }
    Box(modifier.fillMaxSize().background(AssembleTheme.colors.midnight))
}
