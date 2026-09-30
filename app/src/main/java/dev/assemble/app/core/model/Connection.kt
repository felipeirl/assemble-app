package dev.assemble.app.core.model

import java.time.Instant

/**
 * Conexão criada quando o usuário deu Assemble e o score atingiu o limiar.
 * Guarda o score e o limiar do momento (não é reciprocidade do personagem).
 */
data class Connection(
    val id: String,
    val characterId: String,
    val score: Int,
    val threshold: Int,
    val createdAt: Instant,
    /** true depois que o perfil completo foi aberto a primeira vez (animação de desbloqueio só uma vez). */
    val profileUnlockSeen: Boolean = false,
)
