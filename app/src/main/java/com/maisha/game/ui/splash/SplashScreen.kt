// app/src/main/java/com/maisha/game/ui/splash/SplashScreen.kt (new)
package com.maisha.game.ui.splash

import android.app.Activity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android 12+ SplashScreen API (androidx.core.splashscreen compat).
 * Keeps the branded splash visible via [keepOnScreen] while async startup work runs
 * (locale apply, onboarding flag read) — no artificial delay.
 */
object MaishaSplash {

    fun install(activity: Activity, keepOnScreen: AtomicBoolean): SplashScreen {
        return activity.installSplashScreen().apply {
            setKeepOnScreenCondition { keepOnScreen.get() }
        }
    }
}
