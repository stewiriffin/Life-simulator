// app/src/main/java/com/maisha/game/ui/components/MilestoneTimeline.kt (new)
package com.maisha.game.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.model.RelationshipMilestone
import com.maisha.game.ui.theme.TealPrimary

private const val VISIBLE_MILESTONE_COUNT = 8

@Composable
fun MilestoneTimeline(
    milestones: List<RelationshipMilestone>,
    modifier: Modifier = Modifier
) {
    if (milestones.isEmpty()) return

    val ordered = milestones.asReversed()
    val visible = ordered.take(VISIBLE_MILESTONE_COUNT)
    val hiddenCount = (milestones.size - VISIBLE_MILESTONE_COUNT).coerceAtLeast(0)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        if (hiddenCount > 0) {
            Text(
                text = stringResource(R.string.person_memories_earlier, hiddenCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        visible.forEachIndexed { index, milestone ->
            MilestoneTimelineRow(
                milestone = milestone,
                isLast = index == visible.lastIndex
            )
        }
    }
}

@Composable
private fun MilestoneTimelineRow(
    milestone: RelationshipMilestone,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .padding(0.dp)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = TealPrimary)
                    }
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .padding(top = 2.dp)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
                        drawLine(
                            color = TealPrimary.copy(alpha = 0.35f),
                            start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height),
                            strokeWidth = 2f
                        )
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 10.dp)) {
            Text(
                text = stringResource(R.string.format_age, milestone.ageAtEvent),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = milestoneDescription(milestone),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
