// app/src/main/java/com/maisha/game/di/DatabaseModule.kt
package com.maisha.game.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.maisha.game.data.local.AchievementDao
import com.maisha.game.data.local.CharacterDao
import com.maisha.game.data.local.MaishaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE character_save ADD COLUMN familyJson TEXT NOT NULL DEFAULT '[]'"
        )
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE character_save ADD COLUMN educationJson TEXT NOT NULL DEFAULT '{}'"
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE character_save ADD COLUMN careerJson TEXT NOT NULL DEFAULT '{}'"
        )
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE character_save ADD COLUMN assetsJson TEXT NOT NULL DEFAULT '[]'"
        )
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE character_save ADD COLUMN criminalRecordJson TEXT NOT NULL DEFAULT '{}'"
        )
        db.execSQL(
            "ALTER TABLE character_save ADD COLUMN healthConditionsJson TEXT NOT NULL DEFAULT '[]'"
        )
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS character_save_new (
                slotId INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                age INTEGER NOT NULL,
                gender TEXT NOT NULL,
                health INTEGER NOT NULL,
                happiness INTEGER NOT NULL,
                smarts INTEGER NOT NULL,
                looks INTEGER NOT NULL,
                money INTEGER NOT NULL,
                birthYear INTEGER NOT NULL,
                alive INTEGER NOT NULL,
                eventLogJson TEXT NOT NULL,
                triggeredEventIdsJson TEXT NOT NULL,
                familyJson TEXT NOT NULL DEFAULT '[]',
                educationJson TEXT NOT NULL DEFAULT '{}',
                careerJson TEXT NOT NULL DEFAULT '{}',
                assetsJson TEXT NOT NULL DEFAULT '[]',
                criminalRecordJson TEXT NOT NULL DEFAULT '{}',
                healthConditionsJson TEXT NOT NULL DEFAULT '[]'
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO character_save_new (
                slotId, name, age, gender, health, happiness, smarts, looks, money,
                birthYear, alive, eventLogJson, triggeredEventIdsJson, familyJson,
                educationJson, careerJson, assetsJson, criminalRecordJson, healthConditionsJson
            )
            SELECT
                0, name, age, gender, health, happiness, smarts, looks, money,
                birthYear, alive, eventLogJson, triggeredEventIdsJson, familyJson,
                educationJson, careerJson, assetsJson, criminalRecordJson, healthConditionsJson
            FROM character_save
            LIMIT 1
            """.trimIndent()
        )
        db.execSQL("DROP TABLE character_save")
        db.execSQL("ALTER TABLE character_save_new RENAME TO character_save")
    }
}

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS achievement_progress (
                achievementId TEXT PRIMARY KEY NOT NULL,
                unlocked INTEGER NOT NULL,
                unlockedAt INTEGER
            )
            """.trimIndent()
        )
    }
}

private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE character_save ADD COLUMN countryCode TEXT NOT NULL DEFAULT 'KE'"
        )
        db.execSQL(
            """ALTER TABLE character_save ADD COLUMN avatarConfigJson TEXT NOT NULL DEFAULT '{"skinTone":2,"hairStyle":1,"hairColor":2,"outfitColor":3}'"""
        )
    }
}

private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE character_save ADD COLUMN generationNumber INTEGER NOT NULL DEFAULT 1"
        )
    }
}

private val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE character_save ADD COLUMN birthCountryCode TEXT NOT NULL DEFAULT 'KE'"
        )
        db.execSQL(
            "UPDATE character_save SET birthCountryCode = countryCode"
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MaishaDatabase {
        return Room.databaseBuilder(
            context,
            MaishaDatabase::class.java,
            "maisha_database"
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11
            )
            .build()
    }

    @Provides
    fun provideCharacterDao(database: MaishaDatabase): CharacterDao {
        return database.characterDao()
    }

    @Provides
    fun provideAchievementDao(database: MaishaDatabase): AchievementDao {
        return database.achievementDao()
    }
}
