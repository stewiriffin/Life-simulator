// app/src/main/java/com/maisha/game/data/model/Asset.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class AssetType {
    HOUSE,
    CAR,
    MOTORBIKE,
    HEIRLOOM
}

/**
 * Ownable property, vehicle, or generational heirloom.
 * [condition] affects [currentValue] via [com.maisha.game.domain.FinanceEngine.recalculateValue].
 * Heirlooms skip degradation and appreciate over time.
 */
@Serializable
data class Asset(
    val id: String,
    val type: AssetType,
    val name: String,
    val purchasePrice: Int,
    val currentValue: Int,
    val condition: Int = 100,
    val monthlyUpkeep: Int,
    val isHeirloom: Boolean = false,
    /** [Character.generationNumber] when this heirloom entered the family. */
    val generationAcquired: Int = 1
)
