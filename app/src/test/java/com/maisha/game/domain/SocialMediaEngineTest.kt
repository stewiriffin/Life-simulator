package com.maisha.game.domain

import com.maisha.game.data.model.SocialMediaState
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialMediaEngineTest {

    private val financeEngine = FinanceEngine()
    private val engine = SocialMediaEngine(financeEngine)

    @Test
    fun postContent_increasesFollowersBasedOnCharacterStats() {
        val lowStats = accountHolder(looks = 20, smarts = 20)
        val highStats = accountHolder(looks = 90, smarts = 90)

        var lowGrowth = 0
        var highGrowth = 0
        repeat(80) {
            when (val low = engine.postContent(lowStats)) {
                is SocialMediaResult.Success -> lowGrowth += low.followersGained
                is SocialMediaResult.Failed -> error("Expected post success for low stats")
            }
            when (val high = engine.postContent(highStats)) {
                is SocialMediaResult.Success -> highGrowth += high.followersGained
                is SocialMediaResult.Failed -> error("Expected post success for high stats")
            }
        }

        assertTrue(highGrowth > lowGrowth)
        assertTrue(lowGrowth > 0)
    }

    @Test
    fun monetizeAccount_addsCashToNetWorthIfThresholdMet() {
        val character = accountHolder(looks = 50, smarts = 50).copy(
            stats = Stats(money = 10_000, looks = 50, smarts = 50),
            socialMedia = SocialMediaState(
                hasAccount = true,
                followers = SocialMediaEngine.MONETIZATION_FOLLOWER_THRESHOLD,
                isVerified = true
            )
        )
        when (val result = engine.monetizeAccount(character)) {
            is SocialMediaResult.Success -> {
                assertTrue(result.payout > 0)
                assertEquals(10_000 + result.payout, result.character.stats.money)
                assertEquals(
                    10_000 + result.payout,
                    financeEngine.calculateNetWorth(result.character)
                )
                assertTrue(result.character.socialMedia.monetizedThisYear)
            }
            is SocialMediaResult.Failed -> error("Expected monetization success: ${result.reason}")
        }

        val belowThreshold = character.copy(
            socialMedia = character.socialMedia.copy(
                followers = SocialMediaEngine.MONETIZATION_FOLLOWER_THRESHOLD - 1,
                monetizedThisYear = false
            )
        )
        val failed = engine.monetizeAccount(belowThreshold)
        assertTrue(failed is SocialMediaResult.Failed)
        assertEquals(
            SocialMediaFailure.BELOW_MONETIZATION_THRESHOLD,
            (failed as SocialMediaResult.Failed).reason
        )
    }

    private fun accountHolder(looks: Int, smarts: Int) = TestFixtures.character(
        age = 20,
        stats = Stats(looks = looks, smarts = smarts, money = 0),
        socialMedia = SocialMediaState(hasAccount = true, followers = 100)
    )
}
