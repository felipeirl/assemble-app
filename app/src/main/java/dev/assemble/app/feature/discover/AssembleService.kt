package dev.assemble.app.feature.discover

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Quanto o personagem "pensa" antes de responder a um Assemble (sem backend). */
val CharacterReplyDelay: Duration = 2_500.milliseconds

/**
 * Regra de Assemble compartilhada pelo Discover e pelo CharacterPreview. O card sai na hora;
 * a resposta do personagem (match ou não) chega depois, em [scope], para sobreviver à tela.
 * Quem decide é o [DeckSource]: o cálculo local ou o backend. Com backend, a própria chamada
 * leva alguns segundos (a fala de abertura é gerada antes da resposta), então [replyDelay] é zero.
 */
class AssembleService(
    private val deckSource: DeckSource,
    private val scope: CoroutineScope,
    private val replyDelay: Duration = CharacterReplyDelay,
) {
    // Fila: dois matches seguidos aparecem um depois do outro, nunca um por cima do outro.
    private val matchQueue = MutableStateFlow<List<DiscoverMatch>>(emptyList())
    private val messageState = MutableStateFlow<DiscoverMessage?>(null)
    private val inFlight = MutableStateFlow(0)

    val match: Flow<DiscoverMatch?> = matchQueue.map { it.firstOrNull() }.distinctUntilChanged()
    val message: StateFlow<DiscoverMessage?> = messageState.asStateFlow()

    /** Há Assemble esperando a resposta do personagem (com backend pode levar vários segundos). */
    val assembling: Flow<Boolean> = inFlight.map { it > 0 }.distinctUntilChanged()

    /**
     * Tira o personagem do baralho na hora e agenda a resposta. Com match, a conexão e a fala de
     * abertura já existem quando o pop-up aparece. Se o Assemble falhar, o personagem volta ao
     * baralho e o usuário é avisado.
     */
    suspend fun assemble(characterId: String) {
        deckSource.dismiss(characterId)
        inFlight.update { it + 1 }
        scope.launch {
            try {
                delay(replyDelay)
                when (val outcome = deckSource.assemble(characterId)) {
                    is AssembleOutcome.Matched -> matchQueue.update { it + outcome.match }
                    AssembleOutcome.NotMatched -> messageState.value = DiscoverMessage.NotEnoughInCommon
                }
            } catch (_: IOException) {
                deckSource.restore(characterId)
                messageState.value = DiscoverMessage.AssembleFailed
            } finally {
                inFlight.update { it - 1 }
            }
        }
    }

    suspend fun pass(characterId: String) {
        deckSource.pass(characterId)
    }

    /** Fecha o pop-up atual; o próximo da fila, se houver, aparece em seguida. */
    fun dismissMatch() {
        matchQueue.update { it.drop(1) }
    }

    fun consumeMessage() {
        messageState.value = null
    }
}
