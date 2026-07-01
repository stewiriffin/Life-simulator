// app/src/main/java/com/maisha/game/ui/components/AppLoadingIndicator.kt (new)
package com.maisha.game.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.TealPrimary

/**
 * Branded pulsing arc spinner — lighter than a full Material circular indicator
 * on budget devices (single arc, no layered indeterminate animation).
 */
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val transition = rememberInfiniteTransition(label = "appLoading")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadingSweep"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadingPulse"
    )

    Canvas(modifier = modifier.size(size)) {
        val stroke = 3.5.dp.toPx() * pulse
        val diameter = this.size.minDimension - stroke
        val topLeft = Offset(stroke / 2f, stroke / 2f)
        val arcSize = Size(diameter, diameter)
        drawArc(
            color = TealPrimary.copy(alpha = 0.25f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            topLeft = topLeft,
            size = arcSize
        )
        drawArc(
            color = GoldAccent,
            startAngle = -90f + sweep * 360f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            topLeft = topLeft,
            size = arcSize
        )
    }
}
