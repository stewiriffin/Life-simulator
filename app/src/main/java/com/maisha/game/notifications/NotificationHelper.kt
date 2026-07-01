// app/src/main/java/com/maisha/game/notifications/NotificationHelper.kt (new)
package com.maisha.game.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.maisha.game.MainActivity
import com.maisha.game.R

object NotificationHelper {

    const val CHANNEL_ID = "life_reminders"
    private const val NOTIFICATION_ID_DAILY = 1001
    private const val NOTIFICATION_ID_NUDGE_BASE = 2000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    fun buildAndShowNotification(
        context: Context,
        title: String,
        body: String,
        deepLinkSlotId: Int?
    ) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (deepLinkSlotId != null) {
                putExtra(MainActivity.EXTRA_DEEP_LINK_SLOT_ID, deepLinkSlotId)
            }
        }
        val requestCode = deepLinkSlotId ?: 0
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = deepLinkSlotId?.let { NOTIFICATION_ID_NUDGE_BASE + it }
            ?: NOTIFICATION_ID_DAILY

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
