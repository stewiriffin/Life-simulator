package com.maisha.game.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.feedback.HapticType
import com.maisha.game.ui.feedback.LocalFeedbackManager
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.LifeGreen
import com.maisha.game.ui.theme.TealPrimary

enum class ActionCardAccent {
    DEFAULT,
    CARE,
    RISK,
    GOLD
}

@Composable
fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    metaLabel: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: ActionCardAccent = ActionCardAccent.DEFAULT,
    questHint: String? = null
) {
    val view = LocalView.current
    val feedbackManager = LocalFeedbackManager.current
    val iconTint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        accent == ActionCardAccent.RISK -> CoralNegative
        accent == ActionCardAccent.CARE -> LifeGreen
        accent == ActionCardAccent.GOLD -> GoldAccent
        else -> TealPrimary
    }
    val borderColor = when {
        !enabled -> Color.Transparent
        accent == ActionCardAccent.RISK -> CoralNegative.copy(alpha = 0.45f)
        accent == ActionCardAccent.CARE -> LifeGreen.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.5.dp, borderColor, shape)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = enabled) {
                feedbackManager.triggerHaptic(view, HapticType.LIGHT_TAP)
                onClick()
            },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color.White else Color.White.copy(alpha = 0.72f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaishaIcon(
                icon = icon,
                contentDescription = null,
                size = 32.dp,
                modifier = Modifier
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 1f else 0.55f
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                metaLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabled) GoldAccent else GoldAccent.copy(alpha = 0.45f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (questHint != null && enabled) {
                    Text(
                        text = questHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = LifeGreen,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
