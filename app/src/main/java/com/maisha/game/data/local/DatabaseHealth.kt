package com.maisha.game.data.local

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lazily opens the Room database on first access (off the main thread when callers use IO).
 *
 * Avoids eager SQLite work during [android.app.Application.onCreate] / cold start.
 * If open fails, repositories degrade gracefully instead of crashing launch.
 */
@Singleton
class DatabaseHealth @Inject constructor(
    @ApplicationContext private val context: Context
) : GameDatabaseAccess {

    private val lock = Any()

    @Volatile
    private var database: MaishaDatabase? = null

    @Volatile
    private var openAttempted = false

    @Volatile
    private var available = false

    private fun ensureOpened(): MaishaDatabase? {
        if (openAttempted) return database
        synchronized(lock) {
            if (openAttempted) return database
            openAttempted = true
            try {
                database = MaishaDatabaseFactory.build(context)
                available = true
            } catch (e: Exception) {
                Log.e(TAG, "Room database failed to open; character and achievement saves unavailable", e)
                database = null
                available = false
            }
            return database
        }
    }

    override val isAvailable: Boolean
        get() {
            ensureOpened()
            return available
        }

    override val characterDao: CharacterDao?
        get() = ensureOpened()?.characterDao()

    override val achievementDao: AchievementDao?
        get() = ensureOpened()?.achievementDao()

    companion object {
        private const val TAG = "DatabaseHealth"
    }
}
