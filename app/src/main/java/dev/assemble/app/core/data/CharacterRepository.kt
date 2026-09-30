package dev.assemble.app.core.data

import dev.assemble.app.core.model.Character

interface CharacterRepository {
    /** Catálogo completo. Lança IOException em falha de rede. */
    suspend fun getCharacters(): List<Character>

    /** Personagem pelo id, ou null se a fonte não o tiver. Lança IOException em falha de rede. */
    suspend fun getCharacter(id: String): Character?
}
