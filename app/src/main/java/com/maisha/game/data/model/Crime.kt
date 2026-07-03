// app/src/main/java/com/maisha/game/data/model/Crime.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

/**
 * Criminal history, trial, and incarceration state.
 *
 * Defaults to empty record on new characters. [lastArrestAge] drives clean-streak hire penalty reduction in [com.maisha.game.domain.CareerEngine].
 */
@Serializable
data class CriminalRecord(
    val hasRecord: Boolean = false,
    val timesArrested: Int = 0,
    val currentlyIncarcerated: Boolean = false,
    val yearsRemaining: Int = 0,
    /** Age when last arrested; used for clean-streak hire penalty reduction. */
    val lastArrestAge: Int? = null,
    /** Failed/successful crime tries in the current in-game year; resets on age-up. */
    val crimeAttemptsThisYear: Int = 0,
    /** True after arrest until the player picks legal representation at trial. */
    val awaitingTrial: Boolean = false,
    /** [CrimeType.name] pending resolution at trial. */
    val pendingCrimeType: String? = null,
    /** Full sentence handed down at trial; used for parole eligibility. */
    val totalSentenceYears: Int = 0,
    /** Years already served in the current sentence. */
    val yearsServed: Int = 0,
    /** Bad prison-event choices that disqualify parole for this sentence. */
    val negativePrisonEvents: Int = 0,
    /** Good prison-event choices that boost yearly parole roll chance. */
    val paroleBonus: Int = 0
)

@Serializable
enum class CrimeType {
    PICKPOCKET,
    SHOPLIFT,
    FRAUD
}

@Serializable
enum class LawyerTier {
    PUBLIC_DEFENDER,
    AVERAGE,
    EXPENSIVE
}
