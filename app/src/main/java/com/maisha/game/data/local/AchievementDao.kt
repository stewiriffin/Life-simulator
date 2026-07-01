// app/src/main/java/com/maisha/game/data/local/AchievementDao.kt (new)
package com.maisha.game.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievement_progress")
    fun getAllFlow(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievement_progress")
    suspend fun getAll(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AchievementEntity>)

    @Query("DELETE FROM achievement_progress")
    suspend fun clearAll()
}
