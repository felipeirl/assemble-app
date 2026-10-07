package dev.assemble.app.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.assemble.app.core.model.AvatarFrame
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.ProfileAccent
import dev.assemble.app.core.model.ProfileCover
import dev.assemble.app.core.model.ProfilePrompt
import dev.assemble.app.core.model.ProfileStyle
import dev.assemble.app.core.model.ProfileTitle
import dev.assemble.app.core.model.SessionState
import dev.assemble.app.core.model.Style
import dev.assemble.app.core.model.Team
import dev.assemble.app.core.model.UserProfile
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import androidx.datastore.preferences.core.Preferences as StoredPreferences

private const val MOCK_SESSION_STORE_NAME = "mock_session"
private const val LIST_SEPARATOR = "\n"

val Context.mockSessionDataStore: DataStore<StoredPreferences> by preferencesDataStore(name = MOCK_SESSION_STORE_NAME)

/** O que a sessão simulada guarda entre aberturas: login, onboarding, preferências e perfil (com a personalização). */
data class StoredSession(
    val session: SessionState,
    val preferences: Preferences,
    val profile: UserProfile,
)

/**
 * Sessão do app sem backend, guardada no aparelho para não repetir login e onboarding a cada abertura.
 * Some quando o login real (Firebase) entrar: lá a sessão e o perfil vêm do servidor.
 */
class MockSessionStore(
    private val dataStore: DataStore<StoredPreferences>,
    private val defaults: StoredSession,
) {

    suspend fun read(): StoredSession = dataStore.data
        .catch { error ->
            // Arquivo ilegível: segue com os padrões (recomendação do DataStore); outros erros sobem.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .first()
        .toStoredSession()

    suspend fun save(stored: StoredSession) {
        dataStore.edit { prefs ->
            prefs[Keys.LOGGED_IN] = stored.session.isLoggedIn
            prefs[Keys.ONBOARDING_DONE] = stored.session.hasCompletedOnboarding
            prefs[Keys.ORIGINS] = stored.preferences.origins.mapTo(mutableSetOf()) { it.name }
            prefs[Keys.POWERS] = stored.preferences.powers.mapTo(mutableSetOf()) { it.name }
            prefs[Keys.TEAMS] = stored.preferences.teams.mapTo(mutableSetOf()) { it.name }
            prefs[Keys.STYLES] = stored.preferences.styles.mapTo(mutableSetOf()) { it.name }
            prefs[Keys.FAME] = stored.preferences.fame
            prefs[Keys.NAME] = stored.profile.name
            prefs[Keys.BIO] = stored.profile.bio
            prefs[Keys.AVATAR] = stored.profile.avatarPreset
            stored.profile.photo?.let { prefs[Keys.PHOTO] = it } ?: prefs.remove(Keys.PHOTO)
            val style = stored.profile.style
            prefs[Keys.COVER] = style.cover.name
            prefs[Keys.ACCENT] = style.accent.name
            prefs[Keys.FRAME] = style.frame.name
            prefs[Keys.PROMPT] = style.prompt.name
            prefs[Keys.PROMPT_ANSWER] = style.promptAnswer
            // Listas com ordem (o conjunto do DataStore não guarda ordem): uma linha por item.
            prefs[Keys.FEATURED_CONNECTIONS] = style.featuredConnections.joinToString(LIST_SEPARATOR)
            prefs[Keys.FEATURED_BADGES] = style.featuredBadges.joinToString(LIST_SEPARATOR)
            style.title?.let { prefs[Keys.TITLE] = it.name } ?: prefs.remove(Keys.TITLE)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val LOGGED_IN = booleanPreferencesKey("logged_in")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val ORIGINS = stringSetPreferencesKey("pref_origins")
        val POWERS = stringSetPreferencesKey("pref_powers")
        val TEAMS = stringSetPreferencesKey("pref_teams")
        val STYLES = stringSetPreferencesKey("pref_styles")
        val FAME = floatPreferencesKey("pref_fame")
        val NAME = stringPreferencesKey("profile_name")
        val BIO = stringPreferencesKey("profile_bio")
        val AVATAR = intPreferencesKey("profile_avatar")
        val PHOTO = stringPreferencesKey("profile_photo")
        val COVER = stringPreferencesKey("style_cover")
        val ACCENT = stringPreferencesKey("style_accent")
        val FRAME = stringPreferencesKey("style_frame")
        val PROMPT = stringPreferencesKey("style_prompt")
        val PROMPT_ANSWER = stringPreferencesKey("style_prompt_answer")
        val FEATURED_CONNECTIONS = stringPreferencesKey("style_featured_connections")
        val FEATURED_BADGES = stringPreferencesKey("style_featured_badges")
        val TITLE = stringPreferencesKey("style_title")
    }

    private fun StoredPreferences.toStoredSession(): StoredSession = StoredSession(
        session = SessionState(
            isLoggedIn = this[Keys.LOGGED_IN] ?: defaults.session.isLoggedIn,
            hasCompletedOnboarding = this[Keys.ONBOARDING_DONE] ?: defaults.session.hasCompletedOnboarding,
        ),
        preferences = Preferences(
            origins = this[Keys.ORIGINS]?.let { enumSet(it, Origin.entries) } ?: defaults.preferences.origins,
            powers = this[Keys.POWERS]?.let { enumSet(it, PowerFamily.entries) } ?: defaults.preferences.powers,
            teams = this[Keys.TEAMS]?.let { enumSet(it, Team.entries) } ?: defaults.preferences.teams,
            styles = this[Keys.STYLES]?.let { enumSet(it, Style.entries) } ?: defaults.preferences.styles,
            fame = this[Keys.FAME] ?: defaults.preferences.fame,
        ),
        profile = UserProfile(
            name = this[Keys.NAME] ?: defaults.profile.name,
            bio = this[Keys.BIO] ?: defaults.profile.bio,
            avatarPreset = this[Keys.AVATAR] ?: defaults.profile.avatarPreset,
            style = toProfileStyle(defaults.profile.style),
            photo = this[Keys.PHOTO] ?: defaults.profile.photo,
        ),
    )

    private fun StoredPreferences.toProfileStyle(fallback: ProfileStyle): ProfileStyle = ProfileStyle(
        cover = enumOr(this[Keys.COVER], ProfileCover.entries, fallback.cover),
        accent = enumOr(this[Keys.ACCENT], ProfileAccent.entries, fallback.accent),
        frame = enumOr(this[Keys.FRAME], AvatarFrame.entries, fallback.frame),
        prompt = enumOr(this[Keys.PROMPT], ProfilePrompt.entries, fallback.prompt),
        promptAnswer = this[Keys.PROMPT_ANSWER] ?: fallback.promptAnswer,
        featuredConnections = this[Keys.FEATURED_CONNECTIONS]?.let(::splitList) ?: fallback.featuredConnections,
        featuredBadges = this[Keys.FEATURED_BADGES]?.let(::splitList) ?: fallback.featuredBadges,
        title = this[Keys.TITLE]?.let { name -> ProfileTitle.entries.firstOrNull { it.name == name } } ?: fallback.title,
    )

    private fun splitList(joined: String): List<String> = joined.split(LIST_SEPARATOR).filter { it.isNotBlank() }

    /** Nome guardado → enum; ausente ou desconhecido (versão antiga) usa o padrão. */
    private fun <E : Enum<E>> enumOr(name: String?, entries: List<E>, fallback: E): E =
        entries.firstOrNull { it.name == name } ?: fallback

    /** Nomes guardados → enums; nome desconhecido (versão antiga) é ignorado. */
    private fun <E : Enum<E>> enumSet(names: Set<String>, entries: List<E>): Set<E> =
        entries.filterTo(mutableSetOf()) { it.name in names }
}
