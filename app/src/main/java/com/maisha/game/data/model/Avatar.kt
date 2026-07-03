// app/src/main/java/com/maisha/game/data/model/Avatar.kt
package com.maisha.game.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlin.random.Random

enum class AgeStage {
    BABY,
    CHILD,
    TEEN,
    ADULT,
    SENIOR
}

enum class Expression {
    NEUTRAL,
    HAPPY,
    SAD,
    ANGRY,
    SURPRISED
}

@Serializable
enum class FacialHairStyle {
    STUBBLE,
    MUSTACHE,
    BEARD,
    GOATEE
}

@Serializable
enum class EyewearStyle {
    GLASSES,
    SUNGLASSES,
    READING_GLASSES
}

@Serializable
enum class AgingDetails {
    WRINKLES,
    GRAYING,
    WRINKLES_AND_GRAYING
}

fun ageStageFor(age: Int): AgeStage = when {
    age <= 2 -> AgeStage.BABY
    age <= 12 -> AgeStage.CHILD
    age <= 17 -> AgeStage.TEEN
    age < 60 -> AgeStage.ADULT
    else -> AgeStage.SENIOR
}

/**
 * Visual identity for layered avatar drawables in [com.maisha.game.ui.avatar.AvatarImage].
 *
 * Index ranges are defined in [Companion]; out-of-range values are coerced at render time.
 * Optional genetics-driven and age-evolved accessories live in nullable fields.
 */
@Immutable
@Serializable
data class AvatarConfig(
    val skinTone: Int,
    val hairStyle: Int,
    val hairColor: Int,
    val outfitColor: Int,
    val accessoryId: Int? = null,
    val facialFeature: Int? = null,
    val facialHair: FacialHairStyle? = null,
    val eyewear: EyewearStyle? = null,
    val agingDetails: AgingDetails? = null
) {
    companion object {
        const val SKIN_TONE_COUNT = 8
        const val HAIR_STYLE_COUNT = 8
        const val HAIR_COLOR_COUNT = 6
        const val OUTFIT_COLOR_COUNT = 8
        const val ACCESSORY_COUNT = 3
        const val FACIAL_FEATURE_COUNT = 5
        /** Index of the gray swatch in the hair-color palette (senior aging). */
        const val GRAY_HAIR_COLOR_INDEX = 5

        fun random(): AvatarConfig = AvatarConfig(
            skinTone = Random.nextInt(SKIN_TONE_COUNT),
            hairStyle = Random.nextInt(HAIR_STYLE_COUNT),
            hairColor = Random.nextInt(HAIR_COLOR_COUNT),
            outfitColor = Random.nextInt(OUTFIT_COLOR_COUNT),
            accessoryId = if (Random.nextBoolean()) Random.nextInt(ACCESSORY_COUNT) else null,
            facialFeature = if (Random.nextFloat() < 0.45f) Random.nextInt(FACIAL_FEATURE_COUNT) else null,
            facialHair = null,
            eyewear = null,
            agingDetails = null
        )

        val DEFAULT = AvatarConfig(
            skinTone = 3,
            hairStyle = 1,
            hairColor = 2,
            outfitColor = 3,
            accessoryId = null,
            facialFeature = null,
            facialHair = null,
            eyewear = null,
            agingDetails = null
        )
    }
}
