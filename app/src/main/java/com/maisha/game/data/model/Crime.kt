// app/src/main/java/com/maisha/game/data/model/Crime.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CriminalRecord(
    val hasRecord: Boolean = false,
    val timesArrested: Int = 0,
    val currentlyIncarcerated: Boolean = false,
    val yearsRemaining: Int = 0,
    /** Age when last arrested; used for clean-streak hire penalty reduction. */
    val lastArrestAge: Int? = null
)

@Serializable
enum class CrimeType {
    PICKPOCKET,
    SHOPLIFT,
    FRAUD
}
