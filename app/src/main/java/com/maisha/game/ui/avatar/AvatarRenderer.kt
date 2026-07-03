// app/src/main/java/com/maisha/game/ui/avatar/AvatarRenderer.kt
package com.maisha.game.ui.avatar

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.model.AgeStage
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Expression
import com.maisha.game.data.model.ageStageFor
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.illustrations.EmptyStateIllustrationView
import com.maisha.game.ui.theme.MaishaTheme

private const val EXPRESSION_CROSSFADE_MS = 200

@Immutable
data class AvatarLayerStack(
    /** Back hair → outfit → neck → head → cheeks (stable unless config/age changes). */
    val behindExpression: List<AvatarLayer>,
    /** Front hair → features → wrinkles → accessories (stable unless config/age changes). */
    val inFrontOfExpression: List<AvatarLayer>
)

/**
 * Resolves stacked drawable resource IDs for [config], [ageStage], and [expression].
 */
fun resolveAvatarDrawable(
    config: AvatarConfig,
    ageStage: AgeStage,
    expression: Expression
): List<Int> {
    val stack = buildAvatarLayerStack(config, ageStage)
    return buildList {
        addAll(stack.behindExpression.map { it.drawableRes })
        add(AvatarAssetMapper.getExpressionOverlay(expression))
        addAll(stack.inFrontOfExpression.map { it.drawableRes })
    }
}

fun resolveAvatarLayers(
    config: AvatarConfig,
    ageStage: AgeStage,
    expression: Expression
): List<AvatarLayer> {
    val stack = buildAvatarLayerStack(config, ageStage)
    return stack.behindExpression +
        AvatarLayer(AvatarAssetMapper.getExpressionOverlay(expression)) +
        stack.inFrontOfExpression
}

fun buildAvatarLayerStack(
    config: AvatarConfig,
    ageStage: AgeStage
): AvatarLayerStack {
    val safe = AvatarAssetMapper.sanitize(config)
    val skin = AvatarAssetMapper.skinTint(safe.skinTone)
    val hair = AvatarAssetMapper.hairTint(safe.hairColor, ageStage, safe.agingDetails)
    val outfit = AvatarAssetMapper.outfitTint(safe.outfitColor)
    val featureTint = when (safe.facialFeature) {
        2, 3 -> hair
        else -> null
    }

    fun visible(res: Int, tint: androidx.compose.ui.graphics.Color? = null): AvatarLayer? =
        if (res == R.drawable.avatar_transparent) null else AvatarLayer(res, tint)

    val behind = listOfNotNull(
        // 1. Back hair
        visible(
            AvatarAssetMapper.getBackHairOverlay(safe.hairStyle, safe.hairColor, ageStage),
            hair
        ),
        // Outfit under the head so clothing never covers the face.
        visible(AvatarAssetMapper.getOutfitOverlay(safe.outfitColor, ageStage), outfit),
        visible(AvatarAssetMapper.getNeckOverlay(ageStage), skin),
        // 2. Base head
        visible(AvatarAssetMapper.getBaseHead(safe.skinTone, ageStage), skin),
        visible(AvatarAssetMapper.getCheeksOverlay(ageStage))
    )

    val front = listOfNotNull(
        // Dynamic accessories on the face, beneath front hair
        visible(AvatarAssetMapper.getFacialHairOverlay(safe.facialHair), hair),
        visible(AvatarAssetMapper.getEyewearOverlay(safe.eyewear)),
        visible(AvatarAssetMapper.getAgingDetailsOverlay(safe.agingDetails, ageStage)),
        // 4. Front hair
        visible(
            AvatarAssetMapper.getHairOverlay(safe.hairStyle, safe.hairColor, ageStage),
            hair
        ),
        visible(AvatarAssetMapper.getFacialFeatureOverlay(safe.facialFeature), featureTint),
        visible(AvatarAssetMapper.getAccessoryOverlay(safe.accessoryId)),
        visible(AvatarAssetMapper.getCaneOverlay(ageStage))
    )

    return AvatarLayerStack(behindExpression = behind, inFrontOfExpression = front)
}

@Composable
fun AvatarImage(
    config: AvatarConfig,
    size: Dp,
    modifier: Modifier = Modifier,
    age: Int = 18,
    expression: Expression = Expression.NEUTRAL,
    forPlayerCharacter: Boolean = false,
    hideFromAccessibility: Boolean = false
) {
    val expressionLabel = expressionAccessibilityLabel(expression)
    val accessibilityDescription = when {
        hideFromAccessibility -> null
        forPlayerCharacter -> stringResource(
            R.string.content_desc_player_avatar_expression,
            expressionLabel
        )
        else -> stringResource(
            R.string.content_desc_avatar_expression,
            expressionLabel
        )
    }
    val accessibilityModifier = if (accessibilityDescription == null) {
        Modifier.clearAndSetSemantics { }
    } else {
        Modifier.clearAndSetSemantics {
            contentDescription = accessibilityDescription
        }
    }

    if (AvatarAssetMapper.requiresFallback(config)) {
        AvatarFallbackSilhouette(
            size = size,
            modifier = modifier.then(accessibilityModifier)
        )
        return
    }

    val stage = ageStageFor(age)
    val safeConfig = remember(config) { AvatarAssetMapper.sanitize(config) }
    val stack = remember(safeConfig, stage) {
        buildAvatarLayerStack(safeConfig, stage)
    }

    Box(
        modifier = modifier
            .then(accessibilityModifier)
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        // 1–2. Back hair, outfit, neck, head, cheeks
        stack.behindExpression.forEach { layer ->
            AvatarLayerImage(layer = layer)
        }

        // 3. Expression with smooth flash transitions (~200ms)
        Crossfade(
            targetState = expression,
            animationSpec = tween(durationMillis = EXPRESSION_CROSSFADE_MS),
            label = "avatarExpression"
        ) { face ->
            AvatarLayerImage(
                layer = AvatarLayer(AvatarAssetMapper.getExpressionOverlay(face))
            )
        }

        // 4–5. Front hair, features, accessories
        stack.inFrontOfExpression.forEach { layer ->
            AvatarLayerImage(layer = layer)
        }
    }
}

@Composable
private fun AvatarLayerImage(layer: AvatarLayer) {
    Image(
        painter = painterResource(id = layer.drawableRes),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
        colorFilter = layer.tint?.let { ColorFilter.tint(it) }
    )
}

@Composable
private fun AvatarFallbackSilhouette(
    size: Dp,
    modifier: Modifier = Modifier
) {
    EmptyStateIllustrationView(
        type = EmptyStateIllustration.FAMILY,
        modifier = modifier,
        size = size
    )
}

@Composable
fun expressionAccessibilityLabel(expression: Expression): String = when (expression) {
    Expression.NEUTRAL -> stringResource(R.string.a11y_expression_neutral)
    Expression.HAPPY -> stringResource(R.string.a11y_expression_happy)
    Expression.SAD -> stringResource(R.string.a11y_expression_sad)
    Expression.ANGRY -> stringResource(R.string.a11y_expression_angry)
    Expression.SURPRISED -> stringResource(R.string.a11y_expression_surprised)
}

@Preview(showBackground = true, name = "Avatar layer grid")
@Composable
private fun AvatarLayerPreviewGrid() {
    MaishaTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AvatarImage(
                    config = AvatarConfig(skinTone = 2, hairStyle = 1, hairColor = 0, outfitColor = 0),
                    size = 72.dp,
                    age = 1,
                    expression = Expression.NEUTRAL
                )
                AvatarImage(
                    config = AvatarConfig(skinTone = 4, hairStyle = 4, hairColor = 1, outfitColor = 2),
                    size = 72.dp,
                    age = 70,
                    expression = Expression.NEUTRAL
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AvatarImage(
                    config = AvatarConfig(
                        skinTone = 1,
                        hairStyle = 2,
                        hairColor = 3,
                        outfitColor = 1,
                        accessoryId = 0
                    ),
                    size = 72.dp,
                    age = 28,
                    expression = Expression.HAPPY
                )
                AvatarImage(
                    config = AvatarConfig(
                        skinTone = 5,
                        hairStyle = 7,
                        hairColor = 0,
                        outfitColor = 4,
                        facialFeature = 2
                    ),
                    size = 72.dp,
                    age = 16,
                    expression = Expression.ANGRY
                )
            }
        }
    }
}
