package com.maisha.game.domain

import com.maisha.game.data.model.CriminalRecord

enum class CrimeStatusKind {
    CLEAR,
    AWAITING_TRIAL,
    INCARCERATED
}

data class CrimeStatusView(
    val kind: CrimeStatusKind,
    val yearsRemaining: Int = 0,
    val yearsServed: Int = 0,
    val totalSentenceYears: Int = 0,
    /** Soft parole hint when incarcerated with remaining time. */
    val showParoleHint: Boolean = false
)

/** Pure mapper for Risk-tab crime status chrome. */
object CrimeStatusMapper {
    fun map(record: CriminalRecord): CrimeStatusView = when {
        record.awaitingTrial -> CrimeStatusView(kind = CrimeStatusKind.AWAITING_TRIAL)
        record.currentlyIncarcerated -> CrimeStatusView(
            kind = CrimeStatusKind.INCARCERATED,
            yearsRemaining = record.yearsRemaining.coerceAtLeast(0),
            yearsServed = record.yearsServed.coerceAtLeast(0),
            totalSentenceYears = record.totalSentenceYears.coerceAtLeast(0),
            showParoleHint = record.yearsRemaining > 0
        )
        else -> CrimeStatusView(kind = CrimeStatusKind.CLEAR)
    }
}
