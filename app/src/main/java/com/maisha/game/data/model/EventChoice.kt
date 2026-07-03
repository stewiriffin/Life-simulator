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
    val childRelationshipEffect: Int = 0,
    val triggersHaveChild: Boolean = false,
    val triggersCrime: String? = null,
    val triggersIllnessRoll: Boolean = false,
    val triggersExpulsion: Boolean = false,
    val triggersDropout: Boolean = false,
    val doctorCareTier: String? = null,
    val relocateToCountry: String? = null,
    /** Forces asset condition to this value (0–100) instead of applying [conditionEffect] delta. */
    val forceConditionValue: Int? = null,
    /** Forces or rolls macro-economic shift: `boom`, `bust`, or `random`. */
    val economicShift: String? = null,
    /** Prison-only: positive boosts parole chance; negative marks bad behavior. */
    val paroleEffect: Int = 0,
    /** Grants a catalog heirloom id (see [com.maisha.game.data.AssetCatalog]). */
    val grantHeirloom: String? = null,
    /** Delta applied to [SocialMediaState.followers] when the player has an account. */
    val followerEffect: Int = 0,
    /** Delta applied to each owned business valuation. */
    val businessValuationEffect: Int = 0,
    /** Delta applied to each owned business annual revenue. */
    val businessRevenueEffect: Int = 0
)
