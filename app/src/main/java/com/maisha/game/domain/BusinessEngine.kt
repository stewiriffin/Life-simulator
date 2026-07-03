// app/src/main/java/com/maisha/game/domain/BusinessEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.Business
import com.maisha.game.data.model.BusinessIndustry
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EconomicClimate
import com.maisha.game.util.clampStat
import com.maisha.game.util.formatMoney
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.random.Random

sealed class BusinessResult {
    data class Success(val character: Character, val business: Business? = null) : BusinessResult()
    data class Failed(val reason: BusinessFailure) : BusinessResult()
}

enum class BusinessFailure {
    INELIGIBLE,
    INSUFFICIENT_FUNDS,
    MAX_BUSINESSES,
    NOT_FOUND,
    INVALID_INVESTMENT
}

@Singleton
class BusinessEngine @Inject constructor(
    private val financeEngine: FinanceEngine
) {

    fun startBusiness(
        character: Character,
        name: String,
        industry: BusinessIndustry,
        initialInvestment: Int
    ): BusinessResult {
        if (!canStartBusiness(character)) {
            return BusinessResult.Failed(BusinessFailure.INELIGIBLE)
        }
        if (character.businesses.size >= MAX_BUSINESSES) {
            return BusinessResult.Failed(BusinessFailure.MAX_BUSINESSES)
        }
        if (initialInvestment < minInvestment(character)) {
            return BusinessResult.Failed(BusinessFailure.INVALID_INVESTMENT)
        }
        if (character.stats.money < initialInvestment) {
            return BusinessResult.Failed(BusinessFailure.INSUFFICIENT_FUNDS)
        }

        val smartsFactor = 0.5f + (character.stats.smarts.coerceIn(0, 100) / 100f) * 0.5f
        val industryFactor = industryRevenueFactor(industry)
        val startingRevenue = (initialInvestment * 0.18f * smartsFactor * industryFactor)
            .roundToInt()
            .coerceAtLeast(1_000)
        val startingValuation = (initialInvestment * (0.9f + smartsFactor * 0.4f))
            .roundToInt()
            .coerceAtLeast(initialInvestment)
        val employees = (initialInvestment / employeeCapitalPerHead(character))
            .coerceIn(1, 50)

        val business = Business(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { defaultName(industry) },
            industry = industry,
            valuation = startingValuation,
            revenue = startingRevenue,
            employeeCount = employees,
            lastYearProfit = 0
        )

        val withCash = financeEngine.applyBusinessInvestment(character, initialInvestment)
        val updated = withCash.copy(
            businesses = withCash.businesses + business,
            eventLog = EventLogCap.prepend(
                withCash.eventLog,
                "Started ${business.name} (${industryLabel(industry)}) with ${formatMoney(initialInvestment, character.countryCode)}."
            )
        )
        return BusinessResult.Success(updated, business)
    }

    fun sellBusiness(character: Character, businessId: String): BusinessResult {
        val business = character.businesses.find { it.id == businessId }
            ?: return BusinessResult.Failed(BusinessFailure.NOT_FOUND)
        if (!character.alive) {
            return BusinessResult.Failed(BusinessFailure.INELIGIBLE)
        }

        val withCash = financeEngine.applyBusinessSale(character, business.valuation)
        val updated = withCash.copy(
            businesses = withCash.businesses.filterNot { it.id == businessId },
            eventLog = EventLogCap.prepend(
                withCash.eventLog,
                "Sold ${business.name} for ${formatMoney(business.valuation, character.countryCode)}."
            )
        )
        return BusinessResult.Success(updated, business)
    }

    /**
     * Yearly P&L, valuation drift from economic climate, and employee churn.
     */
    fun processBusinessYear(character: Character): Character {
        if (character.businesses.isEmpty()) return character

        var money = character.stats.money
        var happiness = character.stats.happiness
        val climate = character.economicState.climate
        val marketModifier = character.economicState.marketModifier
        val updatedBusinesses = mutableListOf<Business>()
        var log = character.eventLog

        for (business in character.businesses) {
            val profit = calculateYearlyProfit(business, climate)
            money += profit

            val valuationDrift = valuationMultiplier(climate, marketModifier)
            val newValuation = (business.valuation * valuationDrift)
                .roundToInt()
                .coerceAtLeast(1_000)
            val revenueDrift = 0.92f + Random.nextFloat() * 0.2f +
                if (profit >= 0) 0.05f else -0.08f
            val newRevenue = (business.revenue * revenueDrift)
                .roundToInt()
                .coerceAtLeast(500)
            val employeeDelta = Random.nextInt(-2, 4)
            val newEmployees = (business.employeeCount + employeeDelta).coerceIn(1, 200)

            updatedBusinesses += business.copy(
                valuation = newValuation,
                revenue = newRevenue,
                employeeCount = newEmployees,
                lastYearProfit = profit
            )

            val profitLabel = if (profit >= 0) {
                "profit of ${formatMoney(profit, character.countryCode)}"
            } else {
                "loss of ${formatMoney(-profit, character.countryCode)}"
            }
            log = EventLogCap.prepend(
                log,
                "${business.name} posted a $profitLabel."
            )
        }

        if (money < 0) {
            happiness = clampStat(happiness - BUSINESS_DEBT_HAPPINESS_PENALTY)
            money = 0
        }

        return character.copy(
            businesses = updatedBusinesses,
            stats = character.stats.copy(money = money, happiness = happiness),
            eventLog = log
        )
    }

    fun canStartBusiness(character: Character): Boolean =
        character.alive &&
            character.age >= MIN_BUSINESS_AGE &&
            !character.criminalRecord.currentlyIncarcerated &&
            !character.criminalRecord.awaitingTrial

    fun minInvestment(character: Character): Int =
        EconomyScaler.scaleAmount(MIN_INVESTMENT_KENYA, character.countryCode)

    fun investmentTiers(character: Character): List<Int> = listOf(
        EconomyScaler.scaleAmount(INVESTMENT_SMALL_KENYA, character.countryCode),
        EconomyScaler.scaleAmount(INVESTMENT_MEDIUM_KENYA, character.countryCode),
        EconomyScaler.scaleAmount(INVESTMENT_LARGE_KENYA, character.countryCode)
    )

    fun applyBusinessEffects(
        character: Character,
        valuationDelta: Int,
        revenueDelta: Int
    ): Character {
        if (character.businesses.isEmpty()) return character
        if (valuationDelta == 0 && revenueDelta == 0) return character
        val updated = character.businesses.map { business ->
            business.copy(
                valuation = (business.valuation + valuationDelta).coerceAtLeast(1_000),
                revenue = (business.revenue + revenueDelta).coerceAtLeast(500)
            )
        }
        return character.copy(businesses = updated)
    }

    private fun calculateYearlyProfit(business: Business, climate: EconomicClimate): Int {
        // Net margin on revenue; payroll pressure scales with headcount vs revenue.
        val baseMargin = when (climate) {
            EconomicClimate.BOOM -> 0.18f + Random.nextFloat() * 0.14f
            EconomicClimate.BUST -> -0.04f + Random.nextFloat() * 0.14f
            EconomicClimate.NEUTRAL -> 0.08f + Random.nextFloat() * 0.14f
        }
        val headcountPressure = (business.employeeCount * 0.004f).coerceAtMost(0.08f)
        val margin = (baseMargin - headcountPressure).coerceIn(-0.20f, 0.40f)
        val noise = Random.nextInt(-business.revenue / 25, business.revenue / 20 + 1)
        return (business.revenue * margin).roundToInt() + noise
    }

    private fun valuationMultiplier(climate: EconomicClimate, marketModifier: Float): Float {
        val climateBoost = when (climate) {
            EconomicClimate.BOOM -> 1.08f
            EconomicClimate.BUST -> 0.90f
            EconomicClimate.NEUTRAL -> 1.0f
        }
        val noise = 0.94f + Random.nextFloat() * 0.14f
        return (climateBoost * marketModifier.coerceIn(0.8f, 1.2f) * noise).coerceIn(0.75f, 1.35f)
    }

    private fun industryRevenueFactor(industry: BusinessIndustry): Float = when (industry) {
        BusinessIndustry.TECH -> 1.15f
        BusinessIndustry.REAL_ESTATE -> 1.05f
        BusinessIndustry.ENTERTAINMENT -> 1.10f
        BusinessIndustry.RETAIL -> 0.95f
        BusinessIndustry.FOOD -> 0.90f
    }

    private fun employeeCapitalPerHead(character: Character): Int =
        EconomyScaler.scaleAmount(50_000, character.countryCode).coerceAtLeast(10_000)

    private fun defaultName(industry: BusinessIndustry): String = when (industry) {
        BusinessIndustry.TECH -> "Nova Tech"
        BusinessIndustry.RETAIL -> "Corner Market Co."
        BusinessIndustry.FOOD -> "Home Kitchen Ltd"
        BusinessIndustry.REAL_ESTATE -> "Horizon Properties"
        BusinessIndustry.ENTERTAINMENT -> "Spotlight Media"
    }

    private fun industryLabel(industry: BusinessIndustry): String = when (industry) {
        BusinessIndustry.TECH -> "tech"
        BusinessIndustry.RETAIL -> "retail"
        BusinessIndustry.FOOD -> "food"
        BusinessIndustry.REAL_ESTATE -> "real estate"
        BusinessIndustry.ENTERTAINMENT -> "entertainment"
    }

    companion object {
        const val REQUIRES_BUSINESS_TAG = "requires_business"
        const val MAX_BUSINESSES = 3
        const val MIN_BUSINESS_AGE = 18
        const val MIN_INVESTMENT_KENYA = 50_000
        const val INVESTMENT_SMALL_KENYA = 50_000
        const val INVESTMENT_MEDIUM_KENYA = 150_000
        const val INVESTMENT_LARGE_KENYA = 500_000
        private const val BUSINESS_DEBT_HAPPINESS_PENALTY = 5
    }
}
