package dev.assemble.app.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dev.assemble.app.core.model.AvatarFrame
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.ProfileAccent
import dev.assemble.app.core.model.ProfileCover
import dev.assemble.app.core.model.ProfilePrompt
import dev.assemble.app.core.model.ProfileStyle
import dev.assemble.app.core.model.SessionState
import dev.assemble.app.core.model.Style
import dev.assemble.app.core.model.Team
import dev.assemble.app.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import androidx.datastore.preferences.core.Preferences as StoredPreferences

/** [MockSessionStore] sobre um DataStore em memória (o de arquivo não sobrescreve no Windows). */
class MockSessionStoreTest {

    private class InMemoryDataStore : DataStore<StoredPreferences> {
        val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<StoredPreferences> = state

        override suspend fun updateData(transform: suspend (t: StoredPreferences) -> StoredPreferences): StoredPreferences =
            transform(state.value).also { state.value = it }
    }

    private val defaults = StoredSession(
        session = SessionState(),
        preferences = Preferences.Any,
        profile = UserProfile(name = "Felipe", bio = "", avatarPreset = 0),
    )
    private val dataStore = InMemoryDataStore()
    private val store = MockSessionStore(dataStore, defaults)

    @Test
    fun emptyStore_returnsDefaults() = runBlocking {
        assertEquals(defaults, store.read())
    }

    @Test
    fun save_roundTripsSessionPreferencesAndProfile() = runBlocking {
        val saved = StoredSession(
            session = SessionState(isLoggedIn = true, hasCompletedOnboarding = true),
            preferences = Preferences(
                origins = setOf(Origin.Mutant),
                powers = setOf(PowerFamily.Mind, PowerFamily.Flight),
                teams = setOf(Team.XMen),
                styles = setOf(Style.Leadership),
                fame = 0.3f,
            ),
            profile = UserProfile(
                name = "Jean",
                bio = "Telepath fan",
                avatarPreset = 4,
                style = ProfileStyle(
                    cover = ProfileCover.Comic,
                    accent = ProfileAccent.Violet,
                    frame = AvatarFrame.Hexagon,
                    prompt = ProfilePrompt.IdealTeam,
                    promptAnswer = "X-Men, com a Storm na liderança",
                    featuredConnections = listOf("storm", "rocket", "jean-grey"),
                    featuredBadges = listOf("Crossover", "FirstConnection"),
                ),
            ),
        )
        store.save(saved)
        assertEquals(saved, store.read())
    }

    @Test
    fun photo_isPersistedAndRemoved() = runBlocking {
        val withPhoto = defaults.copy(profile = defaults.profile.copy(photo = "AAAA"))
        store.save(withPhoto)
        assertEquals("AAAA", store.read().profile.photo)

        store.save(withPhoto.copy(profile = withPhoto.profile.copy(photo = null)))
        assertEquals(null, store.read().profile.photo)
    }

    @Test
    fun clear_restoresDefaults() = runBlocking {
        store.save(defaults.copy(session = SessionState(isLoggedIn = true, hasCompletedOnboarding = true)))
        store.clear()
        assertEquals(defaults, store.read())
    }

    @Test
    fun unknownEnumNames_areIgnored() = runBlocking {
        store.save(defaults.copy(preferences = Preferences.Any.copy(origins = setOf(Origin.Mutant))))
        // Simula um valor gravado por uma versão antiga, com um nome que não existe mais.
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply { this[stringSetPreferencesKey("pref_origins")] = setOf("Mutant", "Removed") }
        }
        assertEquals(setOf(Origin.Mutant), store.read().preferences.origins)
    }
}
