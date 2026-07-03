// app/src/main/java/com/maisha/game/data/model/RelationshipMemory.kt (modified)
package com.maisha.game.data.model

import com.maisha.game.domain.InteractionType
import kotlinx.serialization.Serializable

@Serializable
enum class MilestoneKind {
    STARTED_DATING,
    MARRIED,
    QUALITY_TIME,
    BIG_ARGUMENT,
    THOUGHTFUL_GIFT,
    INSULTED,
    TRAVELED_TOGETHER,
    SET_UP_ON_DATE,
    LEGACY_CONTINUED
}

/**
 * Notable relationship moment on a [Person], shown in detail UI.
 */
@Serializable
data class RelationshipMilestone(
    val ageAtEvent: Int,
    val description: String = "",
    val kind: String? = null,
    val subjectName: String? = null,
    val interactionType: String? = null
) {
    companion object {
        fun fromKind(
            age: Int,
            kind: MilestoneKind,
            subjectName: String,
            type: InteractionType? = null
        ): RelationshipMilestone = RelationshipMilestone(
            ageAtEvent = age,
            kind = kind.name,
            subjectName = subjectName,
            interactionType = type?.name
        )

        @Deprecated("Use fromKind for localized UI")
        fun fromInteraction(age: Int, description: String, type: InteractionType): RelationshipMilestone =
            RelationshipMilestone(ageAtEvent = age, description = description, interactionType = type.name)
    }
}

enum class RelationshipTier {
    ESTRANGED,
    DISTANT,
    COOL,
    FRIENDLY,
    CLOSE,
    INSEPARABLE
}

fun relationshipTierFor(level: Int): RelationshipTier = when {
    level < 17 -> RelationshipTier.ESTRANGED
    level < 34 -> RelationshipTier.DISTANT
    level < 51 -> RelationshipTier.COOL
    level < 68 -> RelationshipTier.FRIENDLY
    level < 85 -> RelationshipTier.CLOSE
    else -> RelationshipTier.INSEPARABLE
}
