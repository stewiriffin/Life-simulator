// app/src/main/java/com/maisha/game/data/model/AncestryRecord.kt (new)
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

/**
 * One deceased generation in a slot's legacy chain, stored in [Character.ancestryHistory].
 *
 * @property ageAtDeath Always set when built by [com.maisha.game.domain.LegacyEngine.buildAncestryEntry].
 * @property cause Human-readable death summary; nullable for forward-compatible records.
 */
@Serializable
data class AncestryEntry(
    val generationNumber: Int,
    val characterName: String,
    val countryCode: String,
    val relocatedTo: List<String> = emptyList(),
    val ageAtDeath: Int? = null,
    val cause: String? = null
)
