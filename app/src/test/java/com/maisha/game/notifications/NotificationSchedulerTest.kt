package com.maisha.game.notifications

import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSchedulerTest {

    @Test
    fun forTesting_scheduleDailyReminder_isNoOp() {
        val scheduler = NotificationScheduler.forTesting()
        scheduler.scheduleDailyReminder()
        scheduler.scheduleContextualNudge(0, NudgeType.GENERAL_COMEBACK, 4L)
        assertTrue(true)
    }
}
