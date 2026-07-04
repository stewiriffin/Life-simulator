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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.IllustrationCatalog
import com.maisha.game.data.JobPool
import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.Business
import com.maisha.game.data.model.BusinessIndustry
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.Job
import com.maisha.game.data.model.PoliticalOffice
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.TaxPolicyType
import com.maisha.game.domain.BusinessEngine
import com.maisha.game.domain.PoliticsEngine
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
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.SuccessGreen
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
    onStartBusiness: (String, BusinessIndustry, Int) -> Unit,
    onSellBusiness: (String) -> Unit,
    investmentTiers: List<Int>,
    onLaunchCampaign: (PoliticalOffice, Int) -> Unit,
    onPassTaxPolicy: (TaxPolicyType) -> Unit,
    onCareerMessageDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dropOutConfirm = rememberConfirmableAction<Unit>()
    val retireConfirm = rememberConfirmableAction<Unit>()
    val startBusinessConfirm = rememberConfirmableAction<Unit>()
    val sellBusinessConfirm = rememberConfirmableAction<Business>()
    val campaignConfirm = rememberConfirmableAction<PoliticalOffice>()
    val taxPolicyConfirm = rememberConfirmableAction<TaxPolicyType>()
    var selectedOffice by remember { mutableStateOf(PoliticalOffice.MAYOR) }

    val tiers = investmentTiers.ifEmpty {
        listOf(
            BusinessEngine.INVESTMENT_SMALL_KENYA,
            BusinessEngine.INVESTMENT_MEDIUM_KENYA,
            BusinessEngine.INVESTMENT_LARGE_KENYA
        )
    }
    var businessName by remember { mutableStateOf("") }
    var businessIndustry by remember { mutableStateOf(BusinessIndustry.TECH) }
    var businessInvestment by remember { mutableIntStateOf(tiers.first()) }

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

    ConfirmableActionHost(
        state = startBusinessConfirm,
        onConfirmed = {
            onStartBusiness(businessName, businessIndustry, businessInvestment)
        }
    ) { _, onConfirm, onDismiss ->
        StartBusinessDialog(
            name = businessName,
            onNameChange = { businessName = it },
            industry = businessIndustry,
            onIndustryChange = { businessIndustry = it },
            investment = businessInvestment,
            onInvestmentChange = { businessInvestment = it },
            investmentTiers = tiers,
            countryCode = character.countryCode,
            playerMoney = character.stats.money,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    ConfirmableActionHost(
        state = sellBusinessConfirm,
        onConfirmed = { business -> onSellBusiness(business.id) }
    ) { business, onConfirm, onDismiss ->
        ConfirmActionDialog(
            title = stringResource(R.string.dialog_sell_business_title),
            description = stringResource(
                R.string.dialog_sell_business_description,
                business.name,
                formatMoney(business.valuation, character.countryCode)
            ),
            confirmLabel = stringResource(R.string.btn_sell_business),
            severity = ConfirmSeverity.WARNING,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    val campaignInvestment = EconomyScaler.scaleAmount(
        when (selectedOffice) {
            PoliticalOffice.MAYOR -> 50_000
            PoliticalOffice.GOVERNOR -> 200_000
            PoliticalOffice.PRESIDENT -> 500_000
        },
        character.countryCode
    )

    ConfirmableActionHost(
        state = campaignConfirm,
        onConfirmed = { office -> onLaunchCampaign(office, campaignInvestment) }
    ) { office, onConfirm, onDismiss ->
        ConfirmActionDialog(
            title = stringResource(R.string.dialog_launch_campaign_title),
            description = stringResource(
                R.string.dialog_launch_campaign_body,
                officeLabel(office),
                formatMoney(campaignInvestment, character.countryCode)
            ),
            confirmLabel = stringResource(R.string.btn_launch_campaign),
            severity = ConfirmSeverity.NEUTRAL,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    ConfirmableActionHost(
        state = taxPolicyConfirm,
        onConfirmed = { policy -> onPassTaxPolicy(policy) }
    ) { policy, onConfirm, onDismiss ->
        ConfirmActionDialog(
            title = stringResource(R.string.dialog_tax_policy_title),
            description = when (policy) {
                TaxPolicyType.TAX_CUTS -> stringResource(R.string.dialog_tax_cuts_body)
                TaxPolicyType.WEALTH_TAX -> stringResource(R.string.dialog_wealth_tax_body)
            },
            confirmLabel = stringResource(R.string.btn_pass_policy),
            severity = ConfirmSeverity.WARNING,
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
    val isMilitaryCareer = currentJob?.isMilitary == true
    val showDeploymentBanner = character.career.pendingDeployment || character.career.isDeployed
    val militaryAccent = Color(0xFF556B2F) // olive drab
    val eligibleIds = eligibleJobs.map { it.id }.toSet()
    val canStartBusiness = character.alive &&
        character.age >= BusinessEngine.MIN_BUSINESS_AGE &&
        !character.criminalRecord.currentlyIncarcerated &&
        !character.criminalRecord.awaitingTrial &&
        character.businesses.size < BusinessEngine.MAX_BUSINESSES

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaishaSpacing.md, vertical = MaishaSpacing.sm)
    ) {
        Text(
            text = stringResource(R.string.screen_career),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (isMilitaryCareer) militaryAccent else MaterialTheme.colorScheme.onSurface
        )

        if (showDeploymentBanner) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaishaRadius.cardShape,
                colors = CardDefaults.cardColors(containerColor = militaryAccent)
            ) {
                Text(
                    text = stringResource(R.string.banner_active_deployment),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }

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

        PoliticsSection(
            character = character,
            selectedOffice = selectedOffice,
            onOfficeSelected = { selectedOffice = it },
            campaignInvestment = campaignInvestment,
            onLaunchCampaign = { campaignConfirm.request(selectedOffice) },
            onPassTaxPolicy = { taxPolicyConfirm.request(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isRetired -> {
                RetiredStateCard(
                    pensionAmount = character.career.pensionAmount,
                    countryCode = character.countryCode
                )
                Spacer(modifier = Modifier.height(12.dp))
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

        Text(
            text = stringResource(R.string.section_my_businesses),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TealPrimary
        )
        Spacer(modifier = Modifier.height(MaishaSpacing.sm))

        if (character.businesses.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_businesses),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            character.businesses.forEach { business ->
                BusinessCard(
                    business = business,
                    countryCode = character.countryCode,
                    onSell = { sellBusinessConfirm.request(business) }
                )
                Spacer(modifier = Modifier.height(MaishaSpacing.sm))
            }
        }

        if (canStartBusiness) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    businessName = ""
                    businessIndustry = BusinessIndustry.TECH
                    businessInvestment = tiers.first()
                    startBusinessConfirm.request(Unit)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text(
                    text = stringResource(R.string.btn_start_business),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (!isRetired && currentJob == null) {
            Spacer(modifier = Modifier.height(16.dp))
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
                Column(verticalArrangement = Arrangement.spacedBy(MaishaSpacing.sm)) {
                    JobPool.getJobsForCountry(character.countryCode).forEach { job ->
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
private fun BusinessCard(
    business: Business,
    countryCode: String,
    onSell: () -> Unit
) {
    val profitColor = when {
        business.lastYearProfit > 0 -> SuccessGreen
        business.lastYearProfit < 0 -> CoralNegative
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val profitLabel = when {
        business.lastYearProfit > 0 -> stringResource(
            R.string.format_business_profit,
            formatMoney(business.lastYearProfit, countryCode)
        )
        business.lastYearProfit < 0 -> stringResource(
            R.string.format_business_loss,
            formatMoney(-business.lastYearProfit, countryCode)
        )
        else -> stringResource(R.string.label_business_no_profit_yet)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaishaSpacing.md),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = business.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = businessIndustryLabel(business.industry),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.format_business_valuation,
                    formatMoney(business.valuation, countryCode)
                ),
                style = MaterialTheme.typography.labelLarge,
                color = TealPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = profitLabel,
                style = MaterialTheme.typography.labelMedium,
                color = profitColor
            )
            Text(
                text = stringResource(
                    R.string.format_business_employees,
                    business.employeeCount
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onSell,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.btn_sell_business))
            }
        }
    }
}

@Composable
private fun StartBusinessDialog(
    name: String,
    onNameChange: (String) -> Unit,
    industry: BusinessIndustry,
    onIndustryChange: (BusinessIndustry) -> Unit,
    investment: Int,
    onInvestmentChange: (Int) -> Unit,
    investmentTiers: List<Int>,
    countryCode: String,
    playerMoney: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val canAfford = playerMoney >= investment && name.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_start_business_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.dialog_start_business_description),
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.label_business_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.label_business_industry),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BusinessIndustry.entries.take(3).forEach { option ->
                        FilterChip(
                            selected = industry == option,
                            onClick = { onIndustryChange(option) },
                            label = {
                                Text(
                                    text = businessIndustryLabel(option),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BusinessIndustry.entries.drop(3).forEach { option ->
                        FilterChip(
                            selected = industry == option,
                            onClick = { onIndustryChange(option) },
                            label = {
                                Text(
                                    text = businessIndustryLabel(option),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.label_initial_investment),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                investmentTiers.forEach { tier ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = investment == tier,
                                onClick = { onInvestmentChange(tier) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = investment == tier,
                            onClick = { onInvestmentChange(tier) }
                        )
                        Text(
                            text = formatMoney(tier, countryCode),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text(stringResource(R.string.btn_start_business))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
private fun businessIndustryLabel(industry: BusinessIndustry): String = when (industry) {
    BusinessIndustry.TECH -> stringResource(R.string.industry_tech)
    BusinessIndustry.RETAIL -> stringResource(R.string.industry_retail)
    BusinessIndustry.FOOD -> stringResource(R.string.industry_food)
    BusinessIndustry.REAL_ESTATE -> stringResource(R.string.industry_real_estate)
    BusinessIndustry.ENTERTAINMENT -> stringResource(R.string.industry_entertainment)
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
    val militaryAccent = Color(0xFF556B2F)
    val cardColor = if (job.isMilitary) {
        militaryAccent.copy(alpha = 0.22f)
    } else {
        TealPrimary.copy(alpha = 0.15f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = cardColor)
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
private fun PoliticsSection(
    character: Character,
    selectedOffice: PoliticalOffice,
    onOfficeSelected: (PoliticalOffice) -> Unit,
    campaignInvestment: Int,
    onLaunchCampaign: () -> Unit,
    onPassTaxPolicy: (TaxPolicyType) -> Unit
) {
    val office = character.politics.currentOffice
    Text(
        text = stringResource(R.string.section_politics),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = TealPrimary
    )
    Spacer(modifier = Modifier.height(MaishaSpacing.sm))

    if (office != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaishaRadius.cardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.format_current_office,
                        officeLabel(office)
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                StatBar(
                    type = StatType.PERFORMANCE,
                    value = character.politics.approvalRating,
                    maxValue = 100,
                    label = stringResource(R.string.label_approval_rating),
                    showIcon = false
                )
                character.politics.activeTaxPolicy?.let { policy ->
                    Text(
                        text = stringResource(
                            R.string.format_active_tax_policy,
                            taxPolicyLabel(policy)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (office == PoliticalOffice.GOVERNOR || office == PoliticalOffice.PRESIDENT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onPassTaxPolicy(TaxPolicyType.TAX_CUTS) },
                            modifier = Modifier.weight(1f),
                            shape = MaishaRadius.buttonShape
                        ) {
                            Text(
                                stringResource(R.string.btn_tax_cuts),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = { onPassTaxPolicy(TaxPolicyType.WEALTH_TAX) },
                            modifier = Modifier.weight(1f),
                            shape = MaishaRadius.buttonShape
                        ) {
                            Text(
                                stringResource(R.string.btn_wealth_tax),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    } else if (
        character.alive &&
        character.age >= PoliticsEngine.MIN_OFFICE_AGE &&
        !character.criminalRecord.hasRecord &&
        !character.criminalRecord.currentlyIncarcerated
    ) {
        Text(
            text = stringResource(R.string.section_run_for_office),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PoliticalOffice.entries.forEach { option ->
                FilterChip(
                    selected = selectedOffice == option,
                    onClick = { onOfficeSelected(option) },
                    label = {
                        Text(
                            officeLabel(option),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onLaunchCampaign,
            modifier = Modifier.fillMaxWidth(),
            shape = MaishaRadius.buttonShape,
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NavyDeep)
        ) {
            Text(
                stringResource(
                    R.string.btn_run_for_office_cost,
                    officeLabel(selectedOffice),
                    formatMoney(campaignInvestment, character.countryCode)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        Text(
            text = stringResource(R.string.politics_locked_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun officeLabel(office: PoliticalOffice): String = when (office) {
    PoliticalOffice.MAYOR -> stringResource(R.string.office_mayor)
    PoliticalOffice.GOVERNOR -> stringResource(R.string.office_governor)
    PoliticalOffice.PRESIDENT -> stringResource(R.string.office_president)
}

@Composable
private fun taxPolicyLabel(policy: TaxPolicyType): String = when (policy) {
    TaxPolicyType.TAX_CUTS -> stringResource(R.string.policy_tax_cuts)
    TaxPolicyType.WEALTH_TAX -> stringResource(R.string.policy_wealth_tax)
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
