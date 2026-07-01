// app/src/main/java/com/maisha/game/ui/settings/SettingsViewModel.kt (modified — notifications toggle)
package com.maisha.game.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maisha.game.data.AchievementRepository
import com.maisha.game.data.local.CharacterRepository
import com.maisha.game.data.local.SettingsRepository
import com.maisha.game.notifications.NotificationScheduler
import com.maisha.game.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val language: String = LocaleManager.LANG_EN,
    val showResetConfirm: Boolean = false,
    val resetComplete: Boolean = false,
    val requestNotificationPermission: Boolean = false,
    val preferIsoFlagFallback: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val characterRepository: CharacterRepository,
    private val achievementRepository: AchievementRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    private val systemNotificationsEnabled = MutableStateFlow(true)

    private data class SettingsPrefsSnapshot(
        val sound: Boolean,
        val haptics: Boolean,
        val notificationsPref: Boolean,
        val language: String,
        val systemEnabled: Boolean,
        val flagFallback: Boolean
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            settingsRepository.soundEnabled,
            settingsRepository.hapticsEnabled,
            settingsRepository.notificationsEnabled,
            settingsRepository.language
        ) { sound, haptics, notificationsPref, language ->
            Quadruple(sound, haptics, notificationsPref, language)
        },
        settingsRepository.flagEmojiFallbackPreferred,
        systemNotificationsEnabled
    ) { quad, flagFallback, systemEnabled ->
        SettingsPrefsSnapshot(
            sound = quad.first,
            haptics = quad.second,
            notificationsPref = quad.third,
            language = quad.fourth,
            systemEnabled = systemEnabled,
            flagFallback = flagFallback
        )
    }.combine(_uiState) { prefs, local ->
        local.copy(
            soundEnabled = prefs.sound,
            hapticsEnabled = prefs.haptics,
            notificationsEnabled = prefs.notificationsPref && prefs.systemEnabled,
            language = prefs.language,
            preferIsoFlagFallback = prefs.flagFallback
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState()
    )

    fun refreshSystemNotificationState() {
        systemNotificationsEnabled.value = settingsRepository.areSystemNotificationsEnabled()
    }

    fun onSoundChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
    }

    fun onHapticsChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticsEnabled(enabled) }
    }

    fun onNotificationsChanged(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                settingsRepository.setNotificationsEnabled(true)
                if (settingsRepository.areSystemNotificationsEnabled()) {
                    notificationScheduler.scheduleDailyReminder()
                } else {
                    _uiState.update { it.copy(requestNotificationPermission = true) }
                }
            } else {
                settingsRepository.setNotificationsEnabled(false)
                notificationScheduler.cancelAll()
            }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(requestNotificationPermission = false) }
        refreshSystemNotificationState()
        viewModelScope.launch {
            if (granted) {
                settingsRepository.setNotificationsEnabled(true)
                notificationScheduler.scheduleDailyReminder()
            } else {
                settingsRepository.setNotificationsEnabled(false)
                notificationScheduler.cancelAll()
            }
        }
    }

    fun onLanguageSelected(languageCode: String) {
        viewModelScope.launch { settingsRepository.setLanguage(languageCode) }
    }

    fun onFlagFallbackChanged(preferred: Boolean) {
        viewModelScope.launch { settingsRepository.setFlagEmojiFallbackPreferred(preferred) }
    }

    fun onResetAllDataRequested() {
        _uiState.update { it.copy(showResetConfirm = true) }
    }

    fun onDismissResetConfirm() {
        _uiState.update { it.copy(showResetConfirm = false) }
    }

    fun onConfirmResetAllData() {
        viewModelScope.launch {
            characterRepository.clearAllSlots()
            achievementRepository.clearAllProgress()
            settingsRepository.resetToDefaults()
            notificationScheduler.cancelAll()
            refreshSystemNotificationState()
            _uiState.update { it.copy(showResetConfirm = false, resetComplete = true) }
        }
    }

    fun onResetCompleteHandled() {
        _uiState.update { it.copy(resetComplete = false) }
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
