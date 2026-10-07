package dev.assemble.app.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import androidx.datastore.preferences.core.Preferences as StoredPreferences

class ActiveDaysStoreTest {

    private class InMemoryDataStore : DataStore<StoredPreferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<StoredPreferences> = state

        override suspend fun updateData(transform: suspend (t: StoredPreferences) -> StoredPreferences): StoredPreferences =
            transform(state.value).also { state.value = it }
    }

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val dataStore = InMemoryDataStore()

    private fun storeAt(instant: String) = ActiveDaysStore(dataStore, Clock.fixed(Instant.parse(instant), zone))

    @Test
    fun emptyStore_countsZero() = runBlocking {
        assertEquals(0, storeAt("2026-10-07T12:00:00Z").count.first())
    }

    @Test
    fun sameDayCountsOnce() = runBlocking {
        storeAt("2026-10-07T12:00:00Z").markToday()
        storeAt("2026-10-07T20:00:00Z").markToday()
        assertEquals(1, storeAt("2026-10-07T20:00:00Z").count.first())
    }

    @Test
    fun differentLocalDaysCountSeparately() = runBlocking {
        storeAt("2026-10-07T12:00:00Z").markToday()
        // 02:00Z do dia 8 ainda é dia 7 em São Paulo (UTC-3): não conta.
        storeAt("2026-10-08T02:00:00Z").markToday()
        storeAt("2026-10-09T12:00:00Z").markToday()
        assertEquals(2, storeAt("2026-10-09T12:00:00Z").count.first())
    }
}
