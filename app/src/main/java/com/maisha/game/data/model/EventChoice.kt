// app/src/main/java/com/maisha/game/data/model/EventChoice.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

/**
 * Player pick for a [LifeEvent]. Engine hooks read optional effect fields in [com.maisha.game.domain.GameEngine.applyChoice].
 */
@Serializable
data class EventChoice(
    val label: String,
    val statEffects: Map<String, Int> = emptyMap(),
    val resultText: String,
    val gpaEffect: Float = 0f,
    val siblingRelationshipEffect: Int = 0,
    val universityCourse: String? = null,
    val performanceEffect: Int = 0,
    val conditionEffect: Int = 0,
    val targetAssetType: String? = null,
    val familyRelationshipEffect: Int = 0,
    val spouseRelationshipEffect: Int = 0,
    val triggersHaveChild: Boolean = false,
    val triggersCrime: String? = null,
    val triggersIllnessRoll: Boolean = false,
    val doctorCareTier: String? = null,
    val relocateToCountry: String? = null
)
