// app/src/main/java/com/maisha/game/notifications/NotificationScheduler.kt (new)
package com.maisha.game.notifications

import android.content.Context
import androidx.work.Constraints
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

/**
 * Schedules local notification workers via WorkManager.
 *
 * Daily reminder uses [ExistingPeriodicWorkPolicy.KEEP] because [scheduleDailyReminder] may be
 * called on every cold start when notifications are enabled — KEEP preserves the existing
 * periodic window instead of resetting it (UPDATE would shift timing every launch).
 *
 * No network/charging constraints: workers only read local Room/DataStore and post a notification.
 * A 6-hour flex window lets the OS batch work under Doze/OEM battery management.
 *
 * See docs/KNOWN_PLATFORM_LIMITATIONS.md — we do NOT request battery-optimization exemption.
 */
@Singleton
class NotificationScheduler private constructor(
    private val assetContext: Context?,
    private val noop: Boolean
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, false)

    private val workManager by lazy { WorkManager.getInstance(requireNotNull(assetContext)) }

    fun scheduleDailyReminder() {
        if (noop) return
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            REPEAT_INTERVAL_HOURS,
            TimeUnit.HOURS,
            FLEX_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(Constraints.Builder().build())
            .addTag(DAILY_REMINDER_TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            DAILY_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelDailyReminder() {
        if (noop) return
        workManager.cancelUniqueWork(DAILY_REMINDER_WORK)
    }

    fun scheduleContextualNudge(slotId: Int, nudgeType: NudgeType, delayHours: Long) {
        if (noop) return
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
        if (noop) return
        workManager.cancelAllWorkByTag(CONTEXTUAL_NUDGE_TAG)
    }

    fun cancelAll() {
        cancelDailyReminder()
        cancelAllContextualNudges()
    }

    companion object {
        fun forTesting(): NotificationScheduler = NotificationScheduler(null, true)

        private const val DAILY_REMINDER_WORK = "daily_life_reminder"
        private const val DAILY_REMINDER_TAG = "daily_reminder"
        private const val REPEAT_INTERVAL_HOURS = 24L
        private const val FLEX_INTERVAL_HOURS = 6L
        const val CONTEXTUAL_NUDGE_TAG = "contextual_nudge"
    }
}
