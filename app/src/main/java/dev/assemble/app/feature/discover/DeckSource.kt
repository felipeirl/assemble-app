package dev.assemble.app.feature.discover

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Resposta do personagem a um Assemble. */
sealed interface AssembleOutcome {
    data class Matched(val match: DiscoverMatch) : AssembleOutcome
    data object NotMatched : AssembleOutcome
}

/**
 * De onde vem o baralho do Discover e quem decide Pass, Assemble e Undo. Sem backend, tudo é
 * calculado no aparelho ([LocalDeckSource]); com backend, ele decide ([RemoteDeckSource]).
 */
interface DeckSource {
    /** Baralho atual (Loading até a primeira leitura). */
    val deck: Flow<DeckState>

    /** Há um Pass para desfazer. */
    val canUndo: StateFlow<Boolean>

    /** Lê (ou relê, no "tentar de novo") o baralho. */
    suspend fun reload()

    suspend fun pass(characterId: String)

    /** Desfaz só o último Pass, uma vez. */
    suspend fun undo()

    /** Tira o card da tela na hora, antes da resposta do Assemble. */
    suspend fun dismiss(characterId: String)

    /** Decide o Assemble. Lança IOException em falha (o card volta com [restore]). */
    suspend fun assemble(characterId: String): AssembleOutcome

    /** Devolve ao baralho um card cujo Assemble falhou. */
    suspend fun restore(characterId: String)
}
