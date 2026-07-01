// app/src/main/java/com/maisha/game/ui/components/FloatingStatChange.kt (new)
package com.maisha.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.SuccessGreen
import kotlin.math.roundToInt

data class StatDeltaEvent(
    val type: StatType,
    val delta: Int,
    val id: Long = System.nanoTime()
)

@Composable
fun FloatingStatChange(
    event: StatDeltaEvent,
    label: String,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val offsetY = remember(event.id) { Animatable(0f) }
    val alpha = remember(event.id) { Animatable(1f) }

    LaunchedEffect(event.id) {
        offsetY.animateTo(-36f, animationSpec = tween(750))
        alpha.animateTo(0f, animationSpec = tween(250))
        onFinished()
    }

    val sign = if (event.delta > 0) "+" else ""
    val color = if (event.delta >= 0) SuccessGreen else CoralNegative

    Box(modifier = modifier) {
        Text(
            text = "$sign${event.delta} $label",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .alpha(alpha.value)
        )
    }
}

@Composable
fun FloatingStatChangeLayer(
    events: List<StatDeltaEvent>,
    onEventFinished: (Long) -> Unit,
    statLabel: @Composable (StatType) -> String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        events.forEachIndexed { index, event ->
            FloatingStatChange(
                event = event,
                label = statLabel(event.type),
                onFinished = { onEventFinished(event.id) },
                modifier = Modifier.offset(y = (index * 18).dp)
            )
        }
    }
}
