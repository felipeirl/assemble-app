package dev.assemble.app.core.designsystem.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val DefaultAnimatorScale = 1f

/**
 * false quando o usuário zerou "Escala de duração do Animator" no sistema.
 * Nesse caso, use versões estáticas ou fade simples.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            DefaultAnimatorScale,
        ) != 0f
    }
}
