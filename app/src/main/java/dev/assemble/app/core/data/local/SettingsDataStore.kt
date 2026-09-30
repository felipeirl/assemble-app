package dev.assemble.app.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.assemble.app.core.model.AppSettings
import dev.assemble.app.core.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import androidx.datastore.preferences.core.Preferences as StoredPreferences

private const val SETTINGS_STORE_NAME = "settings"

val Context.settingsDataStore: DataStore<StoredPreferences> by preferencesDataStore(name = SETTINGS_STORE_NAME)

/** Configurações locais do Settings (tema e notificações) persistidas em DataStore. */
class SettingsDataStore(private val dataStore: DataStore<StoredPreferences>) {

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            // Arquivo ilegível: segue com os padrões (recomendação do DataStore); outros erros sobem.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toAppSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { stored ->
            val updated = transform(stored.toAppSettings())
            stored[Keys.THEME] = updated.theme.name
            stored[Keys.NOTIFY_CONNECTIONS] = updated.notifyNewConnections
            stored[Keys.NOTIFY_MESSAGES] = updated.notifyNewMessages
        }
    }

    suspend fun reset() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val NOTIFY_CONNECTIONS = booleanPreferencesKey("notify_new_connections")
        val NOTIFY_MESSAGES = booleanPreferencesKey("notify_new_messages")
    }

    private fun StoredPreferences.toAppSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            theme = this[Keys.THEME]?.let { name -> ThemePreference.entries.firstOrNull { it.name == name } } ?: defaults.theme,
            notifyNewConnections = this[Keys.NOTIFY_CONNECTIONS] ?: defaults.notifyNewConnections,
            notifyNewMessages = this[Keys.NOTIFY_MESSAGES] ?: defaults.notifyNewMessages,
        )
    }
}
