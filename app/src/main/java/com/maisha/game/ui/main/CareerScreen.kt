// app/src/main/java/com/maisha/game/ui/main/CareerScreen.kt
package com.maisha.game.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.maisha.game.data.model.CareerTrack
import com.maisha.game.data.model.SchoolActivity
import com.maisha.game.data.model.SchoolClub
import com.maisha.game.data.model.SchoolPerson
import com.maisha.game.data.model.SchoolPersonAction
import com.maisha.game.data.model.SchoolRole
import com.maisha.game.data.model.StudyEffort
import com.maisha.game.data.model.WorkEffort
import com.maisha.game.domain.BusinessEngine
import com.maisha.game.domain.CareerEngine
import com.maisha.game.domain.EducationEngine
import com.maisha.game.domain.HealthEngine
import com.maisha.game.domain.PoliticsEngine
import com.maisha.game.domain.RelocationEngine
import com.maisha.game.ui.avatar.AvatarImage
import com.maisha.game.ui.components.CategoryFilterChipRow
import com.maisha.game.ui.components.ConfirmActionDialog
import com.maisha.game.ui.components.ConfirmSeverity
import com.maisha.game.ui.components.ConfirmableActionHost
import com.maisha.game.ui.components.EmptyStateCard
import com.maisha.game.ui.components.rememberConfirmableAction
import com.maisha.game.ui.components.IllustrationImage
import com.maisha.game.ui.components.RecordBadge
import com.maisha.game.ui.components.StatBar
import com.maisha.game.ui.components.StatType
import com.maisha.game.ui.components.TabPageHero
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.illustrations.EmptyStateIllustrationView
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.CreamBg
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.InkTertiary
import com.maisha.game.ui.theme.LifeGreen
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.SuccessGreen
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.formatMoney

private const val MIN_RETIREMENT_AGE = 60

private val schoolUiEngine by lazy { EducationEngine(RelocationEngine()) }

private enum class CareerCategory { ALL, WORK, SCHOOL, BUSINESS, POLITICS }

private enum class CareerListContentType {
    Banner,
    Badge,
    Education,
    Politics,
    WorkState,
    JobHeader,
    JobListing,
    JobHistory,
    BusinessHeader,
    BusinessCard,
    StartBusiness
}

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
    onSetWorkEffort: (com.maisha.game.data.model.WorkEffort) -> Unit,
    onSetStudyEffort: (StudyEffort) -> Unit,
    onJoinSchoolClub: (SchoolClub) -> Unit,
    onPerformSchoolActivity: (SchoolActivity, String?) -> Unit = { _, _ -> },
    onSchoolPersonInteraction: (String, SchoolPersonAction) -> Unit = { _, _ -> },
    onStartCareerTrack: (CareerTrack) -> Unit,
    onPracticeCareerTrack: () -> Unit,
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
    val politicsEligible = character.alive &&
        character.age >= PoliticsEngine.MIN_OFFICE_AGE &&
        !character.criminalRecord.currentlyIncarcerated &&
        !character.criminalRecord.awaitingTrial
    val resources = LocalContext.current.resources
    val careerEngine = remember { CareerEngine(HealthEngine(), RelocationEngine()) }
    val hireChance = if (!isRetired && currentJob == null && character.alive) {
        (careerEngine.hireSuccessChance(character) * 100f).toInt()
    } else {
        null
    }

    var selectedCategory by rememberSaveable { mutableIntStateOf(0) }
    val category = CareerCategory.entries.getOrElse(selectedCategory) { CareerCategory.ALL }
    fun show(cat: CareerCategory): Boolean =
        category == CareerCategory.ALL || category == cat

    var historyExpanded by rememberSaveable { mutableStateOf(false) }

    val heroSubtitle = when {
        isRetired -> stringResource(R.string.career_subtitle_retired)
        currentJob != null -> stringResource(
            R.string.career_subtitle_employed,
            currentJob.title
        )
        else -> stringResource(R.string.career_subtitle_open)
    }
    val heroPrimary = when {
        isRetired -> formatMoney(character.career.pensionAmount, character.countryCode)
        currentJob != null -> CareerFormatter.formatSalary(currentJob, resources, character.countryCode)
        else -> formatMoney(character.stats.money, character.countryCode)
    }
    val heroSecondary = when {
        isRetired -> stringResource(R.string.chip_career_pension)
        currentJob != null -> stringResource(
            R.string.format_job_level_short,
            currentJob.level,
            character.career.yearsAtCurrentJob
        )
        hireChance != null -> stringResource(R.string.format_hire_chance, hireChance)
        else -> null
    }
    val heroTertiary = when {
        currentJob != null -> stringResource(
            R.string.format_work_effort_chip,
            when (character.career.plannedWorkEffort) {
                WorkEffort.COAST -> stringResource(R.string.work_effort_coast)
                WorkEffort.NORMAL -> stringResource(R.string.work_effort_normal)
                WorkEffort.GRIND -> stringResource(R.string.work_effort_grind)
            }
        )
        else -> null
    }

    val chipLabels = listOf(
        stringResource(R.string.chip_career_all),
        stringResource(R.string.chip_career_work),
        stringResource(R.string.chip_career_school),
        stringResource(R.string.chip_career_business),
        stringResource(R.string.chip_career_politics)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBg)
    ) {
        TabPageHero(
            title = stringResource(R.string.screen_career),
            subtitle = heroSubtitle,
            primaryChip = heroPrimary,
            secondaryChip = heroSecondary,
            tertiaryChip = heroTertiary
        )

        CategoryFilterChipRow(
            labels = chipLabels,
            selectedIndex = selectedCategory,
            onSelected = { selectedCategory = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showDeploymentBanner) {
                item(
                    key = "deployment_banner",
                    contentType = CareerListContentType.Banner
                ) {
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
            }

            if (character.criminalRecord.hasRecord && show(CareerCategory.WORK)) {
                item(
                    key = "record_badge",
                    contentType = CareerListContentType.Badge
                ) {
                    RecordBadge(timesArrested = character.criminalRecord.timesArrested)
                }
            }

            if (show(CareerCategory.SCHOOL)) {
                item(
                    key = "education",
                    contentType = CareerListContentType.Education
                ) {
                    EducationSectionCard(
                        education = character.education,
                        countryCode = character.countryCode,
                        onDropOut = { dropOutConfirm.request(Unit) },
                        onSetStudyEffort = onSetStudyEffort
                    )
                }
                item(
                    key = "school_life",
                    contentType = CareerListContentType.Education
                ) {
                    SchoolLifeSectionCard(
                        character = character,
                        onPerformSchoolActivity = onPerformSchoolActivity,
                        onSchoolPersonInteraction = onSchoolPersonInteraction
                    )
                }
                item(
                    key = "school_club",
                    contentType = CareerListContentType.Education
                ) {
                    SchoolClubSectionCard(
                        character = character,
                        onJoinSchoolClub = onJoinSchoolClub
                    )
                }
                item(
                    key = "career_track",
                    contentType = CareerListContentType.Education
                ) {
                    CareerTrackSectionCard(
                        character = character,
                        onStartCareerTrack = onStartCareerTrack,
                        onPracticeCareerTrack = onPracticeCareerTrack
                    )
                }
            }

            if (show(CareerCategory.POLITICS)) {
                item(
                    key = "politics",
                    contentType = CareerListContentType.Politics
                ) {
                    if (politicsEligible || character.politics.currentOffice != null) {
                        PoliticsSection(
                            character = character,
                            selectedOffice = selectedOffice,
                            onOfficeSelected = { selectedOffice = it },
                            campaignInvestment = campaignInvestment,
                            onLaunchCampaign = { campaignConfirm.request(selectedOffice) },
                            onPassTaxPolicy = { taxPolicyConfirm.request(it) }
                        )
                    } else {
                        Column {
                            Text(
                                text = stringResource(R.string.section_politics),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TealPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.politics_locked_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = InkTertiary
                            )
                        }
                    }
                }
            }

            if (show(CareerCategory.WORK)) {
                when {
                    isRetired -> {
                        item(
                            key = "retired",
                            contentType = CareerListContentType.WorkState
                        ) {
                            RetiredStateCard(
                                pensionAmount = character.career.pensionAmount,
                                countryCode = character.countryCode
                            )
                        }
                    }
                    currentJob != null -> {
                        item(
                            key = "current_job",
                            contentType = CareerListContentType.WorkState
                        ) {
                            CurrentJobCard(
                                character = character,
                                canRetire = character.age >= MIN_RETIREMENT_AGE,
                                retirementPensionEstimate = retirementPensionEstimate,
                                onQuitJob = onQuitJob,
                                onRetire = { retireConfirm.request(Unit) },
                                onSetWorkEffort = onSetWorkEffort
                            )
                        }
                    }
                }

                if (!isRetired && currentJob == null) {
                    item(
                        key = "job_listings_header",
                        contentType = CareerListContentType.JobHeader
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.section_job_listings),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TealPrimary
                            )
                            if (hireChance != null) {
                                Text(
                                    text = stringResource(R.string.format_hire_outlook, hireChance),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkTertiary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(MaishaSpacing.sm))
                            }
                            Text(
                                text = stringResource(R.string.career_side_hustle_hint),
                                style = MaterialTheme.typography.labelMedium,
                                color = LifeGreen,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            if (eligibleJobs.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                EmptyStateCard(
                                    illustration = EmptyStateIllustration.ACTIONS,
                                    title = stringResource(R.string.screen_career),
                                    message = stringResource(R.string.empty_career_no_eligible)
                                )
                            }
                        }
                    }

                    if (eligibleJobs.isNotEmpty()) {
                        val countryJobs = JobPool.getJobsForCountry(character.countryCode)
                        items(
                            items = countryJobs,
                            key = { it.id },
                            contentType = { CareerListContentType.JobListing }
                        ) { job ->
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

                if (character.career.jobHistory.isNotEmpty()) {
                    item(
                        key = "job_history",
                        contentType = CareerListContentType.JobHistory
                    ) {
                        Column {
                            TextButton(onClick = { historyExpanded = !historyExpanded }) {
                                Text(
                                    text = if (historyExpanded) {
                                        stringResource(R.string.btn_collapse_job_history)
                                    } else {
                                        stringResource(R.string.btn_expand_job_history)
                                    }
                                )
                            }
                            if (historyExpanded) {
                                Text(
                                    text = stringResource(
                                        R.string.format_job_history,
                                        character.career.jobHistory.joinToString(", ")
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (show(CareerCategory.BUSINESS)) {
                item(
                    key = "business_header",
                    contentType = CareerListContentType.BusinessHeader
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.section_my_businesses),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TealPrimary
                        )
                        if (character.businesses.isEmpty()) {
                            Spacer(modifier = Modifier.height(MaishaSpacing.sm))
                            Text(
                                text = stringResource(R.string.empty_businesses),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (character.businesses.isNotEmpty()) {
                    items(
                        items = character.businesses,
                        key = { it.id },
                        contentType = { CareerListContentType.BusinessCard }
                    ) { business ->
                        BusinessCard(
                            business = business,
                            countryCode = character.countryCode,
                            onSell = { sellBusinessConfirm.request(business) }
                        )
                    }
                }

                if (canStartBusiness) {
                    item(
                        key = "start_business",
                        contentType = CareerListContentType.StartBusiness
                    ) {
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
                }
            }
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
    countryCode: String,
    onDropOut: () -> Unit,
    onSetStudyEffort: (StudyEffort) -> Unit
) {
    val resources = LocalContext.current.resources
    val canDropOut = education.stage == SchoolStage.SECONDARY ||
        education.stage == SchoolStage.UNIVERSITY
    val showStudyEffort = education.stage == SchoolStage.PRIMARY ||
        education.stage == SchoolStage.SECONDARY ||
        education.stage == SchoolStage.UNIVERSITY
    val studyEffortEnabled = showStudyEffort &&
        !education.expelled &&
        education.droppedOutFrom == null
    val effortOutlook = when (education.plannedStudyEffort) {
        StudyEffort.SLACK -> stringResource(R.string.study_effort_outlook_slack)
        StudyEffort.NORMAL -> stringResource(R.string.study_effort_outlook_normal)
        StudyEffort.HARD -> stringResource(R.string.study_effort_outlook_hard)
    }

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
                text = EducationFormatter.formatStatus(education, resources, countryCode),
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
            if (studyEffortEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.label_study_effort),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = effortOutlook,
                    style = MaterialTheme.typography.bodySmall,
                    color = TealPrimary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = stringResource(R.string.tip_study_gpa),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StudyEffort.entries.forEach { effort ->
                        FilterChip(
                            selected = education.plannedStudyEffort == effort,
                            onClick = { onSetStudyEffort(effort) },
                            label = {
                                Text(
                                    text = when (effort) {
                                        StudyEffort.SLACK ->
                                            stringResource(R.string.study_effort_slack)
                                        StudyEffort.NORMAL ->
                                            stringResource(R.string.study_effort_normal)
                                        StudyEffort.HARD ->
                                            stringResource(R.string.study_effort_hard)
                                    },
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchoolLifeSectionCard(
    character: Character,
    onPerformSchoolActivity: (SchoolActivity, String?) -> Unit,
    onSchoolPersonInteraction: (String, SchoolPersonAction) -> Unit
) {
    val enrolled = !character.education.expelled &&
        (character.education.stage == SchoolStage.PRIMARY ||
            character.education.stage == SchoolStage.SECONDARY ||
            character.education.stage == SchoolStage.UNIVERSITY)
    if (!enrolled) return

    val available = remember(
        character.education,
        character.age,
        character.alive,
        character.criminalRecord.currentlyIncarcerated
    ) {
        schoolUiEngine.availableSchoolActivities(character)
    }
    var pendingActivity by remember { mutableStateOf<SchoolActivity?>(null) }
    var selectedPersonId by remember { mutableStateOf<String?>(null) }
    val people = character.education.schoolPeople
    val selectedPerson = people.find { it.id == selectedPersonId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.section_school_life),
                style = MaterialTheme.typography.labelMedium,
                color = GoldAccent
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.format_school_reputation,
                    character.education.schoolReputation
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            StatBar(
                type = StatType.HAPPINESS,
                value = character.education.schoolReputation,
                label = stringResource(R.string.label_school_reputation)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    character.education.academicActionDoneThisYear &&
                        character.education.socialActionDoneThisYear ->
                        stringResource(R.string.school_actions_both_done)
                    character.education.academicActionDoneThisYear ->
                        stringResource(R.string.school_actions_academic_done)
                    character.education.socialActionDoneThisYear ->
                        stringResource(R.string.school_actions_social_done)
                    else -> stringResource(R.string.school_actions_available)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.section_classmates),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.school_tap_person_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
            if (people.isEmpty()) {
                Text(
                    text = stringResource(R.string.school_empty_people),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    people.forEach { person ->
                        SchoolPersonRow(
                            person = person,
                            onClick = { selectedPersonId = person.id }
                        )
                    }
                }
            }

            if (available.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.section_school_activities),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    available.forEach { activity ->
                        val blocked = schoolActivityBlocked(character, activity)
                        OutlinedButton(
                            onClick = {
                                if (schoolActivityNeedsPersonPick(activity)) {
                                    pendingActivity = activity
                                } else {
                                    onPerformSchoolActivity(activity, null)
                                }
                            },
                            enabled = !blocked,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(schoolActivityLabel(activity))
                        }
                    }
                }
            }
        }
    }

    pendingActivity?.let { activity ->
        val candidates = peopleForActivity(character, activity)
        AlertDialog(
            onDismissRequest = { pendingActivity = null },
            title = { Text(schoolActivityLabel(activity)) },
            text = {
                if (candidates.isEmpty()) {
                    Text(stringResource(R.string.msg_school_person_missing))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        candidates.forEach { person ->
                            TextButton(
                                onClick = {
                                    onPerformSchoolActivity(activity, person.id)
                                    pendingActivity = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.school_pick_person,
                                        person.name
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pendingActivity = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    selectedPerson?.let { person ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedPersonId = null },
            sheetState = sheetState
        ) {
            SchoolPersonActionSheet(
                character = character,
                person = person,
                onAction = { action ->
                    onSchoolPersonInteraction(person.id, action)
                    selectedPersonId = null
                },
                onDismiss = { selectedPersonId = null }
            )
        }
    }
}

@Composable
private fun SchoolPersonActionSheet(
    character: Character,
    person: SchoolPerson,
    onAction: (SchoolPersonAction) -> Unit,
    onDismiss: () -> Unit
) {
    val actions = remember(character, person.id, person.role, person.age) {
        schoolUiEngine.availableSchoolPersonActions(character, person.id)
    }
    val giftCost = remember(character.countryCode, character.age) {
        schoolUiEngine.schoolGiftCost(character)
    }
    val expression = remember(person.relationshipLevel) {
        when {
            person.relationshipLevel >= 65 -> com.maisha.game.data.model.Expression.HAPPY
            person.relationshipLevel <= 35 -> com.maisha.game.data.model.Expression.SAD
            else -> com.maisha.game.data.model.Expression.NEUTRAL
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AvatarImage(
                config = person.avatarConfig,
                size = 72.dp,
                age = person.age,
                expression = expression,
                seed = person.id
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.format_age, person.age),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = buildString {
                        append(schoolRoleLabel(person.role))
                        person.subject?.let { append(" · ").append(it) }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = TealPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        StatBar(
            type = StatType.HAPPINESS,
            value = person.relationshipLevel,
            label = stringResource(R.string.label_school_bond)
        )

        if (person.traits.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.label_school_traits),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = person.traits.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        person.status?.let { status ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.label_school_status),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.label_school_secret),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = when {
                person.secretRevealed && !person.secret.isNullOrBlank() -> person.secret
                person.secret.isNullOrBlank() -> stringResource(R.string.school_secret_none)
                else -> stringResource(R.string.school_secret_locked)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (person.secretRevealed) CoralNegative else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.section_school_person_actions),
            style = MaterialTheme.typography.labelMedium,
            color = GoldAccent
        )
        Spacer(modifier = Modifier.height(8.dp))

        val rows = actions.chunked(2)
        rows.forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowActions.forEach { action ->
                    val label = when (action) {
                        SchoolPersonAction.CHAT -> stringResource(R.string.school_action_chat)
                        SchoolPersonAction.COMPLIMENT -> stringResource(R.string.school_action_compliment)
                        SchoolPersonAction.INSULT -> stringResource(R.string.school_action_insult)
                        SchoolPersonAction.ASK_OUT -> stringResource(R.string.school_action_ask_out)
                        SchoolPersonAction.SPREAD_RUMOR -> stringResource(R.string.school_action_spread_rumor)
                        SchoolPersonAction.BRIBE_GIFT -> stringResource(
                            R.string.school_action_gift_cost,
                            formatMoney(giftCost, character.countryCode)
                        )
                    }
                    OutlinedButton(
                        onClick = { onAction(action) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = action != SchoolPersonAction.BRIBE_GIFT ||
                            character.stats.money >= giftCost
                    ) {
                        Text(text = label, maxLines = 2, textAlign = TextAlign.Center)
                    }
                }
                if (rowActions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_cancel))
        }
    }
}

@Composable
private fun SchoolPersonRow(
    person: SchoolPerson,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            config = person.avatarConfig,
            size = 40.dp,
            age = person.age,
            seed = person.id
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(schoolRoleLabel(person.role))
                    person.subject?.let { append(" · ").append(it) }
                    if (person.traits.isNotEmpty()) {
                        append(" · ").append(person.traits.first())
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = stringResource(R.string.format_school_bond, person.relationshipLevel),
            style = MaterialTheme.typography.labelSmall,
            color = TealPrimary
        )
    }
}

@Composable
private fun schoolRoleLabel(role: SchoolRole): String = when (role) {
    SchoolRole.CLASSMATE -> stringResource(R.string.school_role_classmate)
    SchoolRole.BEST_CLASSMATE -> stringResource(R.string.school_role_best)
    SchoolRole.BULLY -> stringResource(R.string.school_role_bully)
    SchoolRole.TEACHER -> stringResource(R.string.school_role_teacher)
    SchoolRole.CRUSH -> stringResource(R.string.school_role_crush)
}

@Composable
private fun schoolActivityLabel(activity: SchoolActivity): String = when (activity) {
    SchoolActivity.STUDY_GROUP -> stringResource(R.string.school_activity_study_group)
    SchoolActivity.LIBRARY_STUDY -> stringResource(R.string.school_activity_library)
    SchoolActivity.ASK_TEACHER_HELP -> stringResource(R.string.school_activity_teacher_help)
    SchoolActivity.HANG_OUT -> stringResource(R.string.school_activity_hang_out)
    SchoolActivity.CONFRONT_BULLY -> stringResource(R.string.school_activity_confront_bully)
    SchoolActivity.SKIP_CLASS -> stringResource(R.string.school_activity_skip_class)
    SchoolActivity.SCHOOL_DANCE -> stringResource(R.string.school_activity_dance)
    SchoolActivity.CLUB_PRACTICE -> stringResource(R.string.school_activity_club_practice)
    SchoolActivity.GROUP_PROJECT -> stringResource(R.string.school_activity_group_project)
}

private fun schoolActivityNeedsPersonPick(activity: SchoolActivity): Boolean =
    activity == SchoolActivity.STUDY_GROUP ||
        activity == SchoolActivity.GROUP_PROJECT ||
        activity == SchoolActivity.ASK_TEACHER_HELP ||
        activity == SchoolActivity.HANG_OUT ||
        activity == SchoolActivity.CONFRONT_BULLY ||
        activity == SchoolActivity.SCHOOL_DANCE

private fun schoolActivityBlocked(character: Character, activity: SchoolActivity): Boolean {
    val social = activity == SchoolActivity.HANG_OUT ||
        activity == SchoolActivity.CONFRONT_BULLY ||
        activity == SchoolActivity.SKIP_CLASS ||
        activity == SchoolActivity.SCHOOL_DANCE
    return if (social) {
        character.education.socialActionDoneThisYear
    } else {
        character.education.academicActionDoneThisYear
    }
}

private fun peopleForActivity(character: Character, activity: SchoolActivity): List<SchoolPerson> {
    val people = character.education.schoolPeople
    return when (activity) {
        SchoolActivity.ASK_TEACHER_HELP -> people.filter { it.role == SchoolRole.TEACHER }
        SchoolActivity.CONFRONT_BULLY -> people.filter { it.role == SchoolRole.BULLY }
        SchoolActivity.SCHOOL_DANCE -> {
            val crush = people.filter { it.role == SchoolRole.CRUSH }
            if (crush.isNotEmpty()) crush
            else people.filter {
                it.role == SchoolRole.CLASSMATE ||
                    it.role == SchoolRole.BEST_CLASSMATE
            }
        }
        else -> people.filter {
            it.role == SchoolRole.CLASSMATE ||
                it.role == SchoolRole.BEST_CLASSMATE ||
                it.role == SchoolRole.CRUSH ||
                it.role == SchoolRole.BULLY
        }
    }
}

@Composable
private fun SchoolClubSectionCard(
    character: Character,
    onJoinSchoolClub: (SchoolClub) -> Unit
) {
    val eligible = character.age in EducationEngine.SCHOOL_CLUB_MIN_AGE..EducationEngine.SCHOOL_CLUB_MAX_AGE &&
        !character.education.expelled &&
        character.education.droppedOutFrom == null &&
        (character.education.stage == SchoolStage.SECONDARY ||
            (character.education.stage == SchoolStage.PRIMARY && character.education.currentGrade >= 6))
    if (!eligible) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.section_school_clubs),
                style = MaterialTheme.typography.labelMedium,
                color = GoldAccent
            )
            character.education.schoolClub?.let { active ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.format_active_club, schoolClubLabel(active)),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(SchoolClub.DEBATE, SchoolClub.FOOTBALL, SchoolClub.DRAMA).forEach { club ->
                        FilterChip(
                            selected = character.education.schoolClub == club,
                            onClick = { onJoinSchoolClub(club) },
                            label = { Text(schoolClubLabel(club), maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(SchoolClub.CODING, SchoolClub.MUSIC).forEach { club ->
                        FilterChip(
                            selected = character.education.schoolClub == club,
                            onClick = { onJoinSchoolClub(club) },
                            label = { Text(schoolClubLabel(club), maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun schoolClubLabel(club: SchoolClub): String = when (club) {
    SchoolClub.DEBATE -> stringResource(R.string.club_debate)
    SchoolClub.FOOTBALL -> stringResource(R.string.club_football)
    SchoolClub.DRAMA -> stringResource(R.string.club_drama)
    SchoolClub.CODING -> stringResource(R.string.club_coding)
    SchoolClub.MUSIC -> stringResource(R.string.club_music)
}

@Composable
private fun CareerTrackSectionCard(
    character: Character,
    onStartCareerTrack: (CareerTrack) -> Unit,
    onPracticeCareerTrack: () -> Unit
) {
    if (character.age < CareerEngine.MIN_TRACK_AGE) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.section_career_tracks),
                style = MaterialTheme.typography.labelMedium,
                color = GoldAccent
            )
            Spacer(modifier = Modifier.height(6.dp))
            when (character.career.careerTrack) {
                CareerTrack.NONE -> {
                    Text(
                        text = stringResource(R.string.career_track_pick_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onStartCareerTrack(CareerTrack.ENTERTAINMENT) }) {
                                Text(stringResource(R.string.track_entertainment))
                            }
                            OutlinedButton(onClick = { onStartCareerTrack(CareerTrack.PRO_SPORTS) }) {
                                Text(stringResource(R.string.track_pro_sports))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onStartCareerTrack(CareerTrack.MEDICAL) }) {
                                Text(stringResource(R.string.track_medical))
                            }
                            OutlinedButton(onClick = { onStartCareerTrack(CareerTrack.LEGAL) }) {
                                Text(stringResource(R.string.track_legal))
                            }
                        }
                    }
                }
                else -> {
                    Text(
                        text = stringResource(
                            R.string.format_track_progress,
                            careerTrackLabel(character.career.careerTrack),
                            character.career.trackLevel,
                            character.career.trackProgress
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onPracticeCareerTrack,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.btn_practice_track))
                    }
                }
            }
        }
    }
}

@Composable
private fun careerTrackLabel(track: CareerTrack): String = when (track) {
    CareerTrack.ENTERTAINMENT -> stringResource(R.string.track_entertainment)
    CareerTrack.PRO_SPORTS -> stringResource(R.string.track_pro_sports)
    CareerTrack.MEDICAL -> stringResource(R.string.track_medical)
    CareerTrack.LEGAL -> stringResource(R.string.track_legal)
    CareerTrack.NONE -> ""
}

@Composable
private fun CurrentJobCard(
    character: Character,
    canRetire: Boolean,
    retirementPensionEstimate: Int,
    onQuitJob: () -> Unit,
    onRetire: () -> Unit,
    onSetWorkEffort: (com.maisha.game.data.model.WorkEffort) -> Unit
) {
    val job = character.career.currentJob ?: return
    val resources = LocalContext.current.resources
    val militaryAccent = Color(0xFF556B2F)
    val cardColor = if (job.isMilitary) {
        militaryAccent.copy(alpha = 0.22f)
    } else {
        Color.White
    }
    val effortOutlook = when (character.career.plannedWorkEffort) {
        WorkEffort.COAST -> stringResource(R.string.work_effort_outlook_coast)
        WorkEffort.NORMAL -> stringResource(R.string.work_effort_outlook_normal)
        WorkEffort.GRIND -> stringResource(R.string.work_effort_outlook_grind)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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

            Text(
                text = stringResource(R.string.label_work_effort),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = effortOutlook,
                style = MaterialTheme.typography.bodySmall,
                color = TealPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = stringResource(R.string.work_effort_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WorkEffort.entries.forEach { effort ->
                    FilterChip(
                        selected = character.career.plannedWorkEffort == effort,
                        onClick = { onSetWorkEffort(effort) },
                        enabled = character.alive,
                        label = {
                            Text(
                                text = when (effort) {
                                    WorkEffort.COAST ->
                                        stringResource(R.string.work_effort_coast)
                                    WorkEffort.NORMAL ->
                                        stringResource(R.string.work_effort_normal)
                                    WorkEffort.GRIND ->
                                        stringResource(R.string.work_effort_grind)
                                },
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            StatBar(
                type = StatType.PERFORMANCE,
                value = job.performanceScore,
                label = stringResource(R.string.stat_performance)
            )

            if (canRetire) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.format_pension_estimate,
                        formatMoney(retirementPensionEstimate, character.countryCode)
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onQuitJob,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = character.alive
            ) {
                Text(stringResource(R.string.btn_quit_job))
            }
            if (canRetire) {
                Spacer(modifier = Modifier.height(8.dp))
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
