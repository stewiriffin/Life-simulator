// app/src/main/java/com/maisha/game/ui/components/ConditionBadge.kt
package com.maisha.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.model.HealthCondition
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.StatHappiness

@Composable
fun ConditionBadge(
    condition: HealthCondition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val severityColor = severityColor(condition.severity)
    val severityLabel = severityLabel(condition.severity)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(severityColor.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(severityColor)
        )
        Text(
            text = condition.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = severityLabel,
            style = MaterialTheme.typography.labelSmall,
            color = severityColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun severityColor(severity: Int): Color = when (severity) {
    1 -> StatHappiness
    2 -> GoldAccent
    else -> com.maisha.game.ui.theme.CoralNegative
}

@Composable
private fun severityLabel(severity: Int): String = when (severity) {
    1 -> stringResource(R.string.severity_minor)
    2 -> stringResource(R.string.severity_moderate)
    else -> stringResource(R.string.severity_serious)
}
