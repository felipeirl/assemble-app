package dev.assemble.app.feature.discover

import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.data.remote.RemoteCharacterRepository
import dev.assemble.app.core.data.remote.basicCharacter
import dev.assemble.app.core.model.traitsFromNames
import dev.assemble.app.core.network.ApiDeckCard
import dev.assemble.app.core.network.ApiErrorCode
import dev.assemble.app.core.network.ApiException
import dev.assemble.app.core.network.AssembleApi
import dev.assemble.app.core.network.DecisionChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.util.UUID

/**
 * Baralho do backend (contrato §4): até 30 por dia, sem reposição; quem recebeu Pass não volta.
 * O backend decide o match e já grava a conexão e a fala de abertura antes de responder.
 */
class RemoteDeckSource(
    private val api: AssembleApi,
    private val characters: RemoteCharacterRepository,
    private val userRepository: UserRepository,
    private val newKey: () -> String = { UUID.randomUUID().toString() },
) : DeckSource {

    private sealed interface Remote {
        data object Loading : Remote
        data class Loaded(val cards: List<DiscoverCard>) : Remote
        data object Failed : Remote
    }

    private val state = MutableStateFlow<Remote>(Remote.Loading)
    private val undoState = MutableStateFlow(false)

    /** Cards fora da tela à espera da resposta do Assemble (voltam se ele falhar). */
    private val dismissed = mutableMapOf<String, DiscoverCard>()

    /** Mesma chave para repetir um Assemble que falhou: o backend devolve o mesmo resultado. */
    private val assembleKeys = mutableMapOf<String, String>()

    override val canUndo: StateFlow<Boolean> = undoState.asStateFlow()

    override val deck: Flow<DeckState> = state.map { remote ->
        when (remote) {
            Remote.Loading -> DeckState.Loading
            Remote.Failed -> DeckState.Error
            is Remote.Loaded -> if (remote.cards.isEmpty()) DeckState.Empty else DeckState.Content(remote.cards)
        }
    }

    override suspend fun reload() {
        state.value = Remote.Loading
        state.value = try {
            val deck = api.deck()
            undoState.value = deck.canUndo
            Remote.Loaded(deck.cards.map { it.toCard() })
        } catch (_: IOException) {
            Remote.Failed
        }
    }

    /** O card sai na hora; se o backend recusar, ele volta e o usuário vê de novo. */
    override suspend fun pass(characterId: String) {
        val card = remove(characterId) ?: return
        userRepository.markSeen(characterId)
        try {
            api.decide(characterId, DecisionChoice.PASS, newKey())
            undoState.value = true
        } catch (error: IOException) {
            if (error is ApiException && error.code == ApiErrorCode.ALREADY_DECIDED) return
            userRepository.unmarkSeen(characterId)
            putOnTop(card)
        }
    }

    override suspend fun undo() {
        try {
            val card = api.undo().toCard()
            userRepository.unmarkSeen(card.characterId)
            putOnTop(card)
        } catch (error: IOException) {
            if (error !is ApiException || error.code != ApiErrorCode.NOTHING_TO_UNDO) throw error
        } finally {
            undoState.value = false
        }
    }

    override suspend fun dismiss(characterId: String) {
        remove(characterId)?.let { dismissed[characterId] = it }
        userRepository.markSeen(characterId)
    }

    override suspend fun assemble(characterId: String): AssembleOutcome {
        val key = assembleKeys.getOrPut(characterId, newKey)
        val result = try {
            api.decide(characterId, DecisionChoice.ASSEMBLE, key)
        } catch (error: ApiException) {
            // Decidido antes sem esta chave (outro aparelho, app reinstalado): não há o que mostrar.
            if (error.code == ApiErrorCode.ALREADY_DECIDED) {
                dismissed.remove(characterId)
                return AssembleOutcome.NotMatched
            }
            throw error
        } ?: throw IOException("Assemble sem resposta")
        dismissed.remove(characterId)
        assembleKeys.remove(characterId)
        val character = result.character
        if (!result.matched || character == null || result.connectionId == null || result.score == null) {
            return AssembleOutcome.NotMatched
        }
        characters.remember(basicCharacter(character.characterId, character.name, character.imageUrl))
        return AssembleOutcome.Matched(
            DiscoverMatch(
                characterId = character.characterId,
                name = character.name,
                imageUrl = character.imageUrl,
                score = result.score,
                connectionId = result.connectionId,
                traitsInCommon = traitsFromNames(result.reasons),
            ),
        )
    }

    override suspend fun restore(characterId: String) {
        val card = dismissed.remove(characterId) ?: return
        userRepository.unmarkSeen(characterId)
        putOnTop(card)
    }

    private fun remove(characterId: String): DiscoverCard? {
        val loaded = state.value as? Remote.Loaded ?: return null
        val card = loaded.cards.firstOrNull { it.characterId == characterId } ?: return null
        state.value = Remote.Loaded(loaded.cards - card)
        return card
    }

    private fun putOnTop(card: DiscoverCard) {
        state.update { remote ->
            val cards = (remote as? Remote.Loaded)?.cards.orEmpty().filterNot { it.characterId == card.characterId }
            Remote.Loaded(listOf(card) + cards)
        }
    }

    private fun ApiDeckCard.toCard(): DiscoverCard {
        characters.remember(basicCharacter(characterId, name, imageUrl))
        return DiscoverCard(
            characterId = characterId,
            name = name,
            imageUrl = imageUrl,
            traitsInCommon = traitsFromNames(traitsInCommon),
            tagline = tagline,
        )
    }
}
