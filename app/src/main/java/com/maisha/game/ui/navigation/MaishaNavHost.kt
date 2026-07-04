// app/src/main/java/com/maisha/game/ui/navigation/MaishaNavHost.kt (modified — NavAnimations transitions)
package com.maisha.game.ui.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.maisha.game.ads.AdManager
import com.maisha.game.ui.avatar.AvatarPickerScreen
import com.maisha.game.ui.charactercreation.CharacterCreationScreen
import com.maisha.game.ui.charactercreation.CharacterCreationViewModel
import com.maisha.game.ui.main.LifeScreen
import com.maisha.game.ui.main.LifeViewModel
import com.maisha.game.ui.slots.SlotPickerScreen
import com.maisha.game.ui.slots.SlotPickerViewModel
import com.maisha.game.ui.achievements.AchievementsScreen
import com.maisha.game.ui.achievements.AchievementsViewModel
import com.maisha.game.ui.settings.SettingsScreen
import com.maisha.game.ui.settings.SettingsViewModel
import com.maisha.game.ui.onboarding.OnboardingScreen
import com.maisha.game.ui.onboarding.OnboardingViewModel
import com.maisha.game.ui.notifications.NotificationPermissionEffect
import com.maisha.game.ui.legacy.AncestryScreen
import com.maisha.game.ui.summary.CharacterStatsScreen
import com.maisha.game.ui.summary.LifeSummaryScreen
import com.maisha.game.ui.summary.LifeSummaryViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val SLOT_PICKER = "slot_picker"
    const val CHARACTER_CREATION = "character_creation/{slotId}"
    const val AVATAR_PICKER = "avatar_picker/{slotId}"
    const val LIFE = "life/{slotId}"
    const val LIFE_SUMMARY = "life_summary/{slotId}"
    const val CHARACTER_STATS = "character_stats/{slotId}"
    const val ANCESTRY = "ancestry/{slotId}"
    const val ACHIEVEMENTS = "achievements"
    const val SETTINGS = "settings"

    fun characterCreation(slotId: Int) = "character_creation/$slotId"
    fun avatarPicker(slotId: Int) = "avatar_picker/$slotId"
    fun life(slotId: Int) = "life/$slotId"
    fun lifeSummary(slotId: Int) = "life_summary/$slotId"
    fun characterStats(slotId: Int) = "character_stats/$slotId"
    fun ancestry(slotId: Int) = "ancestry/$slotId"
}

private val slotIdArgument = navArgument("slotId") { type = NavType.IntType }

@Composable
fun MaishaNavHost(
    navController: NavHostController,
    startDestination: String,
    adManager: AdManager,
    deepLinkSlotId: Int? = null
) {
    val activity = LocalContext.current as? Activity

    LaunchedEffect(deepLinkSlotId) {
        deepLinkSlotId?.let { slotId ->
            navController.navigate(Routes.life(slotId)) {
                popUpTo(Routes.SLOT_PICKER) { inclusive = false }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { NavAnimations.enterForward },
        exitTransition = { NavAnimations.exitForward },
        popEnterTransition = { NavAnimations.enterBack },
        popExitTransition = { NavAnimations.exitBack }
    ) {
        composable(Routes.ONBOARDING) {
            val viewModel: OnboardingViewModel = hiltViewModel()
            val onboardingState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(onboardingState.navigateToCharacterCreation) {
                if (onboardingState.navigateToCharacterCreation) {
                    navController.navigate(Routes.characterCreation(0)) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                    viewModel.onNavigationHandled()
                }
            }

            OnboardingScreen(
                onFinish = viewModel::onFinish,
                onSkip = viewModel::onSkip
            )
        }

        composable(Routes.SLOT_PICKER) {
            val viewModel: SlotPickerViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.navigateToLife) {
                uiState.navigateToLife?.let { slotId ->
                    navController.navigate(Routes.life(slotId))
                    viewModel.onNavigationHandled()
                }
            }

            LaunchedEffect(uiState.navigateToCreation) {
                uiState.navigateToCreation?.let { slotId ->
                    navController.navigate(Routes.characterCreation(slotId))
                    viewModel.onNavigationHandled()
                }
            }

            LaunchedEffect(uiState.navigateToSummary) {
                uiState.navigateToSummary?.let { slotId ->
                    navController.navigate(Routes.lifeSummary(slotId))
                    viewModel.onNavigationHandled()
                }
            }

            SlotPickerScreen(
                uiState = uiState,
                onContinue = viewModel::onContinue,
                onViewSummary = viewModel::onViewSummary,
                onStartNewLife = viewModel::onStartNewLife,
                onConfirmOverwrite = viewModel::onConfirmOverwrite,
                onDismissOverwrite = viewModel::onDismissOverwrite,
                onClearCorruptedSlot = viewModel::onClearCorruptedSlot,
                onConfirmClearCorrupted = viewModel::onConfirmClearCorrupted,
                onDismissClearCorrupted = viewModel::onDismissClearCorrupted,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.CHARACTER_CREATION,
            arguments = listOf(slotIdArgument)
        ) {
            val viewModel: CharacterCreationViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val slotId = it.arguments?.getInt("slotId") ?: 0

            LaunchedEffect(uiState.navigateToAvatarPicker) {
                if (uiState.navigateToAvatarPicker) {
                    navController.navigate(Routes.avatarPicker(slotId))
                    viewModel.onAvatarPickerNavigationHandled()
                }
            }

            LaunchedEffect(uiState.navigateToLife) {
                if (uiState.navigateToLife) {
                    navController.navigate(Routes.life(slotId)) {
                        popUpTo(Routes.characterCreation(slotId)) { inclusive = true }
                    }
                    viewModel.onNavigationHandled()
                }
            }

            CharacterCreationScreen(
                uiState = uiState,
                filteredCountries = viewModel.filteredCountries(),
                onNameChange = viewModel::onNameChange,
                onGenderSelected = viewModel::onGenderSelected,
                onCountrySelected = viewModel::onCountrySelected,
                onCountrySearchChange = viewModel::onCountrySearchChange,
                onRandomName = viewModel::onRandomName,
                onContinueToAvatar = viewModel::onContinueToAvatarPicker
            )
        }

        composable(
            route = Routes.AVATAR_PICKER,
            arguments = listOf(slotIdArgument)
        ) {
            val slotId = it.arguments?.getInt("slotId") ?: 0
            val parentEntry = remember(it) {
                navController.getBackStackEntry(Routes.characterCreation(slotId))
            }
            val viewModel: CharacterCreationViewModel = hiltViewModel(parentEntry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.navigateToLife) {
                if (uiState.navigateToLife) {
                    navController.navigate(Routes.life(slotId)) {
                        popUpTo(Routes.characterCreation(slotId)) { inclusive = true }
                    }
                    viewModel.onNavigationHandled()
                }
            }

            AvatarPickerScreen(
                avatarConfig = uiState.avatarConfig,
                isSaving = uiState.isSaving,
                onAvatarChange = viewModel::onAvatarChange,
                onStartLife = viewModel::onStartLife
            )
        }

        composable(
            route = Routes.LIFE,
            arguments = listOf(slotIdArgument)
        ) {
            val slotId = it.arguments?.getInt("slotId") ?: 0
            val viewModel: LifeViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.navigateToLifeSummary) {
                if (uiState.navigateToLifeSummary) {
                    navController.navigate(Routes.lifeSummary(slotId)) {
                        popUpTo(Routes.life(slotId)) { inclusive = true }
                    }
                    viewModel.onLifeSummaryNavigationHandled()
                }
            }

            LaunchedEffect(uiState.navigateToSlotPicker) {
                if (uiState.navigateToSlotPicker) {
                    navController.navigate(Routes.SLOT_PICKER) {
                        popUpTo(Routes.life(slotId)) { inclusive = true }
                    }
                    viewModel.onSlotPickerNavigationHandled()
                }
            }

            LaunchedEffect(uiState.showInterstitialAd, activity) {
                if (uiState.showInterstitialAd && activity != null) {
                    adManager.showInterstitialIfReady(activity) {
                        viewModel.onInterstitialAdHandled()
                    }
                }
            }

            LifeScreen(
                uiState = uiState,
                onAgeUp = viewModel::onAgeUp,
                onChoiceSelected = viewModel::onChoiceSelected,
                onFamilyMemberSelected = viewModel::onFamilyMemberSelected,
                onFamilyMemberDismissed = viewModel::onFamilyMemberDismissed,
                onFamilyInteraction = viewModel::onFamilyInteraction,
                onFamilyInteractionMessageDismissed = viewModel::onFamilyInteractionMessageDismissed,
                onFindDate = viewModel::onFindDate,
                onDismissDatingProspects = viewModel::onDismissDatingProspects,
                onStartDating = viewModel::onStartDating,
                onPropose = viewModel::onPropose,
                onBreakUp = viewModel::onBreakUp,
                onHaveChild = viewModel::onHaveChild,
                onRelationshipMessageDismissed = viewModel::onRelationshipMessageDismissed,
                onApplyForJob = viewModel::onApplyForJob,
                onQuitJob = viewModel::onQuitJob,
                onRetire = viewModel::onRetire,
                retirementPensionEstimate = viewModel.retirementPensionEstimate(),
                onDropOut = viewModel::onDropOut,
                onStartBusiness = viewModel::onStartBusiness,
                onSellBusiness = viewModel::onSellBusiness,
                businessInvestmentTiers = viewModel.businessInvestmentTiers(),
                onLaunchCampaign = viewModel::onLaunchCampaign,
                onPassTaxPolicy = viewModel::onPassTaxPolicy,
                onCareerMessageDismissed = viewModel::onCareerMessageDismissed,
                onPurchaseAsset = viewModel::onPurchaseAsset,
                onSellAsset = viewModel::onSellAsset,
                onRepairAsset = viewModel::onRepairAsset,
                onRentOutProperty = viewModel::onRentOutProperty,
                onEvictTenant = viewModel::onEvictTenant,
                onSaveWill = viewModel::onSaveWill,
                willBeneficiaries = viewModel.willBeneficiaries(),
                onInvestFunds = viewModel::onInvestFunds,
                onWithdrawFunds = viewModel::onWithdrawFunds,
                onAssetsMessageDismissed = viewModel::onAssetsMessageDismissed,
                onAttemptCrime = viewModel::onAttemptCrime,
                onGoToTrial = viewModel::onGoToTrial,
                lawyerPublicAffordable = viewModel.canAffordLawyer(com.maisha.game.data.model.LawyerTier.PUBLIC_DEFENDER),
                lawyerAverageFee = viewModel.lawyerFee(com.maisha.game.data.model.LawyerTier.AVERAGE),
                lawyerAverageAffordable = viewModel.canAffordLawyer(com.maisha.game.data.model.LawyerTier.AVERAGE),
                lawyerExpensiveFee = viewModel.lawyerFee(com.maisha.game.data.model.LawyerTier.EXPENSIVE),
                lawyerExpensiveAffordable = viewModel.canAffordLawyer(com.maisha.game.data.model.LawyerTier.EXPENSIVE),
                onVisitDoctor = viewModel::onVisitDoctor,
                onSetLifestyleOption = viewModel::onSetLifestyleOption,
                onExecuteSideHustle = viewModel::onExecuteSideHustle,
                onAdoptPet = viewModel::onAdoptPet,
                onCreateSocialAccount = viewModel::onCreateSocialAccount,
                onPostSocialContent = viewModel::onPostSocialContent,
                onMonetizeSocialAccount = viewModel::onMonetizeSocialAccount,
                onPracticeSkill = viewModel::onPracticeSkill,
                onTakeMasterclass = viewModel::onTakeMasterclass,
                onRenewVisa = viewModel::onRenewVisa,
                onApplyForCitizenship = viewModel::onApplyForCitizenship,
                onTakeDrivingTest = viewModel::onTakeDrivingTest,
                onVolunteer = viewModel::onVolunteer,
                onDonateToCharity = viewModel::onDonateToCharity,
                donationTiers = viewModel.donationTiers(),
                onActionMessageDismissed = viewModel::onActionMessageDismissed,
                onViewCharacterStats = {
                    navController.navigate(Routes.characterStats(slotId))
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onAchievementDialogDismissed = viewModel::onAchievementDialogDismissed,
                onCelebrationDismissed = viewModel::onCelebrationDismissed,
                onStatDeltaFinished = viewModel::onStatDeltaFinished,
                onFeedbackHandled = viewModel::onFeedbackHandled,
                onDismissFamilyDatingTip = viewModel::onDismissFamilyDatingTip,
                onDismissFamilyDetailTip = viewModel::onDismissFamilyDetailTip,
                onThrowParty = viewModel::onThrowParty
            )

            NotificationPermissionEffect(
                requestPermission = uiState.requestNotificationPermission,
                onHandled = viewModel::onNotificationPermissionResult
            )
        }

        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val settingsState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.refreshSystemNotificationState()
            }

            NotificationPermissionEffect(
                requestPermission = settingsState.requestNotificationPermission,
                onHandled = viewModel::onNotificationPermissionResult
            )

            LaunchedEffect(settingsState.resetComplete) {
                if (settingsState.resetComplete) {
                    navController.navigate(Routes.SLOT_PICKER) {
                        popUpTo(Routes.SLOT_PICKER) { inclusive = true }
                    }
                    viewModel.onResetCompleteHandled()
                }
            }

            SettingsScreen(
                uiState = settingsState,
                onBack = { navController.popBackStack() },
                onSoundChanged = viewModel::onSoundChanged,
                onHapticsChanged = viewModel::onHapticsChanged,
                onNotificationsChanged = viewModel::onNotificationsChanged,
                onLanguageSelected = viewModel::onLanguageSelected,
                onResetAllDataRequested = viewModel::onResetAllDataRequested,
                onConfirmResetAllData = viewModel::onConfirmResetAllData,
                onDismissResetConfirm = viewModel::onDismissResetConfirm,
                onFlagFallbackChanged = viewModel::onFlagFallbackChanged,
                preferIsoFlagFallback = settingsState.preferIsoFlagFallback
            )
        }

        composable(
            route = Routes.CHARACTER_STATS,
            arguments = listOf(slotIdArgument)
        ) {
            val slotId = it.arguments?.getInt("slotId") ?: 0
            val parentEntry = remember(navController) {
                navController.getBackStackEntry(Routes.life(slotId))
            }
            val viewModel: LifeViewModel = hiltViewModel(parentEntry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val character = uiState.character

            if (character != null && character.alive) {
                CharacterStatsScreen(
                    character = character,
                    netWorth = uiState.netWorth,
                    onBack = { navController.popBackStack() },
                    onViewAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                    onViewFamilyHeritage = { navController.navigate(Routes.ancestry(slotId)) }
                )
            }
        }

        composable(
            route = Routes.ANCESTRY,
            arguments = listOf(slotIdArgument)
        ) {
            val slotId = it.arguments?.getInt("slotId") ?: 0
            val parentEntry = remember(navController) {
                navController.getBackStackEntry(Routes.life(slotId))
            }
            val viewModel: LifeViewModel = hiltViewModel(parentEntry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val character = uiState.character

            if (character != null && character.alive) {
                AncestryScreen(
                    character = character,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.ACHIEVEMENTS) {
            val viewModel: AchievementsViewModel = hiltViewModel()
            val achievementsState by viewModel.uiState.collectAsStateWithLifecycle()

            AchievementsScreen(
                uiState = achievementsState,
                onBack = { navController.popBackStack() },
                formatUnlockDate = viewModel::formatUnlockDate
            )
        }

        composable(
            route = Routes.LIFE_SUMMARY,
            arguments = listOf(slotIdArgument)
        ) {
            val viewModel: LifeSummaryViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                activity?.let { adManager.preloadRewarded(it.applicationContext) }
            }

            LaunchedEffect(uiState.navigateToSlotPicker) {
                if (uiState.navigateToSlotPicker) {
                    navController.navigate(Routes.SLOT_PICKER) {
                        popUpTo(Routes.SLOT_PICKER) { inclusive = true }
                    }
                    viewModel.onNavigationHandled()
                }
            }

            LaunchedEffect(uiState.navigateToLife) {
                if (uiState.navigateToLife) {
                    val slotId = uiState.slotId
                    navController.navigate(Routes.life(slotId)) {
                        popUpTo(Routes.SLOT_PICKER) { inclusive = false }
                    }
                    viewModel.onLifeNavigationHandled()
                }
            }

            LaunchedEffect(uiState.showRewardedAd, activity) {
                if (uiState.showRewardedAd && activity != null) {
                    adManager.showRewardedIfReady(
                        activity = activity,
                        onReward = {
                            viewModel.onSecondWindRewardEarned()
                            viewModel.onRewardedAdHandled()
                        },
                        onDismissedNoReward = {
                            viewModel.onSecondWindDismissedNoReward()
                            viewModel.onRewardedAdHandled()
                        }
                    )
                }
            }

            LifeSummaryScreen(
                uiState = uiState,
                onStartNewLife = viewModel::onStartNewLife,
                onContinueLegacy = viewModel::onContinueLegacyClicked,
                onHeirSelected = viewModel::onHeirSelected,
                onConfirmLegacyContinuation = viewModel::onConfirmLegacyContinuation,
                onDismissHeirSelection = viewModel::onDismissHeirSelection,
                onDismissLegacyConfirmation = viewModel::onDismissLegacyConfirmation,
                onWatchSecondWind = viewModel::onWatchSecondWind,
                onDismissAchievementsTip = viewModel::onDismissAchievementsTip,
                onShareMyLife = viewModel::onShareMyLifeClicked,
                onDismissSharePreview = viewModel::onDismissSharePreview,
                onShareCapturingStarted = viewModel::onShareCapturingStarted,
                onShareCompleted = viewModel::onShareCompleted,
                onShareFailed = viewModel::onShareFailed,
                onDismissShareError = viewModel::onDismissShareError
            )
        }
    }
}
