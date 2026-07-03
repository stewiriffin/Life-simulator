// app/src/main/java/com/maisha/game/ui/components/AchievementUnlockedDialog.kt
package com.maisha.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maisha.game.R
import com.maisha.game.data.model.Achievement
import com.maisha.game.feedback.HapticType
import com.maisha.game.ui.feedback.LocalFeedbackManager
import com.maisha.game.util.achievementDescription
import com.maisha.game.ui.theme.AppIcons
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.TealPrimary
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private const val EXIT_ANIMATION_MS = 280L

@Composable
fun AchievementUnlockedDialog(
    achievement: Achievement,
    countryCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val feedbackManager = LocalFeedbackManager.current
    var visible by remember(achievement.id) { mutableStateOf(false) }
    var pendingDismiss by remember(achievement.id) { mutableStateOf(false) }

    LaunchedEffect(achievement.id) {
        visible = true
        feedbackManager.triggerHaptic(view, HapticType.SUCCESS)
    }

    LaunchedEffect(pendingDismiss) {
        if (pendingDismiss) {
            visible = false
            delay(EXIT_ANIMATION_MS)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { pendingDismiss = true },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(durationMillis = 320)
            ) + fadeIn(animationSpec = tween(durationMillis = 280)),
            exit = slideOutVertically(
                targetOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = EXIT_ANIMATION_MS.toInt())
            ) + fadeOut(animationSpec = tween(durationMillis = EXIT_ANIMATION_MS.toInt()))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AchievementSparkleBurst(modifier = Modifier.size(56.dp))
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = GoldAccent.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AppIcons.forAchievementCategory(achievement.category),
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.achievement_unlocked_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldAccent
                            )
                            Text(
                                text = stringResource(achievement.titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2
                            )
                        }
                    }

                    Text(
                        text = achievementDescription(context, achievement, countryCode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )

                    Button(
                        onClick = { pendingDismiss = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.btn_nice),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementSparkleBurst(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "achievementSparkle")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkleRotation"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparklePulse"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.48f * pulse
        rotate(rotation, pivot = center) {
            val rays = 12
            repeat(rays) { index ->
                val angle = (Math.PI * 2.0 * index / rays).toFloat()
                val start = Offset(
                    x = center.x + cos(angle) * radius * 0.25f,
                    y = center.y + sin(angle) * radius * 0.25f
                )
                val end = Offset(
                    x = center.x + cos(angle) * radius,
                    y = center.y + sin(angle) * radius
                )
                drawLine(
                    color = GoldAccent.copy(alpha = 0.35f + (index % 3) * 0.1f),
                    start = start,
                    end = end,
                    strokeWidth = 3f
                )
            }
        }
        drawCircle(
            color = GoldAccent.copy(alpha = 0.12f * pulse),
            radius = radius * 0.55f,
            center = center
        )
    }
}
