package dev.assemble.app.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.emptyPreferences
import dev.assemble.app.core.model.AppSettings
import dev.assemble.app.core.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import androidx.datastore.preferences.core.Preferences as StoredPreferences

/**
 * Testa o mapeamento de [SettingsDataStore] sobre um DataStore em memória.
 * (O armazenamento em arquivo do DataStore não sobrescreve arquivos no Windows, onde estes testes rodam.)
 */
class SettingsDataStoreTest {

    private class InMemoryDataStore : DataStore<StoredPreferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<StoredPreferences> = state

        override suspend fun updateData(transform: suspend (t: StoredPreferences) -> StoredPreferences): StoredPreferences =
            transform(state.value).also { state.value = it }
    }

    private val store = SettingsDataStore(InMemoryDataStore())

    @Test
    fun emptyStore_returnsDefaults() = runBlocking {
        assertEquals(AppSettings(), store.settings.first())
    }

    @Test
    fun update_persistsAllFields() = runBlocking {
        store.update {
            it.copy(theme = ThemePreference.Dark, minimumCompatibility = 80, notifyNewMessages = false)
        }
        val saved = store.settings.first()
        assertEquals(ThemePreference.Dark, saved.theme)
        assertEquals(80, saved.minimumCompatibility)
        assertFalse(saved.notifyNewMessages)
    }

    @Test
    fun minimumCompatibility_isClampedTo50Through90() = runBlocking {
        store.update { it.copy(minimumCompatibility = 120) }
        assertEquals(AppSettings.MAX_MINIMUM_COMPATIBILITY, store.settings.first().minimumCompatibility)
        store.update { it.copy(minimumCompatibility = 10) }
        assertEquals(AppSettings.MIN_MINIMUM_COMPATIBILITY, store.settings.first().minimumCompatibility)
    }

    @Test
    fun reset_restoresDefaults() = runBlocking {
        store.update { it.copy(theme = ThemePreference.Light, minimumCompatibility = 60) }
        store.reset()
        assertEquals(AppSettings(), store.settings.first())
    }
}
