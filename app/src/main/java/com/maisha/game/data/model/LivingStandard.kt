package com.maisha.game.data.model

import kotlinx.serialization.Serializable

/**
 * Player-chosen household spending level. Drives yearly cost of living and happiness pressure.
 * Persisted inside [LifestyleState] (no Room column).
 */
@Serializable
enum class LivingStandard {
    /** Cut corners: lower bills, mild happiness drain. */
    FRUGAL,
    /** Typical local lifestyle for age and household size. */
    MODEST,
    /** Extra comfort, dining out, better housing quality. */
    COMFORTABLE,
    /** Status spending; expensive and punishing if unpaid. */
    LUXURY
}
