package dev.assemble.app.feature.character

import dev.assemble.app.R
import dev.assemble.app.core.data.mock.CHARACTERS_ASSET_PATH
import dev.assemble.app.core.data.mock.MockSeed
import dev.assemble.app.core.data.mock.parseMockCharacters
import dev.assemble.app.core.domain.CompatibilityCalculator
import dev.assemble.app.core.model.Appearance
import dev.assemble.app.core.model.DataSource
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
        val facts = profileFacts(characters.getValue("jean-grey")) // realName e placeOfBirth null
        assertTrue(facts.none { it.label == R.string.profile_real_name })
        assertEquals(
            listOf(
                R.string.profile_aliases,
                R.string.profile_origin,
                R.string.profile_powers,
                R.string.profile_teams,
                R.string.profile_alignment,
                R.string.profile_occupation,
                R.string.profile_base,
                R.string.profile_first_appearance,
                R.string.profile_issue_appearances,
            ),
            facts.map { it.label },
        )
    }

    @Test
    fun profileFacts_tagEachSource() {
        val facts = profileFacts(characters.getValue("storm")).associateBy { it.label }
        assertEquals(DataSource.ComicVine, facts.getValue(R.string.profile_real_name).source)
        assertEquals(DataSource.SuperheroApi, facts.getValue(R.string.profile_place_of_birth).source)
        assertEquals(R.string.alignment_good, facts.getValue(R.string.profile_alignment).valueRes)
    }

    @Test
    fun appearanceFacts_formatUnitsAndSkipMissing() {
        val facts = appearanceFacts(Appearance(gender = "Female", heightCm = 180))
        assertEquals(listOf(R.string.appearance_gender, R.string.appearance_height), facts.map { it.label })
        assertEquals("180 cm", facts[1].text)
        assertTrue(appearanceFacts(null).isEmpty())
    }

    @Test
    fun teammates_shareATeamAndListConnectedFirst() {
        val catalog = characters.values.toList()
        val nodes = teammates(characters.getValue("iron-man"), catalog, connectedIds = setOf("spider-man", "iron-man"))
        assertTrue(nodes.none { it.characterId == "iron-man" })
        assertTrue(nodes.all { node -> Team.Avengers in characters.getValue(node.characterId).teams })
        assertEquals("spider-man", nodes.first().characterId)
        assertTrue(nodes.drop(1).none { it.connected })
    }

    @Test
    fun profileSources_includeSuperheroApiOnlyWhenUsed() {
        val storm = characters.getValue("storm")
        assertEquals(listOf(DataSource.ComicVine, DataSource.SuperheroApi), profileSources(storm, profileFacts(storm)))
        val bare = storm.copy(aliases = emptyList(), placeOfBirth = null, occupation = null, base = null, relatives = null, alignment = null, powerstats = null)
        assertEquals(listOf(DataSource.ComicVine), profileSources(bare, profileFacts(bare)))
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
