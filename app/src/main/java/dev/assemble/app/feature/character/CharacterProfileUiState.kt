package dev.assemble.app.feature.character

import androidx.annotation.StringRes
import dev.assemble.app.core.model.DataSource
import dev.assemble.app.core.model.Powerstats
import dev.assemble.app.core.model.Team

/**
 * Dado de uma fonte. Vazio/nulo nunca chega aqui: o campo é omitido. O valor vem de [text],
 * de [valueRes] (valor traduzível, como o alinhamento) ou de [traits].
 */
data class ProfileFact(
    @StringRes val label: Int,
    val text: String? = null,
    val traits: List<Enum<*>> = emptyList(),
    @StringRes val valueRes: Int? = null,
    val source: DataSource = DataSource.ComicVine,
)

/** Traços que bateram numa categoria (Origin, Powers, Teams, Style). */
data class WhyYouMatchItem(
    @StringRes val category: Int,
    val traits: List<Enum<*>>,
)

/** Personagem conectado com atributos, para comparar no radar. */
data class StatsOption(val characterId: String, val name: String, val powerstats: Powerstats)

/** Colega de equipe no catálogo. Sem conexão, aparece apagado e não abre nada. */
data class TeammateNode(val characterId: String, val name: String, val imageUrl: String?, val connected: Boolean)

sealed interface CharacterProfileUiState {
    data object Loading : CharacterProfileUiState

    data class Content(
        val connectionId: String,
        val name: String,
        val imageUrl: String?,
        /** Score gravado na conexão. */
        val score: Int,
        val whyYouMatch: List<WhyYouMatchItem>,
        val facts: List<ProfileFact>,
        /** true na primeira abertura: roda a animação de desbloqueio. */
        val unlockPending: Boolean,
        val powerstats: Powerstats? = null,
        val compareOptions: List<StatsOption> = emptyList(),
        val appearance: List<ProfileFact> = emptyList(),
        val teams: List<Team> = emptyList(),
        val teammates: List<TeammateNode> = emptyList(),
        val relatives: String? = null,
        /** Fontes usadas neste perfil, na ordem do enum. */
        val sources: List<DataSource> = listOf(DataSource.ComicVine),
    ) : CharacterProfileUiState {
        /** Sem atributos nem aparência, a aba some. */
        val hasAttributes: Boolean get() = powerstats != null || appearance.isNotEmpty()
    }

    /** Sem conexão com o personagem, ou a fonte não o devolveu. */
    data object Unavailable : CharacterProfileUiState

    data object Error : CharacterProfileUiState
}
