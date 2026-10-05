package dev.assemble.app.feature.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.feature.discover.AssembleService
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
    private val details: CharacterDetailsSource,
    private val assembleService: AssembleService,
) : ViewModel() {
    private val state = MutableStateFlow<CharacterPreviewUiState>(CharacterPreviewUiState.Loading)
    val uiState: StateFlow<CharacterPreviewUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        state.value = CharacterPreviewUiState.Loading
        viewModelScope.launch {
            try {
                val loaded = details.preview(characterId)
                state.value = if (loaded == null) {
                    CharacterPreviewUiState.Unavailable
                } else {
                    CharacterPreviewUiState.Content(loaded.name, loaded.imageUrl, loaded.traitsInCommon)
                }
            } catch (_: IOException) {
                state.value = CharacterPreviewUiState.Error
            }
        }
    }

    /** [onDone] roda depois da ação, para a tela voltar ao Discover. */
    fun pass(onDone: () -> Unit) = act(onDone) { assembleService.pass(characterId) }

    fun assemble(onDone: () -> Unit) = act(onDone) { assembleService.assemble(characterId) }

    private fun act(onDone: () -> Unit, action: suspend () -> Unit) {
        val content = state.value as? CharacterPreviewUiState.Content ?: return
        if (content.acting) return
        state.update { content.copy(acting = true) }
        viewModelScope.launch {
            action()
            onDone()
        }
    }
}
