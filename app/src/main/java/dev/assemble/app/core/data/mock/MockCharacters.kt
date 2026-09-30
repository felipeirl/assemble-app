package dev.assemble.app.core.data.mock

import dev.assemble.app.core.model.Character
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Caminho do mock em assets. */
const val CHARACTERS_ASSET_PATH = "mock/characters.json"

@OptIn(ExperimentalSerializationApi::class)
private val MockJson = Json {
    // O arquivo tem comentários marcando o que é ilustrativo.
    allowComments = true
}

/** Converte o JSON de personagens do mock. Lança SerializationException se o arquivo estiver inválido. */
fun parseMockCharacters(json: String): List<Character> =
    MockJson.decodeFromString(ListSerializer(Character.serializer()), json)
