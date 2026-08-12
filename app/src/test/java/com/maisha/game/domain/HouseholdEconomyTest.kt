package com.maisha.game.domain

import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.LivingStandard
import com.maisha.game.data.model.Stats
import com.maisha.game.data.model.WorkEffort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HouseholdEconomyTest {

    private val finance = FinanceEngine()
    private val career = CareerEngine(HealthEngine(), RelocationEngine())

    @Test
    fun incomeTax_isZeroBelowFirstBracket() {
        assertEquals(0, FinanceEngine.calculateIncomeTax(40_000, "KE"))
    }

    @Test
    fun incomeTax_risesWithHigherGross() {
        val low = FinanceEngine.calculateIncomeTax(120_000, "KE")
        val high = FinanceEngine.calculateIncomeTax(600_000, "KE")
        assertTrue(low > 0)
        assertTrue(high > low)
    }

    @Test
    fun savings_depositAndInterestGrowBalance() {
        var character = TestFixtures.character(stats = Stats(money = 100_000))
        val deposited = finance.depositSavings(character, 50_000)
        assertTrue(deposited is FinanceEngine.InvestmentResult.Success)
        character = (deposited as FinanceEngine.InvestmentResult.Success).character
        assertEquals(50_000, character.stats.money)
        assertEquals(50_000, character.savingsBalance)

        character = finance.applySavingsInterest(character)
        assertTrue(character.savingsBalance > 50_000)
        assertTrue(character.lastSavingsInterestPercent > 0)
    }

    @Test
    fun costOfLiving_luxuryCostsMoreThanFrugal() {
        val base = TestFixtures.character(age = 30, stats = Stats(money = 1_000_000))
        val frugal = finance.setLivingStandard(base, LivingStandard.FRUGAL)
        val luxury = finance.setLivingStandard(base, LivingStandard.LUXURY)
        assertTrue(
            finance.estimateAnnualCostOfLiving(luxury) >
                finance.estimateAnnualCostOfLiving(frugal)
        )
    }

    @Test
    fun costOfLiving_homeOwnershipReducesBills() {
        val renter = TestFixtures.character(age = 30, stats = Stats(money = 1_000_000))
        val owner = renter.copy(
            assets = listOf(TestFixtures.asset(type = AssetType.HOUSE, currentValue = 500_000))
        )
        assertTrue(
            finance.estimateAnnualCostOfLiving(renter) >
                finance.estimateAnnualCostOfLiving(owner)
        )
    }

    @Test
    fun workYear_grindNetsMoreThanCoastAfterTax() {
        val job = TestFixtures.character().career.currentJob
            ?: com.maisha.game.data.model.Job(
                id = "clerk",
                title = "Clerk",
                minEducation = com.maisha.game.data.model.SchoolStage.SECONDARY,
                baseSalary = 200_000,
                level = 1
            )
        val employed = TestFixtures.character(
            age = 28,
            stats = Stats(money = 0),
            career = com.maisha.game.data.model.CareerState(currentJob = job)
        )
        val coastMoney = career.workYear(employed, WorkEffort.COAST).stats.money
        val grindMoney = career.workYear(employed, WorkEffort.GRIND).stats.money
        assertTrue(grindMoney > coastMoney)
    }

    @Test
    fun netWorth_includesSavings() {
        val character = TestFixtures.character(
            stats = Stats(money = 10_000),
            assets = listOf(TestFixtures.asset(currentValue = 5_000))
        ).copy(savingsBalance = 7_000, investmentPortfolioValue = 3_000)
        assertEquals(25_000, finance.calculateNetWorth(character))
    }
}
