package com.maisha.game.ui.avatar

import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Expression
import com.maisha.game.data.model.EyewearStyle
import com.maisha.game.data.model.FacialHairStyle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiceBearAvatarUrlTest {

    @Test
    fun build_usesLoreleiPngEndpointAndStableSeed() {
        val config = AvatarConfig(skinTone = 5, hairStyle = 2, hairColor = 1, outfitColor = 0)
        val url = DiceBearAvatarUrl.build(
            config = config,
            age = 28,
            expression = Expression.HAPPY,
            seed = "amina-otieno",
            sizePx = 128
        )
        assertTrue(url.startsWith("https://api.dicebear.com/9.x/lorelei/png?"))
        assertTrue(url.contains("seed=amina-otieno"))
        assertTrue(url.contains("skinColor=8d5524"))
        assertTrue(url.contains("mouthVariant=happy01"))
        assertTrue(url.contains("radius=50"))
        assertTrue(url.contains("backgroundColor=152238"))
    }

    @Test
    fun build_mapsAccessoriesAndSeniorHair() {
        val config = AvatarConfig(
            skinTone = 2,
            hairStyle = 1,
            hairColor = 0,
            outfitColor = 3,
            facialHair = FacialHairStyle.BEARD,
            eyewear = EyewearStyle.GLASSES
        )
        val url = DiceBearAvatarUrl.build(config, age = 72, expression = Expression.NEUTRAL)
        assertTrue(url.contains("beardProbability=100"))
        assertTrue(url.contains("beardVariant=variant02"))
        assertTrue(url.contains("glassesProbability=100"))
        assertTrue(url.contains("hairColor=b0b0b0"))
    }

    @Test
    fun seedFromConfig_isDeterministic() {
        val config = AvatarConfig(skinTone = 1, hairStyle = 3, hairColor = 2, outfitColor = 4)
        val a = DiceBearAvatarUrl.seedFromConfig(config)
        val b = DiceBearAvatarUrl.seedFromConfig(config)
        assertTrue(a == b)
        assertFalse(a.isBlank())
    }
}
