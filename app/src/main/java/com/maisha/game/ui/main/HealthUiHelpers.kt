// app/src/main/java/com/maisha/game/ui/main/HealthUiHelpers.kt
package com.maisha.game.ui.main

import android.content.res.Resources
import com.maisha.game.R
import com.maisha.game.util.formatMoney

/**
 * Display-only treatment costs mirroring [com.maisha.game.domain.HealthEngine] values.
 * UI layer only — domain logic is not duplicated at runtime.
 */
object HealthUiHelpers {

    fun treatmentCostLabel(severity: Int, careType: CareType, res: Resources): String {
        val amount = treatmentCost(severity, careType)
        return res.getString(R.string.format_treatment_cost, formatMoney(amount))
    }

    fun treatmentCost(severity: Int, careType: CareType): Int {
        val privateCare = careType.usePrivateCare()
        return when (severity.coerceIn(1, 3)) {
            1 -> if (privateCare) 8_000 else 2_000
            2 -> if (privateCare) 20_000 else 6_000
            else -> if (privateCare) 45_000 else 12_000
        }
    }

    fun successHint(careType: CareType, res: Resources): String = when (careType) {
        CareType.PUBLIC_CLINIC -> res.getString(R.string.hint_public_clinic_success)
        CareType.PRIVATE_HOSPITAL -> res.getString(R.string.hint_private_hospital_success)
    }
}
