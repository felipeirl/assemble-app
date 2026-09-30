package dev.assemble.app.feature.discover

import dev.assemble.app.core.data.mock.CHARACTERS_ASSET_PATH
import dev.assemble.app.core.data.mock.MockSeed
import dev.assemble.app.core.data.mock.parseMockCharacters
import dev.assemble.app.core.model.MatchBand
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Team
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class BuildDeckTest {

    private val characters = parseMockCharacters(File("src/main/assets/$CHARACTERS_ASSET_PATH").readText())

    @Test
    fun ordersByScore_andExcludesSeenAndConnected() {
        val deck = buildDeck(
            characters = characters,
            preferences = MockSeed.initialPreferences,
            excludedIds = setOf("spider-man", "storm"),
        )
        // Scores com as preferências iniciais: Jean Grey 77, Black Panther 68, Iron Man 66, Captain America 50, Rocket 40.
        assertEquals(listOf("jean-grey", "black-panther", "iron-man", "captain-america", "rocket"), deck.map { it.characterId })
    }

    @Test
    fun bandsUseFixedLimits() {
        val deck = buildDeck(characters, MockSeed.initialPreferences, excludedIds = emptySet())
            .associateBy { it.characterId }
        assertEquals(MatchBand.High, deck.getValue("jean-grey").band)
        assertEquals(MatchBand.Possible, deck.getValue("storm").band)
        assertEquals(MatchBand.Low, deck.getValue("rocket").band)
    }

    @Test
    fun traitsInCommonFollowCategoryOrder() {
        val jean = buildDeck(characters, MockSeed.initialPreferences, emptySet()).first { it.characterId == "jean-grey" }
        assertEquals(listOf(Origin.Mutant, PowerFamily.Mind, Team.XMen), jean.traitsInCommon)
    }
}
