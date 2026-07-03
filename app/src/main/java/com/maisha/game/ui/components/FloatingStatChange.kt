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
import kotlinx.coroutines.delay

private const val LARGE_DELTA_THRESHOLD = 25_000
private const val STAGGER_DELAY_MS = 100

data class StatDeltaEvent(
    val type: StatType,
    val delta: Int,
    val id: Long = System.nanoTime(),
    /** When set, shown instead of a raw signed integer (e.g. formatted net-worth swings). */
    val displayText: String? = null
)

@Composable
fun FloatingStatChange(
    event: StatDeltaEvent,
    label: String,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    startDelayMs: Int = 0
) {
    val offsetY = remember(event.id) { Animatable(0f) }
    val alpha = remember(event.id) { Animatable(0f) }

    LaunchedEffect(event.id, startDelayMs) {
        alpha.snapTo(0f)
        offsetY.snapTo(0f)
        if (startDelayMs > 0) {
            delay(startDelayMs.toLong())
        }
        alpha.snapTo(1f)
        val duration = if (kotlin.math.abs(event.delta) >= LARGE_DELTA_THRESHOLD) 1_100 else 750
        offsetY.animateTo(-36f, animationSpec = tween(duration))
        alpha.animateTo(0f, animationSpec = tween(250))
        onFinished()
    }

    val sign = if (event.delta > 0) "+" else ""
    val color = if (event.delta >= 0) SuccessGreen else CoralNegative
    val deltaText = event.displayText ?: "$sign${event.delta}"

    Box(modifier = modifier) {
        Text(
            text = "$deltaText $label",
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
                startDelayMs = index * STAGGER_DELAY_MS,
                modifier = Modifier.offset(y = (index * 18).dp)
            )
        }
    }
}
