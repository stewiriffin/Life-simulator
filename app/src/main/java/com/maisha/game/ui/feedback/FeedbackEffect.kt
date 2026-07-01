// app/src/main/java/com/maisha/game/ui/feedback/FeedbackEffect.kt (new)
package com.maisha.game.ui.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.maisha.game.feedback.FeedbackCue
import com.maisha.game.feedback.HapticType

@Composable
fun FeedbackEffect(
    cues: List<FeedbackCue>,
    onHandled: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val feedbackManager = LocalFeedbackManager.current

    LaunchedEffect(cues) {
        if (cues.isEmpty()) return@LaunchedEffect
        cues.forEach { cue ->
            cue.sound?.let { feedbackManager.playSound(context, it) }
            cue.haptic?.let { feedbackManager.triggerHaptic(view, it) }
        }
        onHandled()
    }
}

@Composable
fun rememberHapticClickHandler(onClick: () -> Unit): () -> Unit {
    val view = LocalView.current
    val feedbackManager = LocalFeedbackManager.current
    return {
        feedbackManager.triggerHaptic(view, HapticType.LIGHT_TAP)
        onClick()
    }
}
