// app/src/main/java/com/maisha/game/data/model/AncestryRecord.kt (new)
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AncestryEntry(
    val generationNumber: Int,
    val characterName: String,
    val countryCode: String,
    val relocatedTo: List<String> = emptyList(),
    val ageAtDeath: Int? = null,
    val cause: String? = null
)
