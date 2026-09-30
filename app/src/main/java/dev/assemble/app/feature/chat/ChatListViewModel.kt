package dev.assemble.app.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.data.ChatRepository
import dev.assemble.app.core.data.ConnectionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

private const val STOP_TIMEOUT_MILLIS = 5_000L

/** Conversas: uma por conexão, atualizadas ao vivo com novas mensagens. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatListViewModel(
    characterRepository: CharacterRepository,
    connectionRepository: ConnectionRepository,
    chatRepository: ChatRepository,
) : ViewModel() {
    private val reloads = MutableStateFlow(0)

    val uiState: StateFlow<ChatListUiState> = reloads.flatMapLatest {
        flow<ChatListUiState> {
            emit(ChatListUiState.Loading)
            val characters = characterRepository.getCharacters().associateBy { it.id }
            emitAll(
                combine(connectionRepository.observeConnections(), chatRepository.allMessages) { connections, messages ->
                    val summaries = buildConversationSummaries(connections, messages, characters)
                    if (summaries.isEmpty()) ChatListUiState.Empty else ChatListUiState.Content(summaries)
                },
            )
        }.catch { error ->
            if (error !is IOException) throw error
            emit(ChatListUiState.Error)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ChatListUiState.Loading)

    fun retry() {
        reloads.value++
    }
}
