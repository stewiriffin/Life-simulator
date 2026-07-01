// app/src/main/java/com/maisha/game/ui/components/AgeUpButton.kt (modified — spring press animation)
package com.maisha.game.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ageUpScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(MaishaSpacing.xl + MaishaSpacing.xl + 4.dp)
            .scale(scale),
        shape = MaishaRadius.buttonShape,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = MaishaSpacing.sm - 2.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = GoldAccent,
            contentColor = NavyDeep,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = when {
                isLoading -> "Aging..."
                else -> "+ Age Up"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (enabled) NavyDeep else Color.Unspecified
        )
    }
}
