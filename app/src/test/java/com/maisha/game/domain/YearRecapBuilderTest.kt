package com.maisha.game.domain

import com.maisha.game.data.model.HealthCondition
import com.maisha.game.data.model.Stats
import com.maisha.game.data.model.VisaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YearRecapBuilderTest {

    @Test
    fun build_includesCashSavingsPortfolioAndPromotion() {
        val before = TestFixtures.character(
            age = 30,
            stats = Stats(money = 40_000, health = 70, happiness = 60, smarts = 50, looks = 50)
        ).copy(
            savingsBalance = 10_000,
            investmentPortfolioValue = 20_000
        )
        val after = before.copy(
            stats = before.stats.copy(money = 55_000),
            savingsBalance = 11_200,
            investmentPortfolioValue = 22_500
        )
        val facts = YearRecapBuilder.build(
            before = before,
            after = after,
            promoted = true,
            questsCompleted = 2,
            questStreak = 3
        )
        assertTrue(facts.any { it.type == YearRecapBuilder.FactType.CASH_UP && it.value == 15_000 })
        assertTrue(facts.any { it.type == YearRecapBuilder.FactType.SAVINGS_INTEREST && it.value == 1_200 })
        assertTrue(facts.any { it.type == YearRecapBuilder.FactType.PORTFOLIO_UP && it.value == 2_500 })
        assertTrue(facts.any { it.type == YearRecapBuilder.FactType.PROMOTION })
        assertTrue(facts.size <= 4)
    }

    @Test
    fun build_includesVisaAndCultureShockWhenAbroad() {
        val before = TestFixtures.character(
            age = 28,
            countryCode = "US",
            stats = Stats(money = 20_000, health = 70, happiness = 55, smarts = 50, looks = 50)
        ).copy(
            birthCountryCode = "KE",
            citizenships = listOf("KE"),
            currentVisa = VisaType.WORK,
            visaYearsRemaining = 3
        )
        val after = before.copy(
            visaYearsRemaining = 2,
            stats = before.stats.copy(happiness = 48, money = 18_000)
        )
        val facts = YearRecapBuilder.build(before, after)
        assertTrue(facts.any { it.type == YearRecapBuilder.FactType.VISA_TICK && it.value == 2 })
        assertTrue(facts.any { it.type == YearRecapBuilder.FactType.CULTURE_SHOCK })
        assertTrue(facts.any { it.type == YearRecapBuilder.FactType.CASH_DOWN })
    }

    @Test
    fun build_includesIllnessWhenNewConditionAppears() {
        val before = TestFixtures.character(age = 40, stats = Stats(health = 80, money = 10_000))
        val after = before.copy(
            stats = before.stats.copy(health = 70),
            activeConditions = listOf(
                HealthCondition(id = "flu", name = "Flu", severity = 1)
            )
        )
        val facts = YearRecapBuilder.build(before, after)
        assertTrue(facts.any { it.type == YearRecapBuilder.FactType.ILLNESS })
    }

    @Test
    fun evaluate_midYearMoneyGainUpdatesProgressWithoutAgeUp() {
        val engine = YearQuestEngine()
        val snapshot = TestFixtures.character(
            age = 22,
            stats = Stats(money = 5_000, happiness = 50, health = 70, smarts = 50, looks = 50)
        )
        val quest = YearQuest(
            kind = YearQuestKind.EARN_MONEY,
            target = 10_000,
            titleResHint = "year_quest_earn_money"
        )
        val midYear = snapshot.copy(stats = snapshot.stats.copy(money = 16_000))
        val progress = engine.evaluate(listOf(quest), snapshot, midYear).single()
        assertTrue(progress.completed)
        assertEquals(11_000, progress.current)
    }
}
