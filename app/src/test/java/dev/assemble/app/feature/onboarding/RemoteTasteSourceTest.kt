package dev.assemble.app.feature.onboarding

import dev.assemble.app.core.data.remote.FakeAssembleApi
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.network.ApiDeckCard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteTasteSourceTest {
    private val api = FakeAssembleApi()

    @Test
    fun reactionCards_mapTraitsAndTagline() = runBlocking {
        api.onReactionCards = {
            listOf(ApiDeckCard("storm", "Tempestade", traitsInCommon = listOf("Mutant"), tagline = "Controla o clima."))
        }

        val card = RemoteTasteSource(api).reactionCards().single()

        assertEquals("Tempestade", card.name)
        assertEquals(listOf(Origin.Mutant), card.traitsInCommon)
        assertEquals("Controla o clima.", card.tagline)
    }

    @Test
    fun react_sendsTheSignalNotADecision() = runBlocking {
        RemoteTasteSource(api).react("storm", liked = false)

        assertEquals(listOf("storm" to false), api.signals)
        assertEquals(emptyList<FakeAssembleApi.Decision>(), api.decisions)
    }
}
