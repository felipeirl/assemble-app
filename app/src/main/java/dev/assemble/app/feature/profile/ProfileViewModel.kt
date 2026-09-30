package dev.assemble.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.model.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

private const val STOP_TIMEOUT_MILLIS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    characterRepository: CharacterRepository,
    connectionRepository: ConnectionRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val reloads = MutableStateFlow(0)
    private val messageState = MutableStateFlow<ProfileMessage?>(null)
    val message: StateFlow<ProfileMessage?> = messageState.asStateFlow()

    val uiState: StateFlow<ProfileUiState> = reloads.flatMapLatest {
        flow<ProfileUiState> {
            emit(ProfileUiState.Loading)
            val characters = characterRepository.getCharacters().associateBy { it.id }
            emitAll(
                combine(
                    userRepository.observeProfile(),
                    userRepository.preferences,
                    userRepository.seenCharacterIds,
                    connectionRepository.observeConnections(),
                ) { profile, preferences, seen, connections ->
                    val connected = connections.mapNotNull { characters[it.characterId] }
                    ProfileUiState.Content(
                        profile = profile,
                        stats = profileStats(seen, connections),
                        preferences = preferences,
                        topTraits = topTraits(connected),
                        connections = connected.map { ProfileConnection(it.id, it.name, it.imageUrl) },
                    )
                },
            )
        }.catch { error ->
            if (error !is IOException) throw error
            emit(ProfileUiState.Error)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProfileUiState.Loading)

    /** Grava a edição de uma categoria; o Discover recalcula sozinho. */
    fun savePreferences(preferences: Preferences) {
        viewModelScope.launch {
            messageState.value = try {
                userRepository.updatePreferences(preferences)
                ProfileMessage.PreferencesUpdated
            } catch (_: IOException) {
                ProfileMessage.PreferencesFailed
            }
        }
    }

    fun onMessageShown() {
        messageState.value = null
    }

    fun retry() {
        reloads.value++
    }
}
