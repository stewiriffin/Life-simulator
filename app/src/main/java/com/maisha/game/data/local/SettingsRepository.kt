// app/src/main/java/com/maisha/game/data/local/SettingsRepository.kt (modified — notifications + last opened)
package com.maisha.game.data.local

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.maisha.game.util.LocaleManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

object OnboardingTips {
    const val FAMILY_DATING = "family_dating"
    const val FAMILY_DETAIL = "family_detail"
    const val FIRST_DEATH_ACHIEVEMENTS = "first_death_achievements"
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "maisha_settings"
)

/**
 * App-wide preferences via DataStore (not per slot).
 *
 * Sound, locale, onboarding tips, notification toggles, last-opened slot, etc.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.settingsDataStore

    private fun preferencesFlow() = dataStore.data.catch { e ->
        Log.e(TAG, "DataStore read failed; using defaults", e)
        emit(emptyPreferences())
    }

    val soundEnabled: Flow<Boolean> = preferencesFlow().map { prefs ->
        prefs[SOUND_KEY] ?: true
    }

    val hapticsEnabled: Flow<Boolean> = preferencesFlow().map { prefs ->
        prefs[HAPTICS_KEY] ?: true
    }

    val language: Flow<String> = preferencesFlow().map { prefs ->
        prefs[LANGUAGE_KEY] ?: LocaleManager.systemDefaultLanguage()
    }

    val hasCompletedOnboarding: Flow<Boolean> = preferencesFlow().map { prefs ->
        prefs[ONBOARDING_KEY] ?: false
    }

    val seenTipIds: Flow<Set<String>> = preferencesFlow().map { prefs ->
        prefs[SEEN_TIPS_KEY] ?: emptySet()
    }

    val notificationsEnabled: Flow<Boolean> = preferencesFlow().map { prefs ->
        prefs[NOTIFICATIONS_KEY] ?: true
    }

    val lastOpenedTimestamp: Flow<Long> = preferencesFlow().map { prefs ->
        prefs[LAST_OPENED_KEY] ?: System.currentTimeMillis()
    }

    val flagEmojiFallbackPreferred: Flow<Boolean> = preferencesFlow().map { prefs ->
        prefs[FLAG_EMOJI_FALLBACK_KEY] ?: false
    }

    fun areSystemNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun notificationsEnabledEffectiveFlow(): Flow<Boolean> =
        notificationsEnabled.map { stored ->
            stored && areSystemNotificationsEnabled()
        }

    suspend fun getLanguageSnapshot(): String =
        language.first()

    suspend fun hasCompletedOnboardingSnapshot(): Boolean = hasCompletedOnboarding.first()

    suspend fun getSeenTipsSnapshot(): Set<String> = seenTipIds.first()

    suspend fun isNotificationsEnabledNow(): Boolean = notificationsEnabled.first()

    suspend fun hasRecordedFirstAgeUp(): Boolean =
        preferencesFlow().first()[FIRST_AGE_UP_KEY] ?: false

    suspend fun isSoundEnabledNow(): Boolean = soundEnabled.first()

    suspend fun isHapticsEnabledNow(): Boolean = hapticsEnabled.first()

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[SOUND_KEY] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[HAPTICS_KEY] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_KEY] = enabled }
    }

    suspend fun updateLastOpenedTimestamp() {
        dataStore.edit { it[LAST_OPENED_KEY] = System.currentTimeMillis() }
    }

    suspend fun setFirstAgeUpRecorded() {
        dataStore.edit { it[FIRST_AGE_UP_KEY] = true }
    }

    suspend fun setLanguage(lang: String) {
        val normalized = if (lang in LocaleManager.supportedLanguages.map { it.code }) {
            lang
        } else {
            LocaleManager.LANG_EN
        }
        dataStore.edit { it[LANGUAGE_KEY] = normalized }
        LocaleManager.applyLocale(normalized)
    }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { it[ONBOARDING_KEY] = true }
    }

    suspend fun markTipSeen(tipId: String) {
        dataStore.edit { prefs ->
            val current = prefs[SEEN_TIPS_KEY] ?: emptySet()
            prefs[SEEN_TIPS_KEY] = current + tipId
        }
    }

    suspend fun isFlagEmojiFallbackPreferred(): Boolean =
        preferencesFlow().first()[FLAG_EMOJI_FALLBACK_KEY] ?: false

    suspend fun setFlagEmojiFallbackPreferred(preferred: Boolean) {
        dataStore.edit { it[FLAG_EMOJI_FALLBACK_KEY] = preferred }
    }

    suspend fun resetToDefaults() {
        dataStore.edit { prefs ->
            prefs.remove(SOUND_KEY)
            prefs.remove(HAPTICS_KEY)
            prefs.remove(LANGUAGE_KEY)
            prefs.remove(ONBOARDING_KEY)
            prefs.remove(SEEN_TIPS_KEY)
            prefs.remove(NOTIFICATIONS_KEY)
            prefs.remove(LAST_OPENED_KEY)
            prefs.remove(FIRST_AGE_UP_KEY)
            prefs.remove(FLAG_EMOJI_FALLBACK_KEY)
        }
        LocaleManager.applyLocale(LocaleManager.systemDefaultLanguage())
    }

    companion object {
        private const val TAG = "SettingsRepository"
        private val SOUND_KEY = booleanPreferencesKey("sound_enabled")
        private val HAPTICS_KEY = booleanPreferencesKey("haptics_enabled")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val ONBOARDING_KEY = booleanPreferencesKey("has_completed_onboarding")
        private val SEEN_TIPS_KEY = stringSetPreferencesKey("seen_tip_ids")
        private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
        private val LAST_OPENED_KEY = longPreferencesKey("last_opened_timestamp")
        private val FIRST_AGE_UP_KEY = booleanPreferencesKey("first_age_up_recorded")
        private val FLAG_EMOJI_FALLBACK_KEY = booleanPreferencesKey("flag_emoji_fallback")
    }
}
