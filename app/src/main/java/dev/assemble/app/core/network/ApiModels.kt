package dev.assemble.app.core.network

import dev.assemble.app.core.model.Appearance
import dev.assemble.app.core.model.Powerstats
import kotlinx.serialization.Serializable

// Objetos do contrato da API V2 (docs/Assemble-contrato-api.md §3). Campo que a fonte não tem
// não vem no JSON: aqui ele fica null ou vazio, nunca inventado.

@Serializable
data class ApiDeckCard(
    val characterId: String,
    val name: String,
    val imageUrl: String? = null,
    val traitsInCommon: List<String> = emptyList(),
    val tagline: String? = null,
)

@Serializable
data class ApiDeck(
    val date: String,
    val cards: List<ApiDeckCard>,
    val remaining: Int,
    val total: Int,
    val nextDeckAt: String,
    val canUndo: Boolean,
)

/** Rodada de reação do cadastro: cards só para ensinar o gosto, não são o baralho. */
@Serializable
data class ApiReactionCards(val cards: List<ApiDeckCard>)

@Serializable
internal data class ApiTasteSignalRequest(val liked: Boolean)

@Serializable
data class ApiMatchCharacter(val characterId: String, val name: String, val imageUrl: String? = null)

/** Sem match, só `matched = false`: nem score nem motivos. */
@Serializable
data class ApiMatchResult(
    val matched: Boolean,
    val connectionId: String? = null,
    val character: ApiMatchCharacter? = null,
    val score: Int? = null,
    val reasons: List<String> = emptyList(),
)

@Serializable
internal data class ApiDecisionRequest(val characterId: String, val choice: String)

@Serializable
internal data class ApiSendMessageRequest(val text: String)

@Serializable
data class ApiMessage(
    val id: String,
    val connectionId: String,
    val author: String,
    val text: String,
    val createdAt: String,
    val fictional: Boolean = false,
    val blocked: Boolean = false,
)

@Serializable
data class ApiCharacterReply(
    val userMessage: ApiMessage,
    val reply: ApiMessage,
    val suggestions: List<String> = emptyList(),
)

/** Resposta de "gerar outra resposta": a mesma mensagem (mesmo id), com texto novo. */
@Serializable
data class ApiRegenerated(val reply: ApiMessage, val suggestions: List<String> = emptyList())

@Serializable
internal data class ApiRewindRequest(val messageId: String)

@Serializable
data class ApiWhyYouMatch(val category: String, val traits: List<String>)

@Serializable
data class ApiFacts(
    val realName: String? = null,
    val aliases: List<String> = emptyList(),
    val origin: String? = null,
    val powers: List<String> = emptyList(),
    val teams: List<String> = emptyList(),
    val alignment: String? = null,
    val placeOfBirth: String? = null,
    val occupation: String? = null,
    val base: String? = null,
    val firstAppearance: String? = null,
    val issueAppearances: Int? = null,
    val bio: String? = null,
    val relatives: String? = null,
    val powerstats: Powerstats? = null,
    val appearance: Appearance? = null,
)

@Serializable
data class ApiSource(val name: String, val url: String? = null)

@Serializable
data class ApiTeammate(
    val characterId: String,
    val name: String,
    val imageUrl: String? = null,
    val connected: Boolean,
)

@Serializable
data class ApiCompareWith(val characterId: String, val name: String, val powerstats: Powerstats)

/** CharacterPreview (sem conexão) ou CharacterProfile (com conexão), pelo campo [connected]. */
@Serializable
data class ApiCharacterView(
    val characterId: String,
    val name: String,
    val imageUrl: String? = null,
    val connected: Boolean,
    val traitsInCommon: List<String> = emptyList(),
    val connectionId: String? = null,
    val score: Int? = null,
    val whyYouMatch: List<ApiWhyYouMatch> = emptyList(),
    val facts: ApiFacts? = null,
    val factSources: Map<String, String> = emptyMap(),
    val translatedFields: List<String> = emptyList(),
    val sources: List<ApiSource> = emptyList(),
    val teammates: List<ApiTeammate> = emptyList(),
    val compareWith: List<ApiCompareWith> = emptyList(),
)

@Serializable
data class ApiUserStats(
    val connections: Int,
    val messagesSent: Int,
    val charactersSeen: Int,
    val distinctTeams: Int,
)

@Serializable
data class ApiDeactivation(val purgeAt: String)

@Serializable
internal data class ApiErrorBody(val error: String, val message: String = "")
