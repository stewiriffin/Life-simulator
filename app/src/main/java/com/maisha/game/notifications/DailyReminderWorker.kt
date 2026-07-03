// app/src/main/java/com/maisha/game/notifications/DailyReminderWorker.kt (new)
package com.maisha.game.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maisha.game.R
import com.maisha.game.data.local.CharacterRepository
import com.maisha.game.data.local.SavedGameLoadResult
import com.maisha.game.data.local.MAX_SLOTS
import com.maisha.game.data.local.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlin.random.Random

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val characterRepository: CharacterRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!settingsRepository.isNotificationsEnabledNow()) {
                return Result.success()
            }
            if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
                return Result.success()
            }

            val lastOpened = settingsRepository.lastOpenedTimestamp.first()
            val hoursSinceOpen = (System.currentTimeMillis() - lastOpened) / MILLIS_PER_HOUR
            if (hoursSinceOpen < MIN_HOURS_SINCE_OPEN) {
                return Result.success()
            }

            val livingCharacters = loadLivingCharacterNames()
            val message = pickDailyMessage(livingCharacters)

            NotificationHelper.buildAndShowNotification(
                context = applicationContext,
                title = applicationContext.getString(R.string.notification_daily_title),
                body = message,
                deepLinkSlotId = livingCharacters.randomOrNull()?.first
            )
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    private suspend fun loadLivingCharacterNames(): List<Pair<Int, String>> {
        val results = mutableListOf<Pair<Int, String>>()
        for (slotId in 0 until MAX_SLOTS) {
            val game = when (val result = characterRepository.loadGame(slotId)) {
                is SavedGameLoadResult.Success -> result.game
                else -> continue
            }
            val character = game.character
            if (character.alive) {
                results += slotId to character.name
            }
        }
        return results
    }

    private fun pickDailyMessage(livingCharacters: List<Pair<Int, String>>): String {
        val res = applicationContext.resources
        val variantIndex = Random.nextInt(DAILY_BODY_VARIANT_COUNT)
        val name = livingCharacters.randomOrNull()?.second
        return if (name != null) {
            val nameVariants = arrayOf(
                R.string.notification_daily_body_named_1,
                R.string.notification_daily_body_named_2,
                R.string.notification_daily_body_named_3
            )
            res.getString(nameVariants[variantIndex % nameVariants.size], name)
        } else {
            val genericVariants = arrayOf(
                R.string.notification_daily_body_1,
                R.string.notification_daily_body_2,
                R.string.notification_daily_body_3,
                R.string.notification_daily_body_4,
                R.string.notification_daily_body_5,
                R.string.notification_daily_body_6
            )
            res.getString(genericVariants[variantIndex % genericVariants.size])
        }
    }

    companion object {
        private const val MIN_HOURS_SINCE_OPEN = 20L
        private const val DAILY_BODY_VARIANT_COUNT = 6
        private const val MILLIS_PER_HOUR = 3_600_000L
    }
}
