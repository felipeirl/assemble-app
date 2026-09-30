package dev.assemble.app.core.domain

import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.MatchBand
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.Style
import dev.assemble.app.core.model.Team
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToInt

/** Resultado de uma categoria de conjunto: itens em comum e pontos obtidos. */
data class CategoryMatch<T>(
    val matched: Set<T>,
    val points: Double,
    /** true quando o usuário escolheu "Any" (conjunto vazio). */
    val isAny: Boolean,
)

/** Detalhe do cálculo, para "Why you match" e para os chips "In common". */
data class CompatibilityBreakdown(
    val origin: CategoryMatch<Origin>,
    val powers: CategoryMatch<PowerFamily>,
    val teams: CategoryMatch<Team>,
    val styles: CategoryMatch<Style>,
    val famePoints: Double,
) {
    val score: Int
        get() = (origin.points + powers.points + teams.points + styles.points + famePoints)
            .roundToInt()
            .coerceIn(0, CompatibilityCalculator.MAX_SCORE)
}

/**
 * Estimativa explicável de afinidade entre as preferências declaradas e as características
 * catalogadas do personagem. Função pura. No MVP real, o score autoritativo vem do backend.
 */
object CompatibilityCalculator {
    const val MAX_SCORE = 100
    const val WEIGHT_ORIGIN = 25
    const val WEIGHT_POWERS = 30
    const val WEIGHT_TEAMS = 15
    const val WEIGHT_STYLE = 20
    const val WEIGHT_FAME = 10

    /** Largura das faixas abaixo do limiar: Possible = [L-20, L-1]. */
    const val POSSIBLE_BAND_WIDTH = 20

    /** Escala log de fama: 100 aparições = Hidden gems (1), 10.000 = Icons (0). */
    const val FAME_MIN_APPEARANCES = 100
    const val FAME_MAX_APPEARANCES = 10_000

    fun score(preferences: Preferences, character: Character): Int =
        breakdown(preferences, character).score

    fun breakdown(preferences: Preferences, character: Character): CompatibilityBreakdown =
        CompatibilityBreakdown(
            origin = matchSet(preferences.origins, setOfNotNull(character.origin), WEIGHT_ORIGIN),
            powers = matchSet(preferences.powers, character.powers.toSet(), WEIGHT_POWERS),
            teams = matchSet(preferences.teams, character.teams.toSet(), WEIGHT_TEAMS),
            styles = matchSet(preferences.styles, character.styles.toSet(), WEIGHT_STYLE),
            famePoints = famePoints(preferences.fame, character.issueAppearances),
        )

    /** High ≥ L; Possible entre L−20 e L−1; Low < L−20. */
    fun band(score: Int, threshold: Int): MatchBand = when {
        score >= threshold -> MatchBand.High
        score >= threshold - POSSIBLE_BAND_WIDTH -> MatchBand.Possible
        else -> MatchBand.Low
    }

    /** Fama do personagem em 0 (Icons) … 1 (Hidden gems); null se as aparições forem desconhecidas. */
    fun characterFame(issueAppearances: Int?): Double? {
        if (issueAppearances == null) return null
        val clamped = issueAppearances.coerceIn(FAME_MIN_APPEARANCES, FAME_MAX_APPEARANCES)
        val minLog = log10(FAME_MIN_APPEARANCES.toDouble())
        val maxLog = log10(FAME_MAX_APPEARANCES.toDouble())
        val iconness = (log10(clamped.toDouble()) - minLog) / (maxLog - minLog)
        return 1.0 - iconness
    }

    private fun famePoints(preferredFame: Float, issueAppearances: Int?): Double {
        val fame = characterFame(issueAppearances) ?: return 0.0
        val preferred = preferredFame.toDouble().coerceIn(0.0, 1.0)
        return WEIGHT_FAME * (1.0 - abs(preferred - fame))
    }

    private fun <T> matchSet(chosen: Set<T>, characterValues: Set<T>, weight: Int): CategoryMatch<T> {
        if (chosen.isEmpty()) return CategoryMatch(matched = emptySet(), points = weight.toDouble(), isAny = true)
        val matched = chosen intersect characterValues
        val points = weight * matched.size.toDouble() / chosen.size
        return CategoryMatch(matched = matched, points = points, isAny = false)
    }
}
