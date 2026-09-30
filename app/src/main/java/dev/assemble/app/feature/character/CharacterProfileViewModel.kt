package dev.assemble.app.feature.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.R
import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.domain.CompatibilityBreakdown
import dev.assemble.app.core.domain.CompatibilityCalculator
import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.Connection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.NumberFormat

/** Perfil completo: só existe para quem tem conexão com o personagem. */
class CharacterProfileViewModel(
    private val characterId: String,
    private val characterRepository: CharacterRepository,
    private val connectionRepository: ConnectionRepository,
    private val userRepository: UserRepository,
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
                val character = characterRepository.getCharacter(characterId)
                val connection = connectionRepository.getConnectionForCharacter(characterId)
                connectionId = connection?.id
                state.value = if (character == null || connection == null) {
                    CharacterProfileUiState.Unavailable
                } else {
                    buildContent(character, connection)
                }
            } catch (_: IOException) {
                state.value = CharacterProfileUiState.Error
            }
        }
    }

    /** Chamado quando a animação de desbloqueio termina: não roda de novo. */
    fun onUnlockSeen() {
        val id = connectionId ?: return
        viewModelScope.launch { connectionRepository.markProfileUnlockSeen(id) }
    }

    private fun buildContent(character: Character, connection: Connection): CharacterProfileUiState.Content {
        val breakdown = CompatibilityCalculator.breakdown(userRepository.preferences.value, character)
        return CharacterProfileUiState.Content(
            connectionId = connection.id,
            name = character.name,
            imageUrl = character.imageUrl,
            score = connection.score,
            whyYouMatch = whyYouMatch(breakdown),
            facts = profileFacts(character),
            unlockPending = !connection.profileUnlockSeen,
        )
    }
}

/** Categorias com itens em comum. "Any" e categorias sem coincidência ficam de fora. */
internal fun whyYouMatch(breakdown: CompatibilityBreakdown): List<WhyYouMatchItem> = listOf(
    WhyYouMatchItem(R.string.profile_origin, breakdown.origin.matched.sortedBy { it.ordinal }),
    WhyYouMatchItem(R.string.profile_powers, breakdown.powers.matched.sortedBy { it.ordinal }),
    WhyYouMatchItem(R.string.profile_teams, breakdown.teams.matched.sortedBy { it.ordinal }),
    WhyYouMatchItem(R.string.profile_style, breakdown.styles.matched.sortedBy { it.ordinal }),
).filter { it.traits.isNotEmpty() }

/** Dados da Comic Vine na ordem do design; campos nulos ou vazios são omitidos, nunca inventados. */
internal fun profileFacts(character: Character): List<ProfileFact> = listOfNotNull(
    character.realName?.takeIf { it.isNotBlank() }?.let { ProfileFact(R.string.profile_real_name, text = it) },
    character.origin?.let { ProfileFact(R.string.profile_origin, traits = listOf(it)) },
    character.powers.takeIf { it.isNotEmpty() }?.let { ProfileFact(R.string.profile_powers, traits = it) },
    character.teams.takeIf { it.isNotEmpty() }?.let { ProfileFact(R.string.profile_teams, traits = it) },
    character.firstAppearance?.takeIf { it.isNotBlank() }
        ?.let { ProfileFact(R.string.profile_first_appearance, text = it) },
    character.issueAppearances
        ?.let { ProfileFact(R.string.profile_issue_appearances, text = NumberFormat.getIntegerInstance().format(it)) },
)
