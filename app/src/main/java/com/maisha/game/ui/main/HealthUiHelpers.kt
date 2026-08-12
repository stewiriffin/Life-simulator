// app/src/main/java/com/maisha/game/ui/main/HealthUiHelpers.kt
package com.maisha.game.ui.main

import android.content.res.Resources
import com.maisha.game.R
import com.maisha.game.data.model.Character
import com.maisha.game.domain.HealthEngine
import com.maisha.game.util.formatMoney

/**
 * Display-only treatment costs mirroring [HealthEngine] values.
 */
object HealthUiHelpers {

    private val healthEngine = HealthEngine()

    fun treatmentCostLabel(
        character: Character,
        severity: Int,
        careType: CareType,
        res: Resources
    ): String {
        val amount = treatmentCost(character, severity, careType)
        return res.getString(
            R.string.format_treatment_cost,
            formatMoney(amount, character.countryCode)
        )
    }

    fun treatmentCost(character: Character, severity: Int, careType: CareType): Int =
        healthEngine.estimateTreatmentCost(character, severity, careType.usePrivateCare())

    fun successHint(careType: CareType, res: Resources): String = when (careType) {
        CareType.PUBLIC_CLINIC -> res.getString(R.string.hint_public_clinic_success)
        CareType.PRIVATE_HOSPITAL -> res.getString(R.string.hint_private_hospital_success)
    }
}
