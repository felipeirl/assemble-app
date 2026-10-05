package dev.assemble.app.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.assemble.app.AppContainer
import dev.assemble.app.core.designsystem.component.InAppToast
import dev.assemble.app.core.feedback.Cue
import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import java.io.IOException

private data class IncomingToast(val connectionId: String, val name: String, val imageUrl: String?)

/**
 * Mensagens novas do personagem que chegam com o usuário fora daquela conversa.
 * A primeira mensagem (logo após o match) não gera aviso: o pop-up de match já está na tela.
 * Respeita Settings → Notifications → New messages.
 */
@Composable
fun IncomingMessageToastHost(
    container: AppContainer,
    openConnectionId: String?,
    onOpenConversation: (connectionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var toast by remember { mutableStateOf<IncomingToast?>(null) }
    var visible by remember { mutableStateOf(false) }
    val currentOpenConnectionId by rememberUpdatedState(openConnectionId)

    LaunchedEffect(container) {
        var knownIds: Set<String>? = null
        container.chatRepository.allMessages.collect { messages ->
            val previous = knownIds
            knownIds = messages.map { it.id }.toSet()
            if (previous == null) return@collect
            val incoming = newIncomingMessage(messages, previous, currentOpenConnectionId) ?: return@collect
            if (!container.userRepository.settings.value.notifyNewMessages) return@collect
            val characterId = container.connectionRepository.getConnection(incoming.connectionId)?.characterId
                ?: return@collect
            val character = try {
                container.characterRepository.getCharacter(characterId)
            } catch (_: IOException) {
                null // Sem nome não há aviso; a mensagem continua como não lida na lista.
            } ?: return@collect
            toast = IncomingToast(incoming.connectionId, character.name, character.imageUrl)
            container.feedback.play(Cue.Tick)
            visible = true
        }
    }

    toast?.let { current ->
        InAppToast(
            visible = visible,
            characterName = current.name,
            imageUrl = current.imageUrl,
            onClick = {
                visible = false
                onOpenConversation(current.connectionId)
            },
            onDismiss = { visible = false },
            modifier = modifier,
        )
    }
}

/** Última mensagem nova, não lida, do personagem, fora da conversa aberta e que não seja a primeira. */
internal fun newIncomingMessage(
    messages: List<Message>,
    previousIds: Set<String>,
    openConnectionId: String?,
): Message? = messages
    .filter { it.id !in previousIds && it.author == MessageAuthor.Character && !it.read && it.connectionId != openConnectionId }
    .lastOrNull { candidate -> messages.count { it.connectionId == candidate.connectionId } > 1 }
