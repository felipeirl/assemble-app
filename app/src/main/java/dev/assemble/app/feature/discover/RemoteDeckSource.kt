package dev.assemble.app.feature.discover

import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.data.remote.MatchDocument
import dev.assemble.app.core.data.remote.RemoteCharacterRepository
import dev.assemble.app.core.data.remote.RemoteConnectionRepository
import dev.assemble.app.core.data.remote.UserDataStore
import dev.assemble.app.core.data.remote.basicCharacter
import dev.assemble.app.core.model.traitsFromNames
import dev.assemble.app.core.network.ApiDeckCard
import dev.assemble.app.core.network.ApiErrorCode
import dev.assemble.app.core.network.ApiException
import dev.assemble.app.core.network.ApiStatus
import dev.assemble.app.core.network.AssembleApi
import dev.assemble.app.core.network.DecisionChoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Quanto esperar a fila do backend decidir o match (afinidade no Laya e fala de abertura). */
private const val DECISION_WAIT_MILLIS = 90_000L

/** A conexão é gravada antes do resultado: depois dele, ela chega pelo listener em instantes. */
private const val MATCH_WAIT_MILLIS = 30_000L

/** Reenvio dos Pass sem rede: 5 s, 10 s, 20 s... até [OUTBOX_MAX_ROUNDS] rodadas. */
private const val OUTBOX_FIRST_DELAY_MILLIS = 5_000L
private const val OUTBOX_MAX_ROUNDS = 6

private val HTTP_CLIENT_ERRORS = 400..499

/**
 * Baralho do backend (contrato §4): até 40 por dia, sem reposição; quem recebeu Pass não volta.
 * O Assemble é aceito na hora (202) e decidido numa fila do backend: o resultado chega pelo
 * Firestore (a decisão e, com match, a conexão com a fala de abertura).
 */
class RemoteDeckSource(
    private val api: AssembleApi,
    private val characters: RemoteCharacterRepository,
    private val userRepository: UserRepository,
    private val connections: RemoteConnectionRepository,
    private val store: UserDataStore,
    private val uid: StateFlow<String?>,
    private val scope: CoroutineScope,
    private val newKey: () -> String = { UUID.randomUUID().toString() },
    private val decisionWaitMillis: Long = DECISION_WAIT_MILLIS,
    private val matchWaitMillis: Long = MATCH_WAIT_MILLIS,
    private val outboxFirstDelayMillis: Long = OUTBOX_FIRST_DELAY_MILLIS,
) : DeckSource {

    private sealed interface Remote {
        data object Loading : Remote
        data class Loaded(val cards: List<DiscoverCard>) : Remote
        data object Failed : Remote
    }

    private val state = MutableStateFlow<Remote>(Remote.Loading)
    private val undoState = MutableStateFlow(false)

    /** Cards fora da tela à espera da resposta do Assemble (voltam se o envio falhar). */
    private val dismissed = ConcurrentHashMap<String, DiscoverCard>()

    /** Mesma chave para repetir um Assemble que falhou: o backend devolve o mesmo resultado. */
    private val assembleKeys = ConcurrentHashMap<String, String>()

    /** Pass sem rede, com a chave de cada um: reenviados em segundo plano, nunca voltam ao baralho. */
    private val outbox = ConcurrentHashMap<String, String>()
    private var flushJob: Job? = null

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
            // Um Pass ainda na fila local não chegou ao backend: o card não pode voltar.
            Remote.Loaded(deck.cards.filterNot { outbox.containsKey(it.characterId) }.map { it.toCard() })
        } catch (_: IOException) {
            Remote.Failed
        }
    }

    /** O card sai na hora. Sem rede, o Pass fica na fila local; só uma recusa do backend o devolve. */
    override suspend fun pass(characterId: String) {
        val card = remove(characterId) ?: return
        userRepository.markSeen(characterId)
        val key = newKey()
        try {
            api.decide(characterId, DecisionChoice.PASS, key)
            undoState.value = true
        } catch (error: IOException) {
            if (error is ApiException && error.code == ApiErrorCode.ALREADY_DECIDED) return
            if (error is ApiException && error.httpStatus in HTTP_CLIENT_ERRORS) {
                userRepository.unmarkSeen(characterId)
                putOnTop(card)
                return
            }
            outbox[characterId] = key
            flushOutbox()
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

    /**
     * Manda o Assemble e espera a fila do backend decidir. Sem match, ou se a decisão demorar ou
     * falhar (o backend retoma sozinho ao abrir o baralho), nada aparece: a conexão, se vier, fica
     * na lista de conversas. Só falha no envio devolve o card (via [restore]).
     */
    override suspend fun assemble(characterId: String): AssembleOutcome {
        val key = assembleKeys.getOrPut(characterId, newKey)
        val accepted = try {
            api.decide(characterId, DecisionChoice.ASSEMBLE, key)
        } catch (error: ApiException) {
            // Decidido antes sem esta chave (outro aparelho, app reinstalado): não há o que mostrar.
            if (error.code == ApiErrorCode.ALREADY_DECIDED) {
                dismissed.remove(characterId)
                return AssembleOutcome.NotMatched
            }
            throw error
        } ?: throw IOException("Assemble sem resposta")
        val card = dismissed.remove(characterId)
        assembleKeys.remove(characterId)
        val status = if (accepted.status == ApiStatus.PENDING) awaitDecision(characterId) else accepted.status
        if (status != ApiStatus.MATCHED) return AssembleOutcome.NotMatched
        val match = awaitMatch(characterId) ?: return AssembleOutcome.NotMatched
        val name = card?.name ?: knownName(characterId) ?: match.characterName
        val imageUrl = card?.imageUrl ?: match.imageUrl
        characters.remember(basicCharacter(characterId, name, imageUrl))
        return AssembleOutcome.Matched(
            DiscoverMatch(
                characterId = characterId,
                name = name,
                imageUrl = imageUrl,
                score = match.connection.score,
                connectionId = match.connection.id,
                traitsInCommon = traitsFromNames(match.reasons),
            ),
        )
    }

    override suspend fun restore(characterId: String) {
        val card = dismissed.remove(characterId) ?: return
        userRepository.unmarkSeen(characterId)
        putOnTop(card)
    }

    /** Nome no idioma do app, se o personagem já foi lido (ex.: Assemble pela pré-visualização). */
    private suspend fun knownName(characterId: String): String? = try {
        characters.getCharacter(characterId)?.name
    } catch (_: IOException) {
        null
    }

    /** Estado final da decisão no Firestore; null se demorar demais ou a leitura falhar. */
    private suspend fun awaitDecision(characterId: String): String? {
        val current = uid.value ?: return null
        return try {
            withTimeoutOrNull(decisionWaitMillis) {
                store.observeDecision(current, characterId)
                    .map { it?.get("status") as? String }
                    .first { it != null && it != ApiStatus.PENDING }
            }
        } catch (_: IOException) {
            null
        }
    }

    private suspend fun awaitMatch(characterId: String): MatchDocument? = try {
        withTimeoutOrNull(matchWaitMillis) {
            connections.matchDocuments
                .map { list -> list.firstOrNull { it.connection.characterId == characterId } }
                .first { it != null }
        }
    } catch (_: IOException) {
        null
    }

    /** Reenvia os Pass da fila local com espera crescente, numa rotina só. */
    private fun flushOutbox() {
        if (flushJob?.isActive == true) return
        flushJob = scope.launch {
            var wait = outboxFirstDelayMillis
            repeat(OUTBOX_MAX_ROUNDS) {
                delay(wait)
                for ((characterId, key) in outbox.toMap()) {
                    try {
                        api.decide(characterId, DecisionChoice.PASS, key)
                        outbox.remove(characterId)
                    } catch (error: IOException) {
                        // Recusa definitiva (ex.: já decidido): não adianta tentar de novo.
                        if (error is ApiException && error.httpStatus in HTTP_CLIENT_ERRORS) outbox.remove(characterId)
                    }
                }
                if (outbox.isEmpty()) return@launch
                wait *= 2
            }
        }
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
