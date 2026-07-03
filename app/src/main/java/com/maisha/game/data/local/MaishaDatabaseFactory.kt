package com.maisha.game.data.local

import android.content.Context
import androidx.room.Room

/**
 * Central Room builder — migrations only, no destructive fallback.
 *
 * [RoomDatabase.Builder.fallbackToDestructiveMigration] is intentionally omitted so a
 * missing migration fails loudly in development instead of silently wiping player saves.
 */
internal object MaishaDatabaseFactory {

    fun build(context: Context): MaishaDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            MaishaDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(*DatabaseMigrations.ALL)
            .build()

    private const val DATABASE_NAME = "maisha_database"
}
