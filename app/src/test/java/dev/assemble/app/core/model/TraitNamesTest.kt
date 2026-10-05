package dev.assemble.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TraitNamesTest {
    @Test
    fun `contract names map to the app enums`() {
        assertEquals(Origin.Mutant, traitFromName("Mutant"))
        assertEquals(PowerFamily.TechGadgets, traitFromName("TechGadgets"))
        assertEquals(Team.XMen, traitFromName("XMen"))
        assertEquals(Style.Leadership, traitFromName("Leadership"))
    }

    @Test
    fun `unknown names are dropped, order is kept`() {
        assertNull(traitFromName("Wizard"))
        assertEquals(listOf(Team.XMen, Origin.Human), traitsFromNames(listOf("XMen", "Wizard", "Human")))
    }

    @Test
    fun `trait names never repeat across categories`() {
        val all = Origin.entries + PowerFamily.entries + Team.entries + Style.entries
        assertEquals(all.size, all.map { it.name }.toSet().size)
    }
}
