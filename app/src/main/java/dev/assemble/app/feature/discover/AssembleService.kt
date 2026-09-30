package dev.assemble.app.feature.discover

import dev.assemble.app.core.data.ChatRepository
import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.domain.CompatibilityCalculator
import dev.assemble.app.core.model.Character
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

/**
 * Regra de Assemble compartilhada pelo Discover e pelo CharacterPreview.
 * Assemble = interesse do usuário: só vira conexão se o score atingir o limiar.
 * O resultado fica aqui (match e mensagem) e o Discover o exibe.
 */
class AssembleService(
    private val userRepository: UserRepository,
    private val connectionRepository: ConnectionRepository,
    private val chatRepository: ChatRepository,
) {
    private val matchState = MutableStateFlow<DiscoverMatch?>(null)
    private val messageState = MutableStateFlow<DiscoverMessage?>(null)

    val match: StateFlow<DiscoverMatch?> = matchState.asStateFlow()
    val message: StateFlow<DiscoverMessage?> = messageState.asStateFlow()

    suspend fun assemble(character: Character) {
        userRepository.markSeen(character.id)
        val threshold = CompatibilityCalculator.MATCH_THRESHOLD
        val breakdown = CompatibilityCalculator.breakdown(userRepository.preferences.value, character)
        if (breakdown.score < threshold) {
            messageState.value = DiscoverMessage.NotEnoughInCommon
            return
        }
        try {
            val connection = connectionRepository.connect(character.id, breakdown.score, threshold)
            chatRepository.startConversation(connection.id)
            matchState.value = DiscoverMatch(
                characterId = character.id,
                name = character.name,
                imageUrl = character.imageUrl,
                score = breakdown.score,
                connectionId = connection.id,
                traitsInCommon = breakdown.matchedTraits(),
            )
        } catch (_: IOException) {
            userRepository.unmarkSeen(character.id)
            messageState.value = DiscoverMessage.AssembleFailed
        }
    }

    suspend fun pass(characterId: String) {
        userRepository.markSeen(characterId)
    }

    fun dismissMatch() {
        matchState.value = null
    }

    fun consumeMessage() {
        messageState.value = null
    }
}
