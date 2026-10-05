package dev.assemble.app.feature.discover

import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.data.ChatRepository
import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.domain.CompatibilityCalculator
import dev.assemble.app.core.model.Character
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * Baralho sem backend: catálogo local ordenado pela compatibilidade com as preferências atuais,
 * sem os já vistos nem os conectados. Preferências recalculam a pilha ao vivo.
 * Assemble vira conexão quando o score atinge o limiar.
 */
class LocalDeckSource(
    private val characterRepository: CharacterRepository,
    private val userRepository: UserRepository,
    private val connectionRepository: ConnectionRepository,
    private val chatRepository: ChatRepository,
) : DeckSource {

    private sealed interface Catalog {
        data object Loading : Catalog
        data class Loaded(val characters: List<Character>) : Catalog
        data object Failed : Catalog
    }

    private val catalog = MutableStateFlow<Catalog>(Catalog.Loading)
    private val connectedCharacterIds = MutableStateFlow<Set<String>>(emptySet())
    private val lastPassedId = MutableStateFlow<String?>(null)
    private val undoState = MutableStateFlow(false)

    override val canUndo: StateFlow<Boolean> = undoState.asStateFlow()

    override val deck: Flow<DeckState> = combine(
        catalog,
        userRepository.preferences,
        userRepository.seenCharacterIds,
        connectedCharacterIds,
    ) { catalog, preferences, seen, connected ->
        when (catalog) {
            Catalog.Loading -> DeckState.Loading
            Catalog.Failed -> DeckState.Error
            is Catalog.Loaded -> {
                val cards = buildDeck(
                    characters = catalog.characters,
                    preferences = preferences,
                    excludedIds = seen + connected,
                )
                if (cards.isEmpty()) DeckState.Empty else DeckState.Content(cards)
            }
        }
    }

    override suspend fun reload() {
        catalog.value = Catalog.Loading
        try {
            val characters = characterRepository.getCharacters()
            connectedCharacterIds.value = connectionRepository.observeConnections().first()
                .mapTo(mutableSetOf()) { it.characterId }
            catalog.value = Catalog.Loaded(characters)
        } catch (_: IOException) {
            catalog.value = Catalog.Failed
        }
    }

    override suspend fun pass(characterId: String) {
        userRepository.markSeen(characterId)
        lastPassedId.value = characterId
        undoState.value = true
    }

    override suspend fun undo() {
        val characterId = lastPassedId.value ?: return
        lastPassedId.value = null
        undoState.value = false
        userRepository.unmarkSeen(characterId)
    }

    override suspend fun dismiss(characterId: String) {
        lastPassedId.value = null
        undoState.value = false
        userRepository.markSeen(characterId)
    }

    override suspend fun assemble(characterId: String): AssembleOutcome {
        val character = characterRepository.getCharacter(characterId) ?: throw IOException("Personagem fora do catálogo")
        val threshold = CompatibilityCalculator.MATCH_THRESHOLD
        val breakdown = CompatibilityCalculator.breakdown(userRepository.preferences.value, character)
        if (breakdown.score < threshold) return AssembleOutcome.NotMatched
        val connection = connectionRepository.connect(character.id, breakdown.score, threshold)
        chatRepository.startConversation(connection.id)
        connectedCharacterIds.value = connectedCharacterIds.value + character.id
        return AssembleOutcome.Matched(
            DiscoverMatch(
                characterId = character.id,
                name = character.name,
                imageUrl = character.imageUrl,
                score = breakdown.score,
                connectionId = connection.id,
                traitsInCommon = breakdown.matchedTraits(),
            ),
        )
    }

    override suspend fun restore(characterId: String) {
        userRepository.unmarkSeen(characterId)
    }
}
