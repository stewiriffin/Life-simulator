// app/src/main/java/com/maisha/game/ui/components/ActionCard.kt
package com.maisha.game.ui.components

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.feedback.HapticType
import com.maisha.game.ui.feedback.LocalFeedbackManager
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.TealPrimary

@Composable
fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    metaLabel: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val feedbackManager = LocalFeedbackManager.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                feedbackManager.triggerHaptic(view, HapticType.LIGHT_TAP)
                onClick()
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(28.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                metaLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
