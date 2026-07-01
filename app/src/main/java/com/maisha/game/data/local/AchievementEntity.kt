// app/src/main/java/com/maisha/game/data/local/AchievementEntity.kt (new)
package com.maisha.game.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.maisha.game.data.model.AchievementProgress

@Entity(tableName = "achievement_progress")
data class AchievementEntity(
    @PrimaryKey val achievementId: String,
    val unlocked: Boolean,
    val unlockedAt: Long?
) {
    fun toProgress(): AchievementProgress = AchievementProgress(
        achievementId = achievementId,
        unlocked = unlocked,
        unlockedAt = unlockedAt
    )
}
