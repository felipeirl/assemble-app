package dev.assemble.app.core.domain

import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.MatchBand
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.Style
import dev.assemble.app.core.model.Team
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatibilityCalculatorTest {

    private val hero = Character(
        id = "hero",
        name = "Hero",
        realName = null,
        imageUrl = null,
        origin = Origin.Mutant,
        powers = listOf(PowerFamily.Mind, PowerFamily.Flight),
        teams = listOf(Team.XMen),
        styles = listOf(Style.Leadership, Style.Idealist),
        firstAppearance = null,
        issueAppearances = 1_000, // fama 0.5
        bio = null,
    )

    private fun prefs(
        origins: Set<Origin> = emptySet(),
        powers: Set<PowerFamily> = emptySet(),
        teams: Set<Team> = emptySet(),
        styles: Set<Style> = emptySet(),
        fame: Float = 0.5f,
    ) = Preferences(origins, powers, teams, styles, fame)

    @Test
    fun anyInEveryCategory_withMatchingFame_scores100() {
        assertEquals(100, CompatibilityCalculator.score(prefs(fame = 0.5f), hero))
    }

    @Test
    fun anyInEveryCategory_marksCategoriesAsAny_withNoMatchedItems() {
        val breakdown = CompatibilityCalculator.breakdown(prefs(), hero)
        assertTrue(breakdown.origin.isAny && breakdown.powers.isAny && breakdown.teams.isAny && breakdown.styles.isAny)
        assertTrue(breakdown.origin.matched.isEmpty())
    }

    @Test
    fun nothingInCommon_scoresOnlyFame() {
        val noOverlap = prefs(
            origins = setOf(Origin.Robot),
            powers = setOf(PowerFamily.Speed),
            teams = setOf(Team.Avengers),
            styles = setOf(Style.Humor),
            fame = 0.3f,
        )
        // Fama: 10 × (1 − |0.3 − 0.5|) = 8
        assertEquals(8, CompatibilityCalculator.score(noOverlap, hero))
    }

    @Test
    fun partialOverlap_dividesByTheSmallerSet() {
        val partial = prefs(
            origins = setOf(Origin.Mutant, Origin.Human), // 1 ÷ min(2, 1) × 25 = 25
            powers = setOf(PowerFamily.Mind, PowerFamily.Speed, PowerFamily.Magic), // 1 ÷ min(3, 2) × 30 = 15
            teams = setOf(Team.Avengers), // 0
            styles = setOf(Style.Leadership, Style.Idealist, Style.Rebel, Style.Dark), // 2 ÷ min(4, 2) × 20 = 20
            fame = 0.5f, // 10
        )
        assertEquals(70, CompatibilityCalculator.score(partial, hero))
        val breakdown = CompatibilityCalculator.breakdown(partial, hero)
        assertEquals(setOf(Origin.Mutant), breakdown.origin.matched)
        assertEquals(setOf(Style.Leadership, Style.Idealist), breakdown.styles.matched)
    }

    @Test
    fun roundsHalfUp() {
        val half = prefs(
            origins = setOf(Origin.Robot), // 0
            powers = setOf(PowerFamily.Mind, PowerFamily.Speed), // 1 ÷ 2 × 30 = 15
            teams = setOf(Team.Avengers), // 0
            styles = setOf(Style.Humor), // 0
            fame = 0.75f, // 10 × (1 − 0.25) = 7.5 (exato em binário)
        )
        assertEquals(23, CompatibilityCalculator.score(half, hero)) // 22.5 → 23
    }

    @Test
    fun nullOrEmptyField_inChosenCategory_scoresZero() {
        val unknown = hero.copy(origin = null, powers = emptyList(), issueAppearances = null)
        val chosen = prefs(
            origins = setOf(Origin.Mutant),
            powers = setOf(PowerFamily.Mind),
            teams = setOf(Team.XMen), // 15
            styles = setOf(Style.Leadership), // 20
        )
        val breakdown = CompatibilityCalculator.breakdown(chosen, unknown)
        assertEquals(0.0, breakdown.origin.points, DELTA)
        assertEquals(0.0, breakdown.powers.points, DELTA)
        assertEquals(0.0, breakdown.famePoints, DELTA)
        assertEquals(35, breakdown.score)
    }

    @Test
    fun nullField_withAny_keepsFullWeight() {
        val unknown = hero.copy(origin = null, powers = emptyList())
        val breakdown = CompatibilityCalculator.breakdown(prefs(), unknown)
        assertEquals(CompatibilityCalculator.WEIGHT_ORIGIN.toDouble(), breakdown.origin.points, DELTA)
        assertEquals(CompatibilityCalculator.WEIGHT_POWERS.toDouble(), breakdown.powers.points, DELTA)
    }

    @Test
    fun characterFame_isLogScaledAndClamped() {
        assertEquals(1.0, CompatibilityCalculator.characterFame(100)!!, DELTA)
        assertEquals(0.5, CompatibilityCalculator.characterFame(1_000)!!, DELTA)
        assertEquals(0.0, CompatibilityCalculator.characterFame(10_000)!!, DELTA)
        assertEquals(1.0, CompatibilityCalculator.characterFame(5)!!, DELTA)
        assertEquals(0.0, CompatibilityCalculator.characterFame(50_000)!!, DELTA)
        assertNull(CompatibilityCalculator.characterFame(null))
    }

    @Test
    fun band_usesFixedLimits() {
        assertEquals(MatchBand.High, CompatibilityCalculator.band(100))
        assertEquals(MatchBand.High, CompatibilityCalculator.band(70))
        assertEquals(MatchBand.Possible, CompatibilityCalculator.band(69))
        assertEquals(MatchBand.Possible, CompatibilityCalculator.band(50))
        assertEquals(MatchBand.Low, CompatibilityCalculator.band(49))
        assertEquals(MatchBand.Low, CompatibilityCalculator.band(0))
    }

    @Test
    fun score_staysWithinBounds() {
        val outOfRangeFame = prefs(fame = 3f)
        val score = CompatibilityCalculator.score(outOfRangeFame, hero.copy(issueAppearances = 100))
        assertTrue(score in 0..CompatibilityCalculator.MAX_SCORE)
    }

    private companion object {
        const val DELTA = 1e-9
    }
}
