package com.maisha.game.data.local

import android.content.Context
import android.util.Log
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens the Room database once at app start and records whether SQLite is usable.
 *
 * If open fails (corrupted DB file, etc.), repositories degrade gracefully instead of
 * crashing the process on launch.
 */
@Singleton
class DatabaseHealth @Inject constructor(
    @ApplicationContext context: Context
) : GameDatabaseAccess {
    private val database: MaishaDatabase?

    override val isAvailable: Boolean

    override val characterDao: CharacterDao?
    override val achievementDao: AchievementDao?

    init {
        var opened: MaishaDatabase? = null
        var available = false
        try {
            opened = MaishaDatabaseFactory.build(context)
            available = true
        } catch (e: Exception) {
            Log.e(TAG, "Room database failed to open; character and achievement saves unavailable", e)
        }
        database = opened
        isAvailable = available
        characterDao = opened?.characterDao()
        achievementDao = opened?.achievementDao()
    }

    companion object {
        private const val TAG = "DatabaseHealth"
    }
}
