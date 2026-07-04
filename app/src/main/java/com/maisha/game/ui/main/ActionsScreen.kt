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
import com.maisha.game.data.JobPool
import com.maisha.game.data.PetCatalog
import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.CrimeType
import com.maisha.game.data.model.HealthCondition
import com.maisha.game.data.model.HustleType
import com.maisha.game.data.model.LifestyleOption
import com.maisha.game.data.model.PetSpecies
import com.maisha.game.data.model.SkillType
import com.maisha.game.domain.EducationEngine
import com.maisha.game.domain.HealthEngine
import com.maisha.game.domain.RelationshipEngine
import com.maisha.game.domain.RelocationEngine
import com.maisha.game.domain.SkillEngine
import com.maisha.game.domain.SocialMediaEngine
import com.maisha.game.ui.components.ActionCard
import com.maisha.game.ui.components.ConditionBadge
import com.maisha.game.ui.components.ConfirmActionDialog
import com.maisha.game.ui.components.ConfirmSeverity
import com.maisha.game.ui.components.ConfirmableActionHost
import com.maisha.game.ui.components.EmptyStateCard
import com.maisha.game.ui.components.StatBar
import com.maisha.game.ui.components.StatType
import com.maisha.game.ui.components.rememberConfirmableAction
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.theme.AppIcons
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.formatMoney

private const val CRIME_UI_MIN_AGE = 16
private const val SIDE_HUSTLE_UI_MIN_AGE = 16

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
    data object RenewVisa : PendingAction()
    data object ApplyCitizenship : PendingAction()
    data object TakeDrivingTest : PendingAction()
    data object Volunteer : PendingAction()
    data class Donate(val amount: Int) : PendingAction()
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
    onRenewVisa: () -> Unit,
    onApplyForCitizenship: () -> Unit,
    onTakeDrivingTest: () -> Unit,
    onVolunteer: () -> Unit,
    onDonateToCharity: (Int) -> Unit,
    donationTiers: List<Int>,
    onActionMessageDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val incarcerated = character.criminalRecord.currentlyIncarcerated
    val awaitingTrial = character.criminalRecord.awaitingTrial
    val untreated = character.activeConditions.filter { !it.treated }
    val showCrimeActions = character.age >= CRIME_UI_MIN_AGE && !incarcerated && !awaitingTrial && character.alive
    val showLifestyleActions = character.alive && !incarcerated && !awaitingTrial
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
    val hasContent = untreated.isNotEmpty() ||
        showCrimeActions ||
        showSideHustleActions ||
        showAdoptPetActions ||
        showSocialMediaActions ||
        showSkillActions ||
        showImmigrationOffice ||
        showDrivingTest ||
        showPhilanthropy ||
        incarcerated ||
        awaitingTrial ||
        showLifestyleActions

    val pendingAction = rememberConfirmableAction<PendingAction>()
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
                if (awaitingTrial) {
                    item {
                        Text(
                            text = stringResource(R.string.msg_crime_arrested),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoralNegative,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

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
                                    pendingAction.request(
                                        PendingAction.Treatment(
                                            condition = condition,
                                            careType = CareType.PUBLIC_CLINIC
                                        )
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
                                    pendingAction.request(
                                        PendingAction.Treatment(
                                            condition = condition,
                                            careType = CareType.PRIVATE_HOSPITAL
                                        )
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
                            onClick = { pendingAction.request(PendingAction.Crime(CrimeType.PICKPOCKET)) }
                        )
                    }
                    item {
                        ActionCard(
                            icon = AppIcons.CrimeShoplift,
                            title = stringResource(R.string.crime_shoplift_title),
                            description = stringResource(R.string.crime_shoplift_desc),
                            metaLabel = stringResource(R.string.risk_moderate),
                            onClick = { pendingAction.request(PendingAction.Crime(CrimeType.SHOPLIFT)) }
                        )
                    }
                    item {
                        ActionCard(
                            icon = AppIcons.CrimeFraud,
                            title = stringResource(R.string.crime_fraud_title),
                            description = stringResource(R.string.crime_fraud_desc),
                            metaLabel = stringResource(R.string.risk_high),
                            onClick = { pendingAction.request(PendingAction.Crime(CrimeType.FRAUD)) }
                        )
                    }
                }

                if (showSideHustleActions) {
                    item {
                        SectionHeader(title = stringResource(R.string.section_side_hustles))
                    }
                    items(JobPool.getAllSideHustleTypes(), key = { it.name }) { hustleType ->
                        SideHustleActionCard(
                            character = character,
                            hustleType = hustleType,
                            onClick = {
                                pendingAction.request(PendingAction.SideHustle(hustleType))
                            }
                        )
                    }
                }

                if (showSocialMediaActions) {
                    item {
                        SectionHeader(title = stringResource(R.string.section_social_media))
                    }
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
                                onClick = onPostSocialContent
                            )
                        }
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
                                onClick = {
                                    if (canMonetize) {
                                        pendingAction.request(PendingAction.MonetizeSocialAccount)
                                    }
                                }
                            )
                        }
                    }
                }

                if (showAdoptPetActions) {
                    item {
                        SectionHeader(title = stringResource(R.string.section_adopt_pet))
                    }
                    items(PetCatalog.getAll(), key = { it.species.name }) { entry ->
                        val adoptionCost = EconomyScaler.scaleAmount(
                            entry.adoptionFee,
                            character.countryCode
                        )
                        val yearlyCost = EconomyScaler.scaleAmount(
                            entry.yearlyUpkeep,
                            character.countryCode
                        )
                        ActionCard(
                            icon = AppIcons.Family,
                            title = adoptPetTitle(entry.species),
                            description = adoptPetDescription(entry.species),
                            metaLabel = stringResource(
                                R.string.format_pet_adoption_cost,
                                formatMoney(adoptionCost, character.countryCode),
                                formatMoney(yearlyCost, character.countryCode)
                            ),
                            onClick = {
                                pendingAction.request(PendingAction.AdoptPet(entry.species))
                            }
                        )
                    }
                }

                if (showSkillActions) {
                    item {
                        SectionHeader(title = stringResource(R.string.section_hobbies_skills))
                    }
                    items(SkillType.entries.toList(), key = { it.name }) { skillType ->
                        val level = character.skills.find { it.type == skillType }?.level ?: 0
                        SkillActionCard(
                            skillType = skillType,
                            level = level,
                            masterclassCostLabel = formatMoney(masterclassCost, character.countryCode),
                            canAffordMasterclass = character.stats.money >= masterclassCost,
                            onPractice = {
                                pendingAction.request(PendingAction.PracticeSkill(skillType))
                            },
                            onMasterclass = {
                                pendingAction.request(PendingAction.Masterclass(skillType))
                            }
                        )
                    }
                }

                if (showLifestyleActions) {
                    item {
                        SectionHeader(title = stringResource(R.string.section_lifestyle))
                    }
                    item {
                        LifestyleActionCard(
                            character = character,
                            option = LifestyleOption.GYM,
                            activeTitleRes = R.string.lifestyle_gym_active,
                            inactiveTitleRes = R.string.lifestyle_gym_title,
                            descriptionRes = R.string.lifestyle_gym_desc,
                            yearlyCost = HealthEngine.GYM_YEARLY_COST,
                            icon = AppIcons.Health,
                            onToggle = { enable ->
                                pendingAction.request(PendingAction.Lifestyle(LifestyleOption.GYM, enable))
                            }
                        )
                    }
                    if (showDrivingTest) {
                        item {
                            Text(
                                text = stringResource(R.string.section_driving),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        item {
                            ActionCard(
                                icon = AppIcons.Career,
                                title = stringResource(R.string.btn_take_driving_test),
                                description = stringResource(
                                    R.string.driving_test_desc,
                                    formatMoney(drivingTestFee, character.countryCode)
                                ),
                                metaLabel = formatMoney(drivingTestFee, character.countryCode),
                                onClick = { pendingAction.request(PendingAction.TakeDrivingTest) }
                            )
                        }
                    }
                    if (showPhilanthropy) {
                        item {
                            Text(
                                text = stringResource(R.string.section_philanthropy),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        item {
                            ActionCard(
                                icon = AppIcons.Happiness,
                                title = stringResource(R.string.btn_volunteer),
                                description = stringResource(R.string.volunteer_desc),
                                onClick = { pendingAction.request(PendingAction.Volunteer) }
                            )
                        }
                        donationAmounts.forEach { amount ->
                            item(key = "donate_$amount") {
                                ActionCard(
                                    icon = AppIcons.Money,
                                    title = stringResource(
                                        R.string.btn_donate_amount,
                                        formatMoney(amount, character.countryCode)
                                    ),
                                    description = stringResource(R.string.donate_desc),
                                    metaLabel = formatMoney(amount, character.countryCode),
                                    onClick = {
                                        pendingAction.request(PendingAction.Donate(amount))
                                    }
                                )
                            }
                        }
                    }
                    if (showImmigrationOffice) {
                        item {
                            Text(
                                text = stringResource(R.string.section_immigration_office),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
                                ActionCard(
                                    icon = AppIcons.Career,
                                    title = stringResource(R.string.btn_renew_visa),
                                    description = stringResource(
                                        R.string.immigration_renew_visa_desc,
                                        formatMoney(visaRenewalFee, character.countryCode)
                                    ),
                                    metaLabel = formatMoney(visaRenewalFee, character.countryCode),
                                    onClick = { pendingAction.request(PendingAction.RenewVisa) }
                                )
                            }
                        }
                        if (canApplyCitizenship) {
                            item {
                                ActionCard(
                                    icon = AppIcons.Family,
                                    title = stringResource(R.string.btn_apply_citizenship),
                                    description = stringResource(
                                        R.string.immigration_citizenship_desc,
                                        formatMoney(citizenshipFee, character.countryCode)
                                    ),
                                    metaLabel = formatMoney(citizenshipFee, character.countryCode),
                                    onClick = { pendingAction.request(PendingAction.ApplyCitizenship) }
                                )
                            }
                        } else if (showImmigrationOffice) {
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
                    item {
                        LifestyleActionCard(
                            character = character,
                            option = LifestyleOption.DIET,
                            activeTitleRes = R.string.lifestyle_diet_active,
                            inactiveTitleRes = R.string.lifestyle_diet_title,
                            descriptionRes = R.string.lifestyle_diet_desc,
                            yearlyCost = HealthEngine.DIET_YEARLY_COST,
                            icon = AppIcons.Looks,
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
                            yearlyCost = HealthEngine.THERAPIST_YEARLY_COST,
                            icon = AppIcons.Happiness,
                            onToggle = { enable ->
                                pendingAction.request(PendingAction.Lifestyle(LifestyleOption.THERAPIST, enable))
                            }
                        )
                    }
                }
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
                PendingAction.RenewVisa -> onRenewVisa()
                PendingAction.ApplyCitizenship -> onApplyForCitizenship()
                PendingAction.TakeDrivingTest -> onTakeDrivingTest()
                PendingAction.Volunteer -> onVolunteer()
                is PendingAction.Donate -> onDonateToCharity(action.amount)
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
                                formatMoney(lifestyleYearlyCost(action.option), character.countryCode)
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
        }
    }
}

@Composable
private fun SkillActionCard(
    skillType: SkillType,
    level: Int,
    masterclassCostLabel: String,
    canAffordMasterclass: Boolean,
    onPractice: () -> Unit,
    onMasterclass: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StatBar(
            type = StatType.SKILL,
            value = level,
            label = skillTypeLabel(skillType),
            showIcon = false
        )
        ActionCard(
            icon = AppIcons.Smarts,
            title = stringResource(R.string.btn_practice_skill),
            description = stringResource(R.string.skill_practice_desc),
            metaLabel = stringResource(R.string.skill_practice_meta),
            onClick = onPractice
        )
        ActionCard(
            icon = AppIcons.Money,
            title = stringResource(R.string.btn_take_masterclass),
            description = stringResource(R.string.skill_masterclass_desc),
            metaLabel = if (canAffordMasterclass) {
                stringResource(R.string.format_masterclass_cost, masterclassCostLabel)
            } else {
                stringResource(R.string.msg_skill_cannot_afford)
            },
            onClick = { if (canAffordMasterclass) onMasterclass() }
        )
    }
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
private fun SideHustleActionCard(
    character: Character,
    hustleType: HustleType,
    onClick: () -> Unit
) {
    val alreadyDone = character.career.sideHustleDoneThisYear
    val meetsPrerequisites = JobPool.meetsSideHustlePrerequisites(character, hustleType)
    val available = !alreadyDone && meetsPrerequisites
    val metaLabel = when {
        alreadyDone -> stringResource(R.string.msg_side_hustle_already_done)
        !meetsPrerequisites -> sideHustleRequirementLabel(hustleType)
        else -> stringResource(R.string.btn_side_hustle)
    }
    ActionCard(
        icon = AppIcons.Money,
        title = sideHustleTitle(hustleType),
        description = sideHustleDescription(hustleType),
        metaLabel = metaLabel,
        onClick = { if (available) onClick() }
    )
}

@Composable
private fun sideHustleTitle(type: HustleType): String = when (type) {
    HustleType.RIDE_SHARE -> stringResource(R.string.hustle_ride_share_title)
    HustleType.FREELANCE_CODING -> stringResource(R.string.hustle_freelance_coding_title)
    HustleType.TUTORING -> stringResource(R.string.hustle_tutoring_title)
    HustleType.FOOD_DELIVERY -> stringResource(R.string.hustle_food_delivery_title)
    HustleType.RESELLING -> stringResource(R.string.hustle_reselling_title)
}

@Composable
private fun sideHustleDescription(type: HustleType): String = when (type) {
    HustleType.RIDE_SHARE -> stringResource(R.string.hustle_ride_share_desc)
    HustleType.FREELANCE_CODING -> stringResource(R.string.hustle_freelance_coding_desc)
    HustleType.TUTORING -> stringResource(R.string.hustle_tutoring_desc)
    HustleType.FOOD_DELIVERY -> stringResource(R.string.hustle_food_delivery_desc)
    HustleType.RESELLING -> stringResource(R.string.hustle_reselling_desc)
}

@Composable
private fun sideHustleRequirementLabel(type: HustleType): String = when (type) {
    HustleType.RIDE_SHARE -> stringResource(R.string.hustle_ride_share_req)
    HustleType.FREELANCE_CODING -> stringResource(R.string.hustle_freelance_coding_req)
    HustleType.TUTORING -> stringResource(R.string.hustle_tutoring_req)
    HustleType.RESELLING -> stringResource(R.string.hustle_reselling_req)
    HustleType.FOOD_DELIVERY -> stringResource(R.string.msg_side_hustle_prerequisites)
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
    onToggle: (Boolean) -> Unit
) {
    val active = when (option) {
        LifestyleOption.GYM -> character.lifestyle.hasGymMembership
        LifestyleOption.DIET -> character.lifestyle.isVegan
        LifestyleOption.THERAPIST -> character.lifestyle.hasTherapist
    }
    ActionCard(
        icon = icon,
        title = stringResource(if (active) activeTitleRes else inactiveTitleRes),
        description = stringResource(descriptionRes),
        metaLabel = stringResource(
            R.string.format_yearly_cost,
            formatMoney(yearlyCost, character.countryCode)
        ),
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
}

private fun lifestyleYearlyCost(option: LifestyleOption): Int = when (option) {
    LifestyleOption.GYM -> HealthEngine.GYM_YEARLY_COST
    LifestyleOption.DIET -> HealthEngine.DIET_YEARLY_COST
    LifestyleOption.THERAPIST -> HealthEngine.THERAPIST_YEARLY_COST
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
