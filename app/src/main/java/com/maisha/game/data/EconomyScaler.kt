// app/src/main/java/com/maisha/game/data/EconomyScaler.kt
package com.maisha.game.data

import com.maisha.game.data.model.TaxPolicyType

/**
 * Simplified local-currency scaling — not real exchange rates or economic modeling.
 * Base amounts are authored in Kenya-scale units; each country gets a rough multiplier
 * so salaries and prices feel plausible in that country's displayed currency.
 *
 * Political [TaxPolicyType] levers adjust business revenue and one-time wealth hits.
 */
object EconomyScaler {

    private val multipliers: Map<String, Double> = mapOf(
        "KE" to 1.0,
        "NG" to 0.95,
        "ZA" to 0.18,
        "EG" to 0.35,
        "US" to 0.25,
        "CA" to 0.28,
        "GB" to 0.22,
        "FR" to 0.21,
        "DE" to 0.23,
        "IN" to 0.55,
        "JP" to 0.85,
        "PH" to 0.45,
        "ID" to 1.1,
        "BR" to 0.28,
        "MX" to 0.22,
    )

    fun scaleAmount(baseKenyaAmount: Int, countryCode: String): Int {
        val multiplier = multipliers[countryCode] ?: 1.0
        return (baseKenyaAmount * multiplier).toInt().coerceAtLeast(1)
    }

    /** Scales gift/travel costs by life stage so teens aren't priced out of relationships. */
    fun scaleRelationshipCost(baseKenyaAmount: Int, countryCode: String, age: Int): Int {
        val ageMultiplier = when {
            age < 13 -> 0.35
            age < 18 -> 0.55
            age < 22 -> 0.75
            else -> 1.0
        }
        return scaleAmount((baseKenyaAmount * ageMultiplier).toInt().coerceAtLeast(1), countryCode)
    }

    /** Multiplier applied to business revenue while a tax policy is active. */
    fun policyBusinessRevenueMultiplier(policy: TaxPolicyType?): Float = when (policy) {
        TaxPolicyType.TAX_CUTS -> 1.12f
        TaxPolicyType.WEALTH_TAX -> 0.90f
        null -> 1.0f
    }

    /** Fraction of liquid cash seized when enacting a wealth tax (one-time). */
    fun policyWealthTaxCashFraction(policy: TaxPolicyType?): Float = when (policy) {
        TaxPolicyType.WEALTH_TAX -> 0.05f
        else -> 0f
    }
}

