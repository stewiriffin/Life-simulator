// app/src/main/java/com/maisha/game/MaishaApplication.kt (modified — WorkManager + notification channel)
package com.maisha.game

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.gms.ads.MobileAds
import com.maisha.game.data.local.SettingsRepository
import com.maisha.game.feedback.FeedbackManager
import com.maisha.game.notifications.NotificationHelper
import com.maisha.game.notifications.NotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class MaishaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var feedbackManager: FeedbackManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        feedbackManager.preloadSounds(this)
        NotificationHelper.createNotificationChannel(this)

        applicationScope.launch {
            if (settingsRepository.isNotificationsEnabledNow() &&
                NotificationManagerCompat.from(this@MaishaApplication).areNotificationsEnabled()
            ) {
                notificationScheduler.scheduleDailyReminder()
            }
        }
    }
}
