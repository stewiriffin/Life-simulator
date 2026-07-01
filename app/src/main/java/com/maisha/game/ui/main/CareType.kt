// app/src/main/java/com/maisha/game/ui/main/CareType.kt
package com.maisha.game.ui.main

import android.content.res.Resources
import com.maisha.game.R

enum class CareType {
    PUBLIC_CLINIC,
    PRIVATE_HOSPITAL;

    fun usePrivateCare(): Boolean = this == PRIVATE_HOSPITAL

    fun displayName(res: Resources): String = when (this) {
        PUBLIC_CLINIC -> res.getString(R.string.care_public_clinic)
        PRIVATE_HOSPITAL -> res.getString(R.string.care_nairobi_hospital)
    }
}
