package dev.assemble.app.core.feedback

import androidx.annotation.RawRes
import dev.assemble.app.R

/** Efeito sonoro. Os arquivos ficam em `res/raw`. */
enum class Sound(@RawRes val resId: Int) {
    Pass(R.raw.sfx_pass),
    Assemble(R.raw.sfx_assemble),
    Match(R.raw.sfx_match),
    Achievement(R.raw.sfx_achievement),
    MessageIn(R.raw.sfx_message_in),
    MessageOut(R.raw.sfx_message_out),
    Splash(R.raw.sfx_splash),
}

/** Padrão de vibração. [Match] e [MatchLong] são os fortes; os demais são toques curtos. */
enum class Haptic { Tick, Click, DoubleClick, Match, MatchLong }

/** Momentos do app que dão retorno ao usuário (som, vibração ou os dois). Tudo passa por [Feedback.play]. */
enum class Cue(val sound: Sound?, val haptic: Haptic?) {
    Splash(Sound.Splash, Haptic.Tick),
    Tick(null, Haptic.Tick),
    Pass(Sound.Pass, Haptic.Tick),
    Assemble(Sound.Assemble, Haptic.Click),
    Match(Sound.Match, Haptic.Match),
    MessageIn(Sound.MessageIn, Haptic.Tick),
    MessageOut(Sound.MessageOut, Haptic.Tick),
    Achievement(Sound.Achievement, Haptic.Click),
    Error(null, Haptic.DoubleClick),
}

/** Modo de toque do aparelho. */
enum class RingerMode { Normal, Vibrate, Silent }

/** O que tocar de fato: [sound] só no modo normal; [haptic] já adaptado ao modo do aparelho. */
data class FeedbackPlan(val sound: Sound?, val haptic: Haptic?)

/** Decide o que tocar a partir do cue, do modo do aparelho e das opções do app. Pura, para testar sem aparelho. */
object FeedbackPolicy {

    fun plan(
        cue: Cue,
        ringer: RingerMode,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        reduceMotion: Boolean,
    ): FeedbackPlan {
        val sound = cue.sound.takeIf { soundEnabled && ringer == RingerMode.Normal }
        val haptic = cue.haptic?.takeIf { vibrationEnabled }?.let { adapt(it, ringer, reduceMotion) }
        return FeedbackPlan(sound, haptic)
    }

    /**
     * Vibrar: o match ganha o padrão longo, já que não há som. Silencioso: sem vibração forte.
     * Reduzir movimento: os fortes viram um clique.
     */
    private fun adapt(haptic: Haptic, ringer: RingerMode, reduceMotion: Boolean): Haptic? {
        val strong = haptic == Haptic.Match || haptic == Haptic.MatchLong || haptic == Haptic.DoubleClick
        return when {
            strong && ringer == RingerMode.Silent -> null
            strong && reduceMotion -> Haptic.Click
            haptic == Haptic.Match && ringer == RingerMode.Vibrate -> Haptic.MatchLong
            else -> haptic
        }
    }
}
