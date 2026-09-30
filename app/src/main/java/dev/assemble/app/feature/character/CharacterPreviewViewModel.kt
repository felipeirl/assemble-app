package dev.assemble.app.feature.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.domain.CompatibilityCalculator
import dev.assemble.app.core.model.Character
import dev.assemble.app.feature.discover.AssembleService
import dev.assemble.app.feature.discover.matchedTraits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Preview de um personagem ainda sem conexão: só a faixa e os traços em comum.
 * Pass e Assemble usam a mesma regra do Discover; o resultado (match ou aviso) aparece lá.
 */
class CharacterPreviewViewModel(
    private val characterId: String,
    private val characterRepository: CharacterRepository,
    private val userRepository: UserRepository,
    private val assembleService: AssembleService,
) : ViewModel() {
    private val state = MutableStateFlow<CharacterPreviewUiState>(CharacterPreviewUiState.Loading)
    val uiState: StateFlow<CharacterPreviewUiState> = state.asStateFlow()

    private var character: Character? = null

    init {
        load()
    }

    fun load() {
        state.value = CharacterPreviewUiState.Loading
        viewModelScope.launch {
            try {
                val loaded = characterRepository.getCharacter(characterId)
                character = loaded
                state.value = if (loaded == null) CharacterPreviewUiState.Unavailable else loaded.toContent()
            } catch (_: IOException) {
                state.value = CharacterPreviewUiState.Error
            }
        }
    }

    /** [onDone] roda depois da ação, para a tela voltar ao Discover. */
    fun pass(onDone: () -> Unit) = act(onDone) { assembleService.pass(characterId) }

    fun assemble(onDone: () -> Unit) {
        val current = character ?: return
        act(onDone) { assembleService.assemble(current) }
    }

    private fun act(onDone: () -> Unit, action: suspend () -> Unit) {
        val content = state.value as? CharacterPreviewUiState.Content ?: return
        if (content.acting) return
        state.update { content.copy(acting = true) }
        viewModelScope.launch {
            action()
            onDone()
        }
    }

    private fun Character.toContent(): CharacterPreviewUiState.Content {
        val breakdown = CompatibilityCalculator.breakdown(userRepository.preferences.value, this)
        return CharacterPreviewUiState.Content(
            name = name,
            imageUrl = imageUrl,
            band = CompatibilityCalculator.band(breakdown.score),
            traitsInCommon = breakdown.matchedTraits(),
        )
    }
}
