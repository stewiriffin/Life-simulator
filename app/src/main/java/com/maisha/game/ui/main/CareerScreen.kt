// app/src/main/java/com/maisha/game/ui/main/CareerScreen.kt
package com.maisha.game.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.IllustrationCatalog
import com.maisha.game.data.JobPool
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.Job
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.ui.components.ConfirmActionDialog
import com.maisha.game.ui.components.ConfirmSeverity
import com.maisha.game.ui.components.ConfirmableActionHost
import com.maisha.game.ui.components.EmptyStateCard
import com.maisha.game.ui.components.rememberConfirmableAction
import com.maisha.game.ui.components.IllustrationImage
import com.maisha.game.ui.components.RecordBadge
import com.maisha.game.ui.components.StatBar
import com.maisha.game.ui.components.StatType
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.illustrations.EmptyStateIllustrationView
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.formatMoney

private const val MIN_RETIREMENT_AGE = 60

@Composable
fun CareerScreen(
    character: Character,
    eligibleJobs: List<Job>,
    uiState: LifeUiState,
    snackbarHostState: SnackbarHostState,
    onApplyForJob: (String) -> Unit,
    onQuitJob: () -> Unit,
    onRetire: () -> Unit,
    retirementPensionEstimate: Int,
    onDropOut: () -> Unit,
    onCareerMessageDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dropOutConfirm = rememberConfirmableAction<Unit>()
    val retireConfirm = rememberConfirmableAction<Unit>()

    ConfirmableActionHost(
        state = dropOutConfirm,
        onConfirmed = { onDropOut() }
    ) { _, onConfirm, onDismiss ->
        ConfirmActionDialog(
            title = stringResource(R.string.dialog_drop_out_title),
            description = stringResource(R.string.dialog_drop_out_description),
            confirmLabel = stringResource(R.string.btn_drop_out),
            severity = ConfirmSeverity.WARNING,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    ConfirmableActionHost(
        state = retireConfirm,
        onConfirmed = { onRetire() }
    ) { _, onConfirm, onDismiss ->
        ConfirmActionDialog(
            title = stringResource(R.string.dialog_retire_title),
            description = stringResource(
                R.string.dialog_retire_description,
                formatMoney(retirementPensionEstimate, character.countryCode)
            ),
            confirmLabel = stringResource(R.string.btn_retire),
            severity = ConfirmSeverity.NEUTRAL,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    LaunchedEffect(uiState.careerMessage) {
        uiState.careerMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onCareerMessageDismissed()
        }
    }

    val currentJob = character.career.currentJob
    val isRetired = character.career.isRetired
    val eligibleIds = eligibleJobs.map { it.id }.toSet()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaishaSpacing.md, vertical = MaishaSpacing.sm)
    ) {
        Text(
            text = stringResource(R.string.screen_career),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (character.criminalRecord.hasRecord) {
            RecordBadge(timesArrested = character.criminalRecord.timesArrested)
            Spacer(modifier = Modifier.height(8.dp))
        }

        EducationSectionCard(
            education = character.education,
            onDropOut = { dropOutConfirm.request(Unit) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isRetired -> {
                RetiredStateCard(
                    pensionAmount = character.career.pensionAmount,
                    countryCode = character.countryCode
                )
            }
            currentJob != null -> {
                CurrentJobCard(
                    character = character,
                    canRetire = character.age >= MIN_RETIREMENT_AGE,
                    onQuitJob = onQuitJob,
                    onRetire = { retireConfirm.request(Unit) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (!isRetired && currentJob == null) {
            Text(
                text = stringResource(R.string.section_job_listings),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(MaishaSpacing.sm))

            if (eligibleJobs.isEmpty()) {
                EmptyStateCard(
                    illustration = EmptyStateIllustration.ACTIONS,
                    title = stringResource(R.string.screen_career),
                    message = stringResource(R.string.empty_career_no_eligible)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(MaishaSpacing.sm)
                ) {
                    items(JobPool.getJobsForCountry(character.countryCode), key = { it.id }) { job ->
                        val isEligible = job.id in eligibleIds
                        val reason = jobIneligibilityReason(character, job)
                        JobListingCard(
                            job = job,
                            countryCode = character.countryCode,
                            isEligible = isEligible,
                            ineligibilityReason = reason,
                            onApply = { onApplyForJob(job.id) }
                        )
                    }
                }
            }
        }

        if (character.career.jobHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.format_job_history,
                    character.career.jobHistory.joinToString(", ")
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RetiredStateCard(
    pensionAmount: Int,
    countryCode: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = GoldAccent.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmptyStateIllustrationView(
                type = EmptyStateIllustration.RETIRED,
                size = 96.dp
            )
            Text(
                text = stringResource(R.string.label_retired),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )
            Text(
                text = stringResource(R.string.empty_retired_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (pensionAmount > 0) {
                Text(
                    text = stringResource(R.string.label_annual_pension),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.format_annual_pension,
                        formatMoney(pensionAmount, countryCode)
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary
                )
            }
        }
    }
}

@Composable
private fun EducationSectionCard(
    education: EducationState,
    onDropOut: () -> Unit
) {
    val resources = LocalContext.current.resources
    val canDropOut = education.stage == SchoolStage.SECONDARY ||
        education.stage == SchoolStage.UNIVERSITY

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.label_education),
                style = MaterialTheme.typography.labelMedium,
                color = GoldAccent
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = EducationFormatter.formatStatus(education, resources),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (education.gpa > 0f && education.stage != SchoolStage.NONE &&
                education.stage != SchoolStage.GRADUATED && !education.expelled
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.format_gpa, education.gpa),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canDropOut) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onDropOut,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_drop_out))
                }
            }
        }
    }
}

@Composable
private fun CurrentJobCard(
    character: Character,
    canRetire: Boolean,
    onQuitJob: () -> Unit,
    onRetire: () -> Unit
) {
    val job = character.career.currentJob ?: return
    val resources = LocalContext.current.resources

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = TealPrimary.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IllustrationImage(
                    ref = IllustrationCatalog.getIllustrationForJob(job.id),
                    size = 44.dp,
                    contentDescription = job.title
                )
                Text(
                    text = stringResource(R.string.label_current_job),
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldAccent
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = job.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    R.string.format_job_level_salary,
                    job.level,
                    CareerFormatter.formatSalary(job, resources, character.countryCode)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.format_years_at_job,
                    character.career.yearsAtCurrentJob
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            StatBar(
                type = StatType.PERFORMANCE,
                value = job.performanceScore,
                label = stringResource(R.string.stat_performance)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (canRetire) {
                Button(
                    onClick = onRetire,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.btn_retire))
                }
            } else {
                OutlinedButton(
                    onClick = onQuitJob,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_quit_job))
                }
            }
        }
    }
}

@Composable
private fun JobListingCard(
    job: Job,
    countryCode: String,
    isEligible: Boolean,
    ineligibilityReason: String?,
    onApply: () -> Unit
) {
    val alpha = if (isEligible) 1f else 0.65f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IllustrationImage(
                ref = IllustrationCatalog.getIllustrationForJob(job.id),
                size = 48.dp,
                contentDescription = job.title
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.format_job_salary_from,
                        formatMoney(job.baseSalary, countryCode)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isEligible && ineligibilityReason != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = stringResource(R.string.content_desc_locked),
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = ineligibilityReason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Button(
                onClick = onApply,
                enabled = isEligible,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = MaishaRadius.buttonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    if (isEligible) {
                        stringResource(R.string.btn_apply)
                    } else {
                        stringResource(R.string.btn_locked)
                    }
                )
            }
        }
    }
}

@Composable
private fun jobIneligibilityReason(character: Character, job: Job): String? {
    if (character.career.isRetired) {
        return stringResource(R.string.label_retired)
    }
    if (character.career.currentJob != null) {
        return stringResource(R.string.job_ineligible_employed)
    }
    if (character.age < 18) {
        return stringResource(R.string.job_ineligible_age)
    }
    if (character.education.expelled) {
        return stringResource(R.string.job_ineligible_expelled)
    }
    val education = character.education
    if (job.minEducation == SchoolStage.GRADUATED &&
        education.droppedOutFrom == SchoolStage.UNIVERSITY
    ) {
        return stringResource(R.string.job_ineligible_dropout)
    }
    if (job.minEducation == SchoolStage.SECONDARY &&
        education.droppedOutFrom == SchoolStage.SECONDARY &&
        education.kcseGrade == null
    ) {
        return stringResource(R.string.job_ineligible_dropout)
    }
    val stageOrder = listOf(
        SchoolStage.NONE,
        SchoolStage.PRIMARY,
        SchoolStage.SECONDARY,
        SchoolStage.UNIVERSITY,
        SchoolStage.GRADUATED
    )
    val currentIndex = stageOrder.indexOf(character.education.stage)
    val requiredIndex = stageOrder.indexOf(job.minEducation)
    if (currentIndex < requiredIndex) {
        return stringResource(
            R.string.job_ineligible_education,
            formatEducationRequirement(job.minEducation)
        )
    }
    return null
}

@Composable
private fun formatEducationRequirement(stage: SchoolStage): String = when (stage) {
    SchoolStage.PRIMARY -> stringResource(R.string.edu_req_primary)
    SchoolStage.SECONDARY -> stringResource(R.string.edu_req_secondary)
    SchoolStage.UNIVERSITY -> stringResource(R.string.edu_req_university)
    SchoolStage.GRADUATED -> stringResource(R.string.edu_req_degree)
    SchoolStage.NONE -> stringResource(R.string.edu_req_none)
}
