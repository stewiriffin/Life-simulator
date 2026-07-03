// app/src/main/java/com/maisha/game/data/model/Business.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class BusinessIndustry {
    TECH,
    RETAIL,
    FOOD,
    REAL_ESTATE,
    ENTERTAINMENT
}

/**
 * Player-owned company. Yearly profit/loss and valuation are updated by
 * [com.maisha.game.domain.BusinessEngine.processBusinessYear].
 */
@Serializable
data class Business(
    val id: String,
    val name: String,
    val industry: BusinessIndustry,
    val valuation: Int,
    val revenue: Int,
    val employeeCount: Int,
    /** Most recent yearly profit (positive) or loss (negative). */
    val lastYearProfit: Int = 0
)
