package dev.assemble.app.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.domain.CompatibilityBreakdown
import dev.assemble.app.core.domain.CompatibilityCalculator
import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Pilha do Discover: personagens ainda não vistos nem conectados, ordenados pela compatibilidade
 * com as preferências atuais. Preferências e limiar recalculam a pilha ao vivo.
 */
class DiscoverViewModel(
    private val characterRepository: CharacterRepository,
    private val userRepository: UserRepository,
    private val connectionRepository: ConnectionRepository,
    private val assembleService: AssembleService,
) : ViewModel() {

    private sealed interface Catalog {
        data object Loading : Catalog
        data class Loaded(val characters: List<Character>) : Catalog
        data object Failed : Catalog
    }

    private val catalog = MutableStateFlow<Catalog>(Catalog.Loading)
    private val connectedCharacterIds = MutableStateFlow<Set<String>>(emptySet())
    private val lastPassedId = MutableStateFlow<String?>(null)
    private val refreshing = MutableStateFlow(false)

    private val deck = combine(
        catalog,
        userRepository.preferences,
        userRepository.settings,
        userRepository.seenCharacterIds,
        connectedCharacterIds,
    ) { catalog, preferences, settings, seen, connected ->
        when (catalog) {
            Catalog.Loading -> DeckState.Loading
            Catalog.Failed -> DeckState.Error
            is Catalog.Loaded -> {
                val cards = buildDeck(
                    characters = catalog.characters,
                    preferences = preferences,
                    threshold = settings.minimumCompatibility,
                    excludedIds = seen + connected,
                )
                if (cards.isEmpty()) DeckState.Empty else DeckState.Content(cards)
            }
        }
    }

    val uiState: StateFlow<DiscoverUiState> = combine(deck, lastPassedId, assembleService.match, assembleService.message, refreshing) {
            deck, lastPassed, match, message, refreshing ->
        DiscoverUiState(
            deck = deck,
            canUndo = lastPassed != null,
            refreshing = refreshing,
            match = match,
            message = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DiscoverUiState())

    init {
        load()
    }

    /** Carrega (ou recarrega, no pull to refresh) o catálogo e as conexões. */
    fun load(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) refreshing.value = true else catalog.value = Catalog.Loading
            try {
                val characters = characterRepository.getCharacters()
                connectedCharacterIds.value = connectionRepository.observeConnections().first()
                    .map { it.characterId }
                    .toSet()
                catalog.value = Catalog.Loaded(characters)
            } catch (_: IOException) {
                catalog.value = Catalog.Failed
            } finally {
                refreshing.value = false
            }
        }
    }

    fun pass(characterId: String) {
        viewModelScope.launch {
            assembleService.pass(characterId)
            lastPassedId.value = characterId
        }
    }

    /** Desfaz só o último Pass, uma vez. */
    fun undo() {
        val characterId = lastPassedId.value ?: return
        lastPassedId.value = null
        viewModelScope.launch { userRepository.unmarkSeen(characterId) }
    }

    /** Assemble: vira conexão só se o score atingir o limiar (ver [AssembleService]). */
    fun assemble(characterId: String) {
        val character = (catalog.value as? Catalog.Loaded)?.characters?.firstOrNull { it.id == characterId } ?: return
        lastPassedId.value = null
        viewModelScope.launch { assembleService.assemble(character) }
    }

    fun onMatchDismissed() = assembleService.dismissMatch()

    fun onMessageShown() = assembleService.consumeMessage()
}

/**
 * Pilha do Discover: remove vistos/conectados e ordena por score (desc), depois por nome.
 * A faixa usa o limiar atual; o score exato não sai daqui.
 */
internal fun buildDeck(
    characters: List<Character>,
    preferences: Preferences,
    threshold: Int,
    excludedIds: Set<String>,
): List<DiscoverCard> = characters
    .filter { it.id !in excludedIds }
    .map { character -> character to CompatibilityCalculator.breakdown(preferences, character) }
    .sortedWith(compareByDescending<Pair<Character, CompatibilityBreakdown>> { it.second.score }.thenBy { it.first.name })
    .map { (character, breakdown) ->
        DiscoverCard(
            characterId = character.id,
            name = character.name,
            imageUrl = character.imageUrl,
            band = CompatibilityCalculator.band(breakdown.score, threshold),
            traitsInCommon = breakdown.matchedTraits(),
        )
    }

/** Traços que bateram, na ordem Origin → Powers → Teams → Style. */
internal fun CompatibilityBreakdown.matchedTraits(): List<Enum<*>> =
    origin.matched.sortedBy { it.ordinal } + powers.matched.sortedBy { it.ordinal } +
        teams.matched.sortedBy { it.ordinal } + styles.matched.sortedBy { it.ordinal }
