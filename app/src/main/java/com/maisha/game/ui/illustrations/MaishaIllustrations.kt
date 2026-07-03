// app/src/main/java/com/maisha/game/ui/illustrations/MaishaIllustrations.kt (new)
package com.maisha.game.ui.illustrations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.NavyElevated
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.ui.theme.TealLight

/**
 * Simple flat placeholder illustrations (2–3 theme colors, minimal detail).
 * Replace Canvas compositions with commissioned SVG art later — keep enum names stable.
 */
enum class OnboardingIllustration {
    WELCOME,
    AGE_UP,
    CHOICES,
    WORLD,
    READY
}

enum class EmptyStateIllustration {
    FAMILY,
    ASSETS,
    ACTIONS,
    ACHIEVEMENTS,
    RETIRED
}

@Composable
fun OnboardingIllustrationView(
    type: OnboardingIllustration,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        when (type) {
            OnboardingIllustration.WELCOME -> {
                drawCircle(TealPrimary.copy(alpha = 0.25f), radius = w * 0.38f, center = Offset(cx, cy))
                drawCircle(TealPrimary, radius = w * 0.28f, center = Offset(cx, cy), style = Stroke(width = 3f))
                drawArc(
                    color = GoldAccent,
                    startAngle = -30f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(cx - w * 0.2f, cy - w * 0.2f),
                    size = Size(w * 0.4f, w * 0.4f),
                    style = Stroke(width = 4f)
                )
            }
            OnboardingIllustration.AGE_UP -> {
                drawRoundRect(
                    color = NavyElevated,
                    topLeft = Offset(w * 0.2f, h * 0.15f),
                    size = Size(w * 0.6f, h * 0.7f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                drawRoundRect(
                    color = GoldAccent,
                    topLeft = Offset(w * 0.28f, h * 0.62f),
                    size = Size(w * 0.44f, h * 0.12f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawCircle(TealLight, radius = w * 0.1f, center = Offset(cx, cy - h * 0.08f))
            }
            OnboardingIllustration.CHOICES -> {
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.75f)
                    lineTo(cx, h * 0.35f)
                    lineTo(w * 0.35f, h * 0.75f)
                    moveTo(cx, h * 0.35f)
                    lineTo(w * 0.65f, h * 0.75f)
                    moveTo(cx, h * 0.35f)
                    lineTo(w * 0.85f, h * 0.75f)
                }
                drawPath(path, TealPrimary, style = Stroke(width = 4f))
                listOf(0.15f, 0.35f, 0.65f, 0.85f).forEach { x ->
                    drawCircle(GoldAccent, radius = 8f, center = Offset(w * x, h * 0.75f))
                }
                drawCircle(TealLight, radius = 10f, center = Offset(cx, h * 0.35f))
            }
            OnboardingIllustration.WORLD -> {
                drawCircle(TealPrimary.copy(alpha = 0.2f), radius = w * 0.35f, center = Offset(cx, cy))
                drawLine(
                    color = TealPrimary,
                    start = Offset(cx - w * 0.25f, cy),
                    end = Offset(cx + w * 0.25f, cy),
                    strokeWidth = 2f
                )
                drawLine(
                    color = TealPrimary,
                    start = Offset(cx, cy - w * 0.25f),
                    end = Offset(cx, cy + w * 0.25f),
                    strokeWidth = 2f
                )
                drawCircle(GoldAccent, radius = 6f, center = Offset(w * 0.3f, h * 0.35f))
                drawCircle(GoldAccent, radius = 6f, center = Offset(w * 0.7f, h * 0.55f))
            }
            OnboardingIllustration.READY -> {
                val star = Path().apply {
                    moveTo(cx, h * 0.12f)
                    lineTo(cx + w * 0.08f, cy)
                    lineTo(cx + w * 0.22f, cy)
                    lineTo(cx + w * 0.1f, cy + h * 0.12f)
                    lineTo(cx + w * 0.14f, cy + h * 0.28f)
                    lineTo(cx, cy + h * 0.18f)
                    lineTo(cx - w * 0.14f, cy + h * 0.28f)
                    lineTo(cx - w * 0.1f, cy + h * 0.12f)
                    lineTo(cx - w * 0.22f, cy)
                    lineTo(cx - w * 0.08f, cy)
                    close()
                }
                drawPath(star, GoldAccent)
                drawCircle(TealPrimary.copy(alpha = 0.3f), radius = w * 0.32f, center = Offset(cx, cy + h * 0.08f))
            }
        }
    }
}

@Composable
fun EmptyStateIllustrationView(
    type: EmptyStateIllustration,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        val cx = w / 2f
        when (type) {
            EmptyStateIllustration.FAMILY -> {
                drawCircle(TealPrimary.copy(alpha = 0.2f), radius = w * 0.14f, center = Offset(w * 0.35f, h * 0.35f))
                drawCircle(TealPrimary.copy(alpha = 0.2f), radius = w * 0.14f, center = Offset(w * 0.65f, h * 0.35f))
                drawArc(
                    color = TealPrimary,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.22f, h * 0.42f),
                    size = Size(w * 0.56f, h * 0.35f),
                    style = Stroke(width = 3f)
                )
                val heart = Path().apply {
                    moveTo(cx, h * 0.78f)
                    cubicTo(cx - w * 0.12f, h * 0.62f, cx - w * 0.2f, h * 0.68f, cx, h * 0.82f)
                    cubicTo(cx + w * 0.2f, h * 0.68f, cx + w * 0.12f, h * 0.62f, cx, h * 0.78f)
                }
                drawPath(heart, GoldAccent)
            }
            EmptyStateIllustration.ASSETS -> {
                drawRoundRect(
                    NavyElevated,
                    topLeft = Offset(w * 0.2f, h * 0.3f),
                    size = Size(w * 0.6f, h * 0.45f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawArc(
                    GoldAccent,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.2f, h * 0.12f),
                    size = Size(w * 0.6f, h * 0.28f),
                    style = Stroke(width = 4f)
                )
                drawCircle(TealLight, radius = w * 0.08f, center = Offset(cx, h * 0.72f))
            }
            EmptyStateIllustration.ACTIONS -> {
                drawCircle(TealPrimary.copy(alpha = 0.2f), radius = w * 0.35f, center = Offset(cx, h * 0.5f))
                drawCircle(TealPrimary, radius = w * 0.35f, center = Offset(cx, h * 0.5f), style = Stroke(width = 3f))
                drawLine(
                    GoldAccent,
                    Offset(w * 0.32f, h * 0.52f),
                    Offset(w * 0.46f, h * 0.66f),
                    strokeWidth = 4f
                )
                drawLine(
                    GoldAccent,
                    Offset(w * 0.46f, h * 0.66f),
                    Offset(w * 0.68f, h * 0.38f),
                    strokeWidth = 4f
                )
            }
            EmptyStateIllustration.ACHIEVEMENTS -> {
                val trophy = Path().apply {
                    moveTo(w * 0.3f, h * 0.25f)
                    lineTo(w * 0.7f, h * 0.25f)
                    lineTo(w * 0.65f, h * 0.5f)
                    quadraticTo(cx, h * 0.62f, w * 0.35f, h * 0.5f)
                    close()
                    moveTo(cx, h * 0.55f)
                    lineTo(cx, h * 0.72f)
                    lineTo(w * 0.4f, h * 0.78f)
                    lineTo(w * 0.6f, h * 0.78f)
                    lineTo(cx, h * 0.72f)
                }
                drawPath(trophy, TealPrimary.copy(alpha = 0.55f))
                drawPath(trophy, TealPrimary, style = Stroke(width = 2.5f))
            }
            EmptyStateIllustration.RETIRED -> {
                drawCircle(
                    GoldAccent.copy(alpha = 0.35f),
                    radius = w * 0.22f,
                    center = Offset(w * 0.72f, h * 0.22f)
                )
                drawRoundRect(
                    TealPrimary.copy(alpha = 0.25f),
                    topLeft = Offset(w * 0.18f, h * 0.48f),
                    size = Size(w * 0.64f, h * 0.32f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawLine(
                    TealPrimary,
                    Offset(w * 0.22f, h * 0.52f),
                    Offset(w * 0.78f, h * 0.52f),
                    strokeWidth = 3f
                )
                drawLine(
                    TealPrimary,
                    Offset(w * 0.28f, h * 0.68f),
                    Offset(w * 0.72f, h * 0.68f),
                    strokeWidth = 3f
                )
                drawCircle(TealLight.copy(alpha = 0.5f), radius = w * 0.06f, center = Offset(w * 0.32f, h * 0.38f))
            }
        }
    }
}
