// app/src/main/java/com/maisha/game/data/model/Economic.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

/** Macro-economic climate for the current in-game year. */
@Serializable
enum class EconomicClimate {
    NEUTRAL,
    BOOM,
    BUST
}

/**
 * Yearly market conditions affecting asset values and upkeep.
 * Portfolio fields are also stored on [Character] for Room column mapping.
 */
@Serializable
data class EconomicState(
    val climate: EconomicClimate = EconomicClimate.NEUTRAL,
    val marketModifier: Float = 1.0f
)

/**
 * Fictional investment portfolio (not casino gambling).
 * Value fluctuates yearly via [com.maisha.game.domain.FinanceEngine.applyPortfolioMarketTick].
 */
@Serializable
data class InvestmentPortfolio(
    val value: Int = 0,
    /** Last applied return percentage, e.g. -30 to +40. */
    val lastReturnPercent: Int = 0
)
