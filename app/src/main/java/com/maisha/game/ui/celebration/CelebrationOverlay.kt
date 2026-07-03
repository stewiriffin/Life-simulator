// app/src/main/java/com/maisha/game/ui/celebration/CelebrationOverlay.kt (new)
package com.maisha.game.ui.celebration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs
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
    val width: Float,
    val height: Float,
    val initialRotation: Float,
    val rotationSpeed: Float,
    val isStrip: Boolean,
    var age: Float = 0f
)

@Composable
fun CelebrationOverlay(
    type: CelebrationType,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val primary = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.secondary
    val error = MaterialTheme.colorScheme.error
    var progress by remember(type) { mutableFloatStateOf(0f) }
    var dismissed by remember(type) { mutableStateOf(false) }
    val particles = remember(type, primary, accent, error) {
        val palette = listOf(primary, accent, error)
        List(PARTICLE_COUNT) {
            val angle = Random.nextFloat() * (Math.PI * 2).toFloat()
            val speed = Random.nextFloat() * 6f + 1.2f
            val horizontalBias = Random.nextFloat() * 2.4f - 1.2f
            val verticalBoost = Random.nextFloat() * 5f + 1.5f
            val sizeBase = with(density) { (3.5f + Random.nextFloat() * 5f).dp.toPx() }
            Particle(
                originX = 0.15f + Random.nextFloat() * 0.7f,
                originY = -0.08f - Random.nextFloat() * 0.15f,
                velocityX = cos(angle) * speed * horizontalBias,
                velocityY = sin(angle).coerceAtLeast(0.05f) * speed + verticalBoost,
                color = palette[Random.nextInt(palette.size)],
                width = sizeBase * (0.8f + Random.nextFloat() * 0.9f),
                height = sizeBase * (0.35f + Random.nextFloat() * 0.55f),
                initialRotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 720f - 360f,
                isStrip = Random.nextBoolean()
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
                val driftScaleX = w * (0.10f + (p.originX * 0.04f))
                val driftScaleY = h * (0.12f + abs(p.originY) * 0.03f)
                val x = (p.originX * w) + p.velocityX * p.age * driftScaleX
                val y = (p.originY * h) + p.velocityY * p.age * driftScaleY +
                    0.5f * 9.8f * p.age * p.age * h * 0.022f
                val alpha = (1f - p.age * p.age).coerceIn(0f, 1f)
                val rotation = p.initialRotation + p.rotationSpeed * p.age
                if (alpha > 0.02f && y < h + 60f) {
                    rotate(degrees = rotation, pivot = Offset(x, y)) {
                        if (p.isStrip) {
                            drawRect(
                                color = p.color.copy(alpha = alpha),
                                topLeft = Offset(x - p.width / 2f, y - p.height / 2f),
                                size = Size(p.width, p.height)
                            )
                        } else {
                            drawCircle(
                                color = p.color.copy(alpha = alpha),
                                radius = p.width * 0.45f,
                                center = Offset(x, y)
                            )
                        }
                    }
                }
            }
        }
    }
}
