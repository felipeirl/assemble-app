package dev.assemble.app.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import androidx.datastore.preferences.core.Preferences as StoredPreferences

private const val ACTIVITY_STORE_NAME = "activity"

val Context.activityDataStore: DataStore<StoredPreferences> by preferencesDataStore(name = ACTIVITY_STORE_NAME)

/** Dias (data local, ISO) em que o app foi aberto; alimenta "Sentinela". Só no aparelho: reinstalar zera. */
class ActiveDaysStore(private val dataStore: DataStore<StoredPreferences>, private val clock: Clock) {

    val count: Flow<Int> = dataStore.data
        .catch { error ->
            // Arquivo ilegível: conta do zero (recomendação do DataStore); outros erros sobem.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it[Keys.DAYS].orEmpty().size }

    suspend fun markToday() {
        val today = LocalDate.now(clock).toString()
        dataStore.edit { it[Keys.DAYS] = it[Keys.DAYS].orEmpty() + today }
    }

    private object Keys {
        val DAYS = stringSetPreferencesKey("active_days")
    }
}
