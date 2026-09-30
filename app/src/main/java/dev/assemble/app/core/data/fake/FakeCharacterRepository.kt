package dev.assemble.app.core.data.fake

import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.model.Character

/** Catálogo a partir do JSON mock ([catalog] lê e cacheia o arquivo). */
class FakeCharacterRepository(
    private val network: FakeNetwork,
    private val catalog: MockCatalog,
) : CharacterRepository {

    override suspend fun getCharacters(): List<Character> {
        network.call()
        return catalog.characters()
    }

    override suspend fun getCharacter(id: String): Character? {
        network.call()
        return catalog.characters().firstOrNull { it.id == id }
    }
}
