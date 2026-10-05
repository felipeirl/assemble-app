package dev.assemble.app.core.data.fake

import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.model.Character

/**
 * Catálogo a partir do JSON mock. A primeira leitura passa pela rede simulada; as seguintes vêm da memória,
 * como o cache do app real: voltar a uma tela não mostra carregamento de novo.
 * Com o modo falha ligado antes da primeira leitura, o erro aparece; depois dela, o cache segue valendo.
 */
class FakeCharacterRepository(
    private val network: FakeNetwork,
    private val catalog: MockCatalog,
) : CharacterRepository {
    @Volatile
    private var cached: List<Character>? = null

    override suspend fun getCharacters(): List<Character> = cachedOrLoad()

    override suspend fun getCharacter(id: String): Character? = cachedOrLoad().firstOrNull { it.id == id }

    private suspend fun cachedOrLoad(): List<Character> {
        cached?.let { return it }
        network.call()
        return catalog.characters().also { cached = it }
    }
}
