package dev.assemble.app.feature.onboarding

import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.model.traitsFromNames
import dev.assemble.app.core.network.ApiDeckCard
import dev.assemble.app.core.network.AssembleApi
import dev.assemble.app.feature.discover.DiscoverCard

/** Quantos cards a rodada de reação mostra sem backend (o backend escolhe os dele). */
private const val LOCAL_REACTION_CARDS = 12

/**
 * Rodada de reação do cadastro: Curti/Pular só para conhecer o gosto. Não é decisão: o personagem
 * continua no baralho e pode virar conexão depois.
 */
interface TasteSource {
    /** Lança IOException em falha de rede. */
    suspend fun reactionCards(): List<DiscoverCard>

    /** Lança IOException em falha de rede. */
    suspend fun react(characterId: String, liked: Boolean)
}

/** O backend escolhe os cards e guarda cada sinal no gosto aprendido. */
class RemoteTasteSource(private val api: AssembleApi) : TasteSource {
    override suspend fun reactionCards(): List<DiscoverCard> = api.reactionCards().map { it.toCard() }

    override suspend fun react(characterId: String, liked: Boolean) = api.tasteSignal(characterId, liked)

    private fun ApiDeckCard.toCard() = DiscoverCard(
        characterId = characterId,
        name = name,
        imageUrl = imageUrl,
        traitsInCommon = traitsFromNames(traitsInCommon),
        tagline = tagline,
    )
}

/** Sem backend não há gosto aprendido: mostra os mais conhecidos e não guarda o sinal. */
class LocalTasteSource(private val characters: CharacterRepository) : TasteSource {
    override suspend fun reactionCards(): List<DiscoverCard> = characters.getCharacters()
        .sortedByDescending { it.issueAppearances ?: 0 }
        .take(LOCAL_REACTION_CARDS)
        .map { DiscoverCard(it.id, it.name, it.imageUrl, traitsInCommon = emptyList()) }

    override suspend fun react(characterId: String, liked: Boolean) = Unit
}
