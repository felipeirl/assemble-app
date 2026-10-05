package dev.assemble.app.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class Origin { Human, Mutant, Alien, Robot, Radiation, GodEternal, Animal, Cosmic, Infection, Other }

@Serializable
enum class PowerFamily { Strength, Flight, Speed, Mind, Energy, Magic, AgilityCombat, Healing, Shapeshifting, TechGadgets }

@Serializable
enum class Team { Avengers, XMen, FantasticFour, Guardians, Shield, Defenders, Solo }

@Serializable
enum class Style { Science, Humor, Leadership, Loner, Dark, Idealist, Rebel, Strategist }

/** Personagem do catálogo. Campo ausente na fonte fica null/vazio; nunca é inventado. */
@Serializable
data class Character(
    val id: String,
    val name: String,
    val realName: String?,
    val imageUrl: String?,
    val origin: Origin?,
    val powers: List<PowerFamily>,
    val teams: List<Team>,
    val styles: List<Style>,
    val firstAppearance: String?,
    val issueAppearances: Int?,
    val bio: String?,
    val publisher: String = "Marvel",
    val source: String = "Comic Vine",
    // Enriquecimento (Superhero API). Opcionais: sem casamento seguro com a fonte, ficam vazios.
    val aliases: List<String> = emptyList(),
    val placeOfBirth: String? = null,
    val occupation: String? = null,
    val base: String? = null,
    val relatives: String? = null,
    val alignment: Alignment? = null,
    val powerstats: Powerstats? = null,
    val appearance: Appearance? = null,
)
