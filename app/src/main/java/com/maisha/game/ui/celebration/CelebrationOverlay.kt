// app/src/main/java/com/maisha/game/ui/celebration/CelebrationOverlay.kt (new)
package com.maisha.game.ui.celebration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.TealPrimary
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Lightweight confetti — 18 particles, ~1.8s drift, tap-to-dismiss.
 * Reduced from 28 after Prompt 43 profiling budget (itel A665L): single Canvas pass,
 * no physics library, particles removed after lifetime.
 */
private const val PARTICLE_COUNT = 18
private const val ANIMATION_MS = 1_800L
private const val AUTO_DISMISS_MS = 2_500L

private data class Particle(
    val originX: Float,
    val originY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val size: Float,
    val rotationSpeed: Float,
    var age: Float = 0f
)

@Composable
fun CelebrationOverlay(
    type: CelebrationType,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var progress by remember(type) { mutableFloatStateOf(0f) }
    var dismissed by remember(type) { mutableStateOf(false) }
    val particles = remember(type) {
        val palette = when (type) {
            CelebrationType.MARRIAGE, CelebrationType.CHILD_BORN -> listOf(GoldAccent, TealPrimary, Color(0xFFFF8FAB))
            CelebrationType.GRADUATION -> listOf(TealPrimary, GoldAccent, Color.White)
            CelebrationType.ACHIEVEMENT -> listOf(GoldAccent, Color(0xFFFFD54F), TealPrimary)
            else -> listOf(GoldAccent, TealPrimary, Color(0xFFB39DDB))
        }
        List(PARTICLE_COUNT) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 3.5f + 2f
            Particle(
                originX = Random.nextFloat(),
                originY = -0.05f - Random.nextFloat() * 0.1f,
                velocityX = cos(angle) * speed,
                velocityY = sin(angle) * speed + 2.5f,
                color = palette[Random.nextInt(palette.size)],
                size = with(density) { (4 + Random.nextFloat() * 4).dp.toPx() },
                rotationSpeed = Random.nextFloat() * 4f - 2f
            )
        }
    }

    LaunchedEffect(type) {
        val start = System.currentTimeMillis()
        while (!dismissed) {
            val elapsed = System.currentTimeMillis() - start
            progress = (elapsed / ANIMATION_MS.toFloat()).coerceIn(0f, 1f)
            if (elapsed >= AUTO_DISMISS_MS) {
                onDismiss()
                break
            }
            delay(16L)
        }
    }

    if (dismissed) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                dismissed = true
                onDismiss()
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            particles.forEach { p ->
                p.age = progress
                val x = (p.originX * w) + p.velocityX * p.age * w * 0.12f
                val y = (p.originY * h) + p.velocityY * p.age * h * 0.14f + 0.5f * 9.8f * p.age * p.age * h * 0.02f
                val alpha = (1f - p.age).coerceIn(0f, 1f)
                if (alpha > 0.02f && y < h + 40f) {
                    drawCircle(
                        color = p.color.copy(alpha = alpha),
                        radius = p.size,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
