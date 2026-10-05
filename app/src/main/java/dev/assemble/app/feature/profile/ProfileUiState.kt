package dev.assemble.app.feature.profile

import dev.assemble.app.core.domain.Achievement
import dev.assemble.app.core.domain.Archetype
import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.Connection
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.UserProfile
import kotlin.math.roundToInt

private const val TOP_TRAITS_LIMIT = 5

data class ProfileStats(
    val charactersSeen: Int,
    val connections: Int,
    /** Média dos scores das conexões; null sem conexões. */
    val averageMatch: Int?,
)

data class ProfileConnection(
    val characterId: String,
    val name: String,
    val imageUrl: String?,
)

enum class ProfileMessage { PreferencesUpdated, PreferencesFailed }

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Content(
        val profile: UserProfile,
        val stats: ProfileStats,
        val preferences: Preferences,
        val topTraits: List<Enum<*>>,
        val connections: List<ProfileConnection>,
        /** Null quando as preferências estão em "Any". */
        val archetype: Archetype? = null,
        /** Já filtrados: só conexões que existem e conquistas desbloqueadas, na ordem escolhida. */
        val featuredConnections: List<ProfileConnection> = emptyList(),
        val featuredBadges: List<Achievement> = emptyList(),
    ) : ProfileUiState

    data object Error : ProfileUiState
}

/** "Characters seen" conta os vistos no Discover e os já conectados (sem repetir). */
internal fun profileStats(seenIds: Set<String>, connections: List<Connection>): ProfileStats = ProfileStats(
    charactersSeen = (seenIds + connections.map { it.characterId }).size,
    connections = connections.size,
    averageMatch = connections.takeIf { it.isNotEmpty() }?.map { it.score }?.average()?.roundToInt(),
)

/**
 * Traços mais frequentes entre os personagens conectados (origem, poderes, equipes e estilos).
 * Empate: ordem das categorias e do enum.
 */
internal fun topTraits(characters: List<Character>, limit: Int = TOP_TRAITS_LIMIT): List<Enum<*>> {
    val ordered: List<Enum<*>> = characters.flatMap { character ->
        listOfNotNull(character.origin) + character.powers + character.teams + character.styles
    }
    val counts = ordered.groupingBy { it }.eachCount()
    val firstSeen = ordered.distinct().withIndex().associate { (index, trait) -> trait to index }
    return counts.keys
        .sortedWith(compareByDescending<Enum<*>> { counts.getValue(it) }.thenBy { firstSeen.getValue(it) })
        .take(limit)
}
