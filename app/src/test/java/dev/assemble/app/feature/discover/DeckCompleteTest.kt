package dev.assemble.app.feature.discover

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class DeckCompleteTest {

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")

    @Test
    fun nextDeckArrivesAtLocalMidnight() {
        val now = ZonedDateTime.of(2026, 9, 30, 16, 17, 45, 0, saoPaulo)
        assertEquals(Duration.ofHours(7).plusMinutes(42).plusSeconds(15), timeUntilNextDeck(now))
    }

    @Test
    fun justAfterMidnight_waitsAlmostADay() {
        val now = ZonedDateTime.of(2026, 10, 1, 0, 0, 1, 0, saoPaulo)
        assertEquals(Duration.ofDays(1).minusSeconds(1), timeUntilNextDeck(now))
    }

    @Test
    fun countdownAlwaysHasTwoDigitParts() {
        assertEquals("07:42:15", formatCountdown(Duration.ofHours(7).plusMinutes(42).plusSeconds(15)))
        assertEquals("00:00:05", formatCountdown(Duration.ofSeconds(5)))
        assertEquals("00:00:00", formatCountdown(Duration.ofSeconds(-3)))
    }
}
