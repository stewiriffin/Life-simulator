// app/src/main/java/com/maisha/game/data/model/Politics.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class PoliticalOffice {
    MAYOR,
    GOVERNOR,
    PRESIDENT
}

@Serializable
enum class TaxPolicyType {
    TAX_CUTS,
    WEALTH_TAX
}

/**
 * Political career state for a [Character].
 * Persisted as [com.maisha.game.data.local.CharacterEntity.politicsJson].
 */
@Serializable
data class PoliticalState(
    val currentOffice: PoliticalOffice? = null,
    val approvalRating: Int = 50,
    val campaignFunds: Int = 0,
    val activeTaxPolicy: TaxPolicyType? = null,
    val yearsInOffice: Int = 0,
    val campaignedThisYear: Boolean = false
)
