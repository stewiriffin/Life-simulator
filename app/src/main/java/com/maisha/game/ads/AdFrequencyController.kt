// app/src/main/java/com/maisha/game/ads/AdFrequencyController.kt
package com.maisha.game.ads

import com.maisha.game.data.local.MetaBonusRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdFrequencyController @Inject constructor(
    private val metaBonusRepository: MetaBonusRepository
) {

    suspend fun recordAgeUpAndShouldShowInterstitial(): Boolean {
        val count = metaBonusRepository.incrementAgeUpCount()
        return shouldShowInterstitial(count)
    }

    fun shouldShowInterstitial(ageUpCount: Int): Boolean {
        return ageUpCount > 0 && ageUpCount % INTERSTITIAL_EVERY_N_AGE_UPS == 0
    }

    companion object {
        const val INTERSTITIAL_EVERY_N_AGE_UPS = 5
    }
}
