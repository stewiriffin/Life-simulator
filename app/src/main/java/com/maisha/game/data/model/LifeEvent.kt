// app/src/main/java/com/maisha/game/data/model/LifeEvent.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

/**
 * A narrative event offered to the player (loaded from JSON or built by system engines).
 */
@Serializable
data class LifeEvent(
    val id: String,
    val minAge: Int,
    val maxAge: Int,
    val text: String,
    val choices: List<EventChoice>,
    val weight: Int = 1,
    val tags: List<String> = emptyList(),
    val restrictedToCountry: String? = null
)

@Serializable
data class LifeEventList(
    val events: List<LifeEvent>
)
