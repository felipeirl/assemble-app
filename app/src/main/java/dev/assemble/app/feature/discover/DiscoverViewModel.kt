package dev.assemble.app.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.domain.CompatibilityBreakdown
import dev.assemble.app.core.domain.CompatibilityCalculator
import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Discover: o baralho e as decisões vêm do [DeckSource] (cálculo local ou backend). O Assemble
 * passa pelo [AssembleService], que mostra o resultado (pop-up ou aviso) mesmo depois de sair da tela.
 */
class DiscoverViewModel(
    private val deckSource: DeckSource,
    userRepository: UserRepository,
    private val assembleService: AssembleService,
) : ViewModel() {

    val uiState: StateFlow<DiscoverUiState> = combine(
        deckSource.deck,
        deckSource.canUndo,
        assembleService.match,
        assembleService.message,
        assembleService.assembling,
    ) { deck, canUndo, match, message, assembling ->
        DiscoverUiState(
            deck = deck,
            canUndo = canUndo,
            match = match,
            message = message,
            assembling = assembling,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DiscoverUiState())

    /** Para o avatar do usuário no pop-up de match. */
    val userProfile: StateFlow<UserProfile> = userRepository.currentProfile

    init {
        load()
    }

    /** Carrega (ou recarrega, no "tentar de novo") o baralho. */
    fun load() {
        viewModelScope.launch { deckSource.reload() }
    }

    fun pass(characterId: String) {
        viewModelScope.launch { assembleService.pass(characterId) }
    }

    /** Desfaz só o último Pass, uma vez. */
    fun undo() {
        viewModelScope.launch {
            try {
                deckSource.undo()
            } catch (_: IOException) {
                // Sem confirmação do servidor, relê o baralho para mostrar o estado real.
                deckSource.reload()
            }
        }
    }

    /** Assemble com o card já fora da tela: o personagem sai do baralho e responde depois (ver [AssembleService]). */
    fun assemble(characterId: String) {
        viewModelScope.launch { assembleService.assemble(characterId) }
    }

    fun onMatchDismissed() = assembleService.dismissMatch()

    fun onMessageShown() = assembleService.consumeMessage()
}

/**
 * Pilha do Discover: remove vistos/conectados e ordena por score (desc), depois por nome.
 * O score só ordena; não sai daqui.
 */
internal fun buildDeck(
    characters: List<Character>,
    preferences: Preferences,
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
            traitsInCommon = breakdown.matchedTraits(),
        )
    }

/** Traços que bateram, na ordem Origin → Powers → Teams → Style. */
internal fun CompatibilityBreakdown.matchedTraits(): List<Enum<*>> =
    origin.matched.sortedBy { it.ordinal } + powers.matched.sortedBy { it.ordinal } +
        teams.matched.sortedBy { it.ordinal } + styles.matched.sortedBy { it.ordinal }
