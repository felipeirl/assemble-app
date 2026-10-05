package dev.assemble.app.feature.character

import dev.assemble.app.R
import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.data.remote.RemoteCharacterRepository
import dev.assemble.app.core.data.remote.toCharacter
import dev.assemble.app.core.domain.CompatibilityCalculator
import dev.assemble.app.core.model.DataSource
import dev.assemble.app.core.model.traitsFromNames
import dev.assemble.app.core.network.ApiCharacterView
import dev.assemble.app.core.network.ApiErrorCode
import dev.assemble.app.core.network.ApiException
import dev.assemble.app.core.network.AssembleApi
import dev.assemble.app.feature.discover.matchedTraits
import kotlinx.coroutines.flow.first

/** Prévia de um personagem sem conexão: só nome, arte e traços em comum. */
data class CharacterPreviewData(val name: String, val imageUrl: String?, val traitsInCommon: List<Enum<*>>)

/**
 * Dados da prévia e do perfil completo. Sem backend, montados no aparelho a partir do catálogo;
 * com backend, a rota `/v2/characters/{id}` devolve um ou outro conforme a conexão.
 */
interface CharacterDetailsSource {
    /** null = a fonte não tem o personagem. Lança IOException em falha. */
    suspend fun preview(characterId: String): CharacterPreviewData?

    /** null = sem conexão ou a fonte não tem o personagem. Lança IOException em falha. */
    suspend fun profile(characterId: String): CharacterProfileUiState.Content?

    suspend fun markUnlockSeen(connectionId: String)
}

class LocalCharacterDetailsSource(
    private val characterRepository: CharacterRepository,
    private val connectionRepository: ConnectionRepository,
    private val userRepository: UserRepository,
) : CharacterDetailsSource {

    override suspend fun preview(characterId: String): CharacterPreviewData? {
        val character = characterRepository.getCharacter(characterId) ?: return null
        val breakdown = CompatibilityCalculator.breakdown(userRepository.preferences.value, character)
        return CharacterPreviewData(character.name, character.imageUrl, breakdown.matchedTraits())
    }

    override suspend fun profile(characterId: String): CharacterProfileUiState.Content? {
        val character = characterRepository.getCharacter(characterId) ?: return null
        val connection = connectionRepository.getConnectionForCharacter(characterId) ?: return null
        val catalog = characterRepository.getCharacters()
        val connectedIds = connectionRepository.observeConnections().first().mapTo(mutableSetOf()) { it.characterId }
        val breakdown = CompatibilityCalculator.breakdown(userRepository.preferences.value, character)
        val facts = profileFacts(character)
        val appearance = appearanceFacts(character.appearance)
        return CharacterProfileUiState.Content(
            connectionId = connection.id,
            name = character.name,
            imageUrl = character.imageUrl,
            score = connection.score,
            whyYouMatch = whyYouMatch(breakdown),
            facts = facts,
            unlockPending = !connection.profileUnlockSeen,
            powerstats = character.powerstats,
            compareOptions = catalog
                .filter { it.id != character.id && it.id in connectedIds }
                .mapNotNull { other -> other.powerstats?.let { StatsOption(other.id, other.name, it) } },
            appearance = appearance,
            teams = character.teams,
            teammates = teammates(character, catalog, connectedIds),
            relatives = character.relatives?.takeIf { it.isNotBlank() },
            sources = profileSources(character, facts + appearance),
        )
    }

    override suspend fun markUnlockSeen(connectionId: String) {
        connectionRepository.markProfileUnlockSeen(connectionId)
    }
}

/** Com backend: score, "Why you match", colegas e comparação já vêm calculados pelo servidor. */
class RemoteCharacterDetailsSource(
    private val api: AssembleApi,
    private val characters: RemoteCharacterRepository,
    private val connectionRepository: ConnectionRepository,
) : CharacterDetailsSource {

    override suspend fun preview(characterId: String): CharacterPreviewData? {
        val view = fetch(characterId) ?: return null
        return CharacterPreviewData(view.name, view.imageUrl, traitsFromNames(view.traitsInCommon))
    }

    override suspend fun profile(characterId: String): CharacterProfileUiState.Content? {
        val view = fetch(characterId) ?: return null
        if (!view.connected || view.connectionId == null || view.score == null) return null
        val connection = connectionRepository.getConnection(view.connectionId)
        return remoteProfileContent(view, unlockPending = connection?.profileUnlockSeen == false)
    }

    override suspend fun markUnlockSeen(connectionId: String) {
        connectionRepository.markProfileUnlockSeen(connectionId)
    }

    private suspend fun fetch(characterId: String): ApiCharacterView? = try {
        api.character(characterId).also { characters.remember(it.toCharacter()) }
    } catch (error: ApiException) {
        if (error.code == ApiErrorCode.NOT_FOUND) null else throw error
    }
}

/** Perfil completo a partir da resposta do backend; reaproveita as regras de exibição dos fatos. */
internal fun remoteProfileContent(view: ApiCharacterView, unlockPending: Boolean): CharacterProfileUiState.Content {
    val character = view.toCharacter()
    val facts = profileFacts(character)
    val appearance = appearanceFacts(character.appearance)
    return CharacterProfileUiState.Content(
        connectionId = requireNotNull(view.connectionId),
        name = view.name,
        imageUrl = view.imageUrl,
        score = requireNotNull(view.score),
        whyYouMatch = view.whyYouMatch.mapNotNull { item ->
            val label = WHY_YOU_MATCH_LABELS[item.category] ?: return@mapNotNull null
            WhyYouMatchItem(label, traitsFromNames(item.traits)).takeIf { it.traits.isNotEmpty() }
        },
        facts = facts,
        unlockPending = unlockPending,
        powerstats = character.powerstats,
        compareOptions = view.compareWith.map { StatsOption(it.characterId, it.name, it.powerstats) },
        appearance = appearance,
        teams = character.teams,
        teammates = view.teammates.map { TeammateNode(it.characterId, it.name, it.imageUrl, it.connected) },
        relatives = character.relatives?.takeIf { it.isNotBlank() },
        sources = remoteSources(view).ifEmpty { profileSources(character, facts + appearance) },
    )
}

private val WHY_YOU_MATCH_LABELS = mapOf(
    "origin" to R.string.profile_origin,
    "powers" to R.string.profile_powers,
    "teams" to R.string.profile_teams,
    "style" to R.string.profile_style,
)

/** Fontes citadas pelo backend, na ordem do enum; nomes desconhecidos ficam de fora. */
private fun remoteSources(view: ApiCharacterView): List<DataSource> {
    val used = view.sources.mapNotNullTo(mutableSetOf()) { source ->
        when {
            source.name.startsWith("Comic Vine") -> DataSource.ComicVine
            source.name.startsWith("Superhero API") -> DataSource.SuperheroApi
            source.name.startsWith("Marvel Database") -> DataSource.MarvelDatabase
            else -> null
        }
    }
    return DataSource.entries.filter { it in used }
}
