// app/src/main/java/com/maisha/game/data/model/HolidayFlavor.kt (new)
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HolidayFlavor(
    val name: String,
    val approxAgeRelevantDescription: String
)
