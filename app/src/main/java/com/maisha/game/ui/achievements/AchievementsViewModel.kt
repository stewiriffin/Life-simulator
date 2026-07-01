// app/src/main/java/com/maisha/game/ui/achievements/AchievementsViewModel.kt (new)
package com.maisha.game.ui.achievements

import android.content.Context
import android.text.format.DateFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maisha.game.R
import com.maisha.game.data.AchievementCatalog
import com.maisha.game.data.AchievementRepository
import com.maisha.game.data.model.Achievement
import com.maisha.game.data.model.AchievementCategory
import com.maisha.game.data.model.AchievementProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class AchievementListItem(
    val achievement: Achievement,
    val progress: AchievementProgress
)

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val items: List<AchievementListItem> = emptyList()
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val achievementRepository: AchievementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            achievementRepository.getAllProgress().collect { progressList ->
                val progressById = progressList.associateBy { it.achievementId }
                val items = AchievementCatalog.all.map { achievement ->
                    AchievementListItem(
                        achievement = achievement,
                        progress = progressById[achievement.id]
                            ?: AchievementProgress(achievement.id)
                    )
                }
                _uiState.update { it.copy(isLoading = false, items = items) }
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
}
