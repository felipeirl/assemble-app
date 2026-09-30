package dev.assemble.app.core.data.fake

import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.model.Connection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant

/**
 * Conexões em memória. [seed] cria as conexões iniciais na primeira leitura
 * (precisa do catálogo para calcular o score).
 */
class FakeConnectionRepository(
    private val network: FakeNetwork,
    private val clock: Clock,
    private val seed: suspend () -> List<Connection>,
) : ConnectionRepository {
    private val connections = MutableStateFlow<List<Connection>>(emptyList())
    private val seedMutex = Mutex()
    private var seeded = false

    override fun observeConnections(): Flow<List<Connection>> = flow {
        network.call()
        ensureSeeded()
        emitAll(connections.map { list -> list.sortedByDescending { it.createdAt } })
    }

    override suspend fun getConnection(id: String): Connection? {
        ensureSeeded()
        return connections.value.firstOrNull { it.id == id }
    }

    override suspend fun getConnectionForCharacter(characterId: String): Connection? {
        ensureSeeded()
        return connections.value.firstOrNull { it.characterId == characterId }
    }

    override suspend fun connect(characterId: String, score: Int, threshold: Int): Connection {
        network.call()
        ensureSeeded()
        getConnectionForCharacter(characterId)?.let { return it }
        val connection = Connection(
            id = "connection-$characterId",
            characterId = characterId,
            score = score,
            threshold = threshold,
            createdAt = Instant.now(clock),
        )
        connections.update { it + connection }
        return connection
    }

    override suspend fun markProfileUnlockSeen(connectionId: String) {
        connections.update { list ->
            list.map { if (it.id == connectionId) it.copy(profileUnlockSeen = true) else it }
        }
    }

    override suspend fun deleteAll() {
        network.call()
        ensureSeeded()
        connections.value = emptyList()
    }

    private suspend fun ensureSeeded() = seedMutex.withLock {
        if (!seeded) {
            connections.value = seed()
            seeded = true
        }
    }
}
