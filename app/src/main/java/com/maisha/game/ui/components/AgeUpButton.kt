// app/src/main/java/com/maisha/game/ui/components/AgeUpButton.kt (modified — spring press animation)
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.NavyDeep

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
        targetValue = if (pressed && canPress) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ageUpScale"
    )

    val containerColor = if (canPress) {
        GoldAccent
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (canPress) NavyDeep else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MaishaSpacing.xl + MaishaSpacing.xl + 4.dp)
            .scale(scale)
            .clip(MaishaRadius.buttonShape)
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
                isLoading -> "Aging..."
                else -> "+ Age Up"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (canPress) contentColor else Color.Unspecified
        )
    }
}
