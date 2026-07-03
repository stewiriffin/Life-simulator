package com.maisha.game.domain

import com.maisha.game.data.model.Business
import com.maisha.game.data.model.BusinessIndustry
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessEngineTest {

    private val financeEngine = FinanceEngine()
    private val engine = BusinessEngine(financeEngine)

    @Test
    fun startBusiness_deductsCashAndCreatesBusinessEntity() {
        val investment = engine.minInvestment(
            TestFixtures.character(stats = Stats(money = 1_000_000))
        )
        val character = TestFixtures.character(
            age = 30,
            stats = Stats(money = 1_000_000, smarts = 70)
        )
        when (val result = engine.startBusiness(
            character,
            name = "Acme Labs",
            industry = BusinessIndustry.TECH,
            initialInvestment = investment
        )) {
            is BusinessResult.Success -> {
                assertEquals(1, result.character.businesses.size)
                val business = result.character.businesses.first()
                assertEquals("Acme Labs", business.name)
                assertEquals(BusinessIndustry.TECH, business.industry)
                assertTrue(business.valuation > 0)
                assertTrue(business.revenue > 0)
                assertTrue(business.employeeCount >= 1)
                assertEquals(1_000_000 - investment, result.character.stats.money)
            }
            is BusinessResult.Failed -> error("Expected start success: ${result.reason}")
        }
    }

    @Test
    fun processBusinessYear_calculatesProfitAndAddsToCharacterCash() {
        val business = Business(
            id = "b1",
            name = "Corner Shop",
            industry = BusinessIndustry.RETAIL,
            valuation = 200_000,
            revenue = 100_000,
            employeeCount = 3,
            lastYearProfit = 0
        )
        val character = TestFixtures.character(
            age = 35,
            stats = Stats(money = 50_000),
            businesses = listOf(business)
        )
        val after = engine.processBusinessYear(character)
        assertEquals(1, after.businesses.size)
        val updated = after.businesses.first()
        val expectedMoney = (50_000 + updated.lastYearProfit).coerceAtLeast(0)
        assertEquals(expectedMoney, after.stats.money)
        assertTrue(updated.valuation >= 1_000)
        assertTrue(updated.lastYearProfit != 0 || after.stats.money != 50_000)
    }

    @Test
    fun sellBusiness_removesBusinessAndAddsValuationToCash() {
        val business = Business(
            id = "b1",
            name = "Sold Co",
            industry = BusinessIndustry.FOOD,
            valuation = 250_000,
            revenue = 40_000,
            employeeCount = 5
        )
        val character = TestFixtures.character(
            age = 40,
            stats = Stats(money = 10_000),
            businesses = listOf(business)
        )
        when (val result = engine.sellBusiness(character, "b1")) {
            is BusinessResult.Success -> {
                assertTrue(result.character.businesses.isEmpty())
                assertEquals(260_000, result.character.stats.money)
            }
            is BusinessResult.Failed -> error("Expected sell success: ${result.reason}")
        }
    }
}
