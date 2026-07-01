// app/src/main/java/com/maisha/game/feedback/FeedbackManager.kt (new)
package com.maisha.game.feedback

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.media.SoundPool
import com.maisha.game.data.local.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsRepository: SettingsRepository
) {
    private var soundPool: SoundPool? = null
    private val loadedSoundIds = mutableMapOf<SoundEffect, Int>()

    fun preloadSounds(context: Context) {
        try {
            val pool = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
            soundPool = pool
            SoundEffect.entries.forEach { effect ->
                runCatching {
                    val soundId = pool.load(context.applicationContext, effect.rawRes, 1)
                    if (soundId != 0) {
                        loadedSoundIds[effect] = soundId
                    }
                }.onFailure { error ->
                    Log.w(TAG, "Failed to load sound ${effect.name}", error)
                }
            }
        } catch (error: Exception) {
            Log.w(TAG, "SoundPool preload failed", error)
        }
    }

    fun playSound(context: Context, sound: SoundEffect) {
        try {
            val enabled = runBlocking(Dispatchers.IO) { settingsRepository.isSoundEnabledNow() }
            if (!enabled) return
            val pool = soundPool ?: return
            val soundId = loadedSoundIds[sound] ?: return
            pool.play(soundId, 1f, 1f, 1, 0, 1f)
        } catch (error: Exception) {
            Log.w(TAG, "playSound failed for ${sound.name}", error)
        }
    }

    fun triggerHaptic(view: View, type: HapticType) {
        try {
            val enabled = runBlocking(Dispatchers.IO) { settingsRepository.isHapticsEnabledNow() }
            if (!enabled) return
            when (type) {
                HapticType.LIGHT_TAP -> {
                    if (!view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)) {
                        vibrateOneShot(view, durationMs = 12, amplitude = 40)
                    }
                }
                HapticType.SUCCESS -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (!view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)) {
                            vibrateOneShot(view, durationMs = 24, amplitude = 80)
                        }
                    } else {
                        vibrateOneShot(view, durationMs = 24, amplitude = 80)
                    }
                }
                HapticType.WARNING -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (!view.performHapticFeedback(HapticFeedbackConstants.REJECT)) {
                            vibrateOneShot(view, durationMs = 36, amplitude = 120)
                        }
                    } else {
                        vibrateOneShot(view, durationMs = 36, amplitude = 120)
                    }
                }
                HapticType.ERROR -> {
                    vibrateOneShot(view, durationMs = 48, amplitude = 180)
                }
            }
        } catch (error: Exception) {
            Log.w(TAG, "triggerHaptic failed for ${type.name}", error)
        }
    }

    private fun vibrateOneShot(view: View, durationMs: Long, amplitude: Int) {
        val vibrator = resolveVibrator(view.context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    durationMs,
                    amplitude.coerceIn(1, 255)
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun resolveVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (error: Exception) {
            Log.w(TAG, "resolveVibrator failed", error)
            null
        }
    }

    companion object {
        private const val TAG = "FeedbackManager"
    }
}
