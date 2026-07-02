// app/src/main/java/com/maisha/game/data/local/MaishaDatabase.kt (modified — v8 achievements)
package com.maisha.game.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CharacterEntity::class, AchievementEntity::class],
    version = 11,
    exportSchema = false
)
abstract class MaishaDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun achievementDao(): AchievementDao
}

