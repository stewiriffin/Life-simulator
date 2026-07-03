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
 * Persisted on [Character] for save compatibility.
 */
@Serializable
data class EconomicState(
    val climate: EconomicClimate = EconomicClimate.NEUTRAL,
    val marketModifier: Float = 1.0f
)
