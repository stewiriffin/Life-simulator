package com.maisha.game.ui.avatar

import com.maisha.game.R
import com.maisha.game.data.model.AgeStage
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Expression
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarLayerResolverTest {

    @Test
    fun resolveAvatarDrawable_includesAgeHeadAndExpression() {
        val config = AvatarConfig.DEFAULT
        val layers = resolveAvatarDrawable(config, AgeStage.ADULT, Expression.HAPPY)
        assertTrue(layers.contains(R.drawable.avatar_head_adult))
        assertTrue(layers.contains(R.drawable.avatar_expression_happy))
        assertTrue(layers.contains(R.drawable.avatar_outfit_adult))
    }

    @Test
    fun resolveAvatarDrawable_swapsExpressionLayers() {
        val config = AvatarConfig.DEFAULT
        val happy = resolveAvatarDrawable(config, AgeStage.ADULT, Expression.HAPPY)
        val angry = resolveAvatarDrawable(config, AgeStage.ADULT, Expression.ANGRY)
        assertTrue(happy.contains(R.drawable.avatar_expression_happy))
        assertFalse(happy.contains(R.drawable.avatar_expression_angry))
        assertTrue(angry.contains(R.drawable.avatar_expression_angry))
        assertFalse(angry.contains(R.drawable.avatar_expression_happy))
    }

    @Test
    fun resolveAvatarDrawable_seniorAddsWrinklesAndCane() {
        val layers = resolveAvatarDrawable(
            AvatarConfig.DEFAULT,
            AgeStage.SENIOR,
            Expression.NEUTRAL
        )
        assertTrue(layers.contains(R.drawable.avatar_head_senior))
        assertTrue(layers.contains(R.drawable.avatar_aging_wrinkles))
        assertTrue(layers.contains(R.drawable.avatar_cane_senior))
    }

    @Test
    fun resolveAvatarDrawable_babyAddsCheeksAndSkipsNeck() {
        val layers = resolveAvatarDrawable(
            AvatarConfig.DEFAULT,
            AgeStage.BABY,
            Expression.NEUTRAL
        )
        assertTrue(layers.contains(R.drawable.avatar_head_baby))
        assertTrue(layers.contains(R.drawable.avatar_cheeks_baby))
        assertFalse(layers.contains(R.drawable.avatar_neck))
    }

    @Test
    fun resolveAvatarDrawable_baldHairStyleOmitsHairLayer() {
        val bald = AvatarConfig.DEFAULT.copy(hairStyle = 6)
        val layers = resolveAvatarLayers(bald, AgeStage.ADULT, Expression.NEUTRAL)
        val hairIds = setOf(
            R.drawable.avatar_hair_style0,
            R.drawable.avatar_hair_style1,
            R.drawable.avatar_hair_style2,
            R.drawable.avatar_hair_style3,
            R.drawable.avatar_hair_style4,
            R.drawable.avatar_hair_style5,
            R.drawable.avatar_hair_style7
        )
        assertFalse(layers.any { it.drawableRes in hairIds })
    }

    @Test
    fun resolveAvatarLayers_appliesSkinTintToHead() {
        val config = AvatarConfig.DEFAULT.copy(skinTone = 0)
        val layers = resolveAvatarLayers(config, AgeStage.ADULT, Expression.NEUTRAL)
        val head = layers.first { it.drawableRes == R.drawable.avatar_head_adult }
        assertTrue(head.tint != null)
    }

    @Test
    fun buildAvatarLayerStack_keepsExpressionOutOfStableLayers() {
        val stack = buildAvatarLayerStack(AvatarConfig.DEFAULT, AgeStage.ADULT)
        val expressionIds = Expression.entries.map { AvatarAssetMapper.getExpressionOverlay(it) }
        assertFalse(stack.behindExpression.any { it.drawableRes in expressionIds })
        assertFalse(stack.inFrontOfExpression.any { it.drawableRes in expressionIds })
    }
}
