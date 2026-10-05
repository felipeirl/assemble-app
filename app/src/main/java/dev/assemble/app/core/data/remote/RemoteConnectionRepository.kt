package dev.assemble.app.core.data.remote

import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.model.Connection
import dev.assemble.app.core.network.AssembleApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformWhile
import java.io.IOException

/** Leitura das conexões: carregando, lidas ou falha (o primeiro valor da tela pode ser erro). */
sealed interface MatchesState {
    data object Loading : MatchesState
    data class Loaded(val matches: List<MatchDocument>) : MatchesState
    data object Failed : MatchesState
}

/**
 * Conexões vindas de `users/{uid}/matches` (Firestore, tempo real). Quem cria a conexão é o
 * backend, no match; o app só grava `lastReadAt` e `profileUnlockSeenAt`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteConnectionRepository(
    private val uid: StateFlow<String?>,
    private val store: UserDataStore,
    private val api: AssembleApi,
    scope: CoroutineScope,
) : ConnectionRepository {

    /** Um listener só, compartilhado por todas as telas. Conversas ocultas ficam de fora. */
    val matches: StateFlow<MatchesState> = uid
        .flatMapLatest { current ->
            if (current == null) {
                flowOf(MatchesState.Loaded(emptyList()))
            } else {
                store.observeMatches(current)
                    .map<List<Document>, MatchesState> { documents ->
                        MatchesState.Loaded(
                            documents.mapNotNull(::matchDocument)
                                .filterNot { it.hidden }
                                .sortedByDescending { it.connection.createdAt },
                        )
                    }
                    .catch { emit(MatchesState.Failed) }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, MatchesState.Loading)

    /** Documentos das conexões; lança IOException se a leitura falhou. */
    val matchDocuments: Flow<List<MatchDocument>> = matches.transformWhile { state ->
        when (state) {
            MatchesState.Loading -> true
            is MatchesState.Loaded -> {
                emit(state.matches)
                true
            }
            MatchesState.Failed -> throw IOException("Não foi possível ler as conexões")
        }
    }

    override fun observeConnections(): Flow<List<Connection>> =
        matchDocuments.map { list -> list.map { it.connection } }

    override suspend fun getConnection(id: String): Connection? =
        matchDocuments.first().firstOrNull { it.connection.id == id }?.connection

    override suspend fun getConnectionForCharacter(characterId: String): Connection? =
        matchDocuments.first().firstOrNull { it.connection.characterId == characterId }?.connection

    /** No backend, a conexão nasce no match; aqui só a localizamos. */
    override suspend fun connect(characterId: String, score: Int, threshold: Int): Connection =
        getConnectionForCharacter(characterId) ?: throw IOException("Conexão ainda não sincronizada")

    override suspend fun markProfileUnlockSeen(connectionId: String) {
        val current = uid.value ?: return
        store.updateMatch(current, connectionId, mapOf("profileUnlockSeenAt" to ServerTime))
    }

    /** "Delete chats": o backend oculta todas as conversas na hora. */
    override suspend fun deleteAll() {
        api.hideChats()
    }
}
