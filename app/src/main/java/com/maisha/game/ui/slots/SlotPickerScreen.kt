// app/src/main/java/com/maisha/game/ui/slots/SlotPickerScreen.kt
package com.maisha.game.ui.slots

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.local.SlotSummary
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.ui.avatar.AvatarImage
import com.maisha.game.ui.components.AppLoadingIndicator
import com.maisha.game.ui.components.ConfirmActionDialog
import com.maisha.game.ui.components.ConfirmSeverity
import com.maisha.game.ui.components.CountryFlag
import com.maisha.game.ui.components.countryDisplayName
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.MaishaTheme
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.formatMoney
import java.util.Calendar

@Composable
fun SlotPickerScreen(
    uiState: SlotPickerUiState,
    onContinue: (Int) -> Unit,
    onViewSummary: (Int) -> Unit,
    onStartNewLife: (Int) -> Unit,
    onConfirmOverwrite: () -> Unit,
    onDismissOverwrite: () -> Unit,
    onClearCorruptedSlot: (Int) -> Unit,
    onConfirmClearCorrupted: () -> Unit,
    onDismissClearCorrupted: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeOfDayBrush = remember { timeOfDayBackgroundBrush() }
    val needsDaytimeScrim = remember { isDaytimeHour() }
    val rootModifier = modifier
        .fillMaxSize()
        .background(timeOfDayBrush)
        .then(
            if (needsDaytimeScrim) {
                Modifier.background(Color.Black.copy(alpha = 0.42f))
            } else {
                Modifier
            }
        )
        .windowInsetsPadding(WindowInsets.safeDrawing)

    if (uiState.isLoading) {
        Column(
            modifier = rootModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLoadingIndicator()
        }
        return
    }

    if (uiState.isDatabaseUnavailable) {
        DatabaseUnavailableScreen(
            onOpenSettings = onOpenSettings,
            modifier = rootModifier
        )
        return
    }

    Column(
        modifier = rootModifier.padding(horizontal = MaishaSpacing.md, vertical = MaishaSpacing.lg)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.slots, key = { it.slotId }) { slot ->
                SlotCard(
                    slot = slot,
                    onContinue = { onContinue(slot.slotId) },
                    onViewSummary = { onViewSummary(slot.slotId) },
                    onStartNewLife = { onStartNewLife(slot.slotId) },
                    onClearCorrupted = { onClearCorruptedSlot(slot.slotId) }
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

    uiState.pendingClearCorruptedSlotId?.let { slotId ->
        ConfirmActionDialog(
            title = stringResource(R.string.dialog_clear_corrupted_title),
            description = stringResource(R.string.dialog_clear_corrupted_body, slotId + 1),
            confirmLabel = stringResource(R.string.btn_clear_and_start_fresh),
            severity = ConfirmSeverity.WARNING,
            onConfirm = onConfirmClearCorrupted,
            onDismiss = onDismissClearCorrupted
        )
    }
}

@Composable
fun DatabaseUnavailableScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaishaSpacing.md, vertical = MaishaSpacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.BrokenImage,
            contentDescription = null,
            tint = CoralNegative,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.database_unavailable_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = CoralNegative
        )
        Text(
            text = stringResource(R.string.database_unavailable_message),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = stringResource(R.string.database_unavailable_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
            Text(stringResource(R.string.btn_open_settings_reset_data))
        }
    }
}

@Composable
private fun SlotCard(
    slot: SlotSummary,
    onContinue: () -> Unit,
    onViewSummary: () -> Unit,
    onStartNewLife: () -> Unit,
    onClearCorrupted: () -> Unit
) {
    when {
        slot.isCorrupted -> CorruptedSlotCard(slot = slot, onClearCorrupted = onClearCorrupted)
        slot.isEmpty -> EmptySlotCard(slotId = slot.slotId, onStartNewLife = onStartNewLife)
        else -> OccupiedSlotCard(
            slot = slot,
            onContinue = onContinue,
            onViewSummary = onViewSummary,
            onStartNewLife = onStartNewLife
        )
    }
}

@Composable
private fun OccupiedSlotCard(
    slot: SlotSummary,
    onContinue: () -> Unit,
    onViewSummary: () -> Unit,
    onStartNewLife: () -> Unit
) {
    val countryCode = slot.countryCode
    val jobLabel = when {
        slot.isRetired -> stringResource(R.string.label_retired)
        !slot.jobTitle.isNullOrBlank() -> slot.jobTitle
        else -> stringResource(R.string.career_unemployed)
    }
    val netWorthLabel = slot.netWorth?.let { worth ->
        formatMoney(worth, countryCode ?: "KE")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                AvatarImage(
                    config = slot.avatarConfig ?: AvatarConfig.DEFAULT,
                    size = 64.dp,
                    age = slot.age ?: 18,
                    forPlayerCharacter = true
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.format_slot_number, slot.slotId + 1),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (slot.alive == true) {
                            slot.name.orEmpty()
                        } else {
                            stringResource(R.string.format_slot_deceased, slot.name.orEmpty())
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (slot.age != null) {
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
                            color = if (slot.alive == true) TealPrimary else CoralNegative,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (countryCode != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            CountryFlag(countryCode = countryCode, size = 18.dp)
                            Text(
                                text = countryDisplayName(countryCode),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (netWorthLabel != null) {
                    Text(
                        text = stringResource(R.string.format_slot_net_worth, netWorthLabel),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stringResource(R.string.format_slot_job, jobLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (slot.alive == true) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = NavyDeep
                    )
                ) {
                    Text(stringResource(R.string.btn_continue), maxLines = 1)
                }
            } else {
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

@Composable
private fun EmptySlotCard(
    slotId: Int,
    onStartNewLife: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emptySlotPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emptyPulseAlpha"
    )
    val dashColor = TealPrimary.copy(alpha = 0.75f)
    val cornerRadius = 16.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )
                drawRoundRect(
                    color = dashColor,
                    style = stroke,
                    cornerRadius = CornerRadius(cornerRadius.toPx())
                )
            }
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.format_slot_number, slotId + 1),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.slot_empty),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onStartNewLife,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(pulseAlpha),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text(
                    text = stringResource(R.string.btn_create_new_life),
                    maxLines = 1,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CorruptedSlotCard(
    slot: SlotSummary,
    onClearCorrupted: () -> Unit
) {
    val errorBorder = Color(0xFF8B0000)
    val errorSurface = Color(0xFF2A0A0A)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = errorSurface),
        border = BorderStroke(2.dp, errorBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                Icon(
                    imageVector = Icons.Filled.BrokenImage,
                    contentDescription = null,
                    tint = CoralNegative,
                    modifier = Modifier.size(48.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = CoralNegative,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.slot_corrupted_warning_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = CoralNegative,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.format_slot_number, slot.slotId + 1),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.slot_save_data_issue),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CoralNegative,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.slot_could_not_load),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onClearCorrupted,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralNegative)
            ) {
                Text(
                    text = stringResource(R.string.btn_clear_and_start_fresh),
                    maxLines = 2,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun currentHourOfDay(): Int =
    Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

private fun isDaytimeHour(hour: Int = currentHourOfDay()): Boolean =
    hour in 6..17

private fun timeOfDayBackgroundBrush(): Brush {
    val hour = currentHourOfDay()
    return when (hour) {
        in 6..11 -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFD1A8), // soft peach
                Color(0xFFB8D9F0)  // light blue
            )
        )
        in 12..17 -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF4DD0E1), // cyan
                Color(0xFF1E88E5)  // blue
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0A1628), // deep navy
                Color(0xFF2D1B4E)  // purple
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Slot picker mixed states")
@Composable
private fun SlotPickerScreenPreview() {
    MaishaTheme {
        SlotPickerScreen(
            uiState = SlotPickerUiState(
                isLoading = false,
                slots = listOf(
                    SlotSummary(
                        slotId = 0,
                        name = "Amina Okello",
                        age = 34,
                        alive = true,
                        isEmpty = false,
                        countryCode = "KE",
                        avatarConfig = AvatarConfig.DEFAULT,
                        generationNumber = 2,
                        netWorth = 1_250_000,
                        jobTitle = "Software Engineer",
                        isRetired = false
                    ),
                    SlotSummary(
                        slotId = 1,
                        name = "Broken Save",
                        age = 22,
                        alive = true,
                        isEmpty = false,
                        isCorrupted = true,
                        countryCode = "NG"
                    ),
                    SlotSummary(
                        slotId = 2,
                        name = null,
                        age = null,
                        alive = null,
                        isEmpty = true
                    )
                )
            ),
            onContinue = {},
            onViewSummary = {},
            onStartNewLife = {},
            onConfirmOverwrite = {},
            onDismissOverwrite = {},
            onClearCorruptedSlot = {},
            onConfirmClearCorrupted = {},
            onDismissClearCorrupted = {},
            onOpenSettings = {}
        )
    }
}
