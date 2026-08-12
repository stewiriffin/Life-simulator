// app/src/main/java/com/maisha/game/ui/celebration/CelebrationOverlay.kt
package com.maisha.game.ui.celebration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.ui.theme.AccentPink
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.StatHappiness
import com.maisha.game.ui.theme.StatHealth
import com.maisha.game.ui.theme.StatSmarts
import com.maisha.game.ui.theme.SuccessGreen
import com.maisha.game.ui.theme.TealLight
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Lightweight confetti — 18 particles, ~1.8s drift, tap-to-dismiss.
 * Palette and banner copy vary by [CelebrationType] for clearer game juice.
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
    var progress by remember(type) { mutableFloatStateOf(0f) }
    var dismissed by remember(type) { mutableStateOf(false) }
    val palette = remember(type) { paletteFor(type) }
    val banner = stringResource(bannerRes(type))
    val particles = remember(type, palette) {
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
            },
        contentAlignment = Alignment.Center
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
        Text(
            text = banner,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.45f),
                    offset = Offset(0f, 2f),
                    blurRadius = 8f
                )
            ),
            color = palette.first(),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

private fun paletteFor(type: CelebrationType): List<Color> = when (type) {
    CelebrationType.MARRIAGE -> listOf(AccentPink, GoldAccent, Color(0xFFFFF0F5))
    CelebrationType.CHILD_BORN -> listOf(AccentPink, TealLight, StatHappiness)
    CelebrationType.ACHIEVEMENT -> listOf(GoldAccent, TealLight, StatSmarts)
    CelebrationType.GRADUATION -> listOf(StatSmarts, GoldAccent, TealLight)
    CelebrationType.PROMOTION -> listOf(GoldAccent, SuccessGreen, StatSmarts)
    CelebrationType.YEAR_QUEST -> listOf(TealLight, GoldAccent, SuccessGreen)
    CelebrationType.AGE_MILESTONE_18 -> listOf(TealLight, StatHappiness, GoldAccent)
    CelebrationType.AGE_MILESTONE_50 -> listOf(GoldAccent, StatHealth, TealLight)
    CelebrationType.AGE_MILESTONE_100 -> listOf(GoldAccent, AccentPink, StatSmarts)
}

private fun bannerRes(type: CelebrationType): Int = when (type) {
    CelebrationType.MARRIAGE -> R.string.celebration_marriage
    CelebrationType.CHILD_BORN -> R.string.celebration_child_born
    CelebrationType.ACHIEVEMENT -> R.string.celebration_achievement
    CelebrationType.GRADUATION -> R.string.celebration_graduation
    CelebrationType.PROMOTION -> R.string.celebration_promotion
    CelebrationType.YEAR_QUEST -> R.string.celebration_year_quest
    CelebrationType.AGE_MILESTONE_18 -> R.string.celebration_age_18
    CelebrationType.AGE_MILESTONE_50 -> R.string.celebration_age_50
    CelebrationType.AGE_MILESTONE_100 -> R.string.celebration_age_100
}
