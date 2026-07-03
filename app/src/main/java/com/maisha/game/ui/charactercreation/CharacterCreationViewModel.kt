// app/src/main/java/com/maisha/game/ui/charactercreation/CharacterCreationViewModel.kt (modified — country + avatar flow)
package com.maisha.game.ui.charactercreation

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maisha.game.R
import com.maisha.game.data.CountryCatalog
import com.maisha.game.data.NamePool
import com.maisha.game.data.local.CharacterRepository
import com.maisha.game.data.local.MAX_SLOTS
import com.maisha.game.data.local.MetaBonusRepository
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.Stats
import com.maisha.game.domain.FamilyGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.random.Random

data class CharacterCreationUiState(
    val name: String = "",
    val selectedGender: Gender = Gender.MALE,
    val selectedCountryCode: String = "KE",
    val countrySearchQuery: String = "",
    val avatarConfig: AvatarConfig = AvatarConfig.random(),
    val nameError: String? = null,
    val isSaving: Boolean = false,
    val navigateToAvatarPicker: Boolean = false,
    val navigateToLife: Boolean = false,
    val secondWindBonusLabel: String? = null
)

@HiltViewModel
class CharacterCreationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val characterRepository: CharacterRepository,
    private val familyGenerator: FamilyGenerator,
    private val metaBonusRepository: MetaBonusRepository
) : ViewModel() {

    private val slotId: Int = savedStateHandle.get<Int>("slotId")
        ?.takeIf { it in 0 until MAX_SLOTS }
        ?: 0

    private val _uiState = MutableStateFlow(CharacterCreationUiState())
    val uiState: StateFlow<CharacterCreationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val pendingBonus = metaBonusRepository.peekSecondWindBonus()
            if (pendingBonus != null) {
                _uiState.update {
                    it.copy(secondWindBonusLabel = secondWindBonusLabel(pendingBonus))
                }
            }
        }
    }

    fun filteredCountries() = CountryCatalog.search(_uiState.value.countrySearchQuery)

    fun onNameChange(name: String) {
        val trimmed = name.take(MAX_NAME_LENGTH)
        _uiState.update { it.copy(name = trimmed, nameError = null) }
    }

    fun onGenderSelected(gender: Gender) {
        _uiState.update { it.copy(selectedGender = gender) }
    }

    fun onCountrySelected(countryCode: String) {
        _uiState.update { it.copy(selectedCountryCode = countryCode, countrySearchQuery = "") }
    }

    fun onCountrySearchChange(query: String) {
        _uiState.update { it.copy(countrySearchQuery = query) }
    }

    fun onAvatarChange(config: AvatarConfig) {
        _uiState.update { it.copy(avatarConfig = config) }
    }

    fun onRandomName() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                name = NamePool.randomFullName(state.selectedGender, state.selectedCountryCode),
                nameError = null
            )
        }
    }

    fun onContinueToAvatarPicker() {
        val trimmedName = _uiState.value.name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(nameError = context.getString(R.string.error_name_required)) }
            return
        }
        _uiState.update { it.copy(navigateToAvatarPicker = true) }
    }

    fun onAvatarPickerNavigationHandled() {
        _uiState.update { it.copy(navigateToAvatarPicker = false) }
    }

    fun onStartLife() {
        val state = _uiState.value
        val trimmedName = state.name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(nameError = context.getString(R.string.error_name_required)) }
            return
        }
        if (slotId !in 0 until MAX_SLOTS) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val bonusStat = metaBonusRepository.consumeSecondWindBonus()
            val baseStats = Stats(
                health = Random.nextInt(40, 71),
                happiness = Random.nextInt(40, 71),
                smarts = Random.nextInt(40, 71),
                looks = Random.nextInt(40, 71),
                money = 0
            )
            val stats = applySecondWindBonus(baseStats, bonusStat)
            val character = Character(
                name = trimmedName,
                age = 0,
                gender = state.selectedGender,
                stats = stats,
                birthYear = Calendar.getInstance().get(Calendar.YEAR),
                alive = true,
                countryCode = state.selectedCountryCode,
                birthCountryCode = state.selectedCountryCode,
                avatarConfig = state.avatarConfig,
                eventLog = emptyList(),
                family = familyGenerator.generateFamily(
                    characterAge = 0,
                    countryCode = state.selectedCountryCode
                )
            )
            characterRepository.saveGame(slotId, character, triggeredEventIds = emptySet())
            _uiState.update {
                it.copy(
                    isSaving = false,
                    navigateToLife = true,
                    secondWindBonusLabel = null
                )
            }
        }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigateToLife = false) }
    }

    private fun applySecondWindBonus(stats: Stats, bonusStat: String?): Stats {
        if (bonusStat == null) return stats
        val amount = MetaBonusRepository.SECOND_WIND_BONUS_AMOUNT
        return when (bonusStat) {
            MetaBonusRepository.STAT_HEALTH -> stats.copy(health = (stats.health + amount).coerceAtMost(100))
            MetaBonusRepository.STAT_HAPPINESS -> stats.copy(happiness = (stats.happiness + amount).coerceAtMost(100))
            MetaBonusRepository.STAT_SMARTS -> stats.copy(smarts = (stats.smarts + amount).coerceAtMost(100))
            MetaBonusRepository.STAT_LOOKS -> stats.copy(looks = (stats.looks + amount).coerceAtMost(100))
            else -> stats
        }
    }

    private fun secondWindBonusLabel(statKey: String): String = when (statKey) {
        MetaBonusRepository.STAT_HEALTH -> context.getString(R.string.second_wind_bonus_health)
        MetaBonusRepository.STAT_HAPPINESS -> context.getString(R.string.second_wind_bonus_happiness)
        MetaBonusRepository.STAT_SMARTS -> context.getString(R.string.second_wind_bonus_smarts)
        MetaBonusRepository.STAT_LOOKS -> context.getString(R.string.second_wind_bonus_looks)
        else -> context.getString(R.string.second_wind_bonus_random)
    }

    companion object {
        const val MAX_NAME_LENGTH = 40
    }
}
