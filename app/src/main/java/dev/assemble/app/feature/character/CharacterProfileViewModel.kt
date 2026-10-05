package dev.assemble.app.feature.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.R
import dev.assemble.app.core.domain.CompatibilityBreakdown
import dev.assemble.app.core.model.Alignment
import dev.assemble.app.core.model.Appearance
import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.DataSource
import dev.assemble.app.core.model.Team
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.NumberFormat

private const val TEAMMATES_LIMIT = 8
private const val LIST_SEPARATOR = ", "

/** Perfil completo: só existe para quem tem conexão com o personagem. */
class CharacterProfileViewModel(
    private val characterId: String,
    private val details: CharacterDetailsSource,
) : ViewModel() {
    private val state = MutableStateFlow<CharacterProfileUiState>(CharacterProfileUiState.Loading)
    val uiState: StateFlow<CharacterProfileUiState> = state.asStateFlow()

    private var connectionId: String? = null

    init {
        load()
    }

    fun load() {
        state.value = CharacterProfileUiState.Loading
        viewModelScope.launch {
            try {
                val content = details.profile(characterId)
                connectionId = content?.connectionId
                state.value = content ?: CharacterProfileUiState.Unavailable
            } catch (_: IOException) {
                state.value = CharacterProfileUiState.Error
            }
        }
    }

    /** Chamado quando a animação de desbloqueio termina: não roda de novo. */
    fun onUnlockSeen() {
        val id = connectionId ?: return
        viewModelScope.launch {
            try {
                details.markUnlockSeen(id)
            } catch (_: IOException) {
                // Melhor esforço: sem a marca, a animação roda de novo na próxima abertura.
            }
        }
    }
}

/** Categorias com itens em comum. "Any" e categorias sem coincidência ficam de fora. */
internal fun whyYouMatch(breakdown: CompatibilityBreakdown): List<WhyYouMatchItem> = listOf(
    WhyYouMatchItem(R.string.profile_origin, breakdown.origin.matched.sortedBy { it.ordinal }),
    WhyYouMatchItem(R.string.profile_powers, breakdown.powers.matched.sortedBy { it.ordinal }),
    WhyYouMatchItem(R.string.profile_teams, breakdown.teams.matched.sortedBy { it.ordinal }),
    WhyYouMatchItem(R.string.profile_style, breakdown.styles.matched.sortedBy { it.ordinal }),
).filter { it.traits.isNotEmpty() }

/** Fatos na ordem do design, cada um com a fonte; campos nulos ou vazios são omitidos, nunca inventados. */
internal fun profileFacts(character: Character): List<ProfileFact> = listOfNotNull(
    character.realName?.takeIf { it.isNotBlank() }?.let { ProfileFact(R.string.profile_real_name, text = it) },
    character.aliases.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }
        ?.let { ProfileFact(R.string.profile_aliases, text = it.joinToString(LIST_SEPARATOR), source = DataSource.SuperheroApi) },
    character.origin?.let { ProfileFact(R.string.profile_origin, traits = listOf(it)) },
    character.powers.takeIf { it.isNotEmpty() }?.let { ProfileFact(R.string.profile_powers, traits = it) },
    character.teams.takeIf { it.isNotEmpty() }?.let { ProfileFact(R.string.profile_teams, traits = it) },
    character.alignment?.let { ProfileFact(R.string.profile_alignment, valueRes = it.label, source = DataSource.SuperheroApi) },
    sourceText(R.string.profile_place_of_birth, character.placeOfBirth),
    sourceText(R.string.profile_occupation, character.occupation),
    sourceText(R.string.profile_base, character.base),
    character.firstAppearance?.takeIf { it.isNotBlank() }
        ?.let { ProfileFact(R.string.profile_first_appearance, text = it) },
    character.issueAppearances
        ?.let { ProfileFact(R.string.profile_issue_appearances, text = NumberFormat.getIntegerInstance().format(it)) },
)

/** Aparência da Superhero API; unidades métricas como a fonte. */
internal fun appearanceFacts(appearance: Appearance?): List<ProfileFact> {
    if (appearance == null) return emptyList()
    return listOfNotNull(
        sourceText(R.string.appearance_gender, appearance.gender),
        sourceText(R.string.appearance_race, appearance.race),
        sourceText(R.string.appearance_height, appearance.heightCm?.let { "$it cm" }),
        sourceText(R.string.appearance_weight, appearance.weightKg?.let { "$it kg" }),
        sourceText(R.string.appearance_eyes, appearance.eyeColor),
        sourceText(R.string.appearance_hair, appearance.hairColor),
    )
}

/**
 * Personagens do catálogo que dividem uma equipe com [character] ("Solo" não conta).
 * Conectados primeiro, depois por nome; no máximo [TEAMMATES_LIMIT] para o mapa caber na tela.
 */
internal fun teammates(character: Character, catalog: List<Character>, connectedIds: Set<String>): List<TeammateNode> {
    val teams = character.teams.filter { it != Team.Solo }.toSet()
    if (teams.isEmpty()) return emptyList()
    return catalog
        .filter { it.id != character.id && it.teams.any { team -> team in teams } }
        .map { TeammateNode(it.id, it.name, it.imageUrl, connected = it.id in connectedIds) }
        .sortedWith(compareByDescending<TeammateNode> { it.connected }.thenBy { it.name })
        .take(TEAMMATES_LIMIT)
}

/** Fontes citadas no perfil: a dos fatos, mais a Superhero API quando os atributos ou os parentes vêm dela. */
internal fun profileSources(character: Character, facts: List<ProfileFact>): List<DataSource> {
    val used = facts.mapTo(mutableSetOf()) { it.source }
    if (character.powerstats != null || !character.relatives.isNullOrBlank()) used += DataSource.SuperheroApi
    used += DataSource.ComicVine
    return DataSource.entries.filter { it in used }
}

private fun sourceText(label: Int, value: String?): ProfileFact? =
    value?.takeIf { it.isNotBlank() }?.let { ProfileFact(label, text = it, source = DataSource.SuperheroApi) }

private val Alignment.label: Int
    get() = when (this) {
        Alignment.Good -> R.string.alignment_good
        Alignment.Bad -> R.string.alignment_bad
        Alignment.Neutral -> R.string.alignment_neutral
    }
