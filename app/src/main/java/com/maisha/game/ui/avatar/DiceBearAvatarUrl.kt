package com.maisha.game.ui.avatar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.maisha.game.data.model.AgeStage
import com.maisha.game.data.model.AgingDetails
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Expression
import com.maisha.game.data.model.EyewearStyle
import com.maisha.game.data.model.FacialHairStyle
import com.maisha.game.data.model.ageStageFor
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Builds [DiceBear](https://www.dicebear.com/) Lorelei PNG URLs from Maisha [AvatarConfig].
 * Faces are loaded online; layered drawables remain the offline fallback in [AvatarImage].
 */
object DiceBearAvatarUrl {
    const val STYLE = "lorelei"
    private const val API_VERSION = "9.x"
    private const val BASE = "https://api.dicebear.com/$API_VERSION/$STYLE/png"

    private val skinHex = listOf(
        "ffdbac", "ffe0bd", "e8b88a", "d4a574",
        "c68642", "8d5524", "6b4423", "4a2912"
    )
    private val hairHex = listOf(
        "1a1a1a", "4a3728", "8b6914", "b8860b", "6b4423", "808080"
    )
    private const val SENIOR_GREY = "b0b0b0"

    /** Stable identity seed when callers don't supply a person/character id. */
    fun seedFromConfig(config: AvatarConfig): String {
        val safe = AvatarAssetMapper.sanitize(config)
        return buildString {
            append("m")
            append(safe.skinTone)
            append('h')
            append(safe.hairStyle)
            append('c')
            append(safe.hairColor)
            append('o')
            append(safe.outfitColor)
            safe.accessoryId?.let { append('a').append(it) }
            safe.facialFeature?.let { append('f').append(it) }
            safe.facialHair?.let { append('b').append(it.name) }
            safe.eyewear?.let { append('e').append(it.name) }
            safe.agingDetails?.let { append('g').append(it.name) }
        }
    }

    fun build(
        config: AvatarConfig,
        age: Int,
        expression: Expression = Expression.NEUTRAL,
        seed: String? = null,
        sizePx: Int = 128
    ): String {
        val safe = AvatarAssetMapper.sanitize(config)
        val stage = ageStageFor(age)
        val resolvedSeed = (seed?.takeIf { it.isNotBlank() } ?: seedFromConfig(safe))
            .let { encode(it) }

        val params = linkedMapOf<String, String>()
        params["seed"] = resolvedSeed
        params["size"] = sizePx.coerceIn(48, 256).toString()
        params["radius"] = "50"
        params["backgroundColor"] = "152238"
        params["skinColor"] = skinHex[safe.skinTone % skinHex.size]
        params["hairColor"] = hairColorHex(safe, stage)
        params["hairVariant"] = hairVariant(safe.hairStyle)
        params["mouthVariant"] = mouthVariant(expression)
        params["beardProbability"] = if (safe.facialHair != null) "100" else "0"
        if (safe.facialHair != null) {
            params["beardVariant"] = beardVariant(safe.facialHair)
        }
        params["glassesProbability"] = if (safe.eyewear != null) "100" else "0"
        if (safe.eyewear != null) {
            params["glassesVariant"] = glassesVariant(safe.eyewear)
        }
        // Freckles / feature accent when facialFeature is set.
        params["frecklesProbability"] = if (safe.facialFeature != null) "80" else "0"
        // Younger faces slightly larger scale; seniors slightly smaller (aging cue).
        params["scale"] = when (stage) {
            AgeStage.BABY -> "110"
            AgeStage.CHILD -> "105"
            AgeStage.TEEN -> "100"
            AgeStage.ADULT -> "95"
            AgeStage.SENIOR -> "90"
        }

        val query = params.entries.joinToString("&") { (k, v) -> "$k=$v" }
        return "$BASE?$query"
    }

    private fun hairColorHex(config: AvatarConfig, stage: AgeStage): String {
        val graying = stage == AgeStage.SENIOR ||
            config.agingDetails == AgingDetails.GRAYING ||
            config.agingDetails == AgingDetails.WRINKLES_AND_GRAYING
        return if (graying) SENIOR_GREY else hairHex[config.hairColor % hairHex.size]
    }

    private fun hairVariant(hairStyle: Int): String {
        // Lorelei hair variants are hair01.. — map our 0–7 styles into a stable subset.
        val mapped = (hairStyle % 8) + 1
        return "variant${mapped.toString().padStart(2, '0')}"
    }

    private fun mouthVariant(expression: Expression): String = when (expression) {
        Expression.HAPPY -> "happy01"
        Expression.SAD -> "sad01"
        Expression.ANGRY -> "sad05"
        Expression.SURPRISED -> "happy12"
        Expression.NEUTRAL -> "happy08"
    }

    private fun beardVariant(style: FacialHairStyle): String = when (style) {
        FacialHairStyle.STUBBLE, FacialHairStyle.MUSTACHE, FacialHairStyle.GOATEE -> "variant01"
        FacialHairStyle.BEARD -> "variant02"
    }

    private fun glassesVariant(style: EyewearStyle): String = when (style) {
        EyewearStyle.GLASSES -> "variant01"
        EyewearStyle.SUNGLASSES -> "variant03"
        EyewearStyle.READING_GLASSES -> "variant02"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    /** Hex without `#` for DiceBear color query params. */
    fun colorToHex(color: Color): String =
        String.format("%06x", color.toArgb() and 0xFFFFFF)
}
