package dev.assemble.app.core.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.runtime.staticCompositionLocalOf
import dev.assemble.app.core.model.AppSettings

private const val MAX_STREAMS = 4
private const val DEFAULT_ANIMATOR_SCALE = 1f
private const val TICK_MILLIS = 12L
private const val CLICK_MILLIS = 25L
private val DoubleClickTimings = longArrayOf(0, 25, 70, 25)

// Pulso duplo no momento da explosão; no modo vibrar, uma sequência maior que "narra" o match.
private val MatchTimings = longArrayOf(0, 45, 60, 90)
private val MatchAmplitudes = intArrayOf(0, 140, 0, 255)
private val MatchLongTimings = longArrayOf(0, 50, 70, 50, 70, 120, 90, 220)
private val MatchLongAmplitudes = intArrayOf(0, 120, 0, 180, 0, 255, 0, 255)

/** Entrada única de som e vibração. As telas só dizem o momento; o resto é decidido aqui. */
interface Feedback {
    fun play(cue: Cue)
}

/** Sem retorno: previews e testes. */
object NoFeedback : Feedback {
    override fun play(cue: Cue) = Unit
}

val LocalFeedback = staticCompositionLocalOf<Feedback> { NoFeedback }

/**
 * Toca os sons (SoundPool, carregados uma vez) e vibra (Vibrator) conforme [FeedbackPolicy].
 * Lê o modo do aparelho e as opções a cada toque, então mudar o toque do celular ou a configuração vale na hora.
 */
class FeedbackPlayer(
    context: Context,
    private val settings: () -> AppSettings,
) : Feedback {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val vibrator: Vibrator? = findVibrator(appContext)

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val soundIds = Sound.entries.associateWith { soundPool.load(appContext, it.resId, 1) }
    private val loaded = mutableSetOf<Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) loaded += sampleId }
    }

    override fun play(cue: Cue) {
        val current = settings()
        val plan = FeedbackPolicy.plan(
            cue = cue,
            ringer = ringerMode(),
            soundEnabled = current.soundEnabled,
            vibrationEnabled = current.vibrationEnabled,
            reduceMotion = animationsOff(),
        )
        plan.sound?.let(::playSound)
        plan.haptic?.let(::vibrate)
    }

    private fun playSound(sound: Sound) {
        val id = soundIds[sound] ?: return
        if (id in loaded) soundPool.play(id, 1f, 1f, 1, 0, 1f)
    }

    private fun vibrate(haptic: Haptic) {
        val target = vibrator?.takeIf { it.hasVibrator() } ?: return
        target.vibrate(effect(haptic))
    }

    private fun effect(haptic: Haptic): VibrationEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) predefinedEffect(haptic)?.let { return it }
        return when (haptic) {
            Haptic.Tick -> VibrationEffect.createOneShot(TICK_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE)
            Haptic.Click -> VibrationEffect.createOneShot(CLICK_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE)
            Haptic.DoubleClick -> VibrationEffect.createWaveform(DoubleClickTimings, -1)
            Haptic.Match -> VibrationEffect.createWaveform(MatchTimings, MatchAmplitudes, -1)
            Haptic.MatchLong -> VibrationEffect.createWaveform(MatchLongTimings, MatchLongAmplitudes, -1)
        }
    }

    /** Efeitos do próprio aparelho (mais finos que um pulso fixo); só existem a partir do Android 10. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun predefinedEffect(haptic: Haptic): VibrationEffect? = when (haptic) {
        Haptic.Tick -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        Haptic.Click -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        Haptic.DoubleClick -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
        Haptic.Match, Haptic.MatchLong -> null
    }

    private fun ringerMode(): RingerMode = when (audioManager?.ringerMode) {
        AudioManager.RINGER_MODE_VIBRATE -> RingerMode.Vibrate
        AudioManager.RINGER_MODE_SILENT -> RingerMode.Silent
        else -> RingerMode.Normal
    }

    /** "Remover animações" do sistema (escala 0): quem pediu menos movimento também quer menos vibração forte. */
    private fun animationsOff(): Boolean = Settings.Global.getFloat(
        appContext.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        DEFAULT_ANIMATOR_SCALE,
    ) == 0f
}

private fun findVibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
