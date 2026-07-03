package com.maisha.game.data

import com.maisha.game.domain.AchievementEngine
import com.maisha.game.domain.FinanceEngine
import com.maisha.game.data.model.Stats
import com.maisha.game.domain.TestFixtures
import com.maisha.game.util.formatMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementWealthTest {

    private val engine = AchievementEngine(FinanceEngine())

    @Test
    fun formatMoney_producesDistinctCurrencyPerCountry() {
        val ke = formatMoney(AchievementWealth.sixFiguresThreshold("KE"), "KE")
        val ng = formatMoney(AchievementWealth.sixFiguresThreshold("NG"), "NG")
        val us = formatMoney(AchievementWealth.sixFiguresThreshold("US"), "US")

        assertNotEquals(ke, ng)
        assertNotEquals(ke, us)
        assertTrue(ke.startsWith("KSh"))
        assertTrue(ng.startsWith("₦"))
        assertTrue(us.startsWith("$"))
    }

    @Test
    fun sixFigures_thresholdScalesWithEconomyScaler() {
        val keThreshold = AchievementWealth.sixFiguresThreshold("KE")
        val usThreshold = AchievementWealth.sixFiguresThreshold("US")
        assertEquals(100_000, keThreshold)
        assertTrue(usThreshold < keThreshold)
        assertTrue(usThreshold > 0)
    }

    @Test
    fun firstMillion_thresholdScalesWithEconomyScaler() {
        val keThreshold = AchievementWealth.firstMillionThreshold("KE")
        val idThreshold = AchievementWealth.firstMillionThreshold("ID")
        assertEquals(1_000_000, keThreshold)
        assertTrue(idThreshold > keThreshold)
    }

    @Test
    fun sixFigures_unlocksAtScaledThreshold_notFlatKenyaAmount() {
        val usThreshold = AchievementWealth.sixFiguresThreshold("US")
        val below = TestFixtures.character(
            countryCode = "US",
            stats = Stats(money = usThreshold - 1)
        )
        val at = TestFixtures.character(
            countryCode = "US",
            stats = Stats(money = usThreshold)
        )

        assertFalse(engine.checkAchievements(below, emptyList()).any { it.id == "six_figures" })
        assertTrue(engine.checkAchievements(at, emptyList()).any { it.id == "six_figures" })
    }

    @Test
    fun sixFigures_doesNotUnlockAtFlatKenyaThresholdForLowMultiplierCountry() {
        val usThreshold = AchievementWealth.sixFiguresThreshold("US")
        val character = TestFixtures.character(
            countryCode = "US",
            stats = Stats(money = usThreshold + 5_000)
        )
        assertTrue(character.stats.money < 100_000)
        assertTrue(engine.checkAchievements(character, emptyList()).any { it.id == "six_figures" })
    }
}
