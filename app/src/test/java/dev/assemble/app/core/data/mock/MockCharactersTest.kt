package dev.assemble.app.core.data.mock

import dev.assemble.app.core.model.Origin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MockCharactersTest {

    // Testes unitários rodam com o diretório do módulo (app/) como diretório de trabalho.
    private val json = File("src/main/assets/$CHARACTERS_ASSET_PATH").readText()

    @Test
    fun parsesAllSevenCharacters() {
        val characters = parseMockCharacters(json)
        assertEquals(
            listOf("spider-man", "iron-man", "storm", "captain-america", "black-panther", "jean-grey", "rocket"),
            characters.map { it.id },
        )
    }

    @Test
    fun keepsMissingFieldsNull_andDefaultsSource() {
        val characters = parseMockCharacters(json).associateBy { it.id }
        assertNull(characters.getValue("jean-grey").realName)
        assertNull(characters.getValue("rocket").realName)
        assertTrue(characters.values.all { it.bio == null })
        assertTrue(characters.values.all { it.source == "Comic Vine" && it.publisher == "Marvel" })
        assertEquals(Origin.Animal, characters.getValue("rocket").origin)
    }
}
