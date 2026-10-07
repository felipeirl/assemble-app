package dev.assemble.app.feature.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginLayoutTest {

    @Test
    fun largestFittingFontSp_takesTheMaximumWhenItFits() {
        assertEquals(64f, largestFittingFontSp(64f, 28f, 2f) { true })
    }

    @Test
    fun largestFittingFontSp_shrinksUntilItFits() {
        assertEquals(46f, largestFittingFontSp(64f, 28f, 2f) { it <= 47f })
    }

    @Test
    fun largestFittingFontSp_acceptsTheMinimum() {
        assertEquals(28f, largestFittingFontSp(64f, 28f, 2f) { it <= 28f })
    }

    @Test
    fun largestFittingFontSp_isNullWhenNothingFits() {
        assertNull(largestFittingFontSp(64f, 28f, 2f) { false })
    }
}
