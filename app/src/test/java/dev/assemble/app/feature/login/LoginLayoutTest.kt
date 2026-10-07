package dev.assemble.app.feature.login

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginLayoutTest {

    @Test
    fun heroVariantFor_fullFromItsMinimumUp() {
        assertEquals(HeroVariant.Full, heroVariantFor(FullHeroMinHeight))
        assertEquals(HeroVariant.Full, heroVariantFor(600.dp))
    }

    @Test
    fun heroVariantFor_compactBetweenTheLimits() {
        assertEquals(HeroVariant.Compact, heroVariantFor(FullHeroMinHeight - 1.dp))
        assertEquals(HeroVariant.Compact, heroVariantFor(CompactHeroMinHeight))
    }

    @Test
    fun heroVariantFor_hiddenBelowCompactMinimum() {
        assertEquals(HeroVariant.Hidden, heroVariantFor(CompactHeroMinHeight - 1.dp))
        assertEquals(HeroVariant.Hidden, heroVariantFor(0.dp))
    }
}
