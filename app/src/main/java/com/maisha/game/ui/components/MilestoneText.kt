// app/src/main/java/com/maisha/game/ui/components/MilestoneText.kt (new)
package com.maisha.game.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.maisha.game.R
import com.maisha.game.data.model.MilestoneKind
import com.maisha.game.data.model.RelationshipMilestone

@Composable
fun milestoneDescription(milestone: RelationshipMilestone): String {
    val name = milestone.subjectName.orEmpty()
    val kind = milestone.kind?.let { runCatching { MilestoneKind.valueOf(it) }.getOrNull() }
    return when (kind) {
        MilestoneKind.STARTED_DATING -> stringResource(R.string.milestone_started_dating, name)
        MilestoneKind.MARRIED -> stringResource(R.string.milestone_married, name)
        MilestoneKind.QUALITY_TIME -> stringResource(R.string.milestone_quality_time, name)
        MilestoneKind.BIG_ARGUMENT -> stringResource(R.string.milestone_big_argument, name)
        MilestoneKind.THOUGHTFUL_GIFT -> stringResource(R.string.milestone_thoughtful_gift, name)
        MilestoneKind.INSULTED -> stringResource(R.string.milestone_insulted, name)
        MilestoneKind.TRAVELED_TOGETHER -> stringResource(R.string.milestone_traveled, name)
        MilestoneKind.SET_UP_ON_DATE -> stringResource(R.string.milestone_set_up_date, name)
        MilestoneKind.LEGACY_CONTINUED -> stringResource(R.string.milestone_legacy_continued, name)
        null -> milestone.description.ifBlank {
            stringResource(R.string.milestone_generic, milestone.ageAtEvent)
        }
    }
}
