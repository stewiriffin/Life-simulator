// app/src/main/java/com/maisha/game/ui/components/AgeUpButton.kt
package com.maisha.game.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.ui.theme.LifeGreen
import com.maisha.game.ui.theme.LifeGreenPressed
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.MaishaSpacing

/**
 * Primary ritual CTA — green “Live Another Year” pill from the Figma Make UI reference.
 */
@Composable
fun AgeUpButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val canPress = enabled && !isLoading
    val scale by animateFloatAsState(
        targetValue = if (pressed && canPress) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ageUpScale"
    )

    val containerColor = when {
        !canPress -> MaterialTheme.colorScheme.surfaceVariant
        pressed -> LifeGreenPressed
        else -> LifeGreen
    }
    val contentColor = if (canPress) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MaishaSpacing.xl + MaishaSpacing.xl + 6.dp)
            .scale(scale)
            .then(
                if (canPress && !pressed) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = MaishaRadius.ageUpShape,
                        spotColor = LifeGreen.copy(alpha = 0.45f),
                        ambientColor = LifeGreen.copy(alpha = 0.25f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(MaishaRadius.ageUpShape)
            .background(containerColor)
            .semantics { role = Role.Button }
            .pointerInput(canPress, onClick) {
                if (!canPress) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    try {
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            onClick()
                        }
                    } finally {
                        pressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when {
                isLoading -> stringResource(R.string.btn_aging)
                else -> stringResource(R.string.btn_live_another_year)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = contentColor
        )
    }
}
