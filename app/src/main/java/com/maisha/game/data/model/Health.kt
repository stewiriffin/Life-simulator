// app/src/main/java/com/maisha/game/data/model/Health.kt (modified — yearsUntreated tracking)
package com.maisha.game.data.model
import kotlinx.serialization.Serializable

@Serializable
data class HealthCondition(
    val id: String,
    val name: String,
    val severity: Int,
    val treated: Boolean = false,
    val yearsUntreated: Int = 0
)
