// app/src/main/java/com/maisha/game/ui/main/LifeScreen.kt (modified — tab crossfade, celebrations, stat floats)
package com.maisha.game.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maisha.game.R
import com.maisha.game.data.local.OnboardingTips
import com.maisha.game.data.model.BucketGoalKind
import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.CrimeType
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.Expression
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.LivingStandard
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import com.maisha.game.domain.EventLogClassifier
import com.maisha.game.domain.EventLogTone
import com.maisha.game.domain.GiftTier
import com.maisha.game.domain.InteractionType
import com.maisha.game.domain.YearQuest
import com.maisha.game.domain.YearQuestKind
import com.maisha.game.domain.YearQuestProgress
import com.maisha.game.ui.celebration.CelebrationOverlay
import com.maisha.game.ui.components.AchievementUnlockedDialog
import com.maisha.game.ui.components.AgeUpButton
import com.maisha.game.ui.components.AppLoadingIndicator
import com.maisha.game.ui.components.CountryFlag
import com.maisha.game.ui.components.DismissibleTipCard
import com.maisha.game.ui.components.EmptyStateCard
import com.maisha.game.ui.components.FloatingStatChangeLayer
import com.maisha.game.ui.components.MainTab
import com.maisha.game.ui.components.MaishaBottomNav
import com.maisha.game.ui.components.MoneyStatRow
import com.maisha.game.ui.components.PersonAvatar
import com.maisha.game.ui.components.RecordBadge
import com.maisha.game.ui.components.StatBar
import com.maisha.game.ui.components.StatType
import com.maisha.game.ui.components.color
import com.maisha.game.ui.feedback.FeedbackEffect
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.navigation.NavAnimations
import com.maisha.game.ui.theme.AppIcons
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.CreamBg
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.HairlineSoft
import com.maisha.game.ui.theme.InkPrimary
import com.maisha.game.ui.theme.InkTertiary
import com.maisha.game.ui.theme.LifeGreen
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.NavyElevated
import com.maisha.game.ui.theme.NavySurface
import com.maisha.game.ui.theme.StatHealth
import com.maisha.game.ui.theme.StatHappiness
import com.maisha.game.ui.theme.StatLooks
import com.maisha.game.ui.theme.StatSmarts
import com.maisha.game.ui.theme.SuccessGreen
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.formatMoney

@Composable
fun LifeScreen(
    uiState: LifeUiState,
    onAgeUp: () -> Unit,
    onChoiceSelected: (EventChoice) -> Unit,
    onFamilyMemberSelected: (Person) -> Unit,
    onFamilyMemberDismissed: () -> Unit,
    onPetSelected: (com.maisha.game.data.model.Pet) -> Unit,
    onPetDismissed: () -> Unit,
    onPetCare: (String, com.maisha.game.domain.PetCareAction) -> Unit,
    onFamilyInteraction: (String, InteractionType, GiftTier?) -> Unit,
    onFamilyInteractionMessageDismissed: () -> Unit,
    onFindDate: () -> Unit,
    onSeekFriendship: () -> Unit,
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
    onSetWorkEffort: (com.maisha.game.data.model.WorkEffort) -> Unit,
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
    onDepositSavings: (Int) -> Unit,
    onWithdrawSavings: (Int) -> Unit,
    onSetLivingStandard: (com.maisha.game.data.model.LivingStandard) -> Unit,
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
    onShowcaseSkill: (com.maisha.game.data.model.SkillType) -> Unit,
    onAdoptBucketGoal: (String) -> Unit,
    onRenewVisa: () -> Unit,
    onApplyForCitizenship: () -> Unit,
    onTakeDrivingTest: () -> Unit,
    onVolunteer: () -> Unit,
    onDonateToCharity: (Int) -> Unit,
    donationTiers: List<Int>,
    onPerformLeisure: (com.maisha.game.domain.LeisureActivity) -> Unit,
    onActionMessageDismissed: () -> Unit,
    onLifePulseMessageDismissed: () -> Unit,
    onYearRecapDismissed: () -> Unit,
    onViewCharacterStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onAchievementDialogDismissed: () -> Unit,
    onCelebrationDismissed: () -> Unit,
    onStatDeltaFinished: (Long) -> Unit,
    onFeedbackHandled: () -> Unit,
    onDismissFamilyDatingTip: () -> Unit,
    onDismissFamilyDetailTip: () -> Unit,
    onDismissYearQuestsTip: () -> Unit = {},
    onDismissLeisureTip: () -> Unit = {},
    onDismissAssetsMarketsTip: () -> Unit = {},
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
                countryCode = uiState.character?.countryCode ?: "XX",
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

    LaunchedEffect(uiState.lifePulseMessage) {
        uiState.lifePulseMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onLifePulseMessageDismissed()
        }
    }

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
                onYearRecapDismissed = onYearRecapDismissed,
                onDismissYearQuestsTip = onDismissYearQuestsTip,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.FAMILY -> FamilyScreen(
                character = character,
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                onMemberClick = onFamilyMemberSelected,
                onMemberDismiss = onFamilyMemberDismissed,
                onPetClick = onPetSelected,
                onPetDismiss = onPetDismissed,
                onPetCare = onPetCare,
                onInteraction = onFamilyInteraction,
                onMessageDismissed = onFamilyInteractionMessageDismissed,
                onFindDate = onFindDate,
                onSeekFriendship = onSeekFriendship,
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
                onSetWorkEffort = onSetWorkEffort,
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
                onDepositSavings = onDepositSavings,
                onWithdrawSavings = onWithdrawSavings,
                onSetLivingStandard = onSetLivingStandard,
                onAssetsMessageDismissed = onAssetsMessageDismissed,
                onDismissAssetsMarketsTip = onDismissAssetsMarketsTip,
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
                onShowcaseSkill = onShowcaseSkill,
                onAdoptBucketGoal = onAdoptBucketGoal,
                onTakeMasterclass = onTakeMasterclass,
                onRenewVisa = onRenewVisa,
                onApplyForCitizenship = onApplyForCitizenship,
                onTakeDrivingTest = onTakeDrivingTest,
                onVolunteer = onVolunteer,
                onDonateToCharity = onDonateToCharity,
                donationTiers = donationTiers,
                onPerformLeisure = onPerformLeisure,
                onActionMessageDismissed = onActionMessageDismissed,
                onDismissLeisureTip = onDismissLeisureTip,
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
    onYearRecapDismissed: () -> Unit,
    onDismissYearQuestsTip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val journalEntries = remember(character.eventLog) {
        character.eventLog.filterNot { it.startsWith("::DEATH:") }
    }
    var journalExpanded by rememberSaveable { mutableStateOf(false) }
    val visibleJournal = if (journalExpanded || journalEntries.size <= JOURNAL_VISIBLE_CAP) {
        journalEntries
    } else {
        journalEntries.take(JOURNAL_VISIBLE_CAP)
    }
    val hiddenJournalCount = (journalEntries.size - JOURNAL_VISIBLE_CAP).coerceAtLeast(0)
    val spotlightBucket = character.bucketList.firstOrNull { !it.completed }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBg)
    ) {
        LifeHeroHeader(
            character = character,
            expression = uiState.headerExpression,
            dynastyScore = uiState.dynastyScore,
            dynastyTitleKey = uiState.dynastyTitleKey,
            questYearStreak = character.questYearStreak,
            onViewCharacterStats = onViewCharacterStats,
            onOpenSettings = onOpenSettings
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    StatusInfoCard(
                        character = character,
                        education = character.education,
                        career = character.career,
                        netWorth = netWorth,
                        countryCode = character.countryCode,
                        hasCriminalRecord = character.criminalRecord.hasRecord,
                        timesArrested = character.criminalRecord.timesArrested
                    )
                }

                val showQuestsTip = uiState.tipsLoaded &&
                    OnboardingTips.YEAR_QUESTS !in uiState.seenTipIds &&
                    uiState.yearQuests.isNotEmpty()
                if (showQuestsTip) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        DismissibleTipCard(
                            text = stringResource(R.string.tip_year_quests),
                            onDismiss = onDismissYearQuestsTip
                        )
                    }
                }

                if (uiState.yearRecapLines.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        YearRecapCard(
                            lines = uiState.yearRecapLines,
                            onDismiss = onYearRecapDismissed
                        )
                    }
                }

                if (spotlightBucket != null) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        BucketSpotlightRow(kind = spotlightBucket.kind)
                    }
                }

                if (character.unlockedMilestoneIds.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        RecentMilestonesRow(milestoneIds = character.unlockedMilestoneIds)
                    }
                }

                if (uiState.yearQuests.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        YearQuestsCard(
                            quests = uiState.yearQuests,
                            progress = uiState.yearQuestProgress,
                            countryCode = character.countryCode
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.section_event_log),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = InkTertiary,
                        letterSpacing = 0.8.sp
                    )
                }

                if (journalEntries.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.empty_event_log_life),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    itemsIndexed(visibleJournal) { index, entry ->
                        EventLogStrip(
                            entry = entry,
                            ageTag = character.age - index
                        )
                    }
                    if (!journalExpanded && hiddenJournalCount > 0) {
                        item {
                            TextButton(onClick = { journalExpanded = true }) {
                                Text(
                                    text = stringResource(
                                        R.string.btn_show_earlier_years,
                                        hiddenJournalCount
                                    )
                                )
                            }
                        }
                    } else if (journalExpanded && journalEntries.size > JOURNAL_VISIBLE_CAP) {
                        item {
                            TextButton(onClick = { journalExpanded = false }) {
                                Text(text = stringResource(R.string.btn_collapse_earlier_years))
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

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
                        StatType.KARMA -> stringResource(R.string.stat_karma)
                        else -> ""
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = MaishaSpacing.sm, end = MaishaSpacing.sm)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CreamBg)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            AgeUpButton(
                onClick = onAgeUp,
                enabled = character.alive &&
                    !uiState.isAgingUp &&
                    uiState.currentEvent == null &&
                    !character.criminalRecord.awaitingTrial,
                isLoading = uiState.isAgingUp
            )
        }
    }
}

private const val JOURNAL_VISIBLE_CAP = 12

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LifeHeroHeader(
    character: Character,
    expression: Expression,
    dynastyScore: Int,
    dynastyTitleKey: String,
    questYearStreak: Int,
    onViewCharacterStats: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val countryName = com.maisha.game.data.CountryCatalog.getCountry(character.countryCode).displayName
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(NavyDeep, NavySurface, NavyElevated)
                )
            )
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onViewCharacterStats, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = stringResource(R.string.content_desc_view_full_life),
                        tint = GoldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.content_desc_settings),
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.app_name).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.55f),
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(MaishaRadius.avatar))
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                PersonAvatar(
                    avatarConfig = character.avatarConfig,
                    size = 72,
                    age = character.age,
                    expression = expression,
                    forPlayerCharacter = true,
                    seed = character.name
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatMoney(character.stats.money, character.countryCode),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                ) {
                    CountryFlag(countryCode = character.countryCode, size = 14.dp)
                    Text(
                        text = countryName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeroChip(
                        text = stringResource(R.string.format_generation_short, character.generationNumber),
                        bg = LifeGreen.copy(alpha = 0.25f),
                        fg = SuccessGreen
                    )
                    if (dynastyScore > 0 || character.generationNumber > 1) {
                        HeroChip(
                            text = dynastyTitleLabel(dynastyTitleKey),
                            bg = GoldAccent.copy(alpha = 0.2f),
                            fg = GoldAccent
                        )
                        HeroChip(
                            text = stringResource(R.string.format_dynasty_score_short, dynastyScore),
                            bg = GoldAccent.copy(alpha = 0.28f),
                            fg = GoldAccent
                        )
                    }
                    if (questYearStreak > 0) {
                        HeroChip(
                            text = stringResource(R.string.format_quest_streak, questYearStreak),
                            bg = LifeGreen.copy(alpha = 0.35f),
                            fg = GoldAccent
                        )
                    }
                    if (character.socialMedia.hasAccount &&
                        character.socialMedia.fameTier != com.maisha.game.data.model.FameTier.UNKNOWN
                    ) {
                        HeroChip(
                            text = fameTierShortLabel(character.socialMedia.fameTier),
                            bg = GoldAccent.copy(alpha = 0.22f),
                            fg = GoldAccent
                        )
                    }
                    if (character.stats.karma != 50) {
                        HeroChip(
                            text = stringResource(R.string.format_karma_chip, character.stats.karma),
                            bg = if (character.stats.karma > 50) {
                                LifeGreen.copy(alpha = 0.3f)
                            } else {
                                CoralNegative.copy(alpha = 0.28f)
                            },
                            fg = Color.White
                        )
                    }
                    if (character.isLivingAbroad()) {
                        val visaText = when {
                            character.currentVisa != null -> stringResource(
                                R.string.format_visa_chip,
                                character.visaYearsRemaining
                            )
                            else -> stringResource(R.string.chip_abroad_citizen_flavor)
                        }
                        HeroChip(
                            text = visaText,
                            bg = TealPrimary.copy(alpha = 0.3f),
                            fg = Color.White
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = character.age.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    lineHeight = 44.sp
                )
                Text(
                    text = stringResource(R.string.label_years).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.35f),
                    letterSpacing = 0.8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HeroStatBar(
                label = stringResource(R.string.stat_health),
                value = character.stats.health,
                color = StatHealth,
                modifier = Modifier.weight(1f)
            )
            HeroStatBar(
                label = stringResource(R.string.stat_happiness),
                value = character.stats.happiness,
                color = StatHappiness,
                modifier = Modifier.weight(1f)
            )
            HeroStatBar(
                label = stringResource(R.string.stat_smarts),
                value = character.stats.smarts,
                color = StatSmarts,
                modifier = Modifier.weight(1f)
            )
            HeroStatBar(
                label = stringResource(R.string.stat_looks),
                value = character.stats.looks,
                color = StatLooks,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeroChip(text: String, bg: Color, fg: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun HeroStatBar(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 9.sp,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0, 100) / 100f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun dynastyTitleLabel(titleKey: String): String {
    val resId = when (titleKey) {
        "dynasty_title_seedling" -> R.string.dynasty_title_seedling
        "dynasty_title_rooted" -> R.string.dynasty_title_rooted
        "dynasty_title_rising" -> R.string.dynasty_title_rising
        "dynasty_title_powerhouse" -> R.string.dynasty_title_powerhouse
        "dynasty_title_legend" -> R.string.dynasty_title_legend
        else -> R.string.dynasty_title_seedling
    }
    return stringResource(resId)
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
            forPlayerCharacter = true,
            seed = character.name
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
            StatBar(type = StatType.KARMA, value = stats.karma)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusInfoCard(
    character: Character,
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
                icon = { Icon(AppIcons.Wealth, contentDescription = null, tint = GoldAccent) },
                label = stringResource(R.string.label_cash),
                value = formatMoney(character.stats.money, countryCode),
                valueColor = GoldAccent
            )
            StatusInfoRow(
                icon = { Icon(AppIcons.Wealth, contentDescription = null, tint = StatType.MONEY.color()) },
                label = stringResource(R.string.label_net_worth),
                value = formatMoney(netWorth, countryCode)
            )
            StatusInfoRow(
                icon = { Icon(AppIcons.Education, contentDescription = null, tint = StatType.SMARTS.color()) },
                label = stringResource(R.string.label_education),
                value = EducationFormatter.formatStatus(education, resources, countryCode)
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
            Text(
                text = stringResource(R.string.section_lifestyle_pulse),
                style = MaterialTheme.typography.labelSmall,
                color = InkTertiary,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HeroChip(
                    text = livingStandardLabel(character.lifestyle.livingStandard),
                    bg = LifeGreen.copy(alpha = 0.12f),
                    fg = LifeGreen
                )
                if (character.lifestyle.hasHealthInsurance) {
                    HeroChip(
                        text = stringResource(R.string.chip_lifestyle_insurance),
                        bg = TealPrimary.copy(alpha = 0.12f),
                        fg = TealPrimary
                    )
                }
                if (character.lifestyle.hasGymMembership) {
                    HeroChip(
                        text = stringResource(R.string.chip_lifestyle_gym),
                        bg = TealPrimary.copy(alpha = 0.12f),
                        fg = TealPrimary
                    )
                }
                if (character.lifestyle.hasTherapist) {
                    HeroChip(
                        text = stringResource(R.string.chip_lifestyle_therapist),
                        bg = TealPrimary.copy(alpha = 0.12f),
                        fg = TealPrimary
                    )
                }
                if (character.isLivingAbroad()) {
                    HeroChip(
                        text = if (character.currentVisa != null) {
                            stringResource(
                                R.string.format_visa_chip,
                                character.visaYearsRemaining
                            )
                        } else {
                            stringResource(R.string.chip_abroad_citizen_flavor)
                        },
                        bg = TealPrimary.copy(alpha = 0.18f),
                        fg = TealPrimary
                    )
                    if (character.yearsInCurrentCountry > 0) {
                        HeroChip(
                            text = stringResource(
                                R.string.format_years_in_country,
                                character.yearsInCurrentCountry
                            ),
                            bg = TealPrimary.copy(alpha = 0.1f),
                            fg = TealPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun livingStandardLabel(standard: LivingStandard): String = when (standard) {
    LivingStandard.FRUGAL -> stringResource(R.string.living_standard_frugal)
    LivingStandard.MODEST -> stringResource(R.string.living_standard_modest)
    LivingStandard.COMFORTABLE -> stringResource(R.string.living_standard_comfortable)
    LivingStandard.LUXURY -> stringResource(R.string.living_standard_luxury)
}

@Composable
private fun StatusInfoRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified
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
                color = valueColor,
                fontWeight = if (valueColor != Color.Unspecified) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EventLogStrip(entry: String, ageTag: Int) {
    val tone = remember(entry) { EventLogClassifier.classify(entry) }
    val dot = when (tone) {
        EventLogTone.MILESTONE -> GoldAccent
        EventLogTone.POSITIVE -> SuccessGreen
        EventLogTone.NEGATIVE -> CoralNegative
        EventLogTone.NEUTRAL -> InkTertiary
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        if (ageTag >= 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.format_age, ageTag).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = InkTertiary,
                    letterSpacing = 0.8.sp
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(HairlineSoft)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(7.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(dot)
            )
            Text(
                text = entry,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun YearQuestsCard(
    quests: List<YearQuest>,
    progress: List<YearQuestProgress>,
    countryCode: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.section_year_quests),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
            quests.forEach { quest ->
                val match = progress.firstOrNull { it.quest.kind == quest.kind }
                val current = match?.current ?: 0
                val done = match?.completed == true
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = yearQuestTitle(quest, countryCode),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (done) {
                                stringResource(R.string.year_quest_complete_badge)
                            } else {
                                stringResource(
                                    R.string.year_quest_progress,
                                    current.coerceAtMost(quest.target),
                                    quest.target
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (done) SuccessGreen else TealPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    StatBar(
                        type = StatType.PERFORMANCE,
                        value = current.coerceAtMost(quest.target),
                        maxValue = quest.target.coerceAtLeast(1),
                        label = "",
                        showIcon = false
                    )
                }
            }
        }
    }
}

@Composable
private fun yearQuestTitle(quest: YearQuest, countryCode: String): String = when (quest.kind) {
    YearQuestKind.RAISE_HAPPINESS ->
        stringResource(R.string.year_quest_raise_happiness, quest.target)
    YearQuestKind.RAISE_HEALTH ->
        stringResource(R.string.year_quest_raise_health, quest.target)
    YearQuestKind.EARN_MONEY ->
        stringResource(R.string.year_quest_earn_money, formatMoney(quest.target, countryCode))
    YearQuestKind.STUDY_SMARTS ->
        stringResource(R.string.year_quest_study_smarts, quest.target)
    YearQuestKind.BOND_FAMILY ->
        stringResource(R.string.year_quest_bond_family)
    YearQuestKind.STAY_OUT_OF_TROUBLE ->
        stringResource(R.string.year_quest_stay_clean)
    YearQuestKind.GROW_FOLLOWERS ->
        stringResource(R.string.year_quest_grow_followers, quest.target)
    YearQuestKind.RAISE_SKILL ->
        stringResource(R.string.year_quest_raise_skill, quest.target)
    YearQuestKind.HOLD_JOB ->
        stringResource(R.string.year_quest_hold_job)
    YearQuestKind.GROW_SAVINGS ->
        stringResource(
            R.string.year_quest_grow_savings,
            formatMoney(quest.target, countryCode)
        )
}

@Composable
private fun YearRecapCard(lines: List<String>, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.section_year_recap),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
                Text(
                    text = stringResource(R.string.btn_dismiss_year_recap),
                    style = MaterialTheme.typography.labelSmall,
                    color = TealPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            lines.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkPrimary
                )
            }
        }
    }
}

@Composable
private fun BucketSpotlightRow(kind: BucketGoalKind) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.section_bucket_spotlight),
                    style = MaterialTheme.typography.labelSmall,
                    color = InkTertiary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = bucketSpotlightTitle(kind),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            HeroChip(
                text = stringResource(R.string.bucket_spotlight_pending),
                bg = GoldAccent.copy(alpha = 0.18f),
                fg = GoldAccent
            )
        }
    }
}

@Composable
private fun bucketSpotlightTitle(kind: BucketGoalKind): String = when (kind) {
    BucketGoalKind.OWN_HOME -> stringResource(R.string.bucket_own_home_title)
    BucketGoalKind.REACH_FAME -> stringResource(R.string.bucket_reach_fame_title)
    BucketGoalKind.START_BUSINESS -> stringResource(R.string.bucket_start_business_title)
    BucketGoalKind.WIN_OFFICE -> stringResource(R.string.bucket_win_office_title)
    BucketGoalKind.RAISE_CHILD -> stringResource(R.string.bucket_raise_child_title)
    BucketGoalKind.HIT_WEALTH -> stringResource(R.string.bucket_hit_wealth_title)
    BucketGoalKind.MASTER_SKILL -> stringResource(R.string.bucket_master_skill_title)
}

@Composable
private fun RecentMilestonesRow(milestoneIds: List<String>) {
    val recent = milestoneIds.takeLast(4).reversed()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.section_recent_milestones),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = InkTertiary,
            letterSpacing = 0.8.sp
        )
        recent.forEach { id ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "✓",
                    color = GoldAccent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = milestoneTitle(id),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun milestoneTitle(id: String): String = when (id) {
    com.maisha.game.domain.MilestoneEngine.ID_AGE_18 -> stringResource(R.string.milestone_age_18)
    com.maisha.game.domain.MilestoneEngine.ID_AGE_50 -> stringResource(R.string.milestone_age_50)
    com.maisha.game.domain.MilestoneEngine.ID_AGE_75 -> stringResource(R.string.milestone_age_75)
    com.maisha.game.domain.MilestoneEngine.ID_AGE_100 -> stringResource(R.string.milestone_age_100)
    com.maisha.game.domain.MilestoneEngine.ID_FIRST_JOB -> stringResource(R.string.milestone_first_job)
    com.maisha.game.domain.MilestoneEngine.ID_DRIVING -> stringResource(R.string.milestone_driving_license)
    com.maisha.game.domain.MilestoneEngine.ID_MARRIAGE -> stringResource(R.string.milestone_marriage)
    com.maisha.game.domain.MilestoneEngine.ID_FIRST_CHILD -> stringResource(R.string.milestone_first_child)
    com.maisha.game.domain.MilestoneEngine.ID_FIRST_HOME -> stringResource(R.string.milestone_first_home)
    com.maisha.game.domain.MilestoneEngine.ID_FIRST_BUSINESS -> stringResource(R.string.milestone_first_business)
    com.maisha.game.domain.MilestoneEngine.ID_ELECTED -> stringResource(R.string.milestone_elected)
    com.maisha.game.domain.MilestoneEngine.ID_VERIFIED -> stringResource(R.string.milestone_verified)
    com.maisha.game.domain.MilestoneEngine.ID_GRADUATED -> stringResource(R.string.milestone_graduated)
    com.maisha.game.domain.MilestoneEngine.ID_WEALTHY -> stringResource(R.string.milestone_wealthy)
    else -> stringResource(R.string.life_milestone_unknown)
}

@Composable
private fun fameTierShortLabel(tier: com.maisha.game.data.model.FameTier): String = when (tier) {
    com.maisha.game.data.model.FameTier.LOCAL -> stringResource(R.string.fame_tier_local)
    com.maisha.game.data.model.FameTier.REGIONAL -> stringResource(R.string.fame_tier_regional)
    com.maisha.game.data.model.FameTier.NATIONAL -> stringResource(R.string.fame_tier_national)
    com.maisha.game.data.model.FameTier.GLOBAL -> stringResource(R.string.fame_tier_global)
    com.maisha.game.data.model.FameTier.UNKNOWN -> stringResource(R.string.fame_tier_unknown)
}

