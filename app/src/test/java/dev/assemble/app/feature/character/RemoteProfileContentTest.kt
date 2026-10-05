package dev.assemble.app.feature.character

import dev.assemble.app.R
import dev.assemble.app.core.model.DataSource
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.Powerstats
import dev.assemble.app.core.model.Team
import dev.assemble.app.core.network.ApiCharacterView
import dev.assemble.app.core.network.ApiCompareWith
import dev.assemble.app.core.network.ApiFacts
import dev.assemble.app.core.network.ApiSource
import dev.assemble.app.core.network.ApiTeammate
import dev.assemble.app.core.network.ApiWhyYouMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteProfileContentTest {
    private val stats = Powerstats(intelligence = 88, strength = 50, speed = 60, durability = 70, power = 95, combat = 75)

    private val view = ApiCharacterView(
        characterId = "storm",
        name = "Storm",
        imageUrl = "https://img/storm",
        connected = true,
        connectionId = "storm",
        score = 91,
        whyYouMatch = listOf(
            ApiWhyYouMatch("origin", listOf("Mutant")),
            ApiWhyYouMatch("teams", listOf("XMen", "Unknown")),
            ApiWhyYouMatch("fame", listOf("Icon")),
            ApiWhyYouMatch("powers", emptyList()),
        ),
        facts = ApiFacts(realName = "Ororo Munroe", origin = "Mutant", teams = listOf("XMen"), powerstats = stats),
        sources = listOf(ApiSource("Superhero API"), ApiSource("Comic Vine", "https://comicvine.gamespot.com"), ApiSource("Wikipedia")),
        teammates = listOf(ApiTeammate("cyclops", "Cyclops", connected = false)),
        compareWith = listOf(ApiCompareWith("jean-grey", "Jean Grey", stats)),
    )

    @Test
    fun remoteProfileContent_usesServerCalculations() {
        val content = remoteProfileContent(view, unlockPending = true)
        assertEquals(91, content.score)
        assertTrue(content.unlockPending)
        assertEquals(
            listOf(WhyYouMatchItem(R.string.profile_origin, listOf(Origin.Mutant)), WhyYouMatchItem(R.string.profile_teams, listOf(Team.XMen))),
            content.whyYouMatch,
        )
        assertEquals(listOf(TeammateNode("cyclops", "Cyclops", null, connected = false)), content.teammates)
        assertEquals(listOf(StatsOption("jean-grey", "Jean Grey", stats)), content.compareOptions)
        assertEquals(stats, content.powerstats)
        assertEquals(listOf(Team.XMen), content.teams)
    }

    @Test
    fun remoteProfileContent_sourcesFollowEnumOrderAndSkipUnknown() {
        val sources = remoteProfileContent(view, unlockPending = false).sources
        assertEquals(DataSource.entries.filter { it == DataSource.ComicVine || it == DataSource.SuperheroApi }, sources)
    }
}
