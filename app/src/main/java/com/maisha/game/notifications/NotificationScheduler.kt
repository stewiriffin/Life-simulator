// app/src/main/java/com/maisha/game/notifications/NotificationScheduler.kt (new)
package com.maisha.game.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleDailyReminder() {
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .addTag(DAILY_REMINDER_TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            DAILY_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelDailyReminder() {
        workManager.cancelUniqueWork(DAILY_REMINDER_WORK)
    }

    fun scheduleContextualNudge(slotId: Int, nudgeType: NudgeType, delayHours: Long) {
        val data = Data.Builder()
            .putInt(ContextualNudgeWorker.KEY_SLOT_ID, slotId)
            .putString(ContextualNudgeWorker.KEY_NUDGE_TYPE, nudgeType.name)
            .build()
        val request = OneTimeWorkRequestBuilder<ContextualNudgeWorker>()
            .setInitialDelay(delayHours.coerceAtLeast(1L), TimeUnit.HOURS)
            .setInputData(data)
            .addTag(CONTEXTUAL_NUDGE_TAG)
            .build()
        val uniqueName = "nudge_${nudgeType.name}_$slotId"
        workManager.enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelAllContextualNudges() {
        workManager.cancelAllWorkByTag(CONTEXTUAL_NUDGE_TAG)
    }

    fun cancelAll() {
        cancelDailyReminder()
        cancelAllContextualNudges()
    }

    companion object {
        private const val DAILY_REMINDER_WORK = "daily_life_reminder"
        private const val DAILY_REMINDER_TAG = "daily_reminder"
        const val CONTEXTUAL_NUDGE_TAG = "contextual_nudge"
    }
}
