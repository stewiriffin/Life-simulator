// app/src/main/java/com/maisha/game/data/AchievementRepository.kt (new)
package com.maisha.game.data

import com.maisha.game.data.local.AchievementDao
import com.maisha.game.data.local.AchievementEntity
import com.maisha.game.data.model.Achievement
import com.maisha.game.data.model.AchievementProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide achievement unlock state (not per save slot).
 *
 * Merges Room rows with [AchievementCatalog] so locked achievements still appear in UI.
 */
@Singleton
class AchievementRepository @Inject constructor(
    private val achievementDao: AchievementDao
) {

    fun getAllProgress(): Flow<List<AchievementProgress>> =
        achievementDao.getAllFlow().map { entities -> mergeWithCatalog(entities) }

    suspend fun getProgressSnapshot(): List<AchievementProgress> =
        mergeWithCatalog(achievementDao.getAll())

    suspend fun clearAllProgress() {
        achievementDao.clearAll()
    }

    suspend fun unlockAchievements(newlyUnlocked: List<Achievement>) {
        if (newlyUnlocked.isEmpty()) return
        val now = System.currentTimeMillis()
        achievementDao.upsertAll(
            newlyUnlocked.map { achievement ->
                AchievementEntity(
                    achievementId = achievement.id,
                    unlocked = true,
                    unlockedAt = now
                )
            }
        )
    }

    private fun mergeWithCatalog(entities: List<AchievementEntity>): List<AchievementProgress> {
        val byId = entities.associateBy { it.achievementId }
        return AchievementCatalog.all.map { achievement ->
            byId[achievement.id]?.toProgress()
                ?: AchievementProgress(achievementId = achievement.id, unlocked = false, unlockedAt = null)
        }
    }
}
