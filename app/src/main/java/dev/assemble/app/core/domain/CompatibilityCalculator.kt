package dev.assemble.app.core.domain

import dev.assemble.app.core.model.Character
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

    /** Faixas fixas do card: High ≥ 70, Possible 50–69, Low < 50 (não há ajuste pelo usuário). */
    const val MATCH_THRESHOLD = 70

    /** "Any" é neutro: metade do peso, para não combinar com todo mundo. */
    const val ANY_FACTOR = 0.5

    /** Sem nada em comum e com um traço rival do que o usuário escolheu: perde metade do peso. */
    const val RIVAL_PENALTY = 0.5

    /**
     * Rivalidades (simétricas), iguais às do backend. Equipes e origens só com conflito
     * documentado nas HQs: Avengers x X-Men ("Avengers vs. X-Men", 2012), Avengers x Defenders
     * ("The Avengers/Defenders War", 1973), X-Men x S.H.I.E.L.D. (Uncanny X-Men, 2013),
     * Mutante x Humano (preconceito anti-mutante), Mutante x Robô (os Sentinelas).
     * Estilos: opostos diretos de atitude. Poderes não têm rivalidade.
     */
    val RIVALRIES: List<Pair<Enum<*>, Enum<*>>> = listOf(
        Team.Avengers to Team.XMen,
        Team.Avengers to Team.Defenders,
        Team.XMen to Team.Shield,
        Origin.Mutant to Origin.Human,
        Origin.Mutant to Origin.Robot,
        Style.Leadership to Style.Loner,
        Style.Leadership to Style.Rebel,
        Style.Idealist to Style.Dark,
    )
    private val rivals: Set<Pair<Enum<*>, Enum<*>>> = RIVALRIES.flatMap { (a, b) -> listOf(a to b, b to a) }.toSet()

    fun areRivals(a: Enum<*>, b: Enum<*>): Boolean = (a to b) in rivals

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

    /**
     * peso × (itens em comum ÷ menor entre escolhidos e os do personagem).
     * Assim, escolher várias opções não pune quem tem só uma (ex.: uma origem).
     * "Any" vale metade do peso; nada em comum com um traço rival tira metade do peso.
     */
    private fun <T : Enum<*>> matchSet(chosen: Set<T>, characterValues: Set<T>, weight: Int): CategoryMatch<T> {
        if (chosen.isEmpty()) return CategoryMatch(matched = emptySet(), points = weight * ANY_FACTOR, isAny = true)
        if (characterValues.isEmpty()) return CategoryMatch(matched = emptySet(), points = 0.0, isAny = false)
        val matched = chosen intersect characterValues
        if (matched.isEmpty()) {
            val rival = chosen.any { mine -> characterValues.any { theirs -> areRivals(mine, theirs) } }
            return CategoryMatch(matched = emptySet(), points = if (rival) -weight * RIVAL_PENALTY else 0.0, isAny = false)
        }
        val points = weight * matched.size.toDouble() / minOf(chosen.size, characterValues.size)
        return CategoryMatch(matched = matched, points = points, isAny = false)
    }
}
