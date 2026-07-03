// app/src/main/java/com/maisha/game/ui/avatar/AvatarAssetMapper.kt
package com.maisha.game.ui.avatar

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.maisha.game.R
import com.maisha.game.data.model.AgeStage
import com.maisha.game.data.model.AgingDetails
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Expression
import com.maisha.game.data.model.EyewearStyle
import com.maisha.game.data.model.FacialHairStyle

/**
 * Maps [AvatarConfig] / age / expression to local vector drawable layers.
 * Missing combinations resolve to [R.drawable.avatar_transparent] so rendering never throws.
 */
object AvatarAssetMapper {

    private val skinTones = listOf(
        Color(0xFFFFDBAC), Color(0xFFFFE0BD), Color(0xFFE8B88A), Color(0xFFD4A574),
        Color(0xFFC68642), Color(0xFF8D5524), Color(0xFF6B4423), Color(0xFF4A2912)
    )
    private val hairColors = listOf(
        Color(0xFF1A1A1A), Color(0xFF4A3728), Color(0xFF8B6914),
        Color(0xFFB8860B), Color(0xFF6B4423), Color(0xFF808080)
    )
    private val outfitColors = listOf(
        Color(0xFF1A8A8A), Color(0xFF2E5AAC), Color(0xFFE85D5D), Color(0xFFF4B942),
        Color(0xFF7E57C2), Color(0xFF4CAF50), Color(0xFFCE93D8), Color(0xFF455A64)
    )
    private val seniorGrey = Color(0xFFB0B0B0)

    /** Hair styles drawn behind the head (long / voluminous). */
    private val backHairStyles = setOf(4, 5, 7)

    @DrawableRes
    fun getBaseHead(skinTone: Int, ageStage: AgeStage): Int = when (ageStage) {
        AgeStage.BABY -> R.drawable.avatar_head_baby
        AgeStage.CHILD -> R.drawable.avatar_head_child
        AgeStage.TEEN -> R.drawable.avatar_head_teen
        AgeStage.ADULT -> R.drawable.avatar_head_adult
        AgeStage.SENIOR -> R.drawable.avatar_head_senior
    }

    /**
     * Front hair layer (bangs / crown). Bald styles and back-only styles return transparent.
     */
    @DrawableRes
    fun getHairOverlay(hairStyle: Int, hairColor: Int, ageStage: AgeStage): Int {
        val style = hairStyle.floorMod(AvatarConfig.HAIR_STYLE_COUNT)
        if (style == 6) return R.drawable.avatar_transparent
        if (style in backHairStyles) return R.drawable.avatar_transparent
        return hairStyleDrawable(style)
    }

    /**
     * Back hair layer for long styles. Short styles and bald return transparent.
     */
    @DrawableRes
    fun getBackHairOverlay(hairStyle: Int, hairColor: Int, ageStage: AgeStage): Int {
        val style = hairStyle.floorMod(AvatarConfig.HAIR_STYLE_COUNT)
        if (style !in backHairStyles) return R.drawable.avatar_transparent
        return hairStyleDrawable(style)
    }

    @DrawableRes
    fun getExpressionOverlay(expression: Expression): Int = when (expression) {
        Expression.NEUTRAL -> R.drawable.avatar_expression_neutral
        Expression.HAPPY -> R.drawable.avatar_expression_happy
        Expression.SAD -> R.drawable.avatar_expression_sad
        Expression.ANGRY -> R.drawable.avatar_expression_angry
        Expression.SURPRISED -> R.drawable.avatar_expression_surprised
    }

    @DrawableRes
    fun getOutfitOverlay(outfitColor: Int, ageStage: AgeStage): Int = when (ageStage) {
        AgeStage.BABY -> R.drawable.avatar_outfit_baby
        AgeStage.CHILD -> R.drawable.avatar_outfit_child
        AgeStage.TEEN -> R.drawable.avatar_outfit_teen
        AgeStage.ADULT -> R.drawable.avatar_outfit_adult
        AgeStage.SENIOR -> R.drawable.avatar_outfit_senior
    }

    @DrawableRes
    fun getNeckOverlay(ageStage: AgeStage): Int =
        if (ageStage == AgeStage.BABY) R.drawable.avatar_transparent else R.drawable.avatar_neck

    @DrawableRes
    fun getCheeksOverlay(ageStage: AgeStage): Int =
        if (ageStage == AgeStage.BABY) R.drawable.avatar_cheeks_baby else R.drawable.avatar_transparent

    @DrawableRes
    fun getWrinklesOverlay(ageStage: AgeStage): Int =
        if (ageStage == AgeStage.SENIOR) R.drawable.avatar_wrinkles_senior else R.drawable.avatar_transparent

    @DrawableRes
    fun getCaneOverlay(ageStage: AgeStage): Int =
        if (ageStage == AgeStage.SENIOR) R.drawable.avatar_cane_senior else R.drawable.avatar_transparent

    @DrawableRes
    fun getAccessoryOverlay(accessoryId: Int?): Int = when (accessoryId) {
        0 -> R.drawable.avatar_accessory_0
        1 -> R.drawable.avatar_accessory_1
        2 -> R.drawable.avatar_accessory_2
        else -> R.drawable.avatar_transparent
    }

    @DrawableRes
    fun getFacialFeatureOverlay(facialFeature: Int?): Int = when (facialFeature) {
        1 -> R.drawable.avatar_feature_1
        2 -> R.drawable.avatar_feature_2
        3 -> R.drawable.avatar_feature_3
        4 -> R.drawable.avatar_feature_4
        else -> R.drawable.avatar_transparent
    }

    /** Stacked on the face, beneath front hair. */
    @DrawableRes
    fun getFacialHairOverlay(facialHair: FacialHairStyle?): Int = when (facialHair) {
        FacialHairStyle.STUBBLE -> R.drawable.avatar_facial_hair_stubble
        FacialHairStyle.MUSTACHE -> R.drawable.avatar_facial_hair_mustache
        FacialHairStyle.BEARD -> R.drawable.avatar_facial_hair_beard
        FacialHairStyle.GOATEE -> R.drawable.avatar_facial_hair_goatee
        null -> R.drawable.avatar_transparent
    }

    /** Stacked on the face, beneath front hair. */
    @DrawableRes
    fun getEyewearOverlay(eyewear: EyewearStyle?): Int = when (eyewear) {
        EyewearStyle.GLASSES -> R.drawable.avatar_eyewear_glasses
        EyewearStyle.SUNGLASSES -> R.drawable.avatar_eyewear_sunglasses
        EyewearStyle.READING_GLASSES -> R.drawable.avatar_eyewear_reading_glasses
        null -> R.drawable.avatar_transparent
    }

    /** Wrinkle / aging overlay driven by [AgingDetails] or senior age stage. */
    @DrawableRes
    fun getAgingDetailsOverlay(agingDetails: AgingDetails?, ageStage: AgeStage): Int {
        val needsWrinkles = agingDetails == AgingDetails.WRINKLES ||
            agingDetails == AgingDetails.WRINKLES_AND_GRAYING ||
            ageStage == AgeStage.SENIOR
        return if (needsWrinkles) R.drawable.avatar_aging_wrinkles else R.drawable.avatar_transparent
    }

    fun skinTint(skinTone: Int): Color =
        skinTones[skinTone.floorMod(skinTones.size)]

    fun hairTint(hairColor: Int, ageStage: AgeStage, agingDetails: AgingDetails? = null): Color {
        val graying = ageStage == AgeStage.SENIOR ||
            agingDetails == AgingDetails.GRAYING ||
            agingDetails == AgingDetails.WRINKLES_AND_GRAYING
        return if (graying) seniorGrey
        else hairColors[hairColor.floorMod(hairColors.size)]
    }

    fun outfitTint(outfitColor: Int): Color =
        outfitColors[outfitColor.floorMod(outfitColors.size)]

    /** Coerces indices into valid ranges so corrupt save data cannot crash rendering. */
    fun sanitize(config: AvatarConfig): AvatarConfig = AvatarConfig(
        skinTone = config.skinTone.floorMod(AvatarConfig.SKIN_TONE_COUNT),
        hairStyle = config.hairStyle.floorMod(AvatarConfig.HAIR_STYLE_COUNT),
        hairColor = config.hairColor.floorMod(AvatarConfig.HAIR_COLOR_COUNT),
        outfitColor = config.outfitColor.floorMod(AvatarConfig.OUTFIT_COLOR_COUNT),
        accessoryId = config.accessoryId?.takeIf { it in 0 until AvatarConfig.ACCESSORY_COUNT },
        facialFeature = config.facialFeature?.takeIf { it in 0 until AvatarConfig.FACIAL_FEATURE_COUNT },
        facialHair = config.facialHair,
        eyewear = config.eyewear,
        agingDetails = config.agingDetails
    )

    /**
     * True when [config] looks unusable (extreme garbage values that should show a silhouette).
     * Normal out-of-range indices are handled by [sanitize].
     */
    fun requiresFallback(config: AvatarConfig): Boolean {
        val fields = listOf(
            config.skinTone,
            config.hairStyle,
            config.hairColor,
            config.outfitColor,
            config.accessoryId ?: 0,
            config.facialFeature ?: 0
        )
        return fields.any { it < -1_000 || it > 1_000_000 }
    }

    /** Every drawable ID this mapper can return (for tests). */
    fun allKnownDrawableIds(): Set<Int> = buildSet {
        AgeStage.entries.forEach { stage ->
            add(getBaseHead(0, stage))
            add(getOutfitOverlay(0, stage))
            add(getNeckOverlay(stage))
            add(getCheeksOverlay(stage))
            add(getWrinklesOverlay(stage))
            add(getCaneOverlay(stage))
        }
        Expression.entries.forEach { add(getExpressionOverlay(it)) }
        for (style in 0 until AvatarConfig.HAIR_STYLE_COUNT) {
            add(getHairOverlay(style, 0, AgeStage.ADULT))
            add(getBackHairOverlay(style, 0, AgeStage.ADULT))
        }
        for (id in 0 until AvatarConfig.ACCESSORY_COUNT) add(getAccessoryOverlay(id))
        for (id in 0 until AvatarConfig.FACIAL_FEATURE_COUNT) add(getFacialFeatureOverlay(id))
        FacialHairStyle.entries.forEach { add(getFacialHairOverlay(it)) }
        EyewearStyle.entries.forEach { add(getEyewearOverlay(it)) }
        AgingDetails.entries.forEach { add(getAgingDetailsOverlay(it, AgeStage.ADULT)) }
        add(R.drawable.avatar_transparent)
    }

    @DrawableRes
    private fun hairStyleDrawable(style: Int): Int = when (style) {
        0 -> R.drawable.avatar_hair_style0
        1 -> R.drawable.avatar_hair_style1
        2 -> R.drawable.avatar_hair_style2
        3 -> R.drawable.avatar_hair_style3
        4 -> R.drawable.avatar_hair_style4
        5 -> R.drawable.avatar_hair_style5
        7 -> R.drawable.avatar_hair_style7
        else -> R.drawable.avatar_transparent
    }

    private fun Int.floorMod(size: Int): Int {
        if (size <= 0) return 0
        val r = this % size
        return if (r < 0) r + size else r
    }
}

@Immutable
data class AvatarLayer(
    @DrawableRes val drawableRes: Int,
    val tint: Color? = null
)
