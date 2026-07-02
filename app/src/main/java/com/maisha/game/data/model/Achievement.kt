// app/src/main/java/com/maisha/game/data/model/Achievement.kt (modified — string resource IDs)
package com.maisha.game.data.model

import androidx.annotation.StringRes

data class Achievement(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val category: AchievementCategory,
    val iconName: String
)

enum class AchievementCategory {
    CAREER,
    EDUCATION,
    FAMILY,
    WEALTH,
    LONGEVITY,
    MISCHIEF,
    WORLDLY
}

data class AchievementProgress(
    val achievementId: String,
    val unlocked: Boolean = false,
    val unlockedAt: Long? = null
)
