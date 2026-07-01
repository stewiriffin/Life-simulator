// app/src/main/java/com/maisha/game/ui/feedback/FeedbackLocals.kt (new)
package com.maisha.game.ui.feedback

import androidx.compose.runtime.compositionLocalOf
import com.maisha.game.feedback.FeedbackManager

val LocalFeedbackManager = compositionLocalOf<FeedbackManager> {
    error("FeedbackManager not provided")
}
