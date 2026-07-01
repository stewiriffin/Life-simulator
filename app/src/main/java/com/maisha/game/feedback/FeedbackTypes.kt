// app/src/main/java/com/maisha/game/feedback/FeedbackTypes.kt (new)
package com.maisha.game.feedback

import androidx.annotation.RawRes
import com.maisha.game.R

enum class SoundEffect(@RawRes val rawRes: Int) {
    AGE_UP(R.raw.age_up),
    ACHIEVEMENT_UNLOCK(R.raw.achievement_unlock),
    EVENT_POSITIVE(R.raw.event_positive),
    EVENT_NEGATIVE(R.raw.event_negative),
    BUTTON_TAP(R.raw.button_tap),
    DEATH(R.raw.death),
    PURCHASE(R.raw.purchase),
    MONEY_GAIN(R.raw.money_gain)
}

enum class HapticType {
    LIGHT_TAP,
    SUCCESS,
    WARNING,
    ERROR
}

data class FeedbackCue(
    val sound: SoundEffect? = null,
    val haptic: HapticType? = null
)
