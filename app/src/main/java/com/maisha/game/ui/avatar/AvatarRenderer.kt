// app/src/main/java/com/maisha/game/ui/avatar/AvatarRenderer.kt (modified — Prompt 26: 8 tones/styles, facialFeature, stronger age/expression)
package com.maisha.game.ui.avatar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.maisha.game.data.model.AgeStage
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Expression
import com.maisha.game.data.model.ageStageFor

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
private val GoldAccent = Color(0xFFF4B942)

@Composable
fun AvatarImage(
    config: AvatarConfig,
    size: Dp,
    modifier: Modifier = Modifier,
    age: Int = 18,
    expression: Expression = Expression.NEUTRAL
) {
    val stage = ageStageFor(age)
    val skin = skinTones[config.skinTone % skinTones.size]
    val hair = hairColors[config.hairColor % hairColors.size]
    val effectiveHair = if (stage == AgeStage.SENIOR) seniorGrey else hair
    val outfit = outfitColors[config.outfitColor % outfitColors.size]

    val headScale = when (stage) {
        AgeStage.BABY -> 1.4f
        AgeStage.CHILD -> 1.18f
        AgeStage.TEEN -> 0.98f
        AgeStage.ADULT -> 1.0f
        AgeStage.SENIOR -> 1.02f
    }
    val bodyScale = when (stage) {
        AgeStage.BABY -> 0.5f
        AgeStage.CHILD -> 0.72f
        AgeStage.TEEN -> 0.88f
        else -> 1.0f
    }

    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        val headRadius = w * 0.28f * headScale
        val headCenterY = when (stage) {
            AgeStage.BABY -> h * 0.44f
            else -> h * 0.36f
        }
        val headCenter = Offset(w / 2f, headCenterY)

        val outfitTop = headCenter.y + headRadius * 0.7f
        val bodyHeight = (h - outfitTop) * bodyScale
        val bodyWidth = when (stage) {
            AgeStage.TEEN -> w * 0.58f
            else -> w * 0.64f
        }
        val bodyLeft = (w - bodyWidth) / 2f
        drawRoundRect(
            color = outfit,
            topLeft = Offset(bodyLeft, outfitTop),
            size = Size(bodyWidth, bodyHeight.coerceAtLeast(headRadius * 0.3f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f)
        )

        if (stage != AgeStage.BABY) {
            drawRect(
                color = skin,
                topLeft = Offset(w * 0.42f, outfitTop - headRadius * 0.15f),
                size = Size(w * 0.16f, headRadius * 0.2f)
            )
        }

        drawCircle(color = skin, radius = headRadius, center = headCenter)
        drawHair(config, headCenter, headRadius, effectiveHair, stage)

        if (stage == AgeStage.BABY) {
            val cheekR = headRadius * 0.1f
            drawCircle(Color(0xFFFFB6C1).copy(alpha = 0.55f), cheekR, Offset(headCenter.x - headRadius * 0.45f, headCenter.y + headRadius * 0.15f))
            drawCircle(Color(0xFFFFB6C1).copy(alpha = 0.55f), cheekR, Offset(headCenter.x + headRadius * 0.45f, headCenter.y + headRadius * 0.15f))
        }

        if (stage == AgeStage.SENIOR) {
            val wrinkleY = headCenter.y - headRadius * 0.15f
            drawArc(
                color = Color(0xFF6D4C41).copy(alpha = 0.35f),
                startAngle = 200f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(headCenter.x - headRadius * 0.55f, wrinkleY),
                size = Size(headRadius * 1.1f, headRadius * 0.35f),
                style = Stroke(width = headRadius * 0.04f)
            )
            drawLine(
                color = Color(0xFF5D4037),
                start = Offset(w * 0.72f, h * 0.55f),
                end = Offset(w * 0.88f, h * 0.92f),
                strokeWidth = w * 0.035f
            )
            drawCircle(Color(0xFF5D4037), w * 0.04f, Offset(w * 0.88f, h * 0.92f))
        }

        drawFaceExpression(headCenter, headRadius, expression, stage)
        drawFacialFeature(config.facialFeature, headCenter, headRadius, effectiveHair)

        when (config.accessoryId) {
            0 -> drawCircle(
                color = Color(0xFF37474F),
                radius = headRadius * 0.75f,
                center = headCenter,
                style = Stroke(width = headRadius * 0.06f)
            )
            1 -> drawCircle(
                color = GoldAccent,
                radius = headRadius * 0.12f,
                center = Offset(headCenter.x, headCenter.y - headRadius * 0.55f)
            )
            2 -> drawRoundRect(
                color = Color(0xFF37474F),
                topLeft = Offset(headCenter.x - headRadius * 0.75f, headCenter.y + headRadius * 0.02f),
                size = Size(headRadius * 1.5f, headRadius * 0.22f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(headRadius * 0.06f, headRadius * 0.06f),
                style = Stroke(width = headRadius * 0.05f)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHair(
    config: AvatarConfig,
    headCenter: Offset,
    headRadius: Float,
    hair: Color,
    stage: AgeStage
) {
    when (config.hairStyle % AvatarConfig.HAIR_STYLE_COUNT) {
        0 -> drawArc(
            color = hair,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(headCenter.x - headRadius, headCenter.y - headRadius),
            size = Size(headRadius * 2f, headRadius * 2f)
        )
        1 -> drawCircle(
            color = hair,
            radius = headRadius * 1.05f,
            center = headCenter.copy(y = headCenter.y - headRadius * 0.05f),
            style = Stroke(width = headRadius * 0.35f)
        )
        2 -> {
            val path = Path().apply {
                moveTo(headCenter.x - headRadius, headCenter.y)
                quadraticTo(
                    headCenter.x, headCenter.y - headRadius * 1.4f,
                    headCenter.x + headRadius, headCenter.y
                )
                lineTo(headCenter.x + headRadius * 0.8f, headCenter.y + headRadius * 0.3f)
                lineTo(headCenter.x - headRadius * 0.8f, headCenter.y + headRadius * 0.3f)
                close()
            }
            drawPath(path, hair)
        }
        3 -> drawRoundRect(
            color = hair,
            topLeft = Offset(headCenter.x - headRadius * 0.95f, headCenter.y - headRadius * 1.05f),
            size = Size(headRadius * 1.9f, headRadius * 0.45f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(headRadius * 0.15f, headRadius * 0.15f)
        )
        4 -> {
            drawArc(
                color = hair, startAngle = 160f, sweepAngle = 220f, useCenter = true,
                topLeft = Offset(headCenter.x - headRadius * 1.05f, headCenter.y - headRadius * 1.1f),
                size = Size(headRadius * 2.1f, headRadius * 2.2f)
            )
            drawRect(
                color = hair,
                topLeft = Offset(headCenter.x - headRadius * 0.85f, headCenter.y),
                size = Size(headRadius * 1.7f, headRadius * 0.55f)
            )
        }
        5 -> {
            val braidX = listOf(-0.35f, 0f, 0.35f)
            braidX.forEach { offset ->
                drawRoundRect(
                    color = hair,
                    topLeft = Offset(headCenter.x + headRadius * offset - headRadius * 0.08f, headCenter.y - headRadius * 0.9f),
                    size = Size(headRadius * 0.16f, headRadius * 1.1f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(headRadius * 0.06f, headRadius * 0.06f)
                )
            }
        }
        6 -> Unit // bald
        else -> drawRoundRect(
            color = hair,
            topLeft = Offset(headCenter.x - headRadius * 1.05f, headCenter.y - headRadius * 1.25f),
            size = Size(headRadius * 2.1f, headRadius * 1.35f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(headRadius * 0.35f, headRadius * 0.35f)
        )
    }
    if (stage == AgeStage.SENIOR && config.hairStyle % AvatarConfig.HAIR_STYLE_COUNT != 6) {
        drawArc(
            color = seniorGrey.copy(alpha = 0.5f),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(headCenter.x - headRadius * 1.1f, headCenter.y - headRadius * 1.2f),
            size = Size(headRadius * 2.2f, headRadius * 2.2f),
            style = Stroke(width = headRadius * 0.12f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFacialFeature(
    facialFeature: Int?,
    headCenter: Offset,
    headRadius: Float,
    hairColor: Color
) {
    when (facialFeature) {
        1 -> {
            val eyeY = headCenter.y + headRadius * 0.05f
            drawRoundRect(
                color = Color(0xFF37474F),
                topLeft = Offset(headCenter.x - headRadius * 0.7f, eyeY - headRadius * 0.06f),
                size = Size(headRadius * 1.4f, headRadius * 0.2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(headRadius * 0.05f, headRadius * 0.05f),
                style = Stroke(width = headRadius * 0.05f)
            )
        }
        2 -> drawArc(
            color = hairColor,
            startAngle = 20f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(headCenter.x - headRadius * 0.55f, headCenter.y + headRadius * 0.15f),
            size = Size(headRadius * 1.1f, headRadius * 0.75f),
            style = Stroke(width = headRadius * 0.14f)
        )
        3 -> drawArc(
            color = hairColor,
            startAngle = 10f, sweepAngle = 160f, useCenter = false,
            topLeft = Offset(headCenter.x - headRadius * 0.35f, headCenter.y + headRadius * 0.22f),
            size = Size(headRadius * 0.7f, headRadius * 0.22f),
            style = Stroke(width = headRadius * 0.07f)
        )
        4 -> {
            val freckleR = headRadius * 0.035f
            listOf(
                Offset(headCenter.x - headRadius * 0.2f, headCenter.y + headRadius * 0.12f),
                Offset(headCenter.x + headRadius * 0.15f, headCenter.y + headRadius * 0.18f),
                Offset(headCenter.x - headRadius * 0.05f, headCenter.y + headRadius * 0.28f)
            ).forEach { drawCircle(Color(0xFFC68642).copy(alpha = 0.65f), freckleR, it) }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFaceExpression(
    headCenter: Offset,
    headRadius: Float,
    expression: Expression,
    stage: AgeStage
) {
    val eyeY = headCenter.y + headRadius * 0.02f
    val eyeOffset = headRadius * 0.32f
    val eyeR = headRadius * 0.08f
    val dark = Color(0xFF2A2A2A)
    val strokeW = headRadius * 0.055f

    when (expression) {
        Expression.HAPPY -> {
            drawArc(
                color = dark, startAngle = 0f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(headCenter.x - eyeOffset - eyeR, eyeY - eyeR * 0.5f),
                size = Size(eyeR * 2.2f, eyeR * 2.2f),
                style = Stroke(width = strokeW)
            )
            drawArc(
                color = dark, startAngle = 0f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(headCenter.x + eyeOffset - eyeR, eyeY - eyeR * 0.5f),
                size = Size(eyeR * 2.2f, eyeR * 2.2f),
                style = Stroke(width = strokeW)
            )
            drawArc(
                color = dark, startAngle = 15f, sweepAngle = 150f, useCenter = false,
                topLeft = Offset(headCenter.x - headRadius * 0.28f, headCenter.y + headRadius * 0.15f),
                size = Size(headRadius * 0.56f, headRadius * 0.32f),
                style = Stroke(width = strokeW)
            )
        }
        Expression.SAD -> {
            drawCircle(dark, eyeR, Offset(headCenter.x - eyeOffset, eyeY))
            drawCircle(dark, eyeR, Offset(headCenter.x + eyeOffset, eyeY))
            drawArc(
                color = dark, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(headCenter.x - headRadius * 0.24f, headCenter.y + headRadius * 0.3f),
                size = Size(headRadius * 0.48f, headRadius * 0.24f),
                style = Stroke(width = strokeW)
            )
        }
        Expression.ANGRY -> {
            drawLineAngryBrow(headCenter, headRadius, dark, left = true)
            drawLineAngryBrow(headCenter, headRadius, dark, left = false)
            drawCircle(dark, eyeR, Offset(headCenter.x - eyeOffset, eyeY + eyeR * 0.3f))
            drawCircle(dark, eyeR, Offset(headCenter.x + eyeOffset, eyeY + eyeR * 0.3f))
            drawLine(
                color = dark,
                start = Offset(headCenter.x - headRadius * 0.14f, headCenter.y + headRadius * 0.35f),
                end = Offset(headCenter.x + headRadius * 0.14f, headCenter.y + headRadius * 0.4f),
                strokeWidth = strokeW
            )
        }
        Expression.SURPRISED -> {
            drawCircle(dark, eyeR * 1.35f, Offset(headCenter.x - eyeOffset, eyeY))
            drawCircle(dark, eyeR * 1.35f, Offset(headCenter.x + eyeOffset, eyeY))
            drawCircle(
                color = Color(0xFF8B4513),
                radius = headRadius * 0.12f,
                center = Offset(headCenter.x, headCenter.y + headRadius * 0.32f),
                style = Stroke(width = strokeW)
            )
        }
        Expression.NEUTRAL -> {
            drawCircle(dark, eyeR, Offset(headCenter.x - eyeOffset, eyeY))
            drawCircle(dark, eyeR, Offset(headCenter.x + eyeOffset, eyeY))
            drawLine(
                color = dark.copy(alpha = 0.7f),
                start = Offset(headCenter.x - headRadius * 0.12f, headCenter.y + headRadius * 0.3f),
                end = Offset(headCenter.x + headRadius * 0.12f, headCenter.y + headRadius * 0.3f),
                strokeWidth = strokeW
            )
        }
    }

    if (stage == AgeStage.BABY && expression == Expression.NEUTRAL) {
        drawCircle(
            Color(0xFFE57373).copy(alpha = 0.5f),
            headRadius * 0.06f,
            Offset(headCenter.x, headCenter.y + headRadius * 0.28f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLineAngryBrow(
    headCenter: Offset,
    headRadius: Float,
    color: Color,
    left: Boolean
) {
    val sign = if (left) -1f else 1f
    val cx = headCenter.x + sign * headRadius * 0.32f
    drawLine(
        color = color,
        start = Offset(cx - sign * headRadius * 0.12f, headCenter.y - headRadius * 0.08f),
        end = Offset(cx + sign * headRadius * 0.08f, headCenter.y - headRadius * 0.02f),
        strokeWidth = headRadius * 0.055f
    )
}
