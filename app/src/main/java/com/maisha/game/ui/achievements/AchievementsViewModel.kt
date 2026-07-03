// app/src/main/java/com/maisha/game/ui/achievements/AchievementsViewModel.kt
package com.maisha.game.ui.achievements

import android.content.Context
import android.text.format.DateFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maisha.game.R
import com.maisha.game.data.AchievementCatalog
import com.maisha.game.data.AchievementRepository
import com.maisha.game.data.AchievementWealth
import com.maisha.game.data.local.CharacterRepository
import com.maisha.game.data.local.MAX_SLOTS
import com.maisha.game.data.local.SavedGameLoadResult
import com.maisha.game.data.model.Achievement
import com.maisha.game.data.model.AchievementCategory
import com.maisha.game.data.model.AchievementProgress
import com.maisha.game.domain.FinanceEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class AchievementListItem(
    val achievement: Achievement,
    val progress: AchievementProgress,
    /** 0f–1f when tiered progress applies; null for binary achievements. */
    val progressFraction: Float? = null,
    val progressCurrent: Int? = null,
    val progressTarget: Int? = null
)

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val items: List<AchievementListItem> = emptyList(),
    val displayCountryCode: String = "KE",
    val bestNetWorth: Int = 0,
    val bestAge: Int = 0
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val achievementRepository: AchievementRepository,
    private val characterRepository: CharacterRepository,
    private val financeEngine: FinanceEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val slots = characterRepository.getAllSlots().first()
            val displayCountryCode = slots
                .firstOrNull { !it.isEmpty && !it.isCorrupted }
                ?.countryCode
                ?: "KE"

            var bestNetWorth = 0
            var bestAge = 0
            for (slotId in 0 until MAX_SLOTS) {
                when (val result = characterRepository.loadGame(slotId)) {
                    is SavedGameLoadResult.Success -> {
                        val character = result.game.character
                        bestNetWorth = maxOf(
                            bestNetWorth,
                            financeEngine.calculateNetWorth(character)
                        )
                        bestAge = maxOf(bestAge, character.age)
                    }
                    else -> Unit
                }
            }
            // Slot summaries can still contribute age if a full load fails.
            slots.forEach { summary ->
                bestAge = maxOf(bestAge, summary.age ?: 0)
            }

            achievementRepository.getAllProgress().collect { progressList ->
                val progressById = progressList.associateBy { it.achievementId }
                val items = AchievementCatalog.all.map { achievement ->
                    val progress = progressById[achievement.id]
                        ?: AchievementProgress(achievement.id)
                    val tier = tierProgress(
                        achievementId = achievement.id,
                        unlocked = progress.unlocked,
                        countryCode = displayCountryCode,
                        bestNetWorth = bestNetWorth,
                        bestAge = bestAge
                    )
                    AchievementListItem(
                        achievement = achievement,
                        progress = progress,
                        progressFraction = tier?.fraction,
                        progressCurrent = tier?.current,
                        progressTarget = tier?.target
                    )
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = items,
                        displayCountryCode = displayCountryCode,
                        bestNetWorth = bestNetWorth,
                        bestAge = bestAge
                    )
                }
            }
        }
    }

    fun formatUnlockDate(unlockedAt: Long?): String? {
        unlockedAt ?: return null
        val formatter = DateFormat.getDateFormat(context)
        val dateText = formatter.format(Date(unlockedAt))
        return context.getString(R.string.format_achievement_unlocked_date, dateText)
    }

    fun itemsForCategory(category: AchievementCategory): List<AchievementListItem> =
        _uiState.value.items.filter { it.achievement.category == category }

    private data class TierProgress(val fraction: Float, val current: Int, val target: Int)

    private fun tierProgress(
        achievementId: String,
        unlocked: Boolean,
        countryCode: String,
        bestNetWorth: Int,
        bestAge: Int
    ): TierProgress? {
        if (unlocked) return null
        return when (achievementId) {
            "six_figures" -> {
                val target = AchievementWealth.sixFiguresThreshold(countryCode)
                TierProgress(
                    fraction = (bestNetWorth.toFloat() / target).coerceIn(0f, 1f),
                    current = bestNetWorth,
                    target = target
                )
            }
            "first_million" -> {
                val target = AchievementWealth.firstMillionThreshold(countryCode)
                TierProgress(
                    fraction = (bestNetWorth.toFloat() / target).coerceIn(0f, 1f),
                    current = bestNetWorth,
                    target = target
                )
            }
            "half_century" -> TierProgress(
                fraction = (bestAge.toFloat() / 50f).coerceIn(0f, 1f),
                current = bestAge,
                target = 50
            )
            "golden_years" -> TierProgress(
                fraction = (bestAge.toFloat() / 80f).coerceIn(0f, 1f),
                current = bestAge,
                target = 80
            )
            "centenarian" -> TierProgress(
                fraction = (bestAge.toFloat() / 100f).coerceIn(0f, 1f),
                current = bestAge,
                target = 100
            )
            else -> null
        }
    }
}
