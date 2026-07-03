// app/src/main/java/com/maisha/game/data/model/Health.kt (modified — yearsUntreated tracking)
package com.maisha.game.data.model
import kotlinx.serialization.Serializable

/**
 * An active illness. Untreated conditions drain health yearly; severity 2+ for 3+ years can contribute to death risk.
 */
@Serializable
data class HealthCondition(
    val id: String,
    val name: String,
    val severity: Int,
    val treated: Boolean = false,
    val yearsUntreated: Int = 0
)
