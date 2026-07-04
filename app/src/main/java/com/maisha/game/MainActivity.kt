// app/src/main/java/com/maisha/game/MainActivity.kt
package com.maisha.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.tracing.Trace
import com.maisha.game.ads.AdManager
import com.maisha.game.data.events.EventRepository
import com.maisha.game.data.local.CharacterRepository
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var feedbackManager: FeedbackManager

    @Inject
    lateinit var eventRepository: EventRepository

    @Inject
    lateinit var characterRepository: CharacterRepository

    private var deepLinkSlotId by mutableIntStateOf(-1)
    private val keepSplashOnScreen = AtomicBoolean(true)
    private val coldStartTraceActive = AtomicBoolean(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        Trace.beginSection(COLD_START_TRACE)
        MaishaSplash.install(this, keepSplashOnScreen)
        deepLinkSlotId = intent?.getIntExtra(EXTRA_DEEP_LINK_SLOT_ID, -1) ?: -1
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        var appReady by mutableStateOf(false)
        var startDestination by mutableStateOf(Routes.SLOT_PICKER)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                LocaleManager.applyLocale(settingsRepository.getLanguageSnapshot())
            }

            val eventsLoad = async(Dispatchers.IO) { eventRepository.ensureLoaded() }
            val onboardingComplete = withContext(Dispatchers.IO) {
                settingsRepository.hasCompletedOnboardingSnapshot()
            }

            // Hold splash until the 3 save slots are readable (avoids empty Slot Picker flash).
            if (onboardingComplete) {
                withContext(Dispatchers.IO) {
                    characterRepository.getAllSlots().first()
                }
            }

            eventsLoad.await()
            startDestination = if (onboardingComplete) {
                Routes.SLOT_PICKER
            } else {
                Routes.ONBOARDING
            }
            keepSplashOnScreen.set(false)
            appReady = true
        }

        // AdMob load APIs must run on the main thread (#008).
        lifecycleScope.launch {
            adManager.preloadInterstitial(applicationContext)
            adManager.preloadRewarded(applicationContext)
        }

        setContent {
            if (!appReady) return@setContent
            MaishaTheme {
                CompositionLocalProvider(LocalFeedbackManager provides feedbackManager) {
                    MaishaApp(
                        adManager = adManager,
                        startDestination = startDestination,
                        deepLinkSlotId = deepLinkSlotId.takeIf { it >= 0 }
                    )
                    SideEffect {
                        if (coldStartTraceActive.compareAndSet(true, false)) {
                            Trace.endSection()
                        }
                    }
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
        private const val COLD_START_TRACE = "MaishaColdStart"
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
