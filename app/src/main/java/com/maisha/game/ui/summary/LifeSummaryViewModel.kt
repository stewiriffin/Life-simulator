// app/src/main/java/com/maisha/game/ui/summary/LifeSummaryViewModel.kt (modified — share card data)
package com.maisha.game.ui.summary

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maisha.game.R
import com.maisha.game.ads.SessionAdTracker
import com.maisha.game.data.AchievementCatalog
import com.maisha.game.data.JobPool
import com.maisha.game.data.AchievementRepository
import com.maisha.game.data.local.CharacterRepository
import com.maisha.game.data.local.MAX_SLOTS
import com.maisha.game.data.local.MetaBonusRepository
import com.maisha.game.data.local.OnboardingTips
import com.maisha.game.data.local.SettingsRepository
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.relationshipTierFor
import com.maisha.game.data.model.RelationshipTier
import com.maisha.game.domain.AchievementEngine
import com.maisha.game.domain.LegacyEngine
import com.maisha.game.domain.DeathCause
import com.maisha.game.domain.FinanceEngine
import com.maisha.game.domain.GameEngine
import com.maisha.game.domain.MortalityEngine
import com.maisha.game.ui.main.EducationFormatter
import com.maisha.game.ui.share.ShareAchievementBadge
import com.maisha.game.ui.share.ShareCardData
import com.maisha.game.ui.share.achievementEmoji
import com.maisha.game.util.formatMoney
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class LifeSummaryUiState(
    val isLoading: Boolean = true,
    val character: Character? = null,
    val deathCauseLabel: String = "",
    val deathFlavorText: String = "",
    val netWorth: Int = 0,
    val careerRecap: String = "",
    val educationRecap: String = "",
    val spouseRecap: String = "",
    val closestBondRecap: String = "",
    val childrenCount: Int = 0,
    val eventHighlights: List<String> = emptyList(),
    val showSecondWindButton: Boolean = false,
    val secondWindMessage: String? = null,
    val navigateToSlotPicker: Boolean = false,
    val showRewardedAd: Boolean = false,
    val showAchievementsCarryOverTip: Boolean = false,
    val shareCardData: ShareCardData? = null,
    val showSharePreview: Boolean = false,
    val isShareCapturing: Boolean = false,
    val shareErrorMessage: String? = null,
    val eligibleHeirs: List<com.maisha.game.data.model.Person> = emptyList(),
    val showHeirSelection: Boolean = false,
    val selectedHeir: com.maisha.game.data.model.Person? = null,
    val showLegacyConfirmation: Boolean = false,
    val navigateToLife: Boolean = false,
    val slotId: Int = 0
)

@HiltViewModel
class LifeSummaryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val characterRepository: CharacterRepository,
    private val financeEngine: FinanceEngine,
    private val mortalityEngine: MortalityEngine,
    private val sessionAdTracker: SessionAdTracker,
    private val metaBonusRepository: MetaBonusRepository,
    private val settingsRepository: SettingsRepository,
    private val achievementRepository: AchievementRepository,
    private val legacyEngine: LegacyEngine,
    private val achievementEngine: AchievementEngine,
    private val gameEngine: GameEngine
) : ViewModel() {

    private val slotId: Int = savedStateHandle.get<Int>("slotId")
        ?.takeIf { it in 0 until MAX_SLOTS }
        ?: 0

    private val _uiState = MutableStateFlow(LifeSummaryUiState())
    val uiState: StateFlow<LifeSummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = characterRepository.loadGame(slotId)
            val character = saved?.character
            if (character == null || character.alive) {
                _uiState.update {
                    it.copy(isLoading = false, navigateToSlotPicker = true)
                }
                return@launch
            }

            val cause = mortalityEngine.parseDeathCause(character) ?: DeathCause.OLD_AGE
            val highlights = character.eventLog
                .filter { !it.startsWith("::DEATH:") }
                .take(5)
            val seenTips = settingsRepository.getSeenTipsSnapshot()
            val showAchievementsTip =
                OnboardingTips.FIRST_DEATH_ACHIEVEMENTS !in seenTips
            val netWorth = financeEngine.calculateNetWorth(character)
            val deathLabel = deathCauseLabel(cause)
            val achievementProgress = achievementRepository.getProgressSnapshot()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    slotId = slotId,
                    character = character,
                    deathCauseLabel = deathLabel,
                    deathFlavorText = deathFlavorText(cause),
                    netWorth = netWorth,
                    careerRecap = buildCareerRecap(character),
                    educationRecap = EducationFormatter.formatHighestEducation(
                        character.education,
                        context.resources
                    ),
                    spouseRecap = buildSpouseRecap(character),
                    closestBondRecap = buildClosestBondRecap(character),
                    childrenCount = character.family.count { person ->
                        person.relation == RelationType.CHILD
                    },
                    eventHighlights = highlights,
                    showSecondWindButton = sessionAdTracker.canShowSecondWindOffer(),
                    showAchievementsCarryOverTip = showAchievementsTip,
                    shareCardData = buildShareCardData(
                        character = character,
                        deathCauseLabel = deathLabel,
                        netWorth = netWorth,
                        unlockedAchievementIds = achievementProgress
                            .filter { progress -> progress.unlocked }
                            .map { progress -> progress.achievementId }
                    ),
                    eligibleHeirs = legacyEngine.eligibleHeirs(character)
                )
            }
        }
    }

    fun onShareMyLifeClicked() {
        _uiState.update {
            it.copy(showSharePreview = true, shareErrorMessage = null)
        }
    }

    fun onDismissSharePreview() {
        if (_uiState.value.isShareCapturing) return
        _uiState.update { it.copy(showSharePreview = false) }
    }

    fun onShareCapturingStarted() {
        _uiState.update { it.copy(isShareCapturing = true, shareErrorMessage = null) }
    }

    fun onShareCompleted() {
        _uiState.update {
            it.copy(isShareCapturing = false, showSharePreview = false)
        }
    }

    fun onShareFailed(message: String) {
        _uiState.update {
            it.copy(isShareCapturing = false, shareErrorMessage = message)
        }
    }

    fun onDismissShareError() {
        _uiState.update { it.copy(shareErrorMessage = null) }
    }

    fun onWatchSecondWind() {
        if (!sessionAdTracker.canShowSecondWindOffer()) return
        _uiState.update { it.copy(showRewardedAd = true) }
    }

    fun onDismissAchievementsTip() {
        viewModelScope.launch {
            settingsRepository.markTipSeen(OnboardingTips.FIRST_DEATH_ACHIEVEMENTS)
            _uiState.update { it.copy(showAchievementsCarryOverTip = false) }
        }
    }

    fun onRewardedAdHandled() {
        _uiState.update { it.copy(showRewardedAd = false) }
    }

    fun onSecondWindRewardEarned() {
        viewModelScope.launch {
            val statKey = randomSecondWindStat()
            metaBonusRepository.setSecondWindBonus(statKey)
            sessionAdTracker.markSecondWindOfferUsed()
            val label = secondWindBonusLabel(statKey)
            _uiState.update {
                it.copy(
                    showSecondWindButton = false,
                    secondWindMessage = context.getString(R.string.msg_second_wind_earned, label)
                )
            }
        }
    }

    fun onSecondWindDismissedNoReward() {
        _uiState.update {
            it.copy(secondWindMessage = context.getString(R.string.msg_second_wind_no_bonus))
        }
    }

    fun onStartNewLife() {
        _uiState.update { it.copy(navigateToSlotPicker = true) }
    }

    fun onContinueLegacyClicked() {
        if (_uiState.value.eligibleHeirs.isEmpty()) return
        _uiState.update { it.copy(showHeirSelection = true) }
    }

    fun onHeirSelected(heir: com.maisha.game.data.model.Person) {
        _uiState.update {
            it.copy(selectedHeir = heir, showLegacyConfirmation = true)
        }
    }

    fun onDismissLegacyConfirmation() {
        _uiState.update {
            it.copy(showLegacyConfirmation = false, selectedHeir = null)
        }
    }

    fun onDismissHeirSelection() {
        _uiState.update {
            it.copy(
                showHeirSelection = false,
                showLegacyConfirmation = false,
                selectedHeir = null
            )
        }
    }

    fun onConfirmLegacyContinuation() {
        val deceased = _uiState.value.character ?: return
        val heir = _uiState.value.selectedHeir ?: return
        viewModelScope.launch {
            var legacyCharacter = legacyEngine.createLegacyCharacter(deceased, heir)
            legacyCharacter = gameEngine.applyLegacyFamilyMilestones(legacyCharacter)
            val progress = achievementRepository.getProgressSnapshot()
            val newlyUnlocked = achievementEngine.checkAchievements(legacyCharacter, progress)
            if (newlyUnlocked.isNotEmpty()) {
                achievementRepository.unlockAchievements(newlyUnlocked)
            }
            characterRepository.saveGame(slotId, legacyCharacter, triggeredEventIds = emptySet())
            _uiState.update {
                it.copy(
                    showHeirSelection = false,
                    showLegacyConfirmation = false,
                    selectedHeir = null,
                    navigateToLife = true
                )
            }
        }
    }

    fun onLifeNavigationHandled() {
        _uiState.update { it.copy(navigateToLife = false) }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigateToSlotPicker = false) }
    }

    private fun buildShareCardData(
        character: Character,
        deathCauseLabel: String,
        netWorth: Int,
        unlockedAchievementIds: List<String>
    ): ShareCardData {
        val (topStatLabel, topStatValue) = topStatForShare(character)
        val badges = unlockedAchievementIds
            .mapNotNull { id -> AchievementCatalog.byId(id) }
            .take(2)
            .map { achievement ->
                ShareAchievementBadge(
                    titleRes = achievement.titleRes,
                    emoji = achievementEmoji(achievement.iconName),
                    category = achievement.category
                )
            }
        val careerJobId = resolveCareerJobId(character)
        val topAsset = character.assets.maxByOrNull { it.currentValue }
        val topAssetType = topAsset?.type
        val topAssetSummary = topAsset?.name
        return ShareCardData(
            characterName = character.name,
            avatarConfig = character.avatarConfig,
            birthYear = character.birthYear,
            deathYear = character.birthYear + character.age,
            ageAtDeath = character.age,
            countryCode = character.countryCode,
            deathCauseLabel = deathCauseLabel,
            topStatLabel = topStatLabel,
            topStatValue = topStatValue,
            netWorthFormatted = formatMoney(netWorth, character.countryCode),
            careerHeadline = buildShareCareerHeadline(character),
            careerJobId = careerJobId,
            topAssetType = topAssetType,
            topAssetSummary = topAssetSummary,
            familySummary = buildShareFamilySummary(character),
            closestBondSummary = buildClosestBondRecap(character),
            achievementBadges = badges
        )
    }

    private fun resolveCareerJobId(character: Character): String? {
        character.career.currentJob?.id?.let { return it }
        val lastTitle = character.career.jobHistory.lastOrNull() ?: return null
        return JobPool.jobs.find { it.title == lastTitle }?.id
    }

    private fun topStatForShare(character: Character): Pair<String, Int> {
        val stats = listOf(
            context.getString(R.string.stat_health) to character.stats.health,
            context.getString(R.string.stat_happiness) to character.stats.happiness,
            context.getString(R.string.stat_smarts) to character.stats.smarts,
            context.getString(R.string.stat_looks) to character.stats.looks
        )
        return stats.maxByOrNull { it.second } ?: (context.getString(R.string.stat_health) to 0)
    }

    private fun buildShareCareerHeadline(character: Character): String {
        val job = character.career.currentJob
        return when {
            job != null -> context.getString(
                R.string.share_career_job_level,
                job.title,
                job.level
            )
            character.career.jobHistory.isNotEmpty() -> context.getString(
                R.string.share_career_former,
                character.career.jobHistory.last()
            )
            else -> context.getString(R.string.share_career_freespirit)
        }
    }

    private fun buildShareFamilySummary(character: Character): String {
        val spouse = character.family.firstOrNull { it.relation == RelationType.SPOUSE }
        val kids = character.family.count { it.relation == RelationType.CHILD }
        return when {
            spouse != null && kids > 0 -> context.getString(
                R.string.share_family_married_kids,
                spouse.name,
                kids
            )
            spouse != null -> context.getString(R.string.share_family_married, spouse.name)
            kids > 0 -> context.getString(R.string.share_family_kids_only, kids)
            else -> context.getString(R.string.share_family_never_settled)
        }
    }

    private fun randomSecondWindStat(): String {
        val stats = listOf(
            MetaBonusRepository.STAT_HEALTH,
            MetaBonusRepository.STAT_HAPPINESS,
            MetaBonusRepository.STAT_SMARTS,
            MetaBonusRepository.STAT_LOOKS
        )
        return stats.random(Random.Default)
    }

    private fun buildCareerRecap(character: Character): String {
        val job = character.career.currentJob
        return if (job != null) {
            context.getString(R.string.career_job_level, job.title, job.level)
        } else if (character.career.jobHistory.isNotEmpty()) {
            context.getString(R.string.format_career_formerly, character.career.jobHistory.last())
        } else {
            context.getString(R.string.career_never_held_job)
        }
    }

    private fun buildSpouseRecap(character: Character): String {
        val spouse = character.family.firstOrNull { it.relation == RelationType.SPOUSE }
        return spouse?.let { partner ->
            if (partner.isMarried) {
                context.getString(R.string.format_married_to, partner.name)
            } else {
                context.getString(R.string.format_partner_name, partner.name)
            }
        } ?: context.getString(R.string.family_no_spouse)
    }

    private fun buildClosestBondRecap(character: Character): String {
        val closest = character.family.maxByOrNull { it.relationshipLevel }
            ?: return context.getString(R.string.recap_no_close_bond)
        val tierName = when (relationshipTierFor(closest.relationshipLevel)) {
            RelationshipTier.ESTRANGED -> context.getString(R.string.tier_estranged)
            RelationshipTier.DISTANT -> context.getString(R.string.tier_distant)
            RelationshipTier.COOL -> context.getString(R.string.tier_cool)
            RelationshipTier.FRIENDLY -> context.getString(R.string.tier_friendly)
            RelationshipTier.CLOSE -> context.getString(R.string.tier_close)
            RelationshipTier.INSEPARABLE -> context.getString(R.string.tier_inseparable)
        }
        return context.getString(R.string.recap_closest_bond, closest.name, tierName)
    }

    private fun deathCauseLabel(cause: DeathCause): String = when (cause) {
        DeathCause.OLD_AGE -> context.getString(R.string.death_cause_old_age)
        DeathCause.HEALTH_FAILURE -> context.getString(R.string.death_cause_health_failure)
        DeathCause.ACCIDENT -> context.getString(R.string.death_cause_accident)
        DeathCause.ILLNESS -> context.getString(R.string.death_cause_illness)
    }

    private fun deathFlavorText(cause: DeathCause): String = when (cause) {
        DeathCause.OLD_AGE -> context.getString(R.string.death_flavor_old_age)
        DeathCause.HEALTH_FAILURE -> context.getString(R.string.death_flavor_health_failure)
        DeathCause.ACCIDENT -> context.getString(R.string.death_flavor_accident)
        DeathCause.ILLNESS -> context.getString(R.string.death_flavor_illness)
    }

    private fun secondWindBonusLabel(statKey: String): String = when (statKey) {
        MetaBonusRepository.STAT_HEALTH -> context.getString(R.string.second_wind_bonus_health)
        MetaBonusRepository.STAT_HAPPINESS -> context.getString(R.string.second_wind_bonus_happiness)
        MetaBonusRepository.STAT_SMARTS -> context.getString(R.string.second_wind_bonus_smarts)
        MetaBonusRepository.STAT_LOOKS -> context.getString(R.string.second_wind_bonus_looks)
        else -> context.getString(R.string.second_wind_bonus_random)
    }
}
