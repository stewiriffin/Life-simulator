// app/src/main/java/com/maisha/game/notifications/ContextualNudgeWorker.kt (new)
package com.maisha.game.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maisha.game.R
import com.maisha.game.data.local.CharacterRepository
import com.maisha.game.data.local.SavedGameLoadResult
import com.maisha.game.data.local.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ContextualNudgeWorker @AssistedInject constructor(
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

            val slotId = inputData.getInt(KEY_SLOT_ID, -1)
            if (slotId < 0) return Result.success()

            val nudgeType = inputData.getString(KEY_NUDGE_TYPE)
                ?.let { runCatching { NudgeType.valueOf(it) }.getOrNull() }
                ?: return Result.success()

            val game = when (val result = characterRepository.loadGame(slotId)) {
                is SavedGameLoadResult.Success -> result.game
                else -> return Result.success()
            }
            val character = game.character
            if (!character.alive) return Result.success()

            val (title, body) = when (nudgeType) {
                NudgeType.UNTREATED_CONDITION -> {
                    val condition = character.activeConditions.firstOrNull { !it.treated }
                    if (condition == null || condition.yearsUntreated < 2) {
                        return Result.success()
                    }
                    applicationContext.getString(R.string.notification_nudge_health_title) to
                        applicationContext.getString(
                            R.string.notification_nudge_health_body,
                            character.name,
                            condition.name
                        )
                }
                NudgeType.PENDING_LIFE_DECISION -> {
                    applicationContext.getString(R.string.notification_nudge_decision_title) to
                        applicationContext.getString(
                            R.string.notification_nudge_decision_body,
                            character.name
                        )
                }
                NudgeType.GENERAL_COMEBACK -> {
                    applicationContext.getString(R.string.notification_nudge_comeback_title) to
                        applicationContext.getString(
                            R.string.notification_nudge_comeback_body,
                            character.name
                        )
                }
            }

            NotificationHelper.buildAndShowNotification(
                context = applicationContext,
                title = title,
                body = body,
                deepLinkSlotId = slotId
            )
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_SLOT_ID = "slot_id"
        const val KEY_NUDGE_TYPE = "nudge_type"
    }
}
