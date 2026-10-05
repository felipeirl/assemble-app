package dev.assemble.app.feature.account

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Conta excluída ainda na carência (contrato §3): o backend recusa tudo com `ACCOUNT_DEACTIVATED`.
 * O usuário reativa ou sai; não há como fechar o diálogo sem escolher.
 */
@Composable
fun AccountDeactivatedHost(userRepository: UserRepository) {
    val deactivated by userRepository.accountDeactivated.collectAsStateWithLifecycle()
    if (!deactivated) return
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val colors = AssembleTheme.colors

    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        failed = false
        scope.launch {
            try {
                action()
            } catch (_: IOException) {
                failed = true
            } finally {
                busy = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        containerColor = colors.surface,
        title = { Text(stringResource(R.string.account_deactivated_title), color = colors.text) },
        text = {
            Text(
                stringResource(if (failed) R.string.account_deactivated_error else R.string.account_deactivated_message),
                color = if (failed) colors.error else colors.textMuted,
            )
        },
        confirmButton = {
            TextButton(onClick = { run { userRepository.reactivateAccount() } }, enabled = !busy) {
                Text(stringResource(R.string.account_reactivate), color = colors.accentText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = { run { userRepository.logOut() } }, enabled = !busy) {
                Text(stringResource(R.string.drawer_log_out), color = colors.text)
            }
        },
    )
}
