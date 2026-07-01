// app/src/main/java/com/maisha/game/data/local/MetaBonusRepository.kt
package com.maisha.game.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.metaBonusDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "meta_bonus"
)

@Singleton
class MetaBonusRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.metaBonusDataStore

    suspend fun incrementAgeUpCount(): Int {
        var newCount = 0
        dataStore.edit { prefs ->
            val current = prefs[AGE_UP_COUNT_KEY] ?: 0
            newCount = current + 1
            prefs[AGE_UP_COUNT_KEY] = newCount
        }
        return newCount
    }

    suspend fun getAgeUpCount(): Int {
        return dataStore.data.map { it[AGE_UP_COUNT_KEY] ?: 0 }.first()
    }

    suspend fun setSecondWindBonus(statKey: String) {
        dataStore.edit { prefs ->
            prefs[SECOND_WIND_STAT_KEY] = statKey
        }
    }

    suspend fun peekSecondWindBonus(): String? {
        return dataStore.data.map { it[SECOND_WIND_STAT_KEY] }.first()
    }

    suspend fun consumeSecondWindBonus(): String? {
        var bonus: String? = null
        dataStore.edit { prefs ->
            bonus = prefs.remove(SECOND_WIND_STAT_KEY)
        }
        return bonus
    }

    fun secondWindBonusLabel(statKey: String): String = when (statKey) {
        STAT_HEALTH -> "+8 Health"
        STAT_HAPPINESS -> "+8 Happiness"
        STAT_SMARTS -> "+8 Smarts"
        STAT_LOOKS -> "+8 Looks"
        else -> "+8 to a random stat"
    }

    companion object {
        const val STAT_HEALTH = "health"
        const val STAT_HAPPINESS = "happiness"
        const val STAT_SMARTS = "smarts"
        const val STAT_LOOKS = "looks"
        const val SECOND_WIND_BONUS_AMOUNT = 8

        private val AGE_UP_COUNT_KEY = intPreferencesKey("age_up_count")
        private val SECOND_WIND_STAT_KEY = stringPreferencesKey("second_wind_stat")
    }
}
