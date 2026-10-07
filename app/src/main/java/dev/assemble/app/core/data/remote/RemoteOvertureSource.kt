package dev.assemble.app.core.data.remote

import dev.assemble.app.core.network.ApiStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant

/** Personagem que quer dar Assemble com o usuário e ainda espera a resposta dele. */
data class Overture(val characterId: String, val createdAt: Instant)

/**
 * Propostas pendentes (`users/{uid}/overtures`, status `pending`), da mais antiga para a mais nova.
 * O usuário responde pelo fluxo normal de Assemble ou Pass; o backend marca a proposta.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteOvertureSource(private val uid: StateFlow<String?>, private val store: UserDataStore) {
    val pending: Flow<List<Overture>> = uid
        .flatMapLatest { current ->
            if (current == null) flowOf(emptyList()) else store.observeOvertures(current).map(::pendingOf)
        }
        .catch { emit(emptyList()) } // Sem leitura, sem aviso: nada na tela depende disso.
}

internal fun pendingOf(documents: List<Document>): List<Overture> = documents
    .filter { it.data["status"] == ApiStatus.PENDING }
    .mapNotNull { doc -> (doc.data["createdAt"] as? Instant)?.let { Overture(doc.id, it) } }
    .sortedBy { it.createdAt }
