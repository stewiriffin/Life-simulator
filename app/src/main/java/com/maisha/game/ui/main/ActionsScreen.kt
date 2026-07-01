// app/src/main/java/com/maisha/game/ui/main/ActionsScreen.kt
package com.maisha.game.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.CrimeType
import com.maisha.game.data.model.HealthCondition
import com.maisha.game.ui.components.ActionCard
import com.maisha.game.ui.components.ConditionBadge
import com.maisha.game.ui.components.ConfirmActionDialog
import com.maisha.game.ui.components.ConfirmSeverity
import com.maisha.game.ui.components.EmptyStateCard
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.theme.AppIcons
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.formatMoney

private const val CRIME_UI_MIN_AGE = 16

private sealed class PendingAction {
    data class Crime(val type: CrimeType) : PendingAction()
    data class Treatment(val condition: HealthCondition, val careType: CareType) : PendingAction()
}

@Composable
fun ActionsScreen(
    character: Character,
    uiState: LifeUiState,
    snackbarHostState: SnackbarHostState,
    onAttemptCrime: (CrimeType) -> Unit,
    onVisitDoctor: (String, CareType) -> Unit,
    onActionMessageDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val incarcerated = character.criminalRecord.currentlyIncarcerated
    val untreated = character.activeConditions.filter { !it.treated }
    val showCrimeActions = character.age >= CRIME_UI_MIN_AGE && !incarcerated && character.alive
    val hasContent = untreated.isNotEmpty() || showCrimeActions || incarcerated

    var pendingAction by remember { mutableStateOf<PendingAction?>(null) }
    var expandedConditionId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(untreated.map { it.id }) {
        expandedConditionId = when {
            untreated.size == 1 -> untreated.first().id
            expandedConditionId != null && untreated.none { it.id == expandedConditionId } -> null
            else -> expandedConditionId
        }
    }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onActionMessageDismissed()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaishaSpacing.md, vertical = MaishaSpacing.sm)
    ) {
        Text(
            text = stringResource(R.string.screen_actions),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (!hasContent) {
            EmptyStateCard(
                illustration = EmptyStateIllustration.ACTIONS,
                title = stringResource(R.string.empty_actions_title),
                message = stringResource(R.string.empty_actions_body),
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (incarcerated) {
                    item {
                        Text(
                            text = stringResource(
                                R.string.msg_in_prison,
                                character.criminalRecord.yearsRemaining
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoralNegative,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (untreated.isNotEmpty()) {
                    item {
                        SectionHeader(title = stringResource(R.string.section_health))
                    }
                    items(untreated, key = { it.id }) { condition ->
                        val isExpanded = expandedConditionId == condition.id
                        ConditionBadge(
                            condition = condition,
                            onClick = {
                                expandedConditionId = if (isExpanded) null else condition.id
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(4.dp))
                            ActionCard(
                                icon = AppIcons.HealthClinic,
                                title = stringResource(R.string.care_public_clinic),
                                description = successHint(CareType.PUBLIC_CLINIC),
                                metaLabel = treatmentCostLabel(
                                    condition.severity,
                                    CareType.PUBLIC_CLINIC
                                ),
                                onClick = {
                                    pendingAction = PendingAction.Treatment(
                                        condition = condition,
                                        careType = CareType.PUBLIC_CLINIC
                                    )
                                }
                            )
                            ActionCard(
                                icon = AppIcons.HealthHospital,
                                title = stringResource(R.string.care_nairobi_hospital),
                                description = successHint(CareType.PRIVATE_HOSPITAL),
                                metaLabel = treatmentCostLabel(
                                    condition.severity,
                                    CareType.PRIVATE_HOSPITAL
                                ),
                                onClick = {
                                    pendingAction = PendingAction.Treatment(
                                        condition = condition,
                                        careType = CareType.PRIVATE_HOSPITAL
                                    )
                                }
                            )
                        }
                    }
                }

                if (showCrimeActions) {
                    item {
                        SectionHeader(title = stringResource(R.string.section_opportunities))
                    }
                    item {
                        ActionCard(
                            icon = AppIcons.CrimePickpocket,
                            title = stringResource(R.string.crime_pickpocket_title),
                            description = stringResource(R.string.crime_pickpocket_desc),
                            metaLabel = stringResource(R.string.risk_moderate),
                            onClick = { pendingAction = PendingAction.Crime(CrimeType.PICKPOCKET) }
                        )
                    }
                    item {
                        ActionCard(
                            icon = AppIcons.CrimeShoplift,
                            title = stringResource(R.string.crime_shoplift_title),
                            description = stringResource(R.string.crime_shoplift_desc),
                            metaLabel = stringResource(R.string.risk_moderate),
                            onClick = { pendingAction = PendingAction.Crime(CrimeType.SHOPLIFT) }
                        )
                    }
                    item {
                        ActionCard(
                            icon = AppIcons.CrimeFraud,
                            title = stringResource(R.string.crime_fraud_title),
                            description = stringResource(R.string.crime_fraud_desc),
                            metaLabel = stringResource(R.string.risk_high),
                            onClick = { pendingAction = PendingAction.Crime(CrimeType.FRAUD) }
                        )
                    }
                }
            }
        }
    }

    pendingAction?.let { action ->
        when (action) {
            is PendingAction.Crime -> {
                val (title, description) = crimeConfirmCopy(action.type)
                ConfirmActionDialog(
                    title = title,
                    description = description,
                    confirmLabel = stringResource(R.string.btn_attempt),
                    severity = ConfirmSeverity.WARNING,
                    onConfirm = {
                        onAttemptCrime(action.type)
                        pendingAction = null
                    },
                    onDismiss = { pendingAction = null }
                )
            }
            is PendingAction.Treatment -> {
                val careName = when (action.careType) {
                    CareType.PUBLIC_CLINIC -> stringResource(R.string.care_public_clinic)
                    CareType.PRIVATE_HOSPITAL -> stringResource(R.string.care_nairobi_hospital)
                }
                ConfirmActionDialog(
                    title = stringResource(R.string.dialog_seek_treatment_title),
                    description = stringResource(
                        R.string.dialog_seek_treatment_desc,
                        action.condition.name,
                        careName,
                        treatmentCostLabel(action.condition.severity, action.careType),
                        successHint(action.careType)
                    ),
                    confirmLabel = stringResource(R.string.btn_visit_doctor),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = {
                        onVisitDoctor(action.condition.id, action.careType)
                        pendingAction = null
                    },
                    onDismiss = { pendingAction = null }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = TealPrimary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun treatmentCostLabel(severity: Int, careType: CareType): String {
    val amount = HealthUiHelpers.treatmentCost(severity, careType)
    return stringResource(R.string.format_treatment_cost, formatMoney(amount))
}

@Composable
private fun successHint(careType: CareType): String = when (careType) {
    CareType.PUBLIC_CLINIC -> stringResource(R.string.hint_public_clinic_success)
    CareType.PRIVATE_HOSPITAL -> stringResource(R.string.hint_private_hospital_success)
}

@Composable
private fun crimeConfirmCopy(type: CrimeType): Pair<String, String> = when (type) {
    CrimeType.PICKPOCKET -> stringResource(R.string.dialog_crime_pickpocket_title) to
        stringResource(R.string.dialog_crime_pickpocket_desc)
    CrimeType.SHOPLIFT -> stringResource(R.string.dialog_crime_shoplift_title) to
        stringResource(R.string.dialog_crime_shoplift_desc)
    CrimeType.FRAUD -> stringResource(R.string.dialog_crime_fraud_title) to
        stringResource(R.string.dialog_crime_fraud_desc)
}
