package dev.assemble.app.core.model

import kotlinx.serialization.Serializable

/** Alinhamento do personagem segundo a fonte (Superhero API: good, bad, neutral). */
@Serializable
enum class Alignment { Good, Bad, Neutral }

/**
 * Seis atributos de 0 a 100, como a Superhero API publica. São estimativas da fonte, não do Assemble.
 * A ordem de [values] é a dos eixos do radar.
 */
@Serializable
data class Powerstats(
    val intelligence: Int,
    val strength: Int,
    val speed: Int,
    val durability: Int,
    val power: Int,
    val combat: Int,
) {
    val values: List<Int> get() = listOf(intelligence, strength, speed, durability, power, combat)
}

/** Aparência segundo a fonte. Campo que ela não traz fica null e não aparece. */
@Serializable
data class Appearance(
    val gender: String? = null,
    val race: String? = null,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val eyeColor: String? = null,
    val hairColor: String? = null,
)

/** De onde veio cada dado mostrado no perfil (a sigla aparece ao lado do campo). */
enum class DataSource { ComicVine, SuperheroApi, MarvelDatabase }
