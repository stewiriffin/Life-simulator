package com.maisha.game.domain

import com.maisha.game.data.model.Character

/** Expat integration tier based on [Character.yearsInCurrentCountry]. */
enum class IntegrationLevel {
    RECENT_ARRIVAL,
    ADAPTING,
    FULLY_INTEGRATED
}

fun Character.isExpat(): Boolean = isLivingAbroad()

fun integrationLevelFor(yearsInCurrentCountry: Int): IntegrationLevel = when {
    yearsInCurrentCountry < 2 -> IntegrationLevel.RECENT_ARRIVAL
    yearsInCurrentCountry < 5 -> IntegrationLevel.ADAPTING
    else -> IntegrationLevel.FULLY_INTEGRATED
}

fun Character.integrationLevel(): IntegrationLevel =
    integrationLevelFor(yearsInCurrentCountry)
