// app/src/main/java/com/maisha/game/ui/slots/SlotPickerViewModel.kt (new)
package com.maisha.game.ui.slots

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maisha.game.R
import com.maisha.game.data.local.CharacterRepository
import com.maisha.game.data.local.MAX_SLOTS
import com.maisha.game.data.local.SlotSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SlotPickerUiState(
    val slots: List<SlotSummary> = emptyList(),
    val isLoading: Boolean = true,
    val isDatabaseUnavailable: Boolean = false,
    val pendingOverwriteSlotId: Int? = null,
    val navigateToLife: Int? = null,
    val navigateToCreation: Int? = null,
    val navigateToSummary: Int? = null
)

@HiltViewModel
class SlotPickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val characterRepository: CharacterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SlotPickerUiState())
    val uiState: StateFlow<SlotPickerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (!characterRepository.isDatabaseAvailable) {
                _uiState.update {
                    it.copy(isLoading = false, isDatabaseUnavailable = true)
                }
                return@launch
            }
            characterRepository.getAllSlots().collect { slots ->
                _uiState.update { it.copy(slots = slots, isLoading = false) }
            }
        }
    }

    fun onContinue(slotId: Int) {
        if (slotId !in 0 until MAX_SLOTS) return
        _uiState.update { it.copy(navigateToLife = slotId) }
    }

    fun onViewSummary(slotId: Int) {
        if (slotId !in 0 until MAX_SLOTS) return
        _uiState.update { it.copy(navigateToSummary = slotId) }
    }

    fun onStartNewLife(slotId: Int) {
        if (slotId !in 0 until MAX_SLOTS) return
        val slot = _uiState.value.slots.find { it.slotId == slotId } ?: return
        if (slot.isCorrupted) return
        if (slot.isEmpty) {
            _uiState.update { it.copy(navigateToCreation = slotId) }
        } else {
            _uiState.update { it.copy(pendingOverwriteSlotId = slotId) }
        }
    }

    fun onClearCorruptedSlot(slotId: Int) {
        if (slotId !in 0 until MAX_SLOTS) return
        viewModelScope.launch {
            characterRepository.clearSlot(slotId)
            _uiState.update {
                it.copy(navigateToCreation = slotId)
            }
        }
    }

    fun onConfirmOverwrite() {
        val slotId = _uiState.value.pendingOverwriteSlotId ?: return
        viewModelScope.launch {
            characterRepository.clearSlot(slotId)
            _uiState.update {
                it.copy(
                    pendingOverwriteSlotId = null,
                    navigateToCreation = slotId
                )
            }
        }
    }

    fun onDismissOverwrite() {
        _uiState.update { it.copy(pendingOverwriteSlotId = null) }
    }

    fun onNavigationHandled() {
        _uiState.update {
            it.copy(
                navigateToLife = null,
                navigateToCreation = null,
                navigateToSummary = null
            )
        }
    }

    fun formatSlotNumber(slotId: Int): String =
        context.getString(R.string.format_slot_number, slotId + 1)

    fun formatSlotDisplayName(slot: SlotSummary): String = when {
        slot.isCorrupted -> context.getString(R.string.slot_save_data_issue)
        slot.isEmpty -> context.getString(R.string.slot_empty)
        slot.alive == true -> slot.name.orEmpty()
        else -> context.getString(R.string.format_slot_deceased, slot.name.orEmpty())
    }

    fun formatSlotAgeLabel(slot: SlotSummary): String? {
        if (slot.isEmpty || slot.age == null) return null
        val age = context.getString(R.string.format_age, slot.age)
        return if (slot.alive == true) {
            age
        } else {
            context.getString(R.string.format_died_at_age, age)
        }
    }

    fun formatOverwriteDialogBody(slotId: Int, slot: SlotSummary?): String {
        val saveName = if (slot?.name != null) {
            context.getString(R.string.format_slot_named_save, slot.name)
        } else {
            context.getString(R.string.dialog_overwrite_existing_save)
        }
        return context.getString(R.string.dialog_overwrite_slot_body, slotId + 1, saveName)
    }
}
