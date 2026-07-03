// app/src/main/java/com/maisha/game/domain/SocialMediaEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.model.Character
import com.maisha.game.data.model.SocialMediaState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.random.Random

sealed class SocialMediaResult {
    data class Success(
        val character: Character,
        val followersGained: Int = 0,
        val payout: Int = 0,
        val wentViral: Boolean = false
    ) : SocialMediaResult()

    data class Failed(val reason: SocialMediaFailure) : SocialMediaResult()
}

enum class SocialMediaFailure {
    ALREADY_HAS_ACCOUNT,
    NO_ACCOUNT,
    INELIGIBLE,
    BELOW_MONETIZATION_THRESHOLD,
    ALREADY_MONETIZED_THIS_YEAR
}

@Singleton
class SocialMediaEngine @Inject constructor(
    private val financeEngine: FinanceEngine
) {

    fun createAccount(character: Character): SocialMediaResult {
        if (!character.alive ||
            character.age < MIN_ACCOUNT_AGE ||
            character.criminalRecord.currentlyIncarcerated ||
            character.criminalRecord.awaitingTrial
        ) {
            return SocialMediaResult.Failed(SocialMediaFailure.INELIGIBLE)
        }
        if (character.socialMedia.hasAccount) {
            return SocialMediaResult.Failed(SocialMediaFailure.ALREADY_HAS_ACCOUNT)
        }
        val updated = character.copy(
            socialMedia = SocialMediaState(hasAccount = true),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "Created a social media account."
            )
        )
        return SocialMediaResult.Success(updated)
    }

    fun deleteAccount(character: Character): SocialMediaResult {
        if (!character.socialMedia.hasAccount) {
            return SocialMediaResult.Failed(SocialMediaFailure.NO_ACCOUNT)
        }
        val updated = character.copy(
            socialMedia = SocialMediaState(),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "Deleted your social media account."
            )
        )
        return SocialMediaResult.Success(updated)
    }

    /**
     * Posts content and grows followers from looks/smarts, with a small viral chance.
     */
    fun postContent(character: Character): SocialMediaResult {
        if (!canUseSocialMedia(character)) {
            return SocialMediaResult.Failed(SocialMediaFailure.INELIGIBLE)
        }
        if (!character.socialMedia.hasAccount) {
            return SocialMediaResult.Failed(SocialMediaFailure.NO_ACCOUNT)
        }

        val wentViral = Random.nextFloat() < VIRAL_CHANCE
        val growth = calculateFollowerGrowth(character, wentViral)
        val newFollowers = (character.socialMedia.followers + growth).coerceAtLeast(0)
        val isVerified = character.socialMedia.isVerified || newFollowers >= VERIFIED_FOLLOWER_THRESHOLD
        val logLine = if (wentViral) {
            "A post went viral! Gained $growth followers."
        } else {
            "Posted an update. Gained $growth followers."
        }
        val updated = character.copy(
            socialMedia = character.socialMedia.copy(
                followers = newFollowers,
                isVerified = isVerified
            ),
            eventLog = EventLogCap.prepend(character.eventLog, logLine)
        )
        return SocialMediaResult.Success(
            character = updated,
            followersGained = growth,
            wentViral = wentViral
        )
    }

    /**
     * Yearly ad-revenue payout when followers exceed [MONETIZATION_FOLLOWER_THRESHOLD].
     * Delegates cash application to [FinanceEngine.applySocialMediaRevenue].
     */
    fun monetizeAccount(character: Character): SocialMediaResult {
        if (!canUseSocialMedia(character)) {
            return SocialMediaResult.Failed(SocialMediaFailure.INELIGIBLE)
        }
        if (!character.socialMedia.hasAccount) {
            return SocialMediaResult.Failed(SocialMediaFailure.NO_ACCOUNT)
        }
        if (character.socialMedia.followers < MONETIZATION_FOLLOWER_THRESHOLD) {
            return SocialMediaResult.Failed(SocialMediaFailure.BELOW_MONETIZATION_THRESHOLD)
        }
        if (character.socialMedia.monetizedThisYear) {
            return SocialMediaResult.Failed(SocialMediaFailure.ALREADY_MONETIZED_THIS_YEAR)
        }

        val payout = financeEngine.calculateSocialMediaPayout(character)
        val withCash = financeEngine.applySocialMediaRevenue(character, payout)
        val updated = withCash.copy(
            socialMedia = withCash.socialMedia.copy(monetizedThisYear = true),
            eventLog = EventLogCap.prepend(
                withCash.eventLog,
                "Monetized your account and earned ad revenue."
            )
        )
        return SocialMediaResult.Success(character = updated, payout = payout)
    }

    fun canUseSocialMedia(character: Character): Boolean =
        character.alive &&
            character.age >= MIN_ACCOUNT_AGE &&
            !character.criminalRecord.currentlyIncarcerated &&
            !character.criminalRecord.awaitingTrial

    /** Resets yearly monetization flag on age-up. */
    fun resetYearlyFlags(character: Character): Character {
        if (!character.socialMedia.monetizedThisYear) return character
        return character.copy(
            socialMedia = character.socialMedia.copy(monetizedThisYear = false)
        )
    }

    fun calculateFollowerGrowth(character: Character, wentViral: Boolean = false): Int {
        val looks = character.stats.looks.coerceIn(0, 100)
        val smarts = character.stats.smarts.coerceIn(0, 100)
        val statFactor = (looks * LOOKS_WEIGHT + smarts * SMARTS_WEIGHT) / 100f
        val baseRoll = Random.nextInt(BASE_GROWTH_MIN, BASE_GROWTH_MAX + 1)
        var growth = (baseRoll * (0.35f + statFactor * 0.9f)).roundToInt().coerceAtLeast(1)
        if (wentViral) {
            growth = (growth * Random.nextInt(VIRAL_MULTIPLIER_MIN, VIRAL_MULTIPLIER_MAX + 1))
                .coerceAtMost(MAX_VIRAL_GROWTH)
        }
        return growth
    }

    companion object {
        const val REQUIRES_SOCIAL_MEDIA_TAG = "requires_social_media"
        const val MONETIZATION_FOLLOWER_THRESHOLD = 100_000
        const val VERIFIED_FOLLOWER_THRESHOLD = 50_000
        const val MIN_ACCOUNT_AGE = 13
        const val VIRAL_CHANCE = 0.02f
        private const val LOOKS_WEIGHT = 0.6f
        private const val SMARTS_WEIGHT = 0.4f
        private const val BASE_GROWTH_MIN = 40
        private const val BASE_GROWTH_MAX = 180
        private const val VIRAL_MULTIPLIER_MIN = 20
        private const val VIRAL_MULTIPLIER_MAX = 50
        private const val MAX_VIRAL_GROWTH = 250_000
    }
}
