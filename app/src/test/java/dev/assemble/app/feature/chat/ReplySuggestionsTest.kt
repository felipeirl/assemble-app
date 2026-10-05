package dev.assemble.app.feature.chat

import dev.assemble.app.R
import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Team
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplySuggestionsTest {

    private fun character(powers: List<PowerFamily> = emptyList(), teams: List<Team> = emptyList()) = Character(
        id = "storm", name = "Storm", realName = null, imageUrl = null, origin = null,
        powers = powers, teams = teams, styles = emptyList(),
        firstAppearance = null, issueAppearances = null, bio = null,
    )

    @Test
    fun beforeFirstMessage_startsWithHelloUsingName() {
        val suggestions = replySuggestions(character(listOf(PowerFamily.Energy), listOf(Team.XMen)), messagesSent = 0)
        assertEquals(3, suggestions.size)
        assertEquals(R.string.chat_suggest_hello, suggestions.first().text)
        assertEquals(SuggestionArg.Name("Storm"), suggestions.first().arg)
        assertEquals(SuggestionArg.Trait(PowerFamily.Energy), suggestions[1].arg)
    }

    @Test
    fun afterMessages_noHello_andSetRotates() {
        val storm = character(listOf(PowerFamily.Energy), listOf(Team.XMen))
        val first = replySuggestions(storm, messagesSent = 1)
        val second = replySuggestions(storm, messagesSent = 2)
        assertFalse(first.any { it.text == R.string.chat_suggest_hello })
        assertEquals(3, second.size)
        assertNotEquals(first, second)
    }

    @Test
    fun soloAndMissingTraits_areSkipped() {
        val suggestions = replySuggestions(character(teams = listOf(Team.Solo)), messagesSent = 1, count = 10)
        assertTrue(suggestions.none { it.arg is SuggestionArg.Trait })
        assertEquals(3, suggestions.size)
    }
}
