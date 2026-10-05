package dev.assemble.app.core.data.remote

import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.model.Alignment
import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Team
import dev.assemble.app.core.model.traitFromName
import dev.assemble.app.core.network.ApiCharacterView
import dev.assemble.app.core.network.ApiErrorCode
import dev.assemble.app.core.network.ApiException
import dev.assemble.app.core.network.AssembleApi
import dev.assemble.app.i18n.isPortuguese
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap

/**
 * Sem catálogo completo no app: o backend devolve um personagem por vez. Este repositório guarda
 * os que o app já viu (baralho, perfil, conexões) para as telas que listam personagens.
 */
class RemoteCharacterRepository(
    private val api: AssembleApi,
    private val connections: RemoteConnectionRepository,
    private val languageTag: () -> String,
) : CharacterRepository {
    private val known = ConcurrentHashMap<String, Character>()

    /** Personagens conhecidos, incluindo os das conexões (nome no idioma do app). */
    override suspend fun getCharacters(): List<Character> {
        val portuguese = isPortuguese(languageTag())
        connections.matchDocuments.first().forEach { match ->
            val name = if (portuguese) match.characterNamePtBR ?: match.characterName else match.characterName
            known.compute(match.connection.characterId) { id, current ->
                current?.copy(name = name, imageUrl = current.imageUrl ?: match.imageUrl)
                    ?: basicCharacter(id, name, match.imageUrl)
            }
        }
        return known.values.sortedBy { it.name }
    }

    override suspend fun getCharacter(id: String): Character? {
        known[id]?.let { return it }
        return try {
            api.character(id).toCharacter().also { remember(it) }
        } catch (error: ApiException) {
            if (error.code == ApiErrorCode.NOT_FOUND) null else throw error
        }
    }

    /** Guarda (ou completa) o que o backend acabou de devolver; um card não apaga fatos já lidos. */
    fun remember(character: Character) {
        known.merge(character.id, character) { current, incoming ->
            if (incoming.hasFacts()) {
                incoming.copy(imageUrl = incoming.imageUrl ?: current.imageUrl)
            } else {
                current.copy(name = incoming.name, imageUrl = incoming.imageUrl ?: current.imageUrl)
            }
        }
    }

    private fun Character.hasFacts(): Boolean =
        realName != null || origin != null || powers.isNotEmpty() || teams.isNotEmpty() ||
            firstAppearance != null || powerstats != null
}

/** Personagem conhecido só pelo nome e pela arte (card do baralho, conexão, match). */
fun basicCharacter(id: String, name: String, imageUrl: String?): Character =
    Character(id, name, null, imageUrl, null, emptyList(), emptyList(), emptyList(), null, null, null)

/** Personagem do contrato. Sem conexão, só nome e arte; com conexão, os fatos que a fonte tem. */
fun ApiCharacterView.toCharacter(): Character {
    val facts = facts
    return Character(
        id = characterId,
        name = name,
        realName = facts?.realName,
        imageUrl = imageUrl,
        origin = facts?.origin?.let { traitFromName(it) as? Origin },
        powers = facts?.powers.orEmpty().mapNotNull { traitFromName(it) as? PowerFamily },
        teams = facts?.teams.orEmpty().mapNotNull { traitFromName(it) as? Team },
        styles = emptyList(),
        firstAppearance = facts?.firstAppearance,
        issueAppearances = facts?.issueAppearances,
        bio = facts?.bio,
        aliases = facts?.aliases.orEmpty(),
        placeOfBirth = facts?.placeOfBirth,
        occupation = facts?.occupation,
        base = facts?.base,
        relatives = facts?.relatives,
        alignment = facts?.alignment?.let { name -> Alignment.entries.firstOrNull { it.name == name } },
        powerstats = facts?.powerstats,
        appearance = facts?.appearance,
    )
}
