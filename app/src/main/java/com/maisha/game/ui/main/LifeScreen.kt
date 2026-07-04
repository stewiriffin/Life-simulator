// app/src/main/java/com/maisha/game/ui/main/LifeScreen.kt (modified — tab crossfade, celebrations, stat floats)
package com.maisha.game.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.CrimeType
import com.maisha.game.data.model.Expression
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import com.maisha.game.domain.GiftTier
import com.maisha.game.domain.InteractionType
import com.maisha.game.ui.components.AchievementUnlockedDialog
import com.maisha.game.ui.components.AgeUpButton
import com.maisha.game.ui.components.AppLoadingIndicator
import com.maisha.game.ui.components.EmptyStateCard
import com.maisha.game.ui.components.FloatingStatChangeLayer
import com.maisha.game.ui.components.MainTab
import com.maisha.game.ui.components.MaishaBottomNav
import com.maisha.game.ui.components.MoneyStatRow
import com.maisha.game.ui.components.CountryFlag
import com.maisha.game.ui.components.PersonAvatar
import com.maisha.game.ui.components.RecordBadge
import com.maisha.game.ui.components.StatBar
import com.maisha.game.ui.components.StatType
import com.maisha.game.ui.components.color
import com.maisha.game.ui.feedback.FeedbackEffect
import com.maisha.game.ui.celebration.CelebrationOverlay
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.navigation.NavAnimations
import com.maisha.game.ui.theme.AppIcons
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.NavyElevated
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.formatMoney

@Composable
fun LifeScreen(
    uiState: LifeUiState,
    onAgeUp: () -> Unit,
    onChoiceSelected: (EventChoice) -> Unit,
    onFamilyMemberSelected: (Person) -> Unit,
    onFamilyMemberDismissed: () -> Unit,
    onFamilyInteraction: (String, InteractionType, GiftTier?) -> Unit,
    onFamilyInteractionMessageDismissed: () -> Unit,
    onFindDate: () -> Unit,
    onDismissDatingProspects: () -> Unit,
    onStartDating: (Person) -> Unit,
    onPropose: (String) -> Unit,
    onBreakUp: (String) -> Unit,
    onHaveChild: () -> Unit,
    onRelationshipMessageDismissed: () -> Unit,
    onApplyForJob: (String) -> Unit,
    onQuitJob: () -> Unit,
    onRetire: () -> Unit,
    retirementPensionEstimate: Int,
    onDropOut: () -> Unit,
    onStartBusiness: (String, com.maisha.game.data.model.BusinessIndustry, Int) -> Unit,
    onLaunchCampaign: (com.maisha.game.data.model.PoliticalOffice, Int) -> Unit,
    onPassTaxPolicy: (com.maisha.game.data.model.TaxPolicyType) -> Unit,
    onSellBusiness: (String) -> Unit,
    businessInvestmentTiers: List<Int>,
    onCareerMessageDismissed: () -> Unit,
    onPurchaseAsset: (String) -> Unit,
    onSellAsset: (String) -> Unit,
    onRepairAsset: (String) -> Unit,
    onRentOutProperty: (String) -> Unit,
    onEvictTenant: (String) -> Unit,
    onSaveWill: (Map<String, Int>?) -> Unit,
    willBeneficiaries: List<com.maisha.game.data.model.Person>,
    onInvestFunds: (Int) -> Unit,
    onWithdrawFunds: (Int) -> Unit,
    onAssetsMessageDismissed: () -> Unit,
    onAttemptCrime: (CrimeType) -> Unit,
    onGoToTrial: (com.maisha.game.data.model.LawyerTier) -> Unit,
    lawyerPublicAffordable: Boolean,
    lawyerAverageFee: Int,
    lawyerAverageAffordable: Boolean,
    lawyerExpensiveFee: Int,
    lawyerExpensiveAffordable: Boolean,
    onVisitDoctor: (String, CareType) -> Unit,
    onSetLifestyleOption: (com.maisha.game.data.model.LifestyleOption, Boolean) -> Unit,
    onExecuteSideHustle: (com.maisha.game.data.model.HustleType) -> Unit,
    onAdoptPet: (com.maisha.game.data.model.PetSpecies) -> Unit,
    onCreateSocialAccount: () -> Unit,
    onPostSocialContent: () -> Unit,
    onMonetizeSocialAccount: () -> Unit,
    onPracticeSkill: (com.maisha.game.data.model.SkillType) -> Unit,
    onTakeMasterclass: (com.maisha.game.data.model.SkillType) -> Unit,
    onRenewVisa: () -> Unit,
    onApplyForCitizenship: () -> Unit,
    onTakeDrivingTest: () -> Unit,
    onVolunteer: () -> Unit,
    onDonateToCharity: (Int) -> Unit,
    donationTiers: List<Int>,
    onActionMessageDismissed: () -> Unit,
    onViewCharacterStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onAchievementDialogDismissed: () -> Unit,
    onCelebrationDismissed: () -> Unit,
    onStatDeltaFinished: (Long) -> Unit,
    onFeedbackHandled: () -> Unit,
    onDismissFamilyDatingTip: () -> Unit,
    onDismissFamilyDetailTip: () -> Unit,
    onThrowParty: (Int) -> Unit
) {
    if (uiState.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLoadingIndicator()
        }
        return
    }

    val character = uiState.character
    if (character == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaishaSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EmptyStateCard(
                illustration = EmptyStateIllustration.FAMILY,
                title = stringResource(R.string.app_name),
                message = stringResource(R.string.empty_no_character)
            )
        }
        return
    }

    FeedbackEffect(
        cues = uiState.pendingFeedbackCues,
        onHandled = onFeedbackHandled
    )

    if (!character.alive || uiState.navigateToLifeSummary) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLoadingIndicator()
        }
        return
    }

    uiState.currentCelebration?.let { celebration ->
        CelebrationOverlay(
            type = celebration,
            onDismiss = onCelebrationDismissed
        )
    }

    uiState.currentEvent?.let { event ->
        EventDialog(
            event = event,
            character = character,
            expression = uiState.headerExpression,
            onChoiceSelected = onChoiceSelected
        )
    }

    if (uiState.currentEvent == null) {
        uiState.currentAchievementDialog?.let { achievement ->
            AchievementUnlockedDialog(
                achievement = achievement,
                countryCode = uiState.character?.countryCode ?: "KE",
                onDismiss = onAchievementDialogDismissed
            )
        }
    }

    if (character.criminalRecord.awaitingTrial) {
        ArrestTrialDialog(
            character = character,
            publicDefenderAffordable = lawyerPublicAffordable,
            averageFee = lawyerAverageFee,
            averageAffordable = lawyerAverageAffordable,
            expensiveFee = lawyerExpensiveFee,
            expensiveAffordable = lawyerExpensiveAffordable,
            onSelectLawyer = onGoToTrial
        )
    }

    val incarcerated = character.criminalRecord.currentlyIncarcerated
    val disabledTabs = if (incarcerated) {
        setOf(MainTab.CAREER, MainTab.ASSETS)
    } else {
        emptySet()
    }

    var selectedTabOrdinal by rememberSaveable { mutableIntStateOf(MainTab.LIFE.ordinal) }
    val selectedTab = MainTab.entries[selectedTabOrdinal.coerceIn(MainTab.entries.indices)]

    LaunchedEffect(incarcerated, selectedTab) {
        if (selectedTab in disabledTabs) {
            selectedTabOrdinal = MainTab.LIFE.ordinal
        }
    }

    val snackbarHostState = rememberFamilySnackbarHostState()
    val prisonBackground = if (incarcerated) NavyElevated else MaterialTheme.colorScheme.background

    val eventActive = uiState.currentEvent != null
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(if (incarcerated) NavyDeep else MaterialTheme.colorScheme.background)
    ) {
    Scaffold(
        modifier = Modifier.then(
            if (eventActive) Modifier.blur(16.dp) else Modifier
        ),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = prisonBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            MaishaBottomNav(
                selectedTab = selectedTab,
                onTabSelected = { selectedTabOrdinal = it.ordinal },
                disabledTabs = disabledTabs
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                NavAnimations.tabEnter togetherWith NavAnimations.tabExit
            },
            label = "mainTabCrossfade"
        ) { tab ->
            when (tab) {
            MainTab.LIFE -> LifeTabContent(
                character = character,
                uiState = uiState,
                netWorth = uiState.netWorth,
                onAgeUp = onAgeUp,
                onViewCharacterStats = onViewCharacterStats,
                onOpenSettings = onOpenSettings,
                onStatDeltaFinished = onStatDeltaFinished,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.FAMILY -> FamilyScreen(
                character = character,
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                onMemberClick = onFamilyMemberSelected,
                onMemberDismiss = onFamilyMemberDismissed,
                onInteraction = onFamilyInteraction,
                onMessageDismissed = onFamilyInteractionMessageDismissed,
                onFindDate = onFindDate,
                onDismissDatingProspects = onDismissDatingProspects,
                onStartDating = onStartDating,
                onPropose = onPropose,
                onBreakUp = onBreakUp,
                onHaveChild = onHaveChild,
                onRelationshipMessageDismissed = onRelationshipMessageDismissed,
                onDismissFamilyDatingTip = onDismissFamilyDatingTip,
                onDismissFamilyDetailTip = onDismissFamilyDetailTip,
                onThrowParty = onThrowParty,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.CAREER -> CareerScreen(
                character = character,
                eligibleJobs = uiState.eligibleJobs,
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                onApplyForJob = onApplyForJob,
                onQuitJob = onQuitJob,
                onRetire = onRetire,
                retirementPensionEstimate = retirementPensionEstimate,
                onDropOut = onDropOut,
                onStartBusiness = onStartBusiness,
                onSellBusiness = onSellBusiness,
                investmentTiers = businessInvestmentTiers,
                onLaunchCampaign = onLaunchCampaign,
                onPassTaxPolicy = onPassTaxPolicy,
                onCareerMessageDismissed = onCareerMessageDismissed,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.ASSETS -> AssetsScreen(
                character = character,
                netWorth = uiState.netWorth,
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                onPurchaseAsset = onPurchaseAsset,
                onSellAsset = onSellAsset,
                onRepairAsset = onRepairAsset,
                onRentOutProperty = onRentOutProperty,
                onEvictTenant = onEvictTenant,
                onSaveWill = onSaveWill,
                willBeneficiaries = willBeneficiaries,
                onInvestFunds = onInvestFunds,
                onWithdrawFunds = onWithdrawFunds,
                onAssetsMessageDismissed = onAssetsMessageDismissed,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.ACTIONS -> ActionsScreen(
                character = character,
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                onAttemptCrime = onAttemptCrime,
                onVisitDoctor = onVisitDoctor,
                onSetLifestyleOption = onSetLifestyleOption,
                onExecuteSideHustle = onExecuteSideHustle,
                onAdoptPet = onAdoptPet,
                onCreateSocialAccount = onCreateSocialAccount,
                onPostSocialContent = onPostSocialContent,
                onMonetizeSocialAccount = onMonetizeSocialAccount,
                onPracticeSkill = onPracticeSkill,
                onTakeMasterclass = onTakeMasterclass,
                onRenewVisa = onRenewVisa,
                onApplyForCitizenship = onApplyForCitizenship,
                onTakeDrivingTest = onTakeDrivingTest,
                onVolunteer = onVolunteer,
                onDonateToCharity = onDonateToCharity,
                donationTiers = donationTiers,
                onActionMessageDismissed = onActionMessageDismissed,
                modifier = Modifier.padding(innerPadding)
            )
            }
        }
    }
    }
}

@Composable
private fun LifeTabContent(
    character: Character,
    uiState: LifeUiState,
    netWorth: Int,
    onAgeUp: () -> Unit,
    onViewCharacterStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onStatDeltaFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaishaSpacing.md)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                CharacterHeader(
                    character = character,
                    expression = uiState.headerExpression,
                    onViewCharacterStats = onViewCharacterStats,
                    onOpenSettings = onOpenSettings
                )
            }

            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    StatsCard(stats = character.stats, netWorth = netWorth, countryCode = character.countryCode)
                    FloatingStatChangeLayer(
                        events = uiState.pendingStatDeltas,
                        onEventFinished = onStatDeltaFinished,
                        statLabel = { type ->
                            when (type) {
                                StatType.HEALTH -> stringResource(R.string.stat_health)
                                StatType.HAPPINESS -> stringResource(R.string.stat_happiness)
                                StatType.SMARTS -> stringResource(R.string.stat_smarts)
                                StatType.LOOKS -> stringResource(R.string.stat_looks)
                                StatType.MONEY -> stringResource(R.string.stat_money)
                                StatType.NET_WORTH -> stringResource(R.string.label_net_worth)
                                StatType.FOLLOWERS -> stringResource(R.string.stat_followers)
                                StatType.SKILL -> stringResource(R.string.stat_skill)
                                else -> ""
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = MaishaSpacing.sm, end = MaishaSpacing.sm)
                    )
                }
            }

            item {
                StatusInfoCard(
                    education = character.education,
                    career = character.career,
                    netWorth = netWorth,
                    countryCode = character.countryCode,
                    hasCriminalRecord = character.criminalRecord.hasRecord,
                    timesArrested = character.criminalRecord.timesArrested
                )
            }

            item {
                Text(
                    text = stringResource(R.string.section_event_log),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (character.eventLog.filterNot { it.startsWith("::DEATH:") }.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.empty_event_log_life),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                itemsIndexed(
                    character.eventLog.filterNot { it.startsWith("::DEATH:") }
                ) { index, entry ->
                    EventLogCard(
                        entry = entry,
                        ageTag = character.age - index
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        AgeUpButton(
            onClick = onAgeUp,
            enabled = character.alive &&
                !uiState.isAgingUp &&
                uiState.currentEvent == null &&
                !character.criminalRecord.awaitingTrial,
            isLoading = uiState.isAgingUp,
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}

@Composable
private fun CharacterHeader(
    character: Character,
    expression: Expression,
    onViewCharacterStats: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val genderLabel = when (character.gender) {
        Gender.MALE -> stringResource(R.string.gender_male)
        Gender.FEMALE -> stringResource(R.string.gender_female)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PersonAvatar(
            avatarConfig = character.avatarConfig,
            size = 52,
            age = character.age,
            expression = expression,
            forPlayerCharacter = true
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CountryFlag(countryCode = character.countryCode, size = 18.dp)
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = stringResource(
                    R.string.format_character_birth_info,
                    genderLabel,
                    character.birthYear
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onViewCharacterStats,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = stringResource(R.string.content_desc_view_full_life),
                tint = GoldAccent,
                modifier = Modifier.size(28.dp)
            )
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.content_desc_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Card(
            shape = MaishaRadius.buttonShape,
            colors = CardDefaults.cardColors(containerColor = TealPrimary)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = MaishaSpacing.md + 2.dp, vertical = MaishaSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = character.age.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = stringResource(R.string.label_years),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun StatsCard(stats: Stats, netWorth: Int, countryCode: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(MaishaSpacing.md + 2.dp),
            verticalArrangement = Arrangement.spacedBy(MaishaSpacing.sm + 2.dp)
        ) {
            StatBar(type = StatType.HEALTH, value = stats.health)
            StatBar(type = StatType.HAPPINESS, value = stats.happiness)
            StatBar(type = StatType.SMARTS, value = stats.smarts)
            StatBar(type = StatType.LOOKS, value = stats.looks)
            MoneyStatRow(amount = stats.money, countryCode = countryCode)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_net_worth),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatMoney(netWorth, countryCode),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatusInfoCard(
    education: EducationState,
    career: CareerState,
    netWorth: Int,
    countryCode: String,
    hasCriminalRecord: Boolean,
    timesArrested: Int
) {
    val resources = LocalContext.current.resources
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(MaishaSpacing.md + 2.dp),
            verticalArrangement = Arrangement.spacedBy(MaishaSpacing.sm)
        ) {
            StatusInfoRow(
                icon = { Icon(AppIcons.Education, contentDescription = null, tint = StatType.SMARTS.color()) },
                label = stringResource(R.string.label_education),
                value = EducationFormatter.formatStatus(education, resources)
            )
            if (education.gpa > 0f && education.stage != SchoolStage.NONE &&
                education.stage != SchoolStage.GRADUATED
            ) {
                Text(
                    text = stringResource(R.string.format_gpa, education.gpa),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 28.dp)
                )
            }
            StatusInfoRow(
                icon = { Icon(AppIcons.Career, contentDescription = null, tint = GoldAccent) },
                label = stringResource(R.string.label_career),
                value = CareerFormatter.formatStatus(career, resources)
            )
            if (hasCriminalRecord) {
                RecordBadge(timesArrested = timesArrested)
            }
            StatusInfoRow(
                icon = { Icon(AppIcons.Wealth, contentDescription = null, tint = StatType.MONEY.color()) },
                label = stringResource(R.string.label_net_worth),
                value = formatMoney(netWorth, countryCode)
            )
        }
    }
}

@Composable
private fun StatusInfoRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        icon()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EventLogCard(entry: String, ageTag: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.format_age, ageTag.coerceAtLeast(0)),
                style = MaterialTheme.typography.labelSmall,
                color = TealPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = entry,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
