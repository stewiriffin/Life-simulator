// app/src/main/java/com/maisha/game/ui/main/LifeViewModel.kt (modified — celebration + stat delta triggers)
package com.maisha.game.ui.main

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maisha.game.R
import com.maisha.game.ads.AdFrequencyController
import com.maisha.game.data.AchievementRepository
import com.maisha.game.data.events.EventRepository
import com.maisha.game.data.local.CharacterRepository
import com.maisha.game.data.local.MAX_SLOTS
import com.maisha.game.data.local.OnboardingTips
import com.maisha.game.data.local.SavedGameLoadResult
import com.maisha.game.data.local.SettingsRepository
import com.maisha.game.data.model.Achievement
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.CrimeType
import com.maisha.game.data.model.Expression
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.PetCatalog
import com.maisha.game.data.model.HustleType
import com.maisha.game.data.model.PetSpecies
import com.maisha.game.data.model.Job
import com.maisha.game.data.model.LawyerTier
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationshipDecayNotice
import com.maisha.game.data.model.RelationshipTier
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import com.maisha.game.domain.AdoptPetResult
import com.maisha.game.domain.AgeUpResult
import com.maisha.game.domain.AchievementEngine
import com.maisha.game.domain.CareerEngine
import com.maisha.game.domain.CareerResult
import com.maisha.game.domain.CrimeResult
import com.maisha.game.domain.DoctorResult
import com.maisha.game.domain.EventLogCap
import com.maisha.game.domain.FinanceEngine
import com.maisha.game.domain.GameEngine
import com.maisha.game.domain.GiftTier
import com.maisha.game.domain.InteractionType
import com.maisha.game.domain.ProposalResult
import com.maisha.game.domain.PurchaseResult
import com.maisha.game.domain.RepairResult
import com.maisha.game.domain.RetirementResult
import com.maisha.game.domain.SideHustleFailure
import com.maisha.game.domain.SideHustleResult
import com.maisha.game.domain.BusinessFailure
import com.maisha.game.domain.BusinessResult
import com.maisha.game.domain.SkillFailure
import com.maisha.game.domain.SkillResult
import com.maisha.game.data.model.BusinessIndustry
import com.maisha.game.domain.SocialMediaFailure
import com.maisha.game.domain.SocialMediaResult
import com.maisha.game.data.model.SkillType
import com.maisha.game.domain.TrialResult
import com.maisha.game.domain.hasSpouse
import com.maisha.game.feedback.FeedbackCue
import com.maisha.game.ui.avatar.EventOutcome
import com.maisha.game.ui.avatar.ExpressionResolver
import com.maisha.game.ui.celebration.CelebrationType
import com.maisha.game.ui.components.StatDeltaEvent
import com.maisha.game.ui.components.StatType
import com.maisha.game.feedback.HapticType
import com.maisha.game.feedback.SoundEffect
import com.maisha.game.notifications.NotificationScheduler
import com.maisha.game.util.formatMoney
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LifeUiState(
    val character: Character? = null,
    val pendingEvents: List<LifeEvent> = emptyList(),
    val currentEvent: LifeEvent? = null,
    val isLoading: Boolean = true,
    val isAgingUp: Boolean = false,
    val selectedFamilyMember: Person? = null,
    val familyInteractionMessage: String? = null,
    val careerMessage: String? = null,
    val assetsMessage: String? = null,
    val eligibleJobs: List<Job> = emptyList(),
    val netWorth: Int = 0,
    val datingProspects: List<Person> = emptyList(),
    val showDatingProspects: Boolean = false,
    val relationshipMessage: String? = null,
    val navigateToLifeSummary: Boolean = false,
    val navigateToSlotPicker: Boolean = false,
    val showInterstitialAd: Boolean = false,
    val deferredInterstitialAd: Boolean = false,
    val actionMessage: String? = null,
    val pendingAchievementQueue: List<Achievement> = emptyList(),
    val currentAchievementDialog: Achievement? = null,
    val pendingCelebrationQueue: List<CelebrationType> = emptyList(),
    val currentCelebration: CelebrationType? = null,
    val pendingStatDeltas: List<StatDeltaEvent> = emptyList(),
    val pendingFeedbackCues: List<FeedbackCue> = emptyList(),
    val seenTipIds: Set<String> = emptySet(),
    val tipsLoaded: Boolean = false,
    val requestNotificationPermission: Boolean = false,
    val headerExpression: Expression = Expression.NEUTRAL
)

@HiltViewModel
class LifeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val characterRepository: CharacterRepository,
    private val settingsRepository: SettingsRepository,
    private val achievementRepository: AchievementRepository,
    private val gameEngine: GameEngine,
    private val achievementEngine: AchievementEngine,
    private val careerEngine: CareerEngine,
    private val financeEngine: FinanceEngine,
    private val adFrequencyController: AdFrequencyController,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val slotId: Int = savedStateHandle.get<Int>("slotId")
        ?.takeIf { it in 0 until MAX_SLOTS }
        ?: 0

    private var triggeredEventIds: Set<String> = emptySet()

    private val _uiState = MutableStateFlow(LifeUiState())
    val uiState: StateFlow<LifeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val seenTips = settingsRepository.getSeenTipsSnapshot()
            _uiState.update { it.copy(seenTipIds = seenTips, tipsLoaded = true) }
        }
        viewModelScope.launch {
            when (val result = characterRepository.loadGame(slotId)) {
                is SavedGameLoadResult.Success -> {
                    val saved = result.game
                    triggeredEventIds = saved.triggeredEventIds
                    val character = saved.character
                    var introResult: AgeUpResult? = null
                    if (character.age == 0 && character.eventLog.isEmpty()) {
                        introResult = gameEngine.introEventsForNewborn(triggeredEventIds)
                    }
                    _uiState.update {
                        it.copy(
                            character = character,
                            isLoading = false,
                            eligibleJobs = careerEngine.getEligibleJobs(character),
                            netWorth = financeEngine.calculateNetWorth(character),
                            navigateToLifeSummary = !character.alive,
                            headerExpression = ExpressionResolver.resolveExpression(character, null)
                        )
                    }
                    if (character.alive) {
                        introResult?.let { applyAgeUpResult(character, it, persistAge = false) }
                    }
                }
                SavedGameLoadResult.Corrupted, SavedGameLoadResult.NotFound -> {
                    _uiState.update { it.copy(isLoading = false, navigateToSlotPicker = true) }
                }
            }
        }
    }

    fun onAgeUp() {
        val character = _uiState.value.character ?: return
        if (!character.alive) return
        if (character.criminalRecord.awaitingTrial) return
        if (_uiState.value.isAgingUp || _uiState.value.currentEvent != null) return

        enqueueFeedback(FeedbackCue(sound = SoundEffect.AGE_UP, haptic = HapticType.LIGHT_TAP))

        viewModelScope.launch {
            _uiState.update { it.copy(isAgingUp = true) }
            val moneyBefore = character.stats.money
            val hadJob = character.career.currentJob != null
            val educationBefore = character.education.stage
            val statsBefore = character.stats
            val netWorthBefore = financeEngine.calculateNetWorth(character)
            val ageBefore = character.age
            val progress = achievementRepository.getProgressSnapshot()
            val outcome = gameEngine.ageUp(character, triggeredEventIds, progress, slotId)
            if (outcome.newlyUnlockedAchievements.isNotEmpty()) {
                achievementRepository.unlockAchievements(outcome.newlyUnlockedAchievements)
                enqueueAchievementDialogs(outcome.newlyUnlockedAchievements)
            }
            applyAgeUpResult(outcome.character, outcome.result, persistAge = true)
            val statDeltas = buildStatDeltas(statsBefore, outcome.character.stats) +
                buildNetWorthDelta(
                    before = netWorthBefore,
                    after = financeEngine.calculateNetWorth(outcome.character),
                    countryCode = outcome.character.countryCode
                )
            if (educationBefore != SchoolStage.GRADUATED &&
                outcome.character.education.stage == SchoolStage.GRADUATED
            ) {
                enqueueCelebration(CelebrationType.GRADUATION)
            }
            when (outcome.character.age) {
                18 -> if (ageBefore < 18) enqueueCelebration(CelebrationType.AGE_MILESTONE_18)
                50 -> if (ageBefore < 50) enqueueCelebration(CelebrationType.AGE_MILESTONE_50)
                100 -> if (ageBefore < 100) enqueueCelebration(CelebrationType.AGE_MILESTONE_100)
            }
            if (statDeltas.isNotEmpty()) {
                appendStatDeltas(statDeltas)
            }
            val ageUpOutcome = outcomeFromAgeUpResult(outcome.result)
            flashExpression(
                character = outcome.character,
                flash = ExpressionResolver.resolveExpression(outcome.character, ageUpOutcome)
            )
            outcome.relationshipDecayNotices.firstOrNull()?.let { notice ->
                _uiState.update {
                    it.copy(relationshipMessage = formatDecayNotice(notice))
                }
            }
            outcome.newFriendName?.let { friendName ->
                _uiState.update {
                    it.copy(relationshipMessage = context.getString(R.string.msg_new_friend, friendName))
                }
            }
            enqueueAgeUpOutcomeFeedback(
                beforeMoney = moneyBefore,
                hadJob = hadJob,
                character = outcome.character,
                result = outcome.result
            )
            val earnedInterstitialSlot = outcome.character.alive &&
                adFrequencyController.recordAgeUpAndShouldShowInterstitial()
            _uiState.update { state ->
                val blockAd = hasCelebratoryOverlay(state)
                state.copy(
                    isAgingUp = false,
                    showInterstitialAd = earnedInterstitialSlot && !blockAd,
                    deferredInterstitialAd = earnedInterstitialSlot && blockAd
                )
            }
            maybePromptNotificationPermissionAfterFirstAgeUp()
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(requestNotificationPermission = false) }
        viewModelScope.launch {
            if (granted) {
                notificationScheduler.scheduleDailyReminder()
            } else {
                settingsRepository.setNotificationsEnabled(false)
            }
        }
    }

    private suspend fun maybePromptNotificationPermissionAfterFirstAgeUp() {
        if (!settingsRepository.hasRecordedFirstAgeUp()) {
            settingsRepository.setFirstAgeUpRecorded()
            if (settingsRepository.isNotificationsEnabledNow()) {
                _uiState.update { it.copy(requestNotificationPermission = true) }
            }
        }
    }

    fun onFeedbackHandled() {
        _uiState.update { it.copy(pendingFeedbackCues = emptyList()) }
    }

    fun onDismissFamilyDatingTip() {
        dismissTip(OnboardingTips.FAMILY_DATING)
    }

    private fun dismissTip(tipId: String) {
        viewModelScope.launch {
            settingsRepository.markTipSeen(tipId)
            val seenTips = settingsRepository.getSeenTipsSnapshot()
            _uiState.update { it.copy(seenTipIds = seenTips) }
        }
    }

    fun onInterstitialAdHandled() {
        _uiState.update { it.copy(showInterstitialAd = false) }
    }

    fun onSlotPickerNavigationHandled() {
        _uiState.update { it.copy(navigateToSlotPicker = false) }
    }

    fun onLifeSummaryNavigationHandled() {
        _uiState.update { it.copy(navigateToLifeSummary = false) }
    }

    fun onChoiceSelected(choice: EventChoice) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return
        val currentEvent = _uiState.value.currentEvent ?: return

        viewModelScope.launch {
            val statsBefore = character.stats
            val updatedCharacter = gameEngine.applyChoice(character, choice, currentEvent)
            if (EventRepository.ONE_TIME_TAG in currentEvent.tags) {
                triggeredEventIds = triggeredEventIds + currentEvent.id
            }

            val nextPending = _uiState.value.pendingEvents
            val nextEvent = nextPending.firstOrNull()
            val remaining = if (nextEvent != null) nextPending.drop(1) else emptyList()

            val happinessDelta = choice.statEffects["happiness"] ?: 0
            val moneyDelta = choice.statEffects["money"] ?: 0
            val choiceOutcome = ExpressionResolver.outcomeFromChoiceEffects(happinessDelta, moneyDelta)

            persist(updatedCharacter)
            processMidLifeAchievements(updatedCharacter)
            val statDeltas = buildStatDeltas(statsBefore, updatedCharacter.stats)
            _uiState.update {
                it.copy(
                    character = updatedCharacter,
                    currentEvent = nextEvent,
                    pendingEvents = remaining,
                    eligibleJobs = careerEngine.getEligibleJobs(updatedCharacter),
                    netWorth = financeEngine.calculateNetWorth(updatedCharacter),
                    headerExpression = ExpressionResolver.resolveExpression(updatedCharacter, choiceOutcome),
                    pendingStatDeltas = it.pendingStatDeltas + statDeltas
                )
            }
            flashExpression(updatedCharacter, ExpressionResolver.resolveExpression(updatedCharacter, choiceOutcome))
        }
    }

    fun onApplyForJob(jobId: String) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return

        viewModelScope.launch {
            val (updatedCharacter, result) = gameEngine.applyForJob(character, jobId)
            val message = when (result) {
                is CareerResult.Hired -> context.getString(R.string.msg_hired, result.job.title)
                is CareerResult.Rejected -> context.getString(R.string.msg_job_rejected)
            }
            persist(updatedCharacter)
            processMidLifeAchievements(updatedCharacter)
            _uiState.update {
                it.copy(
                    character = updatedCharacter,
                    careerMessage = message,
                    eligibleJobs = careerEngine.getEligibleJobs(updatedCharacter),
                    netWorth = financeEngine.calculateNetWorth(updatedCharacter)
                )
            }
        }
    }

    fun onQuitJob() {
        val character = _uiState.value.character ?: return
        if (!character.alive) return

        viewModelScope.launch {
            val updatedCharacter = gameEngine.quitJob(character)
            persist(updatedCharacter)
            _uiState.update {
                it.copy(
                    character = updatedCharacter,
                    careerMessage = context.getString(R.string.msg_quit_job),
                    eligibleJobs = careerEngine.getEligibleJobs(updatedCharacter),
                    netWorth = financeEngine.calculateNetWorth(updatedCharacter)
                )
            }
        }
    }

    fun onRetire() {
        val character = _uiState.value.character ?: return
        if (!character.alive) return

        viewModelScope.launch {
            when (val result = gameEngine.retire(character)) {
                is RetirementResult.Success -> {
                    val updatedCharacter = result.character
                    persist(updatedCharacter)
                    enqueueFeedback(
                        FeedbackCue(sound = SoundEffect.EVENT_POSITIVE, haptic = HapticType.SUCCESS)
                    )
                    _uiState.update {
                        it.copy(
                            character = updatedCharacter,
                            careerMessage = context.getString(R.string.msg_retired),
                            eligibleJobs = careerEngine.getEligibleJobs(updatedCharacter),
                            netWorth = financeEngine.calculateNetWorth(updatedCharacter)
                        )
                    }
                }
                RetirementResult.Ineligible -> Unit
            }
        }
    }

    /** Mid-point pension quote for the retire confirmation dialog. */
    fun retirementPensionEstimate(): Int {
        val character = _uiState.value.character ?: return 0
        return careerEngine.estimateRetirementPension(character)
    }

    fun onDropOut() {
        val character = _uiState.value.character ?: return
        if (!character.alive) return
        val stage = character.education.stage
        if (stage != SchoolStage.SECONDARY && stage != SchoolStage.UNIVERSITY) return

        viewModelScope.launch {
            val updatedCharacter = gameEngine.dropOut(character)
            persist(updatedCharacter)
            processMidLifeAchievements(updatedCharacter)
            enqueueFeedback(
                FeedbackCue(sound = SoundEffect.EVENT_NEGATIVE, haptic = HapticType.WARNING)
            )
            _uiState.update {
                it.copy(
                    character = updatedCharacter,
                    careerMessage = context.getString(R.string.msg_dropped_out),
                    eligibleJobs = careerEngine.getEligibleJobs(updatedCharacter),
                    netWorth = financeEngine.calculateNetWorth(updatedCharacter)
                )
            }
        }
    }

    fun businessInvestmentTiers(): List<Int> {
        val character = _uiState.value.character ?: return emptyList()
        return gameEngine.businessInvestmentTiers(character)
    }

    fun onStartBusiness(name: String, industry: BusinessIndustry, investment: Int) {
        val character = _uiState.value.character ?: return
        viewModelScope.launch {
            val statsBefore = character.stats
            when (val result = gameEngine.startBusiness(character, name, industry, investment)) {
                is BusinessResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    appendStatDeltas(buildStatDeltas(statsBefore, result.character.stats))
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            careerMessage = context.getString(
                                R.string.msg_business_started,
                                result.business?.name ?: name
                            ),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is BusinessResult.Failed -> {
                    val message = when (result.reason) {
                        BusinessFailure.INSUFFICIENT_FUNDS ->
                            context.getString(R.string.msg_business_insufficient)
                        BusinessFailure.MAX_BUSINESSES ->
                            context.getString(R.string.msg_business_max)
                        else -> context.getString(R.string.msg_business_ineligible)
                    }
                    _uiState.update { it.copy(careerMessage = message) }
                }
            }
        }
    }

    fun onSellBusiness(businessId: String) {
        val character = _uiState.value.character ?: return
        viewModelScope.launch {
            val statsBefore = character.stats
            when (val result = gameEngine.sellBusiness(character, businessId)) {
                is BusinessResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    appendStatDeltas(buildStatDeltas(statsBefore, result.character.stats))
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            careerMessage = context.getString(
                                R.string.msg_business_sold,
                                result.business?.name.orEmpty(),
                                formatMoney(
                                    result.business?.valuation ?: 0,
                                    result.character.countryCode
                                )
                            ),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is BusinessResult.Failed -> {
                    _uiState.update {
                        it.copy(careerMessage = context.getString(R.string.msg_business_ineligible))
                    }
                }
            }
        }
    }

    fun onPurchaseAsset(catalogId: String) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return

        viewModelScope.launch {
            when (val result = gameEngine.purchaseAsset(character, catalogId)) {
                is PurchaseResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    enqueueFeedback(FeedbackCue(sound = SoundEffect.PURCHASE, haptic = HapticType.LIGHT_TAP))
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            assetsMessage = context.getString(R.string.msg_purchase_success),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is PurchaseResult.InsufficientFunds -> {
                    _uiState.update {
                        it.copy(assetsMessage = context.getString(R.string.msg_purchase_insufficient))
                    }
                }
            }
        }
    }

    fun onSellAsset(assetId: String) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return

        viewModelScope.launch {
            val updatedCharacter = gameEngine.sellAsset(character, assetId)
            persist(updatedCharacter)
            _uiState.update {
                it.copy(
                    character = updatedCharacter,
                    assetsMessage = context.getString(R.string.msg_asset_sold),
                    netWorth = financeEngine.calculateNetWorth(updatedCharacter)
                )
            }
        }
    }

    fun willBeneficiaries(): List<com.maisha.game.data.model.Person> {
        val character = _uiState.value.character ?: return emptyList()
        return gameEngine.willBeneficiaries(character)
    }

    fun onInvestFunds(amount: Int) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return
        viewModelScope.launch {
            when (val result = gameEngine.investFunds(character, amount)) {
                is FinanceEngine.InvestmentResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            assetsMessage = context.getString(R.string.msg_invest_success),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is FinanceEngine.InvestmentResult.InsufficientFunds -> {
                    _uiState.update {
                        it.copy(assetsMessage = context.getString(R.string.msg_invest_cannot_afford))
                    }
                }
                is FinanceEngine.InvestmentResult.InvalidAmount -> {
                    _uiState.update {
                        it.copy(assetsMessage = context.getString(R.string.msg_invest_cannot_afford))
                    }
                }
            }
        }
    }

    fun onWithdrawFunds(amount: Int) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return
        viewModelScope.launch {
            when (val result = gameEngine.withdrawFunds(character, amount)) {
                is FinanceEngine.InvestmentResult.Success -> {
                    persist(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            assetsMessage = context.getString(R.string.msg_withdraw_success),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is FinanceEngine.InvestmentResult.InsufficientFunds -> {
                    _uiState.update {
                        it.copy(assetsMessage = context.getString(R.string.msg_withdraw_insufficient))
                    }
                }
                is FinanceEngine.InvestmentResult.InvalidAmount -> {
                    _uiState.update {
                        it.copy(assetsMessage = context.getString(R.string.msg_withdraw_insufficient))
                    }
                }
            }
        }
    }

    fun onSaveWill(will: Map<String, Int>?) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return
        viewModelScope.launch {
            val updated = runCatching { gameEngine.updateWill(character, will) }.getOrElse {
                _uiState.update {
                    it.copy(assetsMessage = context.getString(R.string.msg_will_invalid))
                }
                return@launch
            }
            persist(updated)
            _uiState.update {
                it.copy(
                    character = updated,
                    assetsMessage = context.getString(
                        if (will == null) R.string.msg_will_cleared else R.string.msg_will_saved
                    )
                )
            }
        }
    }

    fun onRentOutProperty(assetId: String) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return
        viewModelScope.launch {
            when (val result = gameEngine.rentOutProperty(character, assetId)) {
                is FinanceEngine.RentalResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            assetsMessage = context.getString(R.string.msg_property_rented),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                else -> {
                    _uiState.update {
                        it.copy(assetsMessage = context.getString(R.string.msg_rent_out_failed))
                    }
                }
            }
        }
    }

    fun onEvictTenant(assetId: String) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return
        viewModelScope.launch {
            when (val result = gameEngine.evictTenant(character, assetId)) {
                is FinanceEngine.RentalResult.Success -> {
                    persist(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            assetsMessage = context.getString(R.string.msg_tenant_evicted),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is FinanceEngine.RentalResult.InsufficientFunds -> {
                    _uiState.update {
                        it.copy(assetsMessage = context.getString(R.string.msg_evict_cannot_afford))
                    }
                }
                else -> {
                    _uiState.update {
                        it.copy(assetsMessage = context.getString(R.string.msg_evict_failed))
                    }
                }
            }
        }
    }

    fun onRepairAsset(assetId: String) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return

        viewModelScope.launch {
            when (val result = gameEngine.repairAsset(character, assetId)) {
                is RepairResult.Success -> {
                    persist(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            assetsMessage = context.getString(R.string.msg_asset_repaired),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is RepairResult.InsufficientFunds -> {
                    _uiState.update {
                        it.copy(assetsMessage = context.getString(R.string.msg_repair_insufficient))
                    }
                }
                is RepairResult.AssetNotFound -> Unit
            }
        }
    }

    fun onCareerMessageDismissed() {
        _uiState.update { it.copy(careerMessage = null) }
    }

    fun onAssetsMessageDismissed() {
        _uiState.update { it.copy(assetsMessage = null) }
    }

    fun onFamilyMemberSelected(person: Person) {
        _uiState.update { it.copy(selectedFamilyMember = person, familyInteractionMessage = null) }
    }

    fun onFamilyMemberDismissed() {
        _uiState.update { it.copy(selectedFamilyMember = null) }
    }

    fun onFamilyInteraction(personId: String, type: InteractionType, giftTier: GiftTier? = null) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return

        viewModelScope.launch {
            val statsBefore = character.stats
            val result = gameEngine.interactWithFamilyMember(character, personId, type, giftTier)
            persist(result.character)
            val statDeltas = buildStatDeltas(statsBefore, result.character.stats)
            _uiState.update {
                it.copy(
                    character = result.character,
                    familyInteractionMessage = result.message,
                    selectedFamilyMember = result.character.family.find { person -> person.id == personId },
                    netWorth = financeEngine.calculateNetWorth(result.character),
                    headerExpression = ExpressionResolver.resolveExpression(
                        result.character,
                        ExpressionResolver.outcomeFromInteraction(type)
                    ),
                    pendingStatDeltas = it.pendingStatDeltas + statDeltas
                )
            }
            flashExpression(
                result.character,
                ExpressionResolver.resolveExpression(
                    result.character,
                    ExpressionResolver.outcomeFromInteraction(type)
                )
            )
        }
    }

    fun onFamilyInteractionMessageDismissed() {
        _uiState.update { it.copy(familyInteractionMessage = null) }
    }

    fun onFindDate() {
        val character = _uiState.value.character ?: return
        if (!character.alive) return
        val prospects = gameEngine.findDatingProspects(character)
        _uiState.update {
            it.copy(
                datingProspects = prospects,
                showDatingProspects = prospects.isNotEmpty(),
                relationshipMessage = if (prospects.isEmpty()) {
                    if (character.age < 18) context.getString(R.string.msg_date_too_young)
                    else if (character.hasSpouse()) context.getString(R.string.msg_already_in_relationship)
                    else context.getString(R.string.msg_no_prospects)
                } else null
            )
        }
    }

    fun onDismissDatingProspects() {
        _uiState.update { it.copy(showDatingProspects = false, datingProspects = emptyList()) }
    }

    fun onStartDating(prospect: Person) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return

        viewModelScope.launch {
            val updatedCharacter = gameEngine.startDating(character, prospect)
            persist(updatedCharacter)
            _uiState.update {
                it.copy(
                    character = updatedCharacter,
                    showDatingProspects = false,
                    datingProspects = emptyList(),
                    relationshipMessage = context.getString(R.string.msg_started_dating, prospect.name),
                    netWorth = financeEngine.calculateNetWorth(updatedCharacter)
                )
            }
        }
    }

    fun onPropose(personId: String) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return

        viewModelScope.launch {
            val (updatedCharacter, result) = gameEngine.proposeMarriage(character, personId)
            val message = when (result) {
                is ProposalResult.Accepted -> context.getString(R.string.msg_proposal_accepted)
                ProposalResult.Rejected -> context.getString(R.string.msg_proposal_rejected)
            }
            persist(updatedCharacter)
            processMidLifeAchievements(updatedCharacter)
            if (result is ProposalResult.Accepted) {
                enqueueCelebration(CelebrationType.MARRIAGE)
            }
            _uiState.update {
                it.copy(
                    character = updatedCharacter,
                    relationshipMessage = message,
                    selectedFamilyMember = updatedCharacter.family.find { person -> person.id == personId },
                    netWorth = financeEngine.calculateNetWorth(updatedCharacter)
                )
            }
        }
    }

    fun onBreakUp(personId: String) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return

        viewModelScope.launch {
            val updatedCharacter = gameEngine.breakUpOrDivorce(character, personId)
            persist(updatedCharacter)
            _uiState.update {
                it.copy(
                    character = updatedCharacter,
                    selectedFamilyMember = null,
                    relationshipMessage = context.getString(R.string.msg_breakup),
                    netWorth = financeEngine.calculateNetWorth(updatedCharacter)
                )
            }
        }
    }

    fun onHaveChild() {
        val character = _uiState.value.character ?: return
        if (!character.alive) return

        viewModelScope.launch {
            val updatedCharacter = gameEngine.haveChild(character)
            val hadChild = updatedCharacter.family.size > character.family.size
            persist(updatedCharacter)
            if (hadChild) {
                processMidLifeAchievements(updatedCharacter)
                enqueueCelebration(CelebrationType.CHILD_BORN)
            }
            _uiState.update {
                it.copy(
                    character = updatedCharacter,
                    relationshipMessage = if (hadChild) {
                        context.getString(R.string.msg_child_welcome)
                    } else {
                        context.getString(R.string.msg_child_need_marriage)
                    },
                    netWorth = financeEngine.calculateNetWorth(updatedCharacter)
                )
            }
        }
    }

    fun onRelationshipMessageDismissed() {
        _uiState.update { it.copy(relationshipMessage = null) }
    }

    fun onAttemptCrime(crimeType: CrimeType) {
        val character = _uiState.value.character ?: return
        if (!character.alive || character.criminalRecord.currentlyIncarcerated) return

        viewModelScope.launch {
            when (val result = gameEngine.attemptCrime(character, crimeType)) {
                is CrimeResult.Success -> {
                    val updated = result.character.copy(
                        eventLog = EventLogCap.prepend(
                            result.character.eventLog,
                            context.getString(
                                R.string.msg_crime_got_away_log,
                                crimeTypeLabel(crimeType),
                                formatMoney(result.moneyGained, result.character.countryCode)
                            )
                        )
                    )
                    persist(updated)
                    processMidLifeAchievements(updated)
                    _uiState.update {
                        it.copy(
                            character = updated,
                            actionMessage = context.getString(
                                R.string.msg_crime_success,
                                formatMoney(result.moneyGained, result.character.countryCode)
                            ),
                            netWorth = financeEngine.calculateNetWorth(updated)
                        )
                    }
                }
                is CrimeResult.Caught -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(R.string.msg_crime_arrested),
                            eligibleJobs = careerEngine.getEligibleJobs(result.character),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
            }
        }
    }

    fun onGoToTrial(lawyerTier: LawyerTier) {
        val character = _uiState.value.character ?: return
        if (!character.criminalRecord.awaitingTrial) return

        viewModelScope.launch {
            when (val result = gameEngine.goToTrial(character, lawyerTier)) {
                is TrialResult.Acquitted -> {
                    persist(result.character)
                    enqueueFeedback(
                        FeedbackCue(sound = SoundEffect.EVENT_POSITIVE, haptic = HapticType.SUCCESS)
                    )
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(R.string.msg_trial_acquitted),
                            eligibleJobs = careerEngine.getEligibleJobs(result.character),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is TrialResult.Sentenced -> {
                    persist(result.character)
                    val message = if (result.sentenceYears > 0) {
                        context.getString(R.string.msg_crime_caught, result.sentenceYears)
                    } else {
                        context.getString(R.string.msg_trial_no_prison_time)
                    }
                    if (result.sentenceYears > 0) {
                        enqueueFeedback(
                            FeedbackCue(sound = SoundEffect.EVENT_NEGATIVE, haptic = HapticType.WARNING)
                        )
                    }
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = message,
                            eligibleJobs = careerEngine.getEligibleJobs(result.character),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                TrialResult.Ineligible -> Unit
            }
        }
    }

    fun lawyerFee(tier: LawyerTier): Int {
        val character = _uiState.value.character ?: return 0
        return gameEngine.lawyerFee(character, tier)
    }

    fun canAffordLawyer(tier: LawyerTier): Boolean {
        val character = _uiState.value.character ?: return false
        return gameEngine.canAffordLawyer(character, tier)
    }

    fun onVisitDoctor(conditionId: String, careType: CareType) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return

        viewModelScope.launch {
            when (val result = gameEngine.visitDoctor(character, conditionId, careType.usePrivateCare())) {
                is DoctorResult.Treated -> {
                    persist(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(R.string.msg_treatment_success),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is DoctorResult.Failed -> {
                    persist(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(R.string.msg_treatment_failed),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
            }
        }
    }

    fun onActionMessageDismissed() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    fun onSetLifestyleOption(option: com.maisha.game.data.model.LifestyleOption, enabled: Boolean) {
        val character = _uiState.value.character ?: return
        if (!character.alive || character.criminalRecord.currentlyIncarcerated) return

        viewModelScope.launch {
            val updated = gameEngine.setLifestyleOption(character, option, enabled)
            persist(updated)
            _uiState.update {
                it.copy(
                    character = updated,
                    actionMessage = context.getString(
                        if (enabled) R.string.msg_lifestyle_enabled else R.string.msg_lifestyle_disabled
                    ),
                    netWorth = financeEngine.calculateNetWorth(updated)
                )
            }
        }
    }

    fun onExecuteSideHustle(hustleType: HustleType) {
        val character = _uiState.value.character ?: return
        if (!character.alive || character.criminalRecord.currentlyIncarcerated) return

        viewModelScope.launch {
            val statsBefore = character.stats
            when (val result = gameEngine.executeSideHustle(character, hustleType)) {
                is SideHustleResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    val statDeltas = buildStatDeltas(statsBefore, result.character.stats)
                    appendStatDeltas(statDeltas)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(
                                R.string.msg_side_hustle_success,
                                formatMoney(result.payout, result.character.countryCode)
                            ),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is SideHustleResult.Failed -> {
                    val message = when (result.reason) {
                        SideHustleFailure.ALREADY_DONE_THIS_YEAR ->
                            context.getString(R.string.msg_side_hustle_already_done)
                        SideHustleFailure.PREREQUISITES_NOT_MET ->
                            context.getString(R.string.msg_side_hustle_prerequisites)
                        SideHustleFailure.INELIGIBLE ->
                            context.getString(R.string.msg_side_hustle_prerequisites)
                    }
                    _uiState.update { it.copy(actionMessage = message) }
                }
            }
        }
    }

    fun onAdoptPet(species: PetSpecies) {
        val character = _uiState.value.character ?: return
        if (!character.alive || character.criminalRecord.currentlyIncarcerated) return

        viewModelScope.launch {
            val defaultName = PetCatalog.findBySpecies(species)?.defaultName ?: "Pet"
            val statsBefore = character.stats
            when (val result = gameEngine.adoptPet(character, species, defaultName)) {
                is AdoptPetResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    appendStatDeltas(buildStatDeltas(statsBefore, result.character.stats))
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(
                                R.string.msg_adopt_pet_success,
                                defaultName
                            ),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                AdoptPetResult.InsufficientFunds -> {
                    _uiState.update {
                        it.copy(actionMessage = context.getString(R.string.msg_adopt_pet_insufficient))
                    }
                }
                AdoptPetResult.MaxPetsReached -> {
                    _uiState.update {
                        it.copy(actionMessage = context.getString(R.string.msg_adopt_pet_max))
                    }
                }
                AdoptPetResult.Ineligible -> Unit
            }
        }
    }

    fun onCreateSocialAccount() {
        val character = _uiState.value.character ?: return
        viewModelScope.launch {
            when (val result = gameEngine.createSocialMediaAccount(character)) {
                is SocialMediaResult.Success -> {
                    persist(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(R.string.msg_social_account_created),
                            eligibleJobs = careerEngine.getEligibleJobs(result.character)
                        )
                    }
                }
                is SocialMediaResult.Failed -> {
                    val message = when (result.reason) {
                        SocialMediaFailure.ALREADY_HAS_ACCOUNT ->
                            context.getString(R.string.msg_social_already_has_account)
                        else -> context.getString(R.string.msg_social_ineligible)
                    }
                    _uiState.update { it.copy(actionMessage = message) }
                }
            }
        }
    }

    fun onPostSocialContent() {
        val character = _uiState.value.character ?: return
        viewModelScope.launch {
            when (val result = gameEngine.postSocialMediaContent(character)) {
                is SocialMediaResult.Success -> {
                    persist(result.character)
                    if (result.followersGained != 0) {
                        val sign = if (result.followersGained > 0) "+" else ""
                        appendStatDeltas(
                            listOf(
                                StatDeltaEvent(
                                    type = StatType.FOLLOWERS,
                                    delta = result.followersGained,
                                    displayText = "$sign${result.followersGained}"
                                )
                            )
                        )
                    }
                    val message = if (result.wentViral) {
                        context.getString(R.string.msg_social_post_viral, result.followersGained)
                    } else {
                        context.getString(R.string.msg_social_post_success, result.followersGained)
                    }
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = message,
                            eligibleJobs = careerEngine.getEligibleJobs(result.character)
                        )
                    }
                }
                is SocialMediaResult.Failed -> {
                    _uiState.update {
                        it.copy(actionMessage = context.getString(R.string.msg_social_no_account))
                    }
                }
            }
        }
    }

    fun onMonetizeSocialAccount() {
        val character = _uiState.value.character ?: return
        viewModelScope.launch {
            val statsBefore = character.stats
            when (val result = gameEngine.monetizeSocialMediaAccount(character)) {
                is SocialMediaResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    appendStatDeltas(buildStatDeltas(statsBefore, result.character.stats))
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(
                                R.string.msg_social_monetize_success,
                                formatMoney(result.payout, result.character.countryCode)
                            ),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is SocialMediaResult.Failed -> {
                    val message = when (result.reason) {
                        SocialMediaFailure.BELOW_MONETIZATION_THRESHOLD ->
                            context.getString(R.string.msg_social_monetize_threshold)
                        SocialMediaFailure.ALREADY_MONETIZED_THIS_YEAR ->
                            context.getString(R.string.msg_social_already_monetized)
                        SocialMediaFailure.NO_ACCOUNT ->
                            context.getString(R.string.msg_social_no_account)
                        else -> context.getString(R.string.msg_social_ineligible)
                    }
                    _uiState.update { it.copy(actionMessage = message) }
                }
            }
        }
    }

    fun onPracticeSkill(skillType: SkillType) {
        val character = _uiState.value.character ?: return
        viewModelScope.launch {
            val statsBefore = character.stats
            when (val result = gameEngine.practiceSkill(character, skillType)) {
                is SkillResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    val deltas = buildStatDeltas(statsBefore, result.character.stats).toMutableList()
                    if (result.levelGained > 0) {
                        deltas += StatDeltaEvent(
                            type = StatType.SKILL,
                            delta = result.levelGained,
                            displayText = "+${result.levelGained}"
                        )
                    }
                    appendStatDeltas(deltas)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(
                                R.string.msg_skill_practice_success,
                                result.levelGained
                            ),
                            eligibleJobs = careerEngine.getEligibleJobs(result.character)
                        )
                    }
                }
                is SkillResult.Failed -> {
                    val message = when (result.reason) {
                        SkillFailure.ALREADY_MASTERED ->
                            context.getString(R.string.msg_skill_mastered)
                        else -> context.getString(R.string.msg_skill_ineligible)
                    }
                    _uiState.update { it.copy(actionMessage = message) }
                }
            }
        }
    }

    fun onTakeMasterclass(skillType: SkillType) {
        val character = _uiState.value.character ?: return
        viewModelScope.launch {
            val statsBefore = character.stats
            when (val result = gameEngine.takeMasterclass(character, skillType)) {
                is SkillResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    val deltas = buildStatDeltas(statsBefore, result.character.stats).toMutableList()
                    if (result.levelGained > 0) {
                        deltas += StatDeltaEvent(
                            type = StatType.SKILL,
                            delta = result.levelGained,
                            displayText = "+${result.levelGained}"
                        )
                    }
                    appendStatDeltas(deltas)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(
                                R.string.msg_skill_masterclass_success,
                                result.levelGained,
                                formatMoney(result.cost, result.character.countryCode)
                            ),
                            eligibleJobs = careerEngine.getEligibleJobs(result.character),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is SkillResult.Failed -> {
                    val message = when (result.reason) {
                        SkillFailure.INSUFFICIENT_FUNDS ->
                            context.getString(R.string.msg_skill_cannot_afford)
                        SkillFailure.ALREADY_MASTERED ->
                            context.getString(R.string.msg_skill_mastered)
                        else -> context.getString(R.string.msg_skill_ineligible)
                    }
                    _uiState.update { it.copy(actionMessage = message) }
                }
            }
        }
    }

    fun onLaunchCampaign(office: com.maisha.game.data.model.PoliticalOffice, investment: Int) {
        val character = _uiState.value.character ?: return
        viewModelScope.launch {
            when (val result = gameEngine.launchCampaign(character, office, investment)) {
                is com.maisha.game.domain.CampaignResult.Won -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            careerMessage = context.getString(R.string.msg_campaign_won),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is com.maisha.game.domain.CampaignResult.Lost -> {
                    persist(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            careerMessage = context.getString(R.string.msg_campaign_lost),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is com.maisha.game.domain.CampaignResult.Failed -> {
                    val message = when (result.reason) {
                        com.maisha.game.domain.CampaignFailure.INSUFFICIENT_FUNDS ->
                            context.getString(R.string.msg_campaign_cannot_afford)
                        com.maisha.game.domain.CampaignFailure.ALREADY_IN_OFFICE ->
                            context.getString(R.string.msg_campaign_already_in_office)
                        com.maisha.game.domain.CampaignFailure.ALREADY_CAMPAIGNED ->
                            context.getString(R.string.msg_campaign_already_this_year)
                        else -> context.getString(R.string.msg_campaign_ineligible)
                    }
                    _uiState.update { it.copy(careerMessage = message) }
                }
            }
        }
    }

    fun onPassTaxPolicy(type: com.maisha.game.data.model.TaxPolicyType) {
        val character = _uiState.value.character ?: return
        viewModelScope.launch {
            when (val result = gameEngine.passTaxPolicy(character, type)) {
                is FinanceEngine.TaxPolicyResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            careerMessage = context.getString(R.string.msg_tax_policy_passed),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is FinanceEngine.TaxPolicyResult.AlreadyActive -> {
                    _uiState.update {
                        it.copy(careerMessage = context.getString(R.string.msg_tax_policy_already_active))
                    }
                }
                is FinanceEngine.TaxPolicyResult.Ineligible -> {
                    _uiState.update {
                        it.copy(careerMessage = context.getString(R.string.msg_tax_policy_ineligible))
                    }
                }
            }
        }
    }

    fun onRenewVisa() {
        val character = _uiState.value.character ?: return
        viewModelScope.launch {
            when (val result = gameEngine.renewVisa(character)) {
                is com.maisha.game.domain.VisaRenewalResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(R.string.msg_visa_renewed),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is com.maisha.game.domain.VisaRenewalResult.CannotAfford -> {
                    _uiState.update {
                        it.copy(actionMessage = context.getString(R.string.msg_visa_cannot_afford))
                    }
                }
                else -> {
                    _uiState.update {
                        it.copy(actionMessage = context.getString(R.string.msg_visa_ineligible))
                    }
                }
            }
        }
    }

    fun onApplyForCitizenship() {
        val character = _uiState.value.character ?: return
        viewModelScope.launch {
            when (val result = gameEngine.applyForCitizenship(character)) {
                is com.maisha.game.domain.CitizenshipApplicationResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(R.string.msg_citizenship_granted),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is com.maisha.game.domain.CitizenshipApplicationResult.FailedTest -> {
                    persist(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(R.string.msg_citizenship_failed_test),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is com.maisha.game.domain.CitizenshipApplicationResult.CannotAfford -> {
                    _uiState.update {
                        it.copy(actionMessage = context.getString(R.string.msg_citizenship_cannot_afford))
                    }
                }
                is com.maisha.game.domain.CitizenshipApplicationResult.NotEligible -> {
                    _uiState.update {
                        it.copy(actionMessage = context.getString(R.string.msg_citizenship_ineligible))
                    }
                }
            }
        }
    }

    fun onAchievementDialogDismissed() {
        _uiState.update { state ->
            val remaining = state.pendingAchievementQueue.drop(1)
            state.copy(
                pendingAchievementQueue = remaining,
                currentAchievementDialog = remaining.firstOrNull()
            )
        }
        maybeReleaseDeferredInterstitial()
    }

    fun onCelebrationDismissed() {
        _uiState.update { state ->
            val remaining = state.pendingCelebrationQueue.drop(1)
            state.copy(
                pendingCelebrationQueue = remaining,
                currentCelebration = remaining.firstOrNull()
            )
        }
        maybeReleaseDeferredInterstitial()
    }

    private fun hasCelebratoryOverlay(state: LifeUiState): Boolean =
        state.currentCelebration != null ||
            state.pendingCelebrationQueue.isNotEmpty() ||
            state.currentAchievementDialog != null ||
            state.pendingAchievementQueue.isNotEmpty()

    private fun maybeReleaseDeferredInterstitial() {
        _uiState.update { state ->
            if (!state.deferredInterstitialAd || hasCelebratoryOverlay(state)) {
                state
            } else {
                state.copy(showInterstitialAd = true, deferredInterstitialAd = false)
            }
        }
    }

    fun onStatDeltaFinished(id: Long) {
        _uiState.update { state ->
            state.copy(pendingStatDeltas = state.pendingStatDeltas.filter { it.id != id })
        }
    }

    private suspend fun processMidLifeAchievements(character: Character) {
        val progress = achievementRepository.getProgressSnapshot()
        val newlyUnlocked = achievementEngine.checkAchievements(character, progress)
        if (newlyUnlocked.isEmpty()) return
        achievementRepository.unlockAchievements(newlyUnlocked)
        enqueueAchievementDialogs(newlyUnlocked)
    }

    private fun enqueueAchievementDialogs(achievements: List<Achievement>) {
        _uiState.update { state ->
            val queue = state.pendingAchievementQueue + achievements
            state.copy(
                pendingAchievementQueue = queue,
                currentAchievementDialog = state.currentAchievementDialog ?: queue.firstOrNull(),
                pendingFeedbackCues = state.pendingFeedbackCues + FeedbackCue(
                    sound = SoundEffect.ACHIEVEMENT_UNLOCK,
                    haptic = HapticType.SUCCESS
                )
            )
        }
        enqueueCelebration(CelebrationType.ACHIEVEMENT)
    }

    private fun enqueueCelebration(type: CelebrationType) {
        _uiState.update { state ->
            val queue = state.pendingCelebrationQueue + type
            state.copy(
                pendingCelebrationQueue = queue,
                currentCelebration = state.currentCelebration ?: queue.firstOrNull()
            )
        }
    }

    private fun appendStatDeltas(deltas: List<StatDeltaEvent>) {
        if (deltas.isEmpty()) return
        _uiState.update { it.copy(pendingStatDeltas = it.pendingStatDeltas + deltas) }
    }

    private fun buildStatDeltas(before: Stats, after: Stats): List<StatDeltaEvent> {
        fun delta(type: StatType, b: Int, a: Int): StatDeltaEvent? {
            val change = a - b
            return if (change != 0) StatDeltaEvent(type, change) else null
        }
        return listOfNotNull(
            delta(StatType.HEALTH, before.health, after.health),
            delta(StatType.HAPPINESS, before.happiness, after.happiness),
            delta(StatType.SMARTS, before.smarts, after.smarts),
            delta(StatType.LOOKS, before.looks, after.looks),
            delta(StatType.MONEY, before.money, after.money)
        )
    }

    private fun buildNetWorthDelta(before: Int, after: Int, countryCode: String): List<StatDeltaEvent> {
        val change = after - before
        if (change == 0) return emptyList()
        val sign = if (change > 0) "+" else "-"
        return listOf(
            StatDeltaEvent(
                type = StatType.NET_WORTH,
                delta = change,
                displayText = "$sign${formatMoney(kotlin.math.abs(change), countryCode)}"
            )
        )
    }

    private fun enqueueFeedback(vararg cues: FeedbackCue) {
        if (cues.isEmpty()) return
        _uiState.update { it.copy(pendingFeedbackCues = it.pendingFeedbackCues + cues) }
    }

    private fun enqueueAgeUpOutcomeFeedback(
        beforeMoney: Int,
        hadJob: Boolean,
        character: Character,
        result: AgeUpResult
    ) {
        val cues = mutableListOf<FeedbackCue>()
        if (!character.alive) {
            cues += FeedbackCue(sound = SoundEffect.DEATH)
        } else {
            eventOutcomeSound(result)?.let { cues += FeedbackCue(sound = it) }
            if (shouldPlayMoneyGain(beforeMoney, hadJob, character, result)) {
                cues += FeedbackCue(sound = SoundEffect.MONEY_GAIN)
            }
        }
        enqueueFeedback(*cues.toTypedArray())
    }

    private fun eventsFromResult(result: AgeUpResult): List<LifeEvent> = when (result) {
        is AgeUpResult.SingleEvent -> listOf(result.event)
        is AgeUpResult.MultipleEvents -> result.events
        AgeUpResult.NoEvent -> emptyList()
    }

    private fun eventOutcomeSound(result: AgeUpResult): SoundEffect? {
        val netEffect = eventsFromResult(result)
            .flatMap { event -> event.choices }
            .flatMap { choice -> choice.statEffects.values }
            .sum()
        return when {
            netEffect > 0 -> SoundEffect.EVENT_POSITIVE
            netEffect < 0 -> SoundEffect.EVENT_NEGATIVE
            else -> null
        }
    }

    private fun shouldPlayMoneyGain(
        beforeMoney: Int,
        hadJob: Boolean,
        character: Character,
        result: AgeUpResult
    ): Boolean {
        val promotionEvent = eventsFromResult(result)
            .any { event -> event.id == CareerEngine.PROMOTION_EVENT_ID }
        if (promotionEvent) return true
        return hadJob && character.stats.money > beforeMoney
    }

    private suspend fun applyAgeUpResult(
        character: Character,
        result: AgeUpResult,
        persistAge: Boolean
    ) {
        if (persistAge) {
            // Consume one-time event ids when an age-up offers them, before persisting the
            // advanced character — prevents process-death from re-eligible duplicate events
            // (and duplicate stat effects on a later age-up) after age was already saved.
            val consumedOneTimeIds = eventsFromResult(result)
                .filter { EventRepository.ONE_TIME_TAG in it.tags }
                .map { it.id }
            if (consumedOneTimeIds.isNotEmpty()) {
                triggeredEventIds = triggeredEventIds + consumedOneTimeIds
            }
            persist(character)
        }
        val isDead = !character.alive
        _uiState.update {
            it.copy(
                character = character,
                eligibleJobs = careerEngine.getEligibleJobs(character),
                netWorth = financeEngine.calculateNetWorth(character),
                navigateToLifeSummary = isDead,
                currentEvent = if (isDead) null else it.currentEvent,
                pendingEvents = if (isDead) emptyList() else it.pendingEvents
            )
        }
        if (isDead) return
        when (result) {
            is AgeUpResult.NoEvent -> Unit
            is AgeUpResult.SingleEvent -> {
                _uiState.update {
                    it.copy(pendingEvents = emptyList(), currentEvent = result.event)
                }
            }
            is AgeUpResult.MultipleEvents -> {
                _uiState.update {
                    it.copy(
                        pendingEvents = result.events.drop(1),
                        currentEvent = result.events.firstOrNull()
                    )
                }
            }
        }
    }

    private suspend fun persist(character: Character) {
        characterRepository.saveGame(slotId, character, triggeredEventIds)
    }

    private fun crimeTypeLabel(crimeType: CrimeType): String = when (crimeType) {
        CrimeType.PICKPOCKET -> context.getString(R.string.crime_type_pickpocket)
        CrimeType.SHOPLIFT -> context.getString(R.string.crime_type_shoplift)
        CrimeType.FRAUD -> context.getString(R.string.crime_type_fraud)
    }

    fun onDismissFamilyDetailTip() {
        dismissTip(OnboardingTips.FAMILY_DETAIL)
    }

    fun donationTiers(): List<Int> {
        val character = _uiState.value.character ?: return emptyList()
        return gameEngine.donationTiers(character.countryCode)
    }

    fun onVolunteer() {
        val character = _uiState.value.character ?: return
        if (!character.alive) return
        viewModelScope.launch {
            when (val result = gameEngine.volunteer(character)) {
                is GameEngine.VolunteerResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(R.string.msg_volunteer_success),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is GameEngine.VolunteerResult.Ineligible -> {
                    _uiState.update {
                        it.copy(actionMessage = context.getString(R.string.msg_philanthropy_ineligible))
                    }
                }
            }
        }
    }

    fun onDonateToCharity(amount: Int) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return
        viewModelScope.launch {
            when (val result = gameEngine.donateToCharity(character, amount)) {
                is GameEngine.DonationResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(
                                R.string.msg_donate_success,
                                formatMoney(amount, result.character.countryCode)
                            ),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is GameEngine.DonationResult.InsufficientFunds -> {
                    _uiState.update {
                        it.copy(actionMessage = context.getString(R.string.msg_donate_cannot_afford))
                    }
                }
                is GameEngine.DonationResult.InvalidAmount -> {
                    _uiState.update {
                        it.copy(actionMessage = context.getString(R.string.msg_donate_cannot_afford))
                    }
                }
            }
        }
    }

    fun onTakeDrivingTest() {
        val character = _uiState.value.character ?: return
        if (!character.alive) return
        viewModelScope.launch {
            when (val result = gameEngine.takeDrivingTest(character)) {
                is com.maisha.game.domain.DrivingTestResult.Passed -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(R.string.msg_driving_test_passed),
                            eligibleJobs = careerEngine.getEligibleJobs(result.character),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is com.maisha.game.domain.DrivingTestResult.Failed -> {
                    persist(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            actionMessage = context.getString(R.string.msg_driving_test_failed),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is com.maisha.game.domain.DrivingTestResult.InsufficientFunds -> {
                    _uiState.update {
                        it.copy(actionMessage = context.getString(R.string.msg_driving_test_cannot_afford))
                    }
                }
                is com.maisha.game.domain.DrivingTestResult.TooYoung -> {
                    _uiState.update {
                        it.copy(actionMessage = context.getString(R.string.msg_driving_test_too_young))
                    }
                }
                is com.maisha.game.domain.DrivingTestResult.AlreadyLicensed -> {
                    _uiState.update {
                        it.copy(actionMessage = context.getString(R.string.msg_driving_test_already_licensed))
                    }
                }
            }
        }
    }

    fun onThrowParty(budget: Int) {
        val character = _uiState.value.character ?: return
        if (!character.alive) return
        viewModelScope.launch {
            when (val result = gameEngine.throwParty(character, budget)) {
                is com.maisha.game.domain.PartyResult.Success -> {
                    persist(result.character)
                    processMidLifeAchievements(result.character)
                    _uiState.update {
                        it.copy(
                            character = result.character,
                            familyInteractionMessage = context.getString(
                                R.string.msg_party_success,
                                result.boost
                            ),
                            netWorth = financeEngine.calculateNetWorth(result.character)
                        )
                    }
                }
                is com.maisha.game.domain.PartyResult.InsufficientFunds -> {
                    _uiState.update {
                        it.copy(
                            familyInteractionMessage = context.getString(R.string.msg_party_cannot_afford)
                        )
                    }
                }
                is com.maisha.game.domain.PartyResult.NoGuests -> {
                    _uiState.update {
                        it.copy(
                            familyInteractionMessage = context.getString(R.string.msg_party_no_guests)
                        )
                    }
                }
                is com.maisha.game.domain.PartyResult.InvalidBudget -> {
                    _uiState.update {
                        it.copy(
                            familyInteractionMessage = context.getString(R.string.msg_party_no_guests)
                        )
                    }
                }
            }
        }
    }

    private fun flashExpression(character: Character, flash: Expression) {
        _uiState.update { it.copy(headerExpression = flash) }
        viewModelScope.launch {
            delay(EXPRESSION_FLASH_MS)
            _uiState.update { state ->
                val current = state.character ?: character
                state.copy(
                    headerExpression = ExpressionResolver.resolveExpression(current, null)
                )
            }
        }
    }

    private fun formatDecayNotice(notice: RelationshipDecayNotice): String =
        context.getString(
            R.string.msg_relationship_decay,
            notice.personName,
            tierLabel(notice.newTier)
        )

    private fun tierLabel(tier: RelationshipTier): String = when (tier) {
        RelationshipTier.ESTRANGED -> context.getString(R.string.tier_estranged)
        RelationshipTier.DISTANT -> context.getString(R.string.tier_distant)
        RelationshipTier.COOL -> context.getString(R.string.tier_cool)
        RelationshipTier.FRIENDLY -> context.getString(R.string.tier_friendly)
        RelationshipTier.CLOSE -> context.getString(R.string.tier_close)
        RelationshipTier.INSEPARABLE -> context.getString(R.string.tier_inseparable)
    }

    private fun outcomeFromAgeUpResult(result: AgeUpResult): EventOutcome {
        val events = eventsFromResult(result)
        if (events.isEmpty()) return EventOutcome.Neutral
        val happinessDeltas = events.flatMap { event -> event.choices }
            .mapNotNull { choice -> choice.statEffects["happiness"] }
        val maxDelta = happinessDeltas.maxOrNull() ?: 0
        val minDelta = happinessDeltas.minOrNull() ?: 0
        return when {
            maxDelta >= 5 -> EventOutcome.Positive(if (maxDelta >= 8) 2 else 1)
            minDelta <= -5 -> EventOutcome.Negative(severity = if (minDelta <= -8) 2 else 1)
            else -> EventOutcome.Neutral
        }
    }

    companion object {
        private const val EXPRESSION_FLASH_MS = 1_500L
    }
}
