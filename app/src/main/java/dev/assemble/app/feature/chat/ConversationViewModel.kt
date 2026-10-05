package dev.assemble.app.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.data.ChatRepository
import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import kotlinx.coroutines.CoroutineScope
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

/**
 * Enquanto a última resposta do personagem é gerada de novo, o texto antigo sai da lista e o
 * "digitando" ocupa o lugar dela: a resposta nova substitui a antiga sem mostrar a velha de novo.
 */
internal fun withoutReplyBeingRegenerated(messages: List<Message>, regenerating: Boolean): List<Message> =
    if (regenerating && messages.lastOrNull()?.author == MessageAuthor.Character) messages.dropLast(1) else messages

/**
 * Conversa com a versão ficcional do personagem. Envio e reenvio rodam em [applicationScope]
 * para a resposta chegar mesmo se o usuário sair da tela (vira InAppToast).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModel(
    private val connectionId: String,
    characterRepository: CharacterRepository,
    connectionRepository: ConnectionRepository,
    private val chatRepository: ChatRepository,
    private val applicationScope: CoroutineScope,
) : ViewModel() {
    private val reloads = MutableStateFlow(0)
    private val draftState = MutableStateFlow("")
    private val noticeState = MutableStateFlow<ConversationNotice?>(null)
    val draft: StateFlow<String> = draftState.asStateFlow()

    val uiState: StateFlow<ConversationUiState> = reloads.flatMapLatest {
        flow<ConversationUiState> {
            emit(ConversationUiState.Loading)
            val connection = connectionRepository.getConnection(connectionId)
            val character = connection?.let { characterRepository.getCharacter(it.characterId) }
            if (character == null) {
                emit(ConversationUiState.Unavailable)
                return@flow
            }
            emitAll(
                combine(
                    chatRepository.observeMessages(connectionId),
                    chatRepository.typingConnectionIds,
                    chatRepository.observeSuggestions(connectionId),
                    noticeState,
                    chatRepository.regeneratingConnectionIds,
                ) { messages, typing, fromBackend, notice, regenerating ->
                    ConversationUiState.Content(
                        characterId = character.id,
                        name = character.name,
                        imageUrl = character.imageUrl,
                        messages = withoutReplyBeingRegenerated(messages, connectionId in regenerating),
                        typing = connectionId in typing,
                        // As do backend vêm junto com cada resposta; sem elas, o app monta as suas.
                        suggestions = fromBackend?.let(::literalSuggestions)
                            ?: replySuggestions(character, messagesSent = messages.count { it.author == MessageAuthor.User }),
                        notice = notice,
                    )
                },
            )
        }.catch { error ->
            if (error !is IOException) throw error
            emit(ConversationUiState.Error)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ConversationUiState.Loading)

    fun onDraftChange(text: String) {
        draftState.value = text
    }

    fun send() {
        val text = draftState.value.trim()
        if (text.isEmpty()) return
        draftState.value = ""
        sendText(text)
    }

    /** Toque numa resposta sugerida: envia direto, sem passar pelo campo. */
    fun sendSuggestion(text: String) {
        sendText(text)
    }

    private fun sendText(text: String) {
        applicationScope.launch { chatRepository.send(connectionId, text) }
    }

    /** Gera outra resposta no lugar da última do personagem. */
    fun regenerate() {
        runAction { chatRepository.regenerateLast(connectionId) }
    }

    /** Volta a conversa até uma resposta do personagem; o que veio depois é apagado. */
    fun rewindTo(messageId: String) {
        runAction { chatRepository.rewindTo(connectionId, messageId) }
    }

    fun onNoticeShown() {
        noticeState.value = null
    }

    private fun runAction(action: suspend () -> Unit) {
        applicationScope.launch {
            try {
                action()
            } catch (_: IOException) {
                noticeState.value = ConversationNotice.ActionFailed
            }
        }
    }

    fun retry(messageId: String) {
        applicationScope.launch { chatRepository.retry(messageId) }
    }

    /** Chamado pela tela visível: as respostas que chegam enquanto ela está aberta contam como lidas. */
    fun markRead() {
        viewModelScope.launch { chatRepository.markRead(connectionId) }
    }

    fun reload() {
        reloads.value++
    }
}
