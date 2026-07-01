// app/src/main/java/com/maisha/game/data/local/CharacterDao.kt (modified — multi-slot)
package com.maisha.game.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {

    @Query("SELECT * FROM character_save WHERE slotId = :slotId LIMIT 1")
    suspend fun getCharacter(slotId: Int): CharacterEntity?

    @Query("SELECT * FROM character_save WHERE slotId = :slotId LIMIT 1")
    fun getCharacterFlow(slotId: Int): Flow<CharacterEntity?>

    @Query("SELECT * FROM character_save ORDER BY slotId ASC")
    fun getAllCharactersFlow(): Flow<List<CharacterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCharacter(entity: CharacterEntity)

    @Query("DELETE FROM character_save WHERE slotId = :slotId")
    suspend fun clearSlot(slotId: Int)

    @Query("DELETE FROM character_save")
    suspend fun clearAll()
}
