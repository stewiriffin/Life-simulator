// app/src/main/java/com/maisha/game/feedback/FeedbackManager.kt
package com.maisha.game.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.VisibleForTesting
import com.maisha.game.data.local.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Singleton
class FeedbackManager private constructor(
    private val appContext: Context,
    soundEnabledFlow: Flow<Boolean>,
    hapticsEnabledFlow: Flow<Boolean>
) {
    @Inject
    constructor(
        @ApplicationContext appContext: Context,
        settingsRepository: SettingsRepository
    ) : this(
        appContext,
        settingsRepository.soundEnabled,
        settingsRepository.hapticsEnabled
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var soundEnabled: Boolean = true

    @Volatile
    private var hapticsEnabled: Boolean = true

    private var soundPool: SoundPool? = null
    private val loadedSoundIds = ConcurrentHashMap<SoundEffect, Int>()
    private val pendingLoads = ConcurrentHashMap.newKeySet<SoundEffect>()

    @VisibleForTesting
    internal var lastSoundPlayAttempted: Boolean = false
        private set

    @VisibleForTesting
    internal var lastSoundPlayExecuted: Boolean = false
        private set

    init {
        scope.launch {
            soundEnabledFlow.collect { soundEnabled = it }
        }
        scope.launch {
            hapticsEnabledFlow.collect { hapticsEnabled = it }
        }
        scope.launch(Dispatchers.IO) {
            preloadSoundsInternal()
        }
    }

    /** Consumes a [FeedbackCue] queued by ViewModels; played from Compose via [FeedbackEffect]. */
    fun playCue(context: Context, cue: FeedbackCue, view: View? = null) {
        cue.sound?.let { playSound(context, it) }
        if (view != null) {
            cue.haptic?.let { triggerHaptic(view, it) }
        }
    }

    fun playSound(context: Context, sound: SoundEffect) {
        lastSoundPlayAttempted = true
        lastSoundPlayExecuted = false
        if (!soundEnabled) return
        try {
            val pool = soundPool ?: return
            val soundId = loadedSoundIds[sound] ?: return
            if (sound in pendingLoads) return
            pool.play(soundId, 1f, 1f, 1, 0, 1f)
            lastSoundPlayExecuted = true
        } catch (error: Exception) {
            Log.w(TAG, "playSound failed for ${sound.name}", error)
        }
    }

    fun triggerHaptic(view: View, type: HapticType) {
        if (!hapticsEnabled) return
        try {
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

    /** Releases native audio resources when the process is torn down. */
    fun release() {
        scope.cancel()
        try {
            soundPool?.release()
        } catch (error: Exception) {
            Log.w(TAG, "SoundPool release failed", error)
        } finally {
            soundPool = null
            loadedSoundIds.clear()
            pendingLoads.clear()
        }
    }

    private fun preloadSoundsInternal() {
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
            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status != 0) {
                    Log.w(TAG, "Sound load failed for sampleId=$sampleId status=$status")
                }
                loadedSoundIds.entries.find { it.value == sampleId }?.key?.let { effect ->
                    pendingLoads.remove(effect)
                }
            }
            soundPool = pool
            SoundEffect.entries.forEach { effect ->
                runCatching {
                    pendingLoads.add(effect)
                    val soundId = pool.load(appContext, effect.rawRes, 1)
                    if (soundId != 0) {
                        loadedSoundIds[effect] = soundId
                    } else {
                        pendingLoads.remove(effect)
                    }
                }.onFailure { error ->
                    pendingLoads.remove(effect)
                    Log.w(TAG, "Failed to load sound ${effect.name}", error)
                }
            }
        } catch (error: Exception) {
            Log.w(TAG, "SoundPool preload failed", error)
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

    @VisibleForTesting
    internal fun resetPlayTrackingForTest() {
        lastSoundPlayAttempted = false
        lastSoundPlayExecuted = false
    }

    companion object {
        private const val TAG = "FeedbackManager"

        @VisibleForTesting
        internal fun forTest(
            context: Context,
            soundEnabledFlow: Flow<Boolean>,
            hapticsEnabledFlow: Flow<Boolean> = flowOf(true)
        ): FeedbackManager = FeedbackManager(context, soundEnabledFlow, hapticsEnabledFlow)
    }
}
