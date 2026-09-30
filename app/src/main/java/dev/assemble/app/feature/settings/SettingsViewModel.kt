package dev.assemble.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.core.data.ChatRepository
import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.model.AppSettings
import dev.assemble.app.core.model.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

enum class SettingsMessage { PreferencesReset, SeenCleared, ChatsDeleted, ActionFailed }

/** Settings lê só dados locais: não há estado de carregamento. */
class SettingsViewModel(
    private val userRepository: UserRepository,
    private val connectionRepository: ConnectionRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = userRepository.settings

    private val messageState = MutableStateFlow<SettingsMessage?>(null)
    val message: StateFlow<SettingsMessage?> = messageState.asStateFlow()

    private val busyState = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = busyState.asStateFlow()

    fun setTheme(theme: ThemePreference) = updateSettings { it.copy(theme = theme) }

    fun setNotifyNewConnections(enabled: Boolean) = updateSettings { it.copy(notifyNewConnections = enabled) }

    fun setNotifyNewMessages(enabled: Boolean) = updateSettings { it.copy(notifyNewMessages = enabled) }

    /** O Discover recalcula faixas e resultados de Assemble com o novo limiar. */
    fun setMinimumCompatibility(value: Int) = updateSettings { it.copy(minimumCompatibility = value) }

    fun resetPreferences() = runAction(SettingsMessage.PreferencesReset) { userRepository.resetPreferences() }

    fun clearSeenCharacters() = runAction(SettingsMessage.SeenCleared) { userRepository.clearSeen() }

    fun deleteChats() = runAction(SettingsMessage.ChatsDeleted) { chatRepository.deleteAll() }

    /** Apaga tudo e encerra a sessão; o app volta ao Login. */
    fun deleteAccount() = runAction(successMessage = null) {
        chatRepository.deleteAll()
        connectionRepository.deleteAll()
        userRepository.deleteAccount()
    }

    fun onMessageShown() {
        messageState.value = null
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { userRepository.updateSettings(transform) }
    }

    private fun runAction(successMessage: SettingsMessage?, action: suspend () -> Unit) {
        if (busyState.value) return
        busyState.value = true
        viewModelScope.launch {
            try {
                action()
                messageState.value = successMessage
            } catch (_: IOException) {
                messageState.value = SettingsMessage.ActionFailed
            } finally {
                busyState.value = false
            }
        }
    }
}
