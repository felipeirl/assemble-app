package dev.assemble.app.feature.character

import dev.assemble.app.R
import dev.assemble.app.core.data.mock.CHARACTERS_ASSET_PATH
import dev.assemble.app.core.data.mock.MockSeed
import dev.assemble.app.core.data.mock.parseMockCharacters
import dev.assemble.app.core.domain.CompatibilityCalculator
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.Team
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CharacterProfileContentTest {

    private val characters = parseMockCharacters(File("src/main/assets/$CHARACTERS_ASSET_PATH").readText())
        .associateBy { it.id }

    @Test
    fun profileFacts_omitNullFields() {
        val facts = profileFacts(characters.getValue("jean-grey")) // realName null
        assertTrue(facts.none { it.label == R.string.profile_real_name })
        assertEquals(
            listOf(
                R.string.profile_origin,
                R.string.profile_powers,
                R.string.profile_teams,
                R.string.profile_first_appearance,
                R.string.profile_issue_appearances,
            ),
            facts.map { it.label },
        )
    }

    @Test
    fun profileFacts_keepRealNameWhenPresent() {
        val facts = profileFacts(characters.getValue("storm"))
        assertEquals("Ororo Munroe", facts.first { it.label == R.string.profile_real_name }.text)
    }

    @Test
    fun whyYouMatch_listsOnlyCategoriesWithCommonItems() {
        val breakdown = CompatibilityCalculator.breakdown(MockSeed.initialPreferences, characters.getValue("jean-grey"))
        val items = whyYouMatch(breakdown)
        assertEquals(listOf(R.string.profile_origin, R.string.profile_powers, R.string.profile_teams), items.map { it.category })
        assertEquals(listOf(listOf(Origin.Mutant), listOf(PowerFamily.Mind), listOf(Team.XMen)), items.map { it.traits })
    }

    @Test
    fun whyYouMatch_isEmptyWhenEverythingIsAny() {
        val breakdown = CompatibilityCalculator.breakdown(Preferences.Any, characters.getValue("rocket"))
        assertTrue(whyYouMatch(breakdown).isEmpty())
    }
}
