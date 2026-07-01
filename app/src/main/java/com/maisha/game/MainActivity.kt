// app/src/main/java/com/maisha/game/MainActivity.kt (modified — splash screen + onboarding read)
package com.maisha.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.maisha.game.ads.AdManager
import com.maisha.game.data.local.SettingsRepository
import com.maisha.game.feedback.FeedbackManager
import com.maisha.game.ui.feedback.LocalFeedbackManager
import com.maisha.game.ui.navigation.MaishaNavHost
import com.maisha.game.ui.navigation.Routes
import com.maisha.game.ui.splash.MaishaSplash
import com.maisha.game.ui.theme.MaishaTheme
import com.maisha.game.util.LocaleManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var feedbackManager: FeedbackManager

    private var deepLinkSlotId by mutableIntStateOf(-1)
    private val keepSplashOnScreen = AtomicBoolean(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        MaishaSplash.install(this, keepSplashOnScreen)
        deepLinkSlotId = intent?.getIntExtra(EXTRA_DEEP_LINK_SLOT_ID, -1) ?: -1
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        runBlocking {
            LocaleManager.applyLocale(settingsRepository.getLanguageSnapshot())
        }

        var appReady by mutableStateOf(false)
        var startDestination by mutableStateOf(Routes.SLOT_PICKER)

        lifecycleScope.launch {
            val onboardingComplete = settingsRepository.hasCompletedOnboardingSnapshot()
            startDestination = if (onboardingComplete) {
                Routes.SLOT_PICKER
            } else {
                Routes.ONBOARDING
            }
            keepSplashOnScreen.set(false)
            appReady = true
        }

        adManager.preloadInterstitial(applicationContext)
        adManager.preloadRewarded(applicationContext)

        setContent {
            if (!appReady) return@setContent
            MaishaTheme {
                CompositionLocalProvider(LocalFeedbackManager provides feedbackManager) {
                    MaishaApp(
                        adManager = adManager,
                        startDestination = startDestination,
                        deepLinkSlotId = deepLinkSlotId.takeIf { it >= 0 }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkSlotId = intent.getIntExtra(EXTRA_DEEP_LINK_SLOT_ID, -1)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            settingsRepository.updateLastOpenedTimestamp()
        }
    }

    companion object {
        const val EXTRA_DEEP_LINK_SLOT_ID = "deep_link_slot_id"
    }
}

@Composable
private fun MaishaApp(
    adManager: AdManager,
    startDestination: String,
    deepLinkSlotId: Int?
) {
    val navController = rememberNavController()
    MaishaNavHost(
        navController = navController,
        startDestination = startDestination,
        adManager = adManager,
        deepLinkSlotId = deepLinkSlotId
    )
}
