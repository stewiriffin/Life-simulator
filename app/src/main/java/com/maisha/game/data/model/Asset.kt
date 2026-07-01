// app/src/main/java/com/maisha/game/data/model/Asset.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class AssetType {
    HOUSE,
    CAR,
    MOTORBIKE
}

@Serializable
data class Asset(
    val id: String,
    val type: AssetType,
    val name: String,
    val purchasePrice: Int,
    val currentValue: Int,
    val condition: Int = 100,
    val monthlyUpkeep: Int
)
