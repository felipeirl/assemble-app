package dev.assemble.app.core.data

import dev.assemble.app.core.model.Connection
import kotlinx.coroutines.flow.Flow

interface ConnectionRepository {
    /** Conexões do usuário, mais recentes primeiro. O primeiro valor pode falhar com IOException. */
    fun observeConnections(): Flow<List<Connection>>

    suspend fun getConnection(id: String): Connection?

    suspend fun getConnectionForCharacter(characterId: String): Connection?

    /**
     * Registra a conexão. Idempotente: repetir para o mesmo personagem devolve a existente.
     * Quem chama garante que score ≥ threshold.
     */
    suspend fun connect(characterId: String, score: Int, threshold: Int): Connection

    suspend fun markProfileUnlockSeen(connectionId: String)

    suspend fun deleteAll()
}
