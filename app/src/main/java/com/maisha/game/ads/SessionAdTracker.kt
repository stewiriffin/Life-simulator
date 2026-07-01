// app/src/main/java/com/maisha/game/ads/SessionAdTracker.kt
package com.maisha.game.ads

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionAdTracker @Inject constructor() {

    private var secondWindOfferUsedThisSession = false

    fun canShowSecondWindOffer(): Boolean = !secondWindOfferUsedThisSession

    fun markSecondWindOfferUsed() {
        secondWindOfferUsedThisSession = true
    }
}
