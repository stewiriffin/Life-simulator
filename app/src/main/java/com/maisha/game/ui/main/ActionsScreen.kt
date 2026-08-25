package com.maisha.game.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maisha.game.R
import com.maisha.game.data.CountryCatalog
import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.JobPool
import com.maisha.game.data.PetCatalog
import com.maisha.game.data.model.BucketGoalKind
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.CrimeType
import com.maisha.game.data.model.FameTier
import com.maisha.game.data.model.HealthCondition
import com.maisha.game.data.model.HustleType
import com.maisha.game.data.model.LifestyleOption
import com.maisha.game.data.model.PetSpecies
import com.maisha.game.data.model.PrisonActivity
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.SkillType
import com.maisha.game.data.local.OnboardingTips
import com.maisha.game.data.model.PartTimeDemand
import com.maisha.game.data.model.PartTimeJob
import com.maisha.game.domain.CareerEngine
import com.maisha.game.domain.CrimeEngine
import com.maisha.game.domain.ActionFamily
import com.maisha.game.domain.ActionQuestHints
import com.maisha.game.domain.BucketListEngine
import com.maisha.game.domain.CrimeStatusKind
import com.maisha.game.domain.CrimeStatusMapper
import com.maisha.game.domain.EducationEngine
import com.maisha.game.domain.HealthEngine
import com.maisha.game.domain.LeisureActivity
import com.maisha.game.domain.LeisureEngine
import com.maisha.game.domain.RelationshipEngine
import com.maisha.game.domain.RelocationEngine
import com.maisha.game.domain.SkillEngine
import com.maisha.game.domain.SkillMasteryTier
import com.maisha.game.domain.SocialMediaEngine
import com.maisha.game.domain.YearQuest
import com.maisha.game.ui.components.ActionCard
import com.maisha.game.ui.components.ActionCardAccent
import com.maisha.game.ui.components.CategoryFilterChipRow
import com.maisha.game.ui.components.ConditionBadge
import com.maisha.game.ui.components.ConfirmActionDialog
import com.maisha.game.ui.components.ConfirmSeverity
import com.maisha.game.ui.components.ConfirmableActionHost
import com.maisha.game.ui.components.DismissibleTipCard
import com.maisha.game.ui.components.EmptyStateCard
import com.maisha.game.ui.components.StatBar
import com.maisha.game.ui.components.StatType
import com.maisha.game.ui.components.TabPageHero
import com.maisha.game.ui.components.rememberConfirmableAction
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.theme.AppIcons
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.CreamBg
import com.maisha.game.ui.theme.InkPrimary
import com.maisha.game.ui.theme.InkTertiary
import com.maisha.game.ui.theme.LifeGreen
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.formatMoney

private const val CRIME_UI_MIN_AGE = 16
private const val SIDE_HUSTLE_UI_MIN_AGE = 14

private enum class ActionCategory {
    ALL, CARE, EARN, GROW, LIVE, RISK
}

private sealed class PendingAction {
    data class Crime(val type: CrimeType) : PendingAction()
    data class Treatment(val condition: HealthCondition, val careType: CareType) : PendingAction()
    data class Lifestyle(val option: LifestyleOption, val enable: Boolean) : PendingAction()
    data class SideHustle(val type: HustleType) : PendingAction()
    data class AdoptPet(val species: PetSpecies) : PendingAction()
    data object CreateSocialAccount : PendingAction()
    data object MonetizeSocialAccount : PendingAction()
    data class PracticeSkill(val type: SkillType) : PendingAction()
    data class Masterclass(val type: SkillType) : PendingAction()
    data class ShowcaseSkill(val type: SkillType) : PendingAction()
    data class AdoptBucket(val templateId: String) : PendingAction()
    data object RenewVisa : PendingAction()
    data object ApplyCitizenship : PendingAction()
    data object TakeDrivingTest : PendingAction()
    data object Volunteer : PendingAction()
    data class Donate(val amount: Int) : PendingAction()
    data class Leisure(val activity: LeisureActivity) : PendingAction()
    data object AdoptChild : PendingAction()
    data object RequestExpungement : PendingAction()
}

@Composable
fun ActionsScreen(
    character: Character,
    uiState: LifeUiState,
    snackbarHostState: SnackbarHostState,
    onAttemptCrime: (CrimeType) -> Unit,
    onVisitDoctor: (String, CareType) -> Unit,
    onSetLifestyleOption: (LifestyleOption, Boolean) -> Unit,
    onExecuteSideHustle: (HustleType) -> Unit,
    onAdoptPet: (PetSpecies) -> Unit,
    onCreateSocialAccount: () -> Unit,
    onPostSocialContent: () -> Unit,
    onMonetizeSocialAccount: () -> Unit,
    onPracticeSkill: (SkillType) -> Unit,
    onTakeMasterclass: (SkillType) -> Unit,
    onShowcaseSkill: (SkillType) -> Unit,
    onAdoptBucketGoal: (String) -> Unit,
    onRenewVisa: () -> Unit,
    onApplyForCitizenship: () -> Unit,
    onTakeDrivingTest: () -> Unit,
    onVolunteer: () -> Unit,
    onDonateToCharity: (Int) -> Unit,
    donationTiers: List<Int>,
    onPerformLeisure: (LeisureActivity) -> Unit,
    onPerformStudySession: () -> Unit = {},
    onPerformPrisonActivity: (PrisonActivity) -> Unit = {},
    onAdoptChild: () -> Unit = {},
    onRequestExpungement: () -> Unit = {},
    onWorkPartTime: (com.maisha.game.data.model.PartTimeJob) -> Unit = {},
    onQuitPartTimeJob: () -> Unit = {},
    onRestStudentEnergy: () -> Unit = {},
    onActionMessageDismissed: () -> Unit,
    onDismissLeisureTip: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val incarcerated = character.criminalRecord.currentlyIncarcerated
    val awaitingTrial = character.criminalRecord.awaitingTrial
    val untreated = character.activeConditions.filter { !it.treated }
    val yearQuests = uiState.yearQuests
    val questHintLabel = stringResource(R.string.action_quest_hint)
    val leisureEngine = remember { LeisureEngine() }

    val showCrimeActions = character.age >= CRIME_UI_MIN_AGE &&
        !incarcerated && !awaitingTrial && character.alive
    val showLifestyleActions = character.alive && !incarcerated && !awaitingTrial
    val showLeisureActions = showLifestyleActions &&
        leisureEngine.activitiesFor(character).isNotEmpty()
    val showSideHustleActions = character.alive &&
        character.age >= SIDE_HUSTLE_UI_MIN_AGE &&
        !incarcerated &&
        !awaitingTrial &&
        !character.career.isRetired
    val showAdoptPetActions = character.alive &&
        !incarcerated &&
        !awaitingTrial &&
        character.pets.size < RelationshipEngine.MAX_PETS
    val showSocialMediaActions = character.alive &&
        character.age >= SocialMediaEngine.MIN_ACCOUNT_AGE &&
        !incarcerated &&
        !awaitingTrial
    val showSkillActions = character.alive &&
        character.age >= SkillEngine.MIN_SKILL_AGE &&
        !incarcerated &&
        !awaitingTrial
    val showBucketList = character.alive &&
        character.age >= 14 &&
        !incarcerated &&
        !awaitingTrial
    val showImmigrationOffice = character.alive &&
        character.isLivingAbroad() &&
        !incarcerated &&
        !awaitingTrial
    val showDrivingTest = character.alive &&
        !character.hasDrivingLicense &&
        character.age >= EducationEngine.MIN_DRIVING_AGE &&
        !incarcerated &&
        !awaitingTrial
    val showPhilanthropy = character.alive &&
        character.age >= 12 &&
        !incarcerated &&
        !awaitingTrial
    val crimeEngine = remember { CrimeEngine() }
    val careerEngine = remember { CareerEngine(HealthEngine(), RelocationEngine()) }
    val showStudentPartTime = character.alive &&
        !incarcerated &&
        !awaitingTrial &&
        careerEngine.canWorkPartTime(character)
    val showAdoptChild = character.alive &&
        character.age >= RelationshipEngine.MIN_ADOPT_AGE &&
        !incarcerated &&
        !awaitingTrial
    val showExpungement = character.alive &&
        !incarcerated &&
        !awaitingTrial &&
        crimeEngine.canRequestExpungement(character)
    val showStudySession = character.alive &&
        !incarcerated &&
        !awaitingTrial &&
        (character.education.stage == SchoolStage.PRIMARY ||
            character.education.stage == SchoolStage.SECONDARY ||
            character.education.stage == SchoolStage.UNIVERSITY) &&
        !character.education.expelled &&
        character.education.droppedOutFrom == null
    val donationAmounts = donationTiers.ifEmpty {
        listOf(100, 1_000, 10_000).map {
            EconomyScaler.scaleAmount(it, character.countryCode)
        }
    }
    val drivingTestFee = EconomyScaler.scaleAmount(
        EducationEngine.DRIVING_TEST_FEE_KENYA,
        character.countryCode
    )
    val canRenewVisa = showImmigrationOffice && character.currentVisa != null
    val canApplyCitizenship = showImmigrationOffice &&
        character.yearsInCurrentCountry >= RelocationEngine.NATURALIZATION_YEARS
    val visaRenewalFee = EconomyScaler.scaleAmount(
        RelocationEngine.VISA_RENEWAL_FEE_KENYA,
        character.countryCode
    )
    val citizenshipFee = EconomyScaler.scaleAmount(
        RelocationEngine.CITIZENSHIP_FEE_KENYA,
        character.countryCode
    )
    val masterclassCost = EconomyScaler.scaleAmount(
        SkillEngine.MASTERCLASS_BASE_COST_KENYA,
        character.countryCode
    )
    val crimeStatus = CrimeStatusMapper.map(character.criminalRecord)

    val showPrisonActions = incarcerated && character.alive
    val showJobsAndHustles = character.alive &&
        !incarcerated &&
        !awaitingTrial &&
        (showStudentPartTime || showSideHustleActions)
    val hasCare = untreated.isNotEmpty() || showLifestyleActions || showLeisureActions || showStudySession
    val hasEarn = showJobsAndHustles || showSocialMediaActions || showSkillActions
    val hasGrow = showSkillActions || showBucketList || showAdoptPetActions || showSocialMediaActions || showStudySession
    val hasLive = showDrivingTest || showPhilanthropy || showImmigrationOffice || showLeisureActions ||
        showStudentPartTime || showAdoptChild || showExpungement
    val hasRisk = showCrimeActions || incarcerated || awaitingTrial || showPrisonActions
    val hasContent = hasCare || hasEarn || hasGrow || hasLive || hasRisk
    val leisureActivities = remember(character.age, character.criminalRecord) {
        leisureEngine.activitiesFor(character)
    }
    val actionCount = remember(
        leisureActivities.size,
        showStudySession,
        untreated.size,
        showJobsAndHustles,
        showSkillActions
    ) {
        var count = leisureActivities.size
        if (showStudySession) count++
        count += untreated.size
        if (showJobsAndHustles) {
            if (showStudentPartTime) count += PartTimeJob.entries.size
            if (showSideHustleActions) count += JobPool.getAllSideHustleTypes().size
        }
        if (showSkillActions) count += SkillType.entries.size
        count
    }

    val defaultCategory = when {
        untreated.isNotEmpty() -> ActionCategory.CARE
        incarcerated -> ActionCategory.RISK
        character.age < 18 -> ActionCategory.LIVE
        else -> ActionCategory.ALL
    }
    var selectedCategory by rememberSaveable { mutableStateOf(defaultCategory.name) }
    val category = ActionCategory.entries.find { it.name == selectedCategory } ?: ActionCategory.ALL

    fun show(cat: ActionCategory): Boolean =
        category == ActionCategory.ALL || category == cat

    val pendingAction = rememberConfirmableAction<PendingAction>()
    var expandedConditionId by remember { mutableStateOf<String?>(null) }
    var expandedSkill by rememberSaveable { mutableStateOf<String?>(null) }

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
            .background(CreamBg)
    ) {
        TabPageHero(
            title = stringResource(R.string.screen_actions),
            subtitle = if (hasContent && actionCount > 0) {
                stringResource(R.string.format_actions_this_year, actionCount)
            } else {
                stringResource(R.string.actions_subtitle)
            },
            primaryChip = formatMoney(character.stats.money, character.countryCode)
        )

        val chipLabels = listOf(
            stringResource(R.string.chip_actions_all),
            stringResource(R.string.chip_actions_care),
            stringResource(R.string.chip_actions_earn),
            stringResource(R.string.chip_actions_grow),
            stringResource(R.string.chip_actions_live),
            stringResource(R.string.chip_actions_risk)
        )
        CategoryFilterChipRow(
            labels = chipLabels,
            selectedIndex = ActionCategory.entries.indexOf(category).coerceAtLeast(0),
            onSelected = { selectedCategory = ActionCategory.entries[it].name },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        val showLeisureTip = uiState.tipsLoaded &&
            OnboardingTips.LEISURE !in uiState.seenTipIds &&
            showLeisureActions
        if (showLeisureTip) {
            DismissibleTipCard(
                text = stringResource(R.string.tip_leisure),
                onDismiss = onDismissLeisureTip,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }

        if (!hasContent) {
            val isToddler = character.age <= 4
            EmptyStateCard(
                illustration = EmptyStateIllustration.ACTIONS,
                title = stringResource(
                    if (isToddler) R.string.empty_actions_toddler_title else R.string.empty_actions_title
                ),
                message = stringResource(
                    if (isToddler) R.string.empty_actions_toddler_body else R.string.empty_actions_body
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = MaishaSpacing.md)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (show(ActionCategory.RISK) && crimeStatus.kind != CrimeStatusKind.CLEAR) {
                    item {
                        CrimeStatusCard(statusKind = crimeStatus.kind, yearsRemaining = crimeStatus.yearsRemaining)
                    }
                }

                if (show(ActionCategory.RISK) && showPrisonActions) {
                    item { SectionHeader(title = stringResource(R.string.section_prison)) }
                    items(PrisonActivity.entries.toList(), key = { it.name }) { activity ->
                        ActionCard(
                            icon = AppIcons.Health,
                            title = prisonActivityTitle(activity),
                            description = prisonActivityDescription(activity),
                            accent = ActionCardAccent.CARE,
                            onClick = { onPerformPrisonActivity(activity) }
                        )
                    }
                }

                if (show(ActionCategory.CARE) && untreated.isNotEmpty()) {
                    item { SectionHeader(title = stringResource(R.string.section_health)) }
                    items(untreated, key = { it.id }) { condition ->
                        val isExpanded = expandedConditionId == condition.id
                        val treatmentHint = questHintIf(yearQuests, ActionFamily.TREATMENT, questHintLabel)
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
                                    character,
                                    condition.severity,
                                    CareType.PUBLIC_CLINIC
                                ),
                                accent = ActionCardAccent.CARE,
                                questHint = treatmentHint,
                                onClick = {
                                    pendingAction.request(
                                        PendingAction.Treatment(condition, CareType.PUBLIC_CLINIC)
                                    )
                                }
                            )
                            ActionCard(
                                icon = AppIcons.HealthHospital,
                                title = CountryCatalog.flavorFor(character.countryCode).privateHospitalName,
                                description = successHint(CareType.PRIVATE_HOSPITAL),
                                metaLabel = treatmentCostLabel(
                                    character,
                                    condition.severity,
                                    CareType.PRIVATE_HOSPITAL
                                ),
                                accent = ActionCardAccent.CARE,
                                questHint = treatmentHint,
                                onClick = {
                                    pendingAction.request(
                                        PendingAction.Treatment(condition, CareType.PRIVATE_HOSPITAL)
                                    )
                                }
                            )
                        }
                    }
                }

                if (show(ActionCategory.CARE) && showLifestyleActions) {
                    item { SectionHeader(title = stringResource(R.string.section_wellness)) }
                    item {
                        LifestyleActionCard(
                            character = character,
                            option = LifestyleOption.GYM,
                            activeTitleRes = R.string.lifestyle_gym_active,
                            inactiveTitleRes = R.string.lifestyle_gym_title,
                            descriptionRes = R.string.lifestyle_gym_desc,
                            yearlyCost = lifestyleYearlyCost(LifestyleOption.GYM, character.countryCode),
                            icon = AppIcons.Health,
                            questHint = questHintIf(yearQuests, ActionFamily.LIFESTYLE_WELLNESS, questHintLabel),
                            onToggle = { enable ->
                                pendingAction.request(PendingAction.Lifestyle(LifestyleOption.GYM, enable))
                            }
                        )
                    }
                    item {
                        LifestyleActionCard(
                            character = character,
                            option = LifestyleOption.DIET,
                            activeTitleRes = R.string.lifestyle_diet_active,
                            inactiveTitleRes = R.string.lifestyle_diet_title,
                            descriptionRes = R.string.lifestyle_diet_desc,
                            yearlyCost = lifestyleYearlyCost(LifestyleOption.DIET, character.countryCode),
                            icon = AppIcons.Looks,
                            questHint = questHintIf(yearQuests, ActionFamily.LIFESTYLE_WELLNESS, questHintLabel),
                            onToggle = { enable ->
                                pendingAction.request(PendingAction.Lifestyle(LifestyleOption.DIET, enable))
                            }
                        )
                    }
                    item {
                        LifestyleActionCard(
                            character = character,
                            option = LifestyleOption.THERAPIST,
                            activeTitleRes = R.string.lifestyle_therapist_active,
                            inactiveTitleRes = R.string.lifestyle_therapist_title,
                            descriptionRes = R.string.lifestyle_therapist_desc,
                            yearlyCost = lifestyleYearlyCost(
                                LifestyleOption.THERAPIST,
                                character.countryCode
                            ),
                            icon = AppIcons.Happiness,
                            questHint = questHintIf(yearQuests, ActionFamily.LIFESTYLE_WELLNESS, questHintLabel),
                            onToggle = { enable ->
                                pendingAction.request(
                                    PendingAction.Lifestyle(LifestyleOption.THERAPIST, enable)
                                )
                            }
                        )
                    }
                    item {
                        LifestyleActionCard(
                            character = character,
                            option = LifestyleOption.HEALTH_INSURANCE,
                            activeTitleRes = R.string.lifestyle_insurance_active,
                            inactiveTitleRes = R.string.lifestyle_insurance_title,
                            descriptionRes = R.string.lifestyle_insurance_desc,
                            yearlyCost = lifestyleYearlyCost(
                                LifestyleOption.HEALTH_INSURANCE,
                                character.countryCode
                            ),
                            icon = AppIcons.Health,
                            questHint = questHintIf(yearQuests, ActionFamily.LIFESTYLE_WELLNESS, questHintLabel),
                            onToggle = { enable ->
                                pendingAction.request(
                                    PendingAction.Lifestyle(LifestyleOption.HEALTH_INSURANCE, enable)
                                )
                            }
                        )
                    }
                }

                if ((show(ActionCategory.CARE) || show(ActionCategory.LIVE)) && showLeisureActions) {
                    item { SectionHeader(title = stringResource(R.string.section_leisure)) }
                    items(leisureActivities, key = { it.name }) { activity ->
                        val cost = leisureEngine.cost(activity, character.countryCode)
                        val isChores = activity == LeisureActivity.CHORES
                        val canAfford = isChores || character.stats.money >= cost
                        val meta = if (isChores) {
                            stringResource(
                                R.string.format_leisure_meta,
                                stringResource(R.string.meta_earn_cash),
                                leisureEffectLabel(activity)
                            )
                        } else {
                            stringResource(
                                R.string.format_leisure_meta,
                                formatMoney(cost, character.countryCode),
                                leisureEffectLabel(activity)
                            )
                        }
                        ActionCard(
                            icon = AppIcons.Happiness,
                            title = leisureTitle(activity),
                            description = leisureDescription(activity),
                            metaLabel = meta,
                            enabled = canAfford,
                            accent = ActionCardAccent.CARE,
                            questHint = questHintIf(yearQuests, ActionFamily.LEISURE, questHintLabel),
                            onClick = {
                                if (canAfford) {
                                    pendingAction.request(PendingAction.Leisure(activity))
                                }
                            }
                        )
                    }
                }

                if ((show(ActionCategory.CARE) || show(ActionCategory.GROW)) && showStudySession) {
                    item { SectionHeader(title = stringResource(R.string.label_education)) }
                    item {
                        ActionCard(
                            icon = AppIcons.Education,
                            title = stringResource(R.string.action_study_session_title),
                            description = stringResource(R.string.action_study_session_desc),
                            metaLabel = stringResource(R.string.study_effort_hard),
                            accent = ActionCardAccent.CARE,
                            questHint = questHintIf(yearQuests, ActionFamily.STUDY, questHintLabel),
                            onClick = onPerformStudySession
                        )
                    }
                }

                if ((show(ActionCategory.EARN) || show(ActionCategory.LIVE)) && showJobsAndHustles) {
                    item { SectionHeader(title = stringResource(R.string.section_jobs_side_hustles)) }
                    item(key = "jobs_hustles_energy") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaishaRadius.cardShape,
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = stringResource(
                                        R.string.jobs_hustles_energy,
                                        character.career.energyLevel
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                StatBar(
                                    type = StatType.HEALTH,
                                    value = character.career.energyLevel,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                character.career.activePartTimeJob?.let { active ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.jobs_hustles_active,
                                            active.displayLabel
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkTertiary
                                    )
                                }
                                if (showStudentPartTime) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.jobs_hustles_balance_hint),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkTertiary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.jobs_hustles_year_end_hint),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkTertiary
                                    )
                                }
                                val canRest = showStudentPartTime &&
                                    !character.career.energyRestedThisYear &&
                                    character.career.energyLevel < 95
                                if (canRest) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    androidx.compose.material3.OutlinedButton(
                                        onClick = onRestStudentEnergy,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.btn_rest_energy))
                                    }
                                }
                                if (character.career.activePartTimeJob != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    androidx.compose.material3.OutlinedButton(
                                        onClick = onQuitPartTimeJob,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.btn_quit_part_time))
                                    }
                                }
                            }
                        }
                    }
                    if (showStudentPartTime) {
                        PartTimeJob.entries.forEach { job ->
                            item(key = "part_time_${job.name}") {
                                val available = careerEngine.isPartTimeListingAvailable(character, job)
                                val (minPay, maxPay) = careerEngine.partTimePayoutRange(
                                    job,
                                    character.countryCode
                                )
                                ActionCard(
                                    icon = AppIcons.Money,
                                    title = partTimeJobTitle(job),
                                    description = partTimeDemandLabel(job.demand),
                                    metaLabel = when {
                                        character.career.partTimeWorkedThisYear ->
                                            stringResource(R.string.msg_part_time_already)
                                        !available -> stringResource(R.string.msg_side_hustle_prerequisites)
                                        else -> stringResource(
                                            R.string.part_time_payout_range,
                                            formatMoney(minPay, character.countryCode),
                                            formatMoney(maxPay, character.countryCode)
                                        )
                                    },
                                    enabled = available,
                                    accent = ActionCardAccent.GOLD,
                                    onClick = { if (available) onWorkPartTime(job) }
                                )
                            }
                        }
                    }
                    if (showSideHustleActions) {
                        items(JobPool.getAllSideHustleTypes(), key = { "hustle_${it.name}" }) { hustleType ->
                            SideHustleActionCard(
                                character = character,
                                hustleType = hustleType,
                                questHint = questHintIf(yearQuests, ActionFamily.SIDE_HUSTLE, questHintLabel),
                                onClick = {
                                    pendingAction.request(PendingAction.SideHustle(hustleType))
                                }
                            )
                        }
                    }
                }

                if ((show(ActionCategory.EARN) || show(ActionCategory.GROW)) && showSocialMediaActions) {
                    item { SectionHeader(title = stringResource(R.string.section_social_media)) }
                    if (!character.socialMedia.hasAccount) {
                        item {
                            ActionCard(
                                icon = AppIcons.Looks,
                                title = stringResource(R.string.btn_create_social_account),
                                description = stringResource(R.string.social_create_desc),
                                metaLabel = stringResource(R.string.social_create_meta),
                                onClick = {
                                    pendingAction.request(PendingAction.CreateSocialAccount)
                                }
                            )
                        }
                    } else {
                        if (show(ActionCategory.EARN) || category == ActionCategory.ALL ||
                            category == ActionCategory.GROW
                        ) {
                            item {
                                Text(
                                    text = stringResource(
                                        R.string.format_fame_badge,
                                        fameTierLabel(character.socialMedia.fameTier)
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealPrimary
                                )
                            }
                        }
                        if (show(ActionCategory.GROW) || category == ActionCategory.ALL ||
                            category == ActionCategory.EARN
                        ) {
                            item {
                                ActionCard(
                                    icon = AppIcons.Looks,
                                    title = stringResource(R.string.btn_post_social_update),
                                    description = stringResource(R.string.social_post_desc),
                                    metaLabel = stringResource(
                                        R.string.format_social_followers,
                                        character.socialMedia.followers,
                                        if (character.socialMedia.isVerified) {
                                            stringResource(R.string.social_verified_badge)
                                        } else {
                                            ""
                                        }
                                    ),
                                    questHint = questHintIf(
                                        yearQuests,
                                        ActionFamily.SOCIAL_POST,
                                        questHintLabel
                                    ),
                                    onClick = onPostSocialContent
                                )
                            }
                        }
                        if (show(ActionCategory.EARN) || category == ActionCategory.ALL) {
                            item {
                                val canMonetize = character.socialMedia.followers >=
                                    SocialMediaEngine.MONETIZATION_FOLLOWER_THRESHOLD &&
                                    !character.socialMedia.monetizedThisYear
                                ActionCard(
                                    icon = AppIcons.Money,
                                    title = stringResource(R.string.btn_monetize_social),
                                    description = stringResource(R.string.social_monetize_desc),
                                    metaLabel = when {
                                        character.socialMedia.monetizedThisYear ->
                                            stringResource(R.string.msg_social_already_monetized)
                                        character.socialMedia.followers <
                                            SocialMediaEngine.MONETIZATION_FOLLOWER_THRESHOLD ->
                                            stringResource(
                                                R.string.format_social_monetize_req,
                                                SocialMediaEngine.MONETIZATION_FOLLOWER_THRESHOLD
                                            )
                                        else -> stringResource(R.string.social_monetize_ready)
                                    },
                                    enabled = canMonetize,
                                    accent = ActionCardAccent.GOLD,
                                    questHint = questHintIf(
                                        yearQuests,
                                        ActionFamily.SOCIAL_MONETIZE,
                                        questHintLabel
                                    ),
                                    onClick = {
                                        if (canMonetize) {
                                            pendingAction.request(PendingAction.MonetizeSocialAccount)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if ((show(ActionCategory.EARN) || show(ActionCategory.GROW)) && showSkillActions) {
                    item { SectionHeader(title = stringResource(R.string.section_hobbies_skills)) }
                    items(SkillType.entries.toList(), key = { it.name }) { skillType ->
                        val level = character.skills.find { it.type == skillType }?.level ?: 0
                        CollapsibleSkillRow(
                            skillType = skillType,
                            level = level,
                            expanded = expandedSkill == skillType.name,
                            onToggleExpand = {
                                expandedSkill =
                                    if (expandedSkill == skillType.name) null else skillType.name
                            },
                            masterclassCost = masterclassCost,
                            countryCode = character.countryCode,
                            money = character.stats.money,
                            canShowcase = level >= SkillEngine.TIER_MASTER_MIN &&
                                !character.skillShowcaseDoneThisYear,
                            practiceHint = questHintIf(
                                yearQuests,
                                ActionFamily.SKILL_PRACTICE,
                                questHintLabel
                            ),
                            showcaseHint = questHintIf(
                                yearQuests,
                                ActionFamily.SKILL_SHOWCASE,
                                questHintLabel
                            ),
                            showEarnActions = show(ActionCategory.EARN) || category == ActionCategory.ALL,
                            showGrowActions = show(ActionCategory.GROW) || category == ActionCategory.ALL,
                            onPractice = {
                                pendingAction.request(PendingAction.PracticeSkill(skillType))
                            },
                            onMasterclass = {
                                pendingAction.request(PendingAction.Masterclass(skillType))
                            },
                            onShowcase = {
                                pendingAction.request(PendingAction.ShowcaseSkill(skillType))
                            }
                        )
                    }
                }

                if (show(ActionCategory.GROW) && showAdoptPetActions) {
                    item { SectionHeader(title = stringResource(R.string.section_adopt_pet)) }
                    items(PetCatalog.getAll(), key = { it.species.name }) { entry ->
                        val adoptionCost = EconomyScaler.scaleAmount(
                            entry.adoptionFee,
                            character.countryCode
                        )
                        val yearlyCost = EconomyScaler.scaleAmount(
                            entry.yearlyUpkeep,
                            character.countryCode
                        )
                        val canAfford = character.stats.money >= adoptionCost
                        ActionCard(
                            icon = AppIcons.Family,
                            title = adoptPetTitle(entry.species),
                            description = adoptPetDescription(entry.species),
                            metaLabel = stringResource(
                                R.string.format_pet_adoption_cost,
                                formatMoney(adoptionCost, character.countryCode),
                                formatMoney(yearlyCost, character.countryCode)
                            ),
                            enabled = canAfford,
                            onClick = {
                                if (canAfford) {
                                    pendingAction.request(PendingAction.AdoptPet(entry.species))
                                }
                            }
                        )
                    }
                }

                if (show(ActionCategory.GROW) && showBucketList) {
                    item { SectionHeader(title = stringResource(R.string.section_bucket_list)) }
                    items(
                        character.bucketList.filter { !it.completed },
                        key = { "active_${it.id}" }
                    ) { goal ->
                        ActionCard(
                            icon = AppIcons.Smarts,
                            title = bucketTitle(goal.kind),
                            description = bucketDescription(goal.kind),
                            metaLabel = stringResource(
                                R.string.format_bucket_in_progress,
                                bucketTitle(goal.kind)
                            ),
                            enabled = false,
                            accent = ActionCardAccent.GOLD,
                            onClick = {}
                        )
                    }
                    items(
                        character.bucketList.filter { it.completed },
                        key = { "done_${it.id}" }
                    ) { goal ->
                        ActionCard(
                            icon = AppIcons.Smarts,
                            title = bucketTitle(goal.kind),
                            description = bucketDescription(goal.kind),
                            metaLabel = stringResource(R.string.bucket_completed_badge),
                            enabled = false,
                            accent = ActionCardAccent.CARE,
                            onClick = {}
                        )
                    }
                    items(
                        BucketListEngine().availableTemplates(character),
                        key = { "tpl_${it.id}" }
                    ) { template ->
                        val cost = EconomyScaler.scaleAmount(
                            template.commitmentKenya,
                            character.countryCode
                        )
                        val canAfford = character.stats.money >= cost
                        ActionCard(
                            icon = AppIcons.Money,
                            title = bucketTitle(template.kind),
                            description = bucketDescription(template.kind),
                            metaLabel = stringResource(
                                R.string.format_bucket_commitment,
                                formatMoney(cost, character.countryCode)
                            ),
                            enabled = canAfford,
                            accent = ActionCardAccent.GOLD,
                            onClick = {
                                if (canAfford) {
                                    pendingAction.request(PendingAction.AdoptBucket(template.id))
                                }
                            }
                        )
                    }
                }

                if (show(ActionCategory.LIVE) && showDrivingTest) {
                    item { SectionHeader(title = stringResource(R.string.section_driving)) }
                    item {
                        val canAfford = character.stats.money >= drivingTestFee
                        ActionCard(
                            icon = AppIcons.Career,
                            title = stringResource(R.string.btn_take_driving_test),
                            description = stringResource(
                                R.string.driving_test_desc,
                                formatMoney(drivingTestFee, character.countryCode)
                            ),
                            metaLabel = formatMoney(drivingTestFee, character.countryCode),
                            enabled = canAfford,
                            onClick = {
                                if (canAfford) {
                                    pendingAction.request(PendingAction.TakeDrivingTest)
                                }
                            }
                        )
                    }
                }

                if (show(ActionCategory.LIVE) && showPhilanthropy) {
                    item { SectionHeader(title = stringResource(R.string.section_philanthropy)) }
                    item {
                        ActionCard(
                            icon = AppIcons.Happiness,
                            title = stringResource(R.string.btn_volunteer),
                            description = stringResource(R.string.volunteer_desc),
                            metaLabel = stringResource(R.string.meta_free_karma),
                            accent = ActionCardAccent.CARE,
                            questHint = questHintIf(yearQuests, ActionFamily.VOLUNTEER, questHintLabel),
                            onClick = { pendingAction.request(PendingAction.Volunteer) }
                        )
                    }
                    donationAmounts.forEach { amount ->
                        item(key = "donate_$amount") {
                            val canAfford = character.stats.money >= amount
                            ActionCard(
                                icon = AppIcons.Money,
                                title = stringResource(
                                    R.string.btn_donate_amount,
                                    formatMoney(amount, character.countryCode)
                                ),
                                description = stringResource(R.string.donate_desc),
                                metaLabel = formatMoney(amount, character.countryCode),
                                enabled = canAfford,
                                accent = ActionCardAccent.CARE,
                                questHint = questHintIf(yearQuests, ActionFamily.DONATE, questHintLabel),
                                onClick = {
                                    if (canAfford) {
                                        pendingAction.request(PendingAction.Donate(amount))
                                    }
                                }
                            )
                        }
                    }
                }

                if (show(ActionCategory.LIVE) && showAdoptChild) {
                    item { SectionHeader(title = stringResource(R.string.btn_adopt_child)) }
                    item {
                        ActionCard(
                            icon = AppIcons.Family,
                            title = stringResource(R.string.btn_adopt_child),
                            description = stringResource(R.string.dialog_adopt_child_body),
                            accent = ActionCardAccent.CARE,
                            onClick = { pendingAction.request(PendingAction.AdoptChild) }
                        )
                    }
                }

                if (show(ActionCategory.LIVE) && showExpungement) {
                    item { SectionHeader(title = stringResource(R.string.btn_request_expungement)) }
                    item {
                        ActionCard(
                            icon = AppIcons.Career,
                            title = stringResource(R.string.btn_request_expungement),
                            description = stringResource(
                                R.string.dialog_expunge_body,
                                formatMoney(crimeEngine.expungementFee(character), character.countryCode)
                            ),
                            accent = ActionCardAccent.RISK,
                            onClick = { pendingAction.request(PendingAction.RequestExpungement) }
                        )
                    }
                }

                if (show(ActionCategory.LIVE) && showImmigrationOffice) {
                    item { SectionHeader(title = stringResource(R.string.section_immigration_office)) }
                    item {
                        Text(
                            text = stringResource(
                                R.string.immigration_status_summary,
                                character.currentVisa?.name?.lowercase()
                                    ?: stringResource(R.string.immigration_no_visa),
                                character.visaYearsRemaining
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (canRenewVisa) {
                        item {
                            val canAfford = character.stats.money >= visaRenewalFee
                            ActionCard(
                                icon = AppIcons.Career,
                                title = stringResource(R.string.btn_renew_visa),
                                description = stringResource(
                                    R.string.immigration_renew_visa_desc,
                                    formatMoney(visaRenewalFee, character.countryCode)
                                ),
                                metaLabel = formatMoney(visaRenewalFee, character.countryCode),
                                enabled = canAfford,
                                onClick = {
                                    if (canAfford) {
                                        pendingAction.request(PendingAction.RenewVisa)
                                    }
                                }
                            )
                        }
                    }
                    if (canApplyCitizenship) {
                        item {
                            val canAfford = character.stats.money >= citizenshipFee
                            ActionCard(
                                icon = AppIcons.Family,
                                title = stringResource(R.string.btn_apply_citizenship),
                                description = stringResource(
                                    R.string.immigration_citizenship_desc,
                                    formatMoney(citizenshipFee, character.countryCode)
                                ),
                                metaLabel = formatMoney(citizenshipFee, character.countryCode),
                                enabled = canAfford,
                                onClick = {
                                    if (canAfford) {
                                        pendingAction.request(PendingAction.ApplyCitizenship)
                                    }
                                }
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = stringResource(
                                    R.string.immigration_citizenship_locked,
                                    RelocationEngine.NATURALIZATION_YEARS,
                                    character.yearsInCurrentCountry
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (show(ActionCategory.RISK) && showCrimeActions) {
                    item { SectionHeader(title = stringResource(R.string.section_street_risks)) }
                    item {
                        ActionCard(
                            icon = AppIcons.CrimePickpocket,
                            title = stringResource(R.string.crime_pickpocket_title),
                            description = stringResource(
                                R.string.crime_pickpocket_desc,
                                CountryCatalog.flavorFor(character.countryCode).commonTransportMode
                            ),
                            metaLabel = stringResource(R.string.meta_risk_moderate),
                            accent = ActionCardAccent.RISK,
                            onClick = {
                                pendingAction.request(PendingAction.Crime(CrimeType.PICKPOCKET))
                            }
                        )
                    }
                    item {
                        ActionCard(
                            icon = AppIcons.CrimeShoplift,
                            title = stringResource(R.string.crime_shoplift_title),
                            description = stringResource(R.string.crime_shoplift_desc),
                            metaLabel = stringResource(R.string.meta_risk_moderate),
                            accent = ActionCardAccent.RISK,
                            onClick = {
                                pendingAction.request(PendingAction.Crime(CrimeType.SHOPLIFT))
                            }
                        )
                    }
                    item {
                        ActionCard(
                            icon = AppIcons.CrimeFraud,
                            title = stringResource(R.string.crime_fraud_title),
                            description = stringResource(R.string.crime_fraud_desc),
                            metaLabel = stringResource(R.string.meta_risk_high),
                            accent = ActionCardAccent.RISK,
                            onClick = {
                                pendingAction.request(PendingAction.Crime(CrimeType.FRAUD))
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    ConfirmableActionHost(
        state = pendingAction,
        onConfirmed = { action ->
            when (action) {
                is PendingAction.Crime -> onAttemptCrime(action.type)
                is PendingAction.Treatment -> onVisitDoctor(action.condition.id, action.careType)
                is PendingAction.Lifestyle -> onSetLifestyleOption(action.option, action.enable)
                is PendingAction.SideHustle -> onExecuteSideHustle(action.type)
                is PendingAction.AdoptPet -> onAdoptPet(action.species)
                PendingAction.CreateSocialAccount -> onCreateSocialAccount()
                PendingAction.MonetizeSocialAccount -> onMonetizeSocialAccount()
                is PendingAction.PracticeSkill -> onPracticeSkill(action.type)
                is PendingAction.Masterclass -> onTakeMasterclass(action.type)
                is PendingAction.ShowcaseSkill -> onShowcaseSkill(action.type)
                is PendingAction.AdoptBucket -> onAdoptBucketGoal(action.templateId)
                PendingAction.RenewVisa -> onRenewVisa()
                PendingAction.ApplyCitizenship -> onApplyForCitizenship()
                PendingAction.TakeDrivingTest -> onTakeDrivingTest()
                PendingAction.Volunteer -> onVolunteer()
                is PendingAction.Donate -> onDonateToCharity(action.amount)
                is PendingAction.Leisure -> onPerformLeisure(action.activity)
                PendingAction.AdoptChild -> onAdoptChild()
                PendingAction.RequestExpungement -> onRequestExpungement()
            }
        }
    ) { action, onConfirm, onDismiss ->
        when (action) {
            is PendingAction.Crime -> {
                val (title, description) = crimeConfirmCopy(action.type)
                ConfirmActionDialog(
                    title = title,
                    description = description,
                    confirmLabel = stringResource(R.string.btn_attempt),
                    severity = ConfirmSeverity.WARNING,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            is PendingAction.Treatment -> {
                val careName = when (action.careType) {
                    CareType.PUBLIC_CLINIC -> stringResource(R.string.care_public_clinic)
                    CareType.PRIVATE_HOSPITAL ->
                        CountryCatalog.flavorFor(character.countryCode).privateHospitalName
                }
                ConfirmActionDialog(
                    title = stringResource(R.string.dialog_seek_treatment_title),
                    description = stringResource(
                        R.string.dialog_seek_treatment_desc,
                        action.condition.name,
                        careName,
                        treatmentCostLabel(character, action.condition.severity, action.careType),
                        successHint(action.careType)
                    ),
                    confirmLabel = stringResource(R.string.btn_visit_doctor),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            is PendingAction.SideHustle -> {
                ConfirmActionDialog(
                    title = stringResource(R.string.dialog_side_hustle_title),
                    description = stringResource(R.string.dialog_side_hustle_description),
                    confirmLabel = stringResource(R.string.btn_side_hustle),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            is PendingAction.AdoptPet -> {
                val entry = PetCatalog.findBySpecies(action.species)
                val adoptionCost = entry?.let {
                    formatMoney(
                        EconomyScaler.scaleAmount(it.adoptionFee, character.countryCode),
                        character.countryCode
                    )
                } ?: ""
                ConfirmActionDialog(
                    title = stringResource(R.string.dialog_adopt_pet_title),
                    description = stringResource(
                        R.string.dialog_adopt_pet_description,
                        adoptPetTitle(action.species),
                        adoptionCost
                    ),
                    confirmLabel = stringResource(R.string.btn_adopt_pet),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            PendingAction.CreateSocialAccount -> {
                ConfirmActionDialog(
                    title = stringResource(R.string.dialog_create_social_title),
                    description = stringResource(R.string.dialog_create_social_description),
                    confirmLabel = stringResource(R.string.btn_create_social_account),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            PendingAction.MonetizeSocialAccount -> {
                ConfirmActionDialog(
                    title = stringResource(R.string.dialog_monetize_social_title),
                    description = stringResource(R.string.dialog_monetize_social_description),
                    confirmLabel = stringResource(R.string.btn_monetize_social),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            is PendingAction.PracticeSkill -> {
                ConfirmActionDialog(
                    title = stringResource(R.string.dialog_practice_skill_title),
                    description = stringResource(
                        R.string.dialog_practice_skill_description,
                        skillTypeLabel(action.type)
                    ),
                    confirmLabel = stringResource(R.string.btn_practice_skill),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            is PendingAction.Masterclass -> {
                ConfirmActionDialog(
                    title = stringResource(R.string.dialog_masterclass_title),
                    description = stringResource(
                        R.string.dialog_masterclass_description,
                        skillTypeLabel(action.type),
                        formatMoney(masterclassCost, character.countryCode)
                    ),
                    confirmLabel = stringResource(R.string.btn_take_masterclass),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            is PendingAction.ShowcaseSkill -> {
                ConfirmActionDialog(
                    title = stringResource(R.string.btn_showcase_skill),
                    description = stringResource(R.string.skill_showcase_desc),
                    confirmLabel = stringResource(R.string.btn_showcase_skill),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            is PendingAction.AdoptBucket -> {
                ConfirmActionDialog(
                    title = stringResource(R.string.section_bucket_list),
                    description = stringResource(R.string.msg_bucket_adopted),
                    confirmLabel = stringResource(R.string.btn_confirm),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            PendingAction.RenewVisa -> {
                ConfirmActionDialog(
                    title = stringResource(R.string.dialog_renew_visa_title),
                    description = stringResource(
                        R.string.dialog_renew_visa_body,
                        formatMoney(visaRenewalFee, character.countryCode)
                    ),
                    confirmLabel = stringResource(R.string.btn_renew_visa),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            PendingAction.ApplyCitizenship -> {
                ConfirmActionDialog(
                    title = stringResource(R.string.dialog_apply_citizenship_title),
                    description = stringResource(
                        R.string.dialog_apply_citizenship_body,
                        formatMoney(citizenshipFee, character.countryCode)
                    ),
                    confirmLabel = stringResource(R.string.btn_apply_citizenship),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            PendingAction.TakeDrivingTest -> {
                ConfirmActionDialog(
                    title = stringResource(R.string.dialog_driving_test_title),
                    description = stringResource(
                        R.string.dialog_driving_test_body,
                        formatMoney(drivingTestFee, character.countryCode)
                    ),
                    confirmLabel = stringResource(R.string.btn_take_driving_test),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            PendingAction.Volunteer -> {
                ConfirmActionDialog(
                    title = stringResource(R.string.dialog_volunteer_title),
                    description = stringResource(R.string.dialog_volunteer_body),
                    confirmLabel = stringResource(R.string.btn_volunteer),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            is PendingAction.Donate -> {
                ConfirmActionDialog(
                    title = stringResource(R.string.dialog_donate_title),
                    description = stringResource(
                        R.string.dialog_donate_body,
                        formatMoney(action.amount, character.countryCode)
                    ),
                    confirmLabel = stringResource(R.string.btn_donate),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            is PendingAction.Leisure -> {
                val cost = leisureEngine.cost(action.activity, character.countryCode)
                ConfirmActionDialog(
                    title = leisureTitle(action.activity),
                    description = stringResource(
                        R.string.dialog_leisure_body,
                        leisureDescription(action.activity),
                        formatMoney(cost, character.countryCode)
                    ),
                    confirmLabel = stringResource(R.string.btn_leisure_go),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            is PendingAction.Lifestyle -> {
                val label = lifestyleLabel(action.option, action.enable)
                if (action.enable) {
                    ConfirmActionDialog(
                        title = stringResource(R.string.dialog_lifestyle_enable_title),
                        description = stringResource(
                            R.string.dialog_lifestyle_enable_desc,
                            label,
                            stringResource(
                                R.string.format_yearly_cost,
                                formatMoney(
                                    lifestyleYearlyCost(action.option, character.countryCode),
                                    character.countryCode
                                )
                            )
                        ),
                        confirmLabel = stringResource(R.string.btn_subscribe),
                        severity = ConfirmSeverity.NEUTRAL,
                        onConfirm = onConfirm,
                        onDismiss = onDismiss
                    )
                } else {
                    ConfirmActionDialog(
                        title = stringResource(R.string.dialog_lifestyle_disable_title),
                        description = stringResource(
                            R.string.dialog_lifestyle_disable_desc,
                            label
                        ),
                        confirmLabel = stringResource(R.string.btn_cancel_subscription),
                        severity = ConfirmSeverity.NEUTRAL,
                        onConfirm = onConfirm,
                        onDismiss = onDismiss
                    )
                }
            }
            PendingAction.AdoptChild -> {
                ConfirmActionDialog(
                    title = stringResource(R.string.dialog_adopt_child_title),
                    description = stringResource(R.string.dialog_adopt_child_body),
                    confirmLabel = stringResource(R.string.btn_adopt_child),
                    severity = ConfirmSeverity.NEUTRAL,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
            PendingAction.RequestExpungement -> {
                ConfirmActionDialog(
                    title = stringResource(R.string.dialog_expunge_title),
                    description = stringResource(
                        R.string.dialog_expunge_body,
                        formatMoney(crimeEngine.expungementFee(character), character.countryCode)
                    ),
                    confirmLabel = stringResource(R.string.btn_request_expungement),
                    severity = ConfirmSeverity.WARNING,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun CrimeStatusCard(statusKind: CrimeStatusKind, yearsRemaining: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = CoralNegative.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.section_street_risks),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = CoralNegative
            )
            when (statusKind) {
                CrimeStatusKind.AWAITING_TRIAL -> {
                    Text(
                        text = stringResource(R.string.crime_status_awaiting_trial),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkPrimary
                    )
                }
                CrimeStatusKind.INCARCERATED -> {
                    Text(
                        text = stringResource(R.string.msg_in_prison, yearsRemaining),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkPrimary
                    )
                    Text(
                        text = stringResource(R.string.crime_status_parole_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkTertiary
                    )
                }
                CrimeStatusKind.CLEAR -> Unit
            }
        }
    }
}

@Composable
private fun CollapsibleSkillRow(
    skillType: SkillType,
    level: Int,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    masterclassCost: Int,
    countryCode: String,
    money: Int,
    canShowcase: Boolean,
    practiceHint: String?,
    showcaseHint: String?,
    showEarnActions: Boolean,
    showGrowActions: Boolean,
    onPractice: () -> Unit,
    onMasterclass: () -> Unit,
    onShowcase: () -> Unit
) {
    val tier = SkillEngine.masteryTierOf(level)
    val (current, next) = SkillEngine.progressToNextTierOf(level)
    val tierLabel = skillTierLabel(tier)
    val progressLabel = if (tier == SkillMasteryTier.MASTER && level >= SkillEngine.MAX_SKILL_LEVEL) {
        stringResource(R.string.format_skill_tier_capped, tierLabel)
    } else {
        stringResource(R.string.format_skill_tier_progress, tierLabel, current, next)
    }
    val canAffordMasterclass = money >= masterclassCost
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = skillTypeLabel(skillType),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )
                    Text(
                        text = progressLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TealPrimary
                    )
                }
                Text(
                    text = if (expanded) "▾" else "▸",
                    color = InkTertiary,
                    fontSize = 16.sp
                )
            }
            if (expanded) {
                StatBar(
                    type = StatType.SKILL,
                    value = level,
                    label = "",
                    showIcon = false,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        if (expanded) {
            if (showGrowActions) {
                ActionCard(
                    icon = AppIcons.Smarts,
                    title = stringResource(R.string.btn_practice_skill),
                    description = stringResource(R.string.skill_practice_desc),
                    metaLabel = stringResource(R.string.skill_practice_meta),
                    questHint = practiceHint,
                    onClick = onPractice
                )
                ActionCard(
                    icon = AppIcons.Money,
                    title = stringResource(R.string.btn_take_masterclass),
                    description = stringResource(R.string.skill_masterclass_desc),
                    metaLabel = if (canAffordMasterclass) {
                        stringResource(
                            R.string.format_masterclass_cost,
                            formatMoney(masterclassCost, countryCode)
                        )
                    } else {
                        stringResource(R.string.msg_skill_cannot_afford)
                    },
                    enabled = canAffordMasterclass,
                    accent = ActionCardAccent.GOLD,
                    onClick = { if (canAffordMasterclass) onMasterclass() }
                )
            }
            if (showEarnActions && tier == SkillMasteryTier.MASTER) {
                ActionCard(
                    icon = AppIcons.Money,
                    title = stringResource(R.string.btn_showcase_skill),
                    description = stringResource(R.string.skill_showcase_desc),
                    metaLabel = if (canShowcase) {
                        progressLabel
                    } else {
                        stringResource(R.string.msg_skill_showcase_done)
                    },
                    enabled = canShowcase,
                    accent = ActionCardAccent.GOLD,
                    questHint = showcaseHint,
                    onClick = { if (canShowcase) onShowcase() }
                )
            }
        }
    }
}

@Composable
private fun questHintIf(
    quests: List<YearQuest>,
    family: ActionFamily,
    label: String
): String? = if (ActionQuestHints.anyMatch(quests, family)) label else null

@Composable
private fun skillTierLabel(tier: SkillMasteryTier): String = when (tier) {
    SkillMasteryTier.NOVICE -> stringResource(R.string.skill_tier_novice)
    SkillMasteryTier.ADEPT -> stringResource(R.string.skill_tier_adept)
    SkillMasteryTier.EXPERT -> stringResource(R.string.skill_tier_expert)
    SkillMasteryTier.MASTER -> stringResource(R.string.skill_tier_master)
}

@Composable
private fun fameTierLabel(tier: FameTier): String = when (tier) {
    FameTier.UNKNOWN -> stringResource(R.string.fame_tier_unknown)
    FameTier.LOCAL -> stringResource(R.string.fame_tier_local)
    FameTier.REGIONAL -> stringResource(R.string.fame_tier_regional)
    FameTier.NATIONAL -> stringResource(R.string.fame_tier_national)
    FameTier.GLOBAL -> stringResource(R.string.fame_tier_global)
}

@Composable
private fun bucketTitle(kind: BucketGoalKind): String = when (kind) {
    BucketGoalKind.OWN_HOME -> stringResource(R.string.bucket_own_home_title)
    BucketGoalKind.REACH_FAME -> stringResource(R.string.bucket_reach_fame_title)
    BucketGoalKind.START_BUSINESS -> stringResource(R.string.bucket_start_business_title)
    BucketGoalKind.WIN_OFFICE -> stringResource(R.string.bucket_win_office_title)
    BucketGoalKind.RAISE_CHILD -> stringResource(R.string.bucket_raise_child_title)
    BucketGoalKind.HIT_WEALTH -> stringResource(R.string.bucket_hit_wealth_title)
    BucketGoalKind.MASTER_SKILL -> stringResource(R.string.bucket_master_skill_title)
}

@Composable
private fun bucketDescription(kind: BucketGoalKind): String = when (kind) {
    BucketGoalKind.OWN_HOME -> stringResource(R.string.bucket_own_home_desc)
    BucketGoalKind.REACH_FAME -> stringResource(R.string.bucket_reach_fame_desc)
    BucketGoalKind.START_BUSINESS -> stringResource(R.string.bucket_start_business_desc)
    BucketGoalKind.WIN_OFFICE -> stringResource(R.string.bucket_win_office_desc)
    BucketGoalKind.RAISE_CHILD -> stringResource(R.string.bucket_raise_child_desc)
    BucketGoalKind.HIT_WEALTH -> stringResource(R.string.bucket_hit_wealth_desc)
    BucketGoalKind.MASTER_SKILL -> stringResource(R.string.bucket_master_skill_desc)
}

@Composable
private fun skillTypeLabel(type: SkillType): String = when (type) {
    SkillType.GUITAR -> stringResource(R.string.skill_guitar)
    SkillType.COOKING -> stringResource(R.string.skill_cooking)
    SkillType.MARTIAL_ARTS -> stringResource(R.string.skill_martial_arts)
    SkillType.PROGRAMMING -> stringResource(R.string.skill_programming)
    SkillType.WRITING -> stringResource(R.string.skill_writing)
}

@Composable
private fun adoptPetTitle(species: PetSpecies): String = when (species) {
    PetSpecies.DOG -> stringResource(R.string.pet_species_dog)
    PetSpecies.CAT -> stringResource(R.string.pet_species_cat)
    PetSpecies.BIRD -> stringResource(R.string.pet_species_bird)
    PetSpecies.FISH -> stringResource(R.string.pet_species_fish)
    PetSpecies.EXOTIC -> stringResource(R.string.pet_species_exotic)
}

@Composable
private fun adoptPetDescription(species: PetSpecies): String = when (species) {
    PetSpecies.DOG -> stringResource(R.string.pet_adopt_dog_desc)
    PetSpecies.CAT -> stringResource(R.string.pet_adopt_cat_desc)
    PetSpecies.BIRD -> stringResource(R.string.pet_adopt_bird_desc)
    PetSpecies.FISH -> stringResource(R.string.pet_adopt_fish_desc)
    PetSpecies.EXOTIC -> stringResource(R.string.pet_adopt_exotic_desc)
}

@Composable
private fun prisonActivityTitle(activity: PrisonActivity): String = when (activity) {
    PrisonActivity.WORK_DETAIL -> stringResource(R.string.prison_work_title)
    PrisonActivity.LIBRARY -> stringResource(R.string.prison_library_title)
    PrisonActivity.EXERCISE -> stringResource(R.string.prison_exercise_title)
    PrisonActivity.GOOD_BEHAVIOR -> stringResource(R.string.prison_good_behavior_title)
}

@Composable
private fun prisonActivityDescription(activity: PrisonActivity): String = when (activity) {
    PrisonActivity.WORK_DETAIL -> stringResource(R.string.prison_work_desc)
    PrisonActivity.LIBRARY -> stringResource(R.string.prison_library_desc)
    PrisonActivity.EXERCISE -> stringResource(R.string.prison_exercise_desc)
    PrisonActivity.GOOD_BEHAVIOR -> stringResource(R.string.prison_good_behavior_desc)
}

@Composable
private fun SideHustleActionCard(
    character: Character,
    hustleType: HustleType,
    questHint: String?,
    onClick: () -> Unit
) {
    val alreadyDone = character.career.sideHustleDoneThisYear
    val meetsPrerequisites = JobPool.meetsSideHustlePrerequisites(character, hustleType)
    val available = !alreadyDone && meetsPrerequisites
    val metaLabel = when {
        alreadyDone -> stringResource(R.string.msg_side_hustle_already_done)
        !meetsPrerequisites -> sideHustleRequirementLabel(hustleType)
        else -> stringResource(R.string.meta_earn_cash)
    }
    ActionCard(
        icon = AppIcons.Money,
        title = sideHustleTitle(hustleType),
        description = sideHustleDescription(hustleType),
        metaLabel = metaLabel,
        enabled = available,
        accent = ActionCardAccent.GOLD,
        questHint = if (available) questHint else null,
        onClick = { if (available) onClick() }
    )
}

@Composable
private fun partTimeJobTitle(job: PartTimeJob): String = when (job) {
    PartTimeJob.RETAIL -> stringResource(R.string.part_time_retail)
    PartTimeJob.FAST_FOOD -> stringResource(R.string.part_time_fast_food)
    PartTimeJob.BARISTA -> stringResource(R.string.part_time_barista)
    PartTimeJob.BABYSITTING -> stringResource(R.string.part_time_babysitting)
    PartTimeJob.TUTORING -> stringResource(R.string.part_time_tutoring)
    PartTimeJob.FREELANCE_CODER -> stringResource(R.string.part_time_freelance_coder)
}

@Composable
private fun partTimeDemandLabel(demand: PartTimeDemand): String = when (demand) {
    PartTimeDemand.HIGH -> stringResource(R.string.part_time_demand_high)
    PartTimeDemand.MEDIUM -> stringResource(R.string.part_time_demand_medium)
    PartTimeDemand.LOW -> stringResource(R.string.part_time_demand_low)
}

@Composable
private fun sideHustleTitle(type: HustleType): String = when (type) {
    HustleType.RIDE_SHARE -> stringResource(R.string.hustle_ride_share_title)
    HustleType.FREELANCE_CODING -> stringResource(R.string.hustle_freelance_coding_title)
    HustleType.TUTORING -> stringResource(R.string.hustle_tutoring_title)
    HustleType.FOOD_DELIVERY -> stringResource(R.string.hustle_food_delivery_title)
    HustleType.RESELLING -> stringResource(R.string.hustle_reselling_title)
    HustleType.HANDMADE_CRAFTS -> stringResource(R.string.hustle_handmade_crafts_title)
    HustleType.STREAMING -> stringResource(R.string.hustle_streaming_title)
    HustleType.SCRIPT_CODING -> stringResource(R.string.hustle_script_coding_title)
}

@Composable
private fun sideHustleDescription(type: HustleType): String = when (type) {
    HustleType.RIDE_SHARE -> stringResource(R.string.hustle_ride_share_desc)
    HustleType.FREELANCE_CODING -> stringResource(R.string.hustle_freelance_coding_desc)
    HustleType.TUTORING -> stringResource(R.string.hustle_tutoring_desc)
    HustleType.FOOD_DELIVERY -> stringResource(R.string.hustle_food_delivery_desc)
    HustleType.RESELLING -> stringResource(R.string.hustle_reselling_desc)
    HustleType.HANDMADE_CRAFTS -> stringResource(R.string.hustle_handmade_crafts_desc)
    HustleType.STREAMING -> stringResource(R.string.hustle_streaming_desc)
    HustleType.SCRIPT_CODING -> stringResource(R.string.hustle_script_coding_desc)
}

@Composable
private fun sideHustleRequirementLabel(type: HustleType): String = when (type) {
    HustleType.RIDE_SHARE -> stringResource(R.string.hustle_ride_share_req)
    HustleType.FREELANCE_CODING -> stringResource(R.string.hustle_freelance_coding_req)
    HustleType.TUTORING -> stringResource(R.string.hustle_tutoring_req)
    HustleType.RESELLING -> stringResource(R.string.hustle_reselling_req)
    HustleType.FOOD_DELIVERY -> stringResource(R.string.msg_side_hustle_prerequisites)
    HustleType.HANDMADE_CRAFTS -> stringResource(R.string.hustle_handmade_crafts_req)
    HustleType.STREAMING -> stringResource(R.string.hustle_streaming_req)
    HustleType.SCRIPT_CODING -> stringResource(R.string.hustle_script_coding_req)
}

@Composable
private fun LifestyleActionCard(
    character: Character,
    option: LifestyleOption,
    activeTitleRes: Int,
    inactiveTitleRes: Int,
    descriptionRes: Int,
    yearlyCost: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    questHint: String?,
    onToggle: (Boolean) -> Unit
) {
    val active = when (option) {
        LifestyleOption.GYM -> character.lifestyle.hasGymMembership
        LifestyleOption.DIET -> character.lifestyle.isVegan
        LifestyleOption.THERAPIST -> character.lifestyle.hasTherapist
        LifestyleOption.HEALTH_INSURANCE -> character.lifestyle.hasHealthInsurance
    }
    ActionCard(
        icon = icon,
        title = stringResource(if (active) activeTitleRes else inactiveTitleRes),
        description = stringResource(descriptionRes),
        metaLabel = stringResource(
            R.string.format_yearly_cost,
            formatMoney(yearlyCost, character.countryCode)
        ),
        accent = ActionCardAccent.CARE,
        questHint = questHint,
        onClick = { onToggle(!active) }
    )
}

@Composable
private fun lifestyleLabel(option: LifestyleOption, enabling: Boolean): String = when (option) {
    LifestyleOption.GYM -> stringResource(
        if (enabling) R.string.lifestyle_gym_title else R.string.lifestyle_gym_active
    )
    LifestyleOption.DIET -> stringResource(
        if (enabling) R.string.lifestyle_diet_title else R.string.lifestyle_diet_active
    )
    LifestyleOption.THERAPIST -> stringResource(
        if (enabling) R.string.lifestyle_therapist_title else R.string.lifestyle_therapist_active
    )
    LifestyleOption.HEALTH_INSURANCE -> stringResource(
        if (enabling) R.string.lifestyle_insurance_title else R.string.lifestyle_insurance_active
    )
}

private fun lifestyleYearlyCost(option: LifestyleOption, countryCode: String): Int {
    val base = when (option) {
        LifestyleOption.GYM -> HealthEngine.GYM_YEARLY_COST
        LifestyleOption.DIET -> HealthEngine.DIET_YEARLY_COST
        LifestyleOption.THERAPIST -> HealthEngine.THERAPIST_YEARLY_COST
        LifestyleOption.HEALTH_INSURANCE -> HealthEngine.HEALTH_INSURANCE_YEARLY_COST
    }
    return EconomyScaler.scaleAmount(base, countryCode)
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = TealPrimary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun treatmentCostLabel(
    character: Character,
    severity: Int,
    careType: CareType
): String {
    val amount = HealthUiHelpers.treatmentCost(character, severity, careType)
    return stringResource(
        R.string.format_treatment_cost,
        formatMoney(amount, character.countryCode)
    )
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
