// app/src/main/java/com/maisha/game/data/model/VisaType.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

/** Legal basis for residing in a country where the character is not a citizen. */
@Serializable
enum class VisaType {
    STUDENT,
    WORK,
    TOURIST
}
