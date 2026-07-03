package com.maisha.game.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarConfigTest {

    @Test
    fun default_holdsExpectedIndices() {
        val config = AvatarConfig.DEFAULT
        assertEquals(3, config.skinTone)
        assertEquals(1, config.hairStyle)
        assertEquals(2, config.hairColor)
        assertEquals(3, config.outfitColor)
        assertEquals(null, config.accessoryId)
        assertEquals(null, config.facialFeature)
        assertEquals(null, config.facialHair)
        assertEquals(null, config.eyewear)
        assertEquals(null, config.agingDetails)
    }

    @Test
    fun random_staysWithinConfiguredRanges() {
        repeat(40) {
            val config = AvatarConfig.random()
            assertTrue(config.skinTone in 0 until AvatarConfig.SKIN_TONE_COUNT)
            assertTrue(config.hairStyle in 0 until AvatarConfig.HAIR_STYLE_COUNT)
            assertTrue(config.hairColor in 0 until AvatarConfig.HAIR_COLOR_COUNT)
            assertTrue(config.outfitColor in 0 until AvatarConfig.OUTFIT_COLOR_COUNT)
            config.accessoryId?.let {
                assertTrue(it in 0 until AvatarConfig.ACCESSORY_COUNT)
            }
            config.facialFeature?.let {
                assertTrue(it in 0 until AvatarConfig.FACIAL_FEATURE_COUNT)
            }
        }
    }

    @Test
    fun copy_preservesUnchangedFields() {
        val original = AvatarConfig.DEFAULT
        val updated = original.copy(hairStyle = 5, accessoryId = 1)
        assertEquals(original.skinTone, updated.skinTone)
        assertEquals(5, updated.hairStyle)
        assertEquals(1, updated.accessoryId)
        assertNotEquals(original, updated)
    }

    @Test
    fun ageStageFor_mapsAgeBands() {
        assertEquals(AgeStage.BABY, ageStageFor(0))
        assertEquals(AgeStage.BABY, ageStageFor(2))
        assertEquals(AgeStage.CHILD, ageStageFor(8))
        assertEquals(AgeStage.TEEN, ageStageFor(15))
        assertEquals(AgeStage.ADULT, ageStageFor(40))
        assertEquals(AgeStage.SENIOR, ageStageFor(60))
        assertEquals(AgeStage.SENIOR, ageStageFor(90))
    }
}
