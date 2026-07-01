// app/src/main/java/com/maisha/game/ui/slots/SlotPickerScreen.kt (modified)
package com.maisha.game.ui.slots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.local.SlotSummary
import com.maisha.game.ui.components.AppLoadingIndicator
import com.maisha.game.ui.components.ConfirmActionDialog
import com.maisha.game.ui.components.ConfirmSeverity
import com.maisha.game.ui.components.CountryFlag
import com.maisha.game.ui.components.PersonAvatar
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.TealPrimary
@Composable
fun SlotPickerScreen(
    uiState: SlotPickerUiState,
    onContinue: (Int) -> Unit,
    onViewSummary: (Int) -> Unit,
    onStartNewLife: (Int) -> Unit,
    onConfirmOverwrite: () -> Unit,
    onDismissOverwrite: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLoadingIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaishaSpacing.md, vertical = MaishaSpacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.screen_slot_picker_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.screen_slot_picker_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.content_desc_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.slots, key = { it.slotId }) { slot ->
                SlotCard(
                    slot = slot,
                    onContinue = { onContinue(slot.slotId) },
                    onViewSummary = { onViewSummary(slot.slotId) },
                    onStartNewLife = { onStartNewLife(slot.slotId) }
                )
            }
        }
    }

    uiState.pendingOverwriteSlotId?.let { slotId ->
        val slot = uiState.slots.find { it.slotId == slotId }
        val saveLabel = if (slot?.name != null) {
            stringResource(R.string.format_slot_named_save, slot.name)
        } else {
            stringResource(R.string.dialog_overwrite_existing_save)
        }
        ConfirmActionDialog(
            title = stringResource(R.string.dialog_overwrite_slot_title),
            description = stringResource(R.string.dialog_overwrite_slot_body, slotId + 1, saveLabel),
            confirmLabel = stringResource(R.string.btn_overwrite),
            severity = ConfirmSeverity.WARNING,
            onConfirm = onConfirmOverwrite,
            onDismiss = onDismissOverwrite
        )
    }
}

@Composable
private fun SlotCard(
    slot: SlotSummary,
    onContinue: () -> Unit,
    onViewSummary: () -> Unit,
    onStartNewLife: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (slot.isEmpty) {
                    PersonAvatar(name = stringResource(R.string.avatar_unknown_initials), size = 44)
                } else {
                    PersonAvatar(
                        avatarConfig = slot.avatarConfig ?: com.maisha.game.data.model.AvatarConfig.DEFAULT,
                        size = 44,
                        age = slot.age ?: 18
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!slot.isEmpty && slot.countryCode != null) {
                            CountryFlag(countryCode = slot.countryCode, size = 16.dp)
                        }
                        Text(
                            text = stringResource(R.string.format_slot_number, slot.slotId + 1),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = when {
                            slot.isEmpty -> stringResource(R.string.slot_empty)
                            slot.alive == true -> slot.name.orEmpty()
                            else -> stringResource(
                                R.string.format_slot_deceased,
                                slot.name.orEmpty()
                            )
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!slot.isEmpty && slot.age != null) {
                        Text(
                            text = if (slot.alive == true) {
                                stringResource(R.string.format_age, slot.age)
                            } else {
                                stringResource(
                                    R.string.format_died_at_age,
                                    stringResource(R.string.format_age, slot.age)
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (slot.alive == true) {
                                TealPrimary
                            } else {
                                CoralNegative
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!slot.isEmpty && slot.generationNumber != null && slot.generationNumber > 1) {
                        Text(
                            text = stringResource(
                                R.string.format_generation_number,
                                slot.generationNumber
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            when {
                slot.isEmpty -> {
                    Button(
                        onClick = onStartNewLife,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text(stringResource(R.string.btn_start_new_life), maxLines = 1)
                    }
                }
                slot.alive == true -> {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NavyDeep)
                    ) {
                        Text(stringResource(R.string.btn_continue), maxLines = 1)
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onViewSummary,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                stringResource(R.string.btn_view_summary),
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Button(
                            onClick = onStartNewLife,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Text(
                                stringResource(R.string.btn_new_life),
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
