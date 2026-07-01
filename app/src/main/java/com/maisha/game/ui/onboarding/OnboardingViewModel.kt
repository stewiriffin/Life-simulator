// app/src/main/java/com/maisha/game/ui/onboarding/OnboardingViewModel.kt (new)
package com.maisha.game.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maisha.game.data.local.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val navigateToCharacterCreation: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onFinish() {
        completeOnboarding()
    }

    fun onSkip() {
        completeOnboarding()
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigateToCharacterCreation = false) }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted()
            _uiState.update { it.copy(navigateToCharacterCreation = true) }
        }
    }
}
