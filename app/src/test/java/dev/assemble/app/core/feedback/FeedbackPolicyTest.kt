package dev.assemble.app.core.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedbackPolicyTest {

    private fun plan(
        cue: Cue,
        ringer: RingerMode = RingerMode.Normal,
        sound: Boolean = true,
        vibration: Boolean = true,
        reduceMotion: Boolean = false,
    ) = FeedbackPolicy.plan(cue, ringer, sound, vibration, reduceMotion)

    @Test
    fun normalMode_playsSoundAndHaptic() {
        assertEquals(FeedbackPlan(Sound.Match, Haptic.Match), plan(Cue.Match))
    }

    @Test
    fun cueWithoutSound_neverPlaysOne() {
        assertNull(plan(Cue.Tick).sound)
        assertEquals(Haptic.Tick, plan(Cue.Tick).haptic)
    }

    @Test
    fun vibrateMode_hasNoSound_andMatchGetsTheLongPattern() {
        assertEquals(FeedbackPlan(null, Haptic.MatchLong), plan(Cue.Match, RingerMode.Vibrate))
        assertEquals(FeedbackPlan(null, Haptic.Click), plan(Cue.Assemble, RingerMode.Vibrate))
    }

    @Test
    fun silentMode_dropsSoundAndStrongHaptics_butKeepsLightOnes() {
        assertEquals(FeedbackPlan(null, null), plan(Cue.Match, RingerMode.Silent))
        assertNull(plan(Cue.Error, RingerMode.Silent).haptic)
        assertEquals(Haptic.Tick, plan(Cue.MessageIn, RingerMode.Silent).haptic)
    }

    @Test
    fun soundOption_offSilencesEverythingButHaptics() {
        assertEquals(FeedbackPlan(null, Haptic.Match), plan(Cue.Match, sound = false))
    }

    @Test
    fun vibrationOption_offKeepsSoundOnly() {
        assertEquals(FeedbackPlan(Sound.Pass, null), plan(Cue.Pass, vibration = false))
    }

    @Test
    fun reduceMotion_turnsStrongHapticsIntoAClick() {
        assertEquals(Haptic.Click, plan(Cue.Match, reduceMotion = true).haptic)
        assertEquals(Haptic.Click, plan(Cue.Error, reduceMotion = true).haptic)
        assertEquals(Haptic.Tick, plan(Cue.Pass, reduceMotion = true).haptic)
    }
}
