package com.maisha.game.ui.avatar

import com.maisha.game.R
import com.maisha.game.data.model.AgeStage
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Expression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarAssetMapperTest {

    @Test
    fun getBaseHead_mapsEverySkinToneAndAgeStageToValidDrawable() {
        for (skinTone in 0 until AvatarConfig.SKIN_TONE_COUNT) {
            for (stage in AgeStage.entries) {
                val res = AvatarAssetMapper.getBaseHead(skinTone, stage)
                assertNotEquals(0, res)
                assertNotEquals(R.drawable.avatar_transparent, res)
            }
        }
    }

    @Test
    fun getHairOverlay_mapsEveryHairStyleAndColorAndAgeStage() {
        for (style in 0 until AvatarConfig.HAIR_STYLE_COUNT) {
            for (color in 0 until AvatarConfig.HAIR_COLOR_COUNT) {
                for (stage in AgeStage.entries) {
                    val front = AvatarAssetMapper.getHairOverlay(style, color, stage)
                    val back = AvatarAssetMapper.getBackHairOverlay(style, color, stage)
                    assertNotEquals(0, front)
                    assertNotEquals(0, back)
                    // Bald: both transparent. Long styles: back only. Short: front only.
                    when (style) {
                        6 -> {
                            assertEquals(R.drawable.avatar_transparent, front)
                            assertEquals(R.drawable.avatar_transparent, back)
                        }
                        4, 5, 7 -> {
                            assertEquals(R.drawable.avatar_transparent, front)
                            assertNotEquals(R.drawable.avatar_transparent, back)
                        }
                        else -> {
                            assertNotEquals(R.drawable.avatar_transparent, front)
                            assertEquals(R.drawable.avatar_transparent, back)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun getExpressionOverlay_mapsEveryExpression() {
        Expression.entries.forEach { expression ->
            val res = AvatarAssetMapper.getExpressionOverlay(expression)
            assertNotEquals(0, res)
            assertNotEquals(R.drawable.avatar_transparent, res)
        }
    }

    @Test
    fun getOutfitOverlay_mapsEveryOutfitColorAndAgeStage() {
        for (outfit in 0 until AvatarConfig.OUTFIT_COLOR_COUNT) {
            for (stage in AgeStage.entries) {
                val res = AvatarAssetMapper.getOutfitOverlay(outfit, stage)
                assertNotEquals(0, res)
                assertNotEquals(R.drawable.avatar_transparent, res)
            }
        }
    }

    @Test
    fun sanitize_coercesOutOfRangeIndices() {
        val corrupt = AvatarConfig(
            skinTone = -3,
            hairStyle = 99,
            hairColor = -1,
            outfitColor = 50,
            accessoryId = 99,
            facialFeature = -5
        )
        val safe = AvatarAssetMapper.sanitize(corrupt)
        assertTrue(safe.skinTone in 0 until AvatarConfig.SKIN_TONE_COUNT)
        assertTrue(safe.hairStyle in 0 until AvatarConfig.HAIR_STYLE_COUNT)
        assertTrue(safe.hairColor in 0 until AvatarConfig.HAIR_COLOR_COUNT)
        assertTrue(safe.outfitColor in 0 until AvatarConfig.OUTFIT_COLOR_COUNT)
        assertEquals(null, safe.accessoryId)
        assertEquals(null, safe.facialFeature)
    }

    @Test
    fun requiresFallback_onlyForExtremeGarbageValues() {
        assertFalse(AvatarAssetMapper.requiresFallback(AvatarConfig.DEFAULT))
        assertFalse(
            AvatarAssetMapper.requiresFallback(
                AvatarConfig(skinTone = 7, hairStyle = 7, hairColor = 5, outfitColor = 7)
            )
        )
        assertTrue(
            AvatarAssetMapper.requiresFallback(
                AvatarConfig(
                    skinTone = Int.MAX_VALUE,
                    hairStyle = 0,
                    hairColor = 0,
                    outfitColor = 0
                )
            )
        )
    }

    @Test
    fun allKnownDrawableIds_arePositiveResourceIds() {
        AvatarAssetMapper.allKnownDrawableIds().forEach { id ->
            assertTrue(id != 0)
        }
    }

    @Test
    fun resolveAvatarDrawable_includesCoreLayersForDefaultAdult() {
        val layers = resolveAvatarDrawable(
            AvatarConfig.DEFAULT,
            AgeStage.ADULT,
            Expression.HAPPY
        )
        assertTrue(layers.contains(R.drawable.avatar_head_adult))
        assertTrue(layers.contains(R.drawable.avatar_expression_happy))
        assertTrue(layers.contains(R.drawable.avatar_outfit_adult))
    }
}
