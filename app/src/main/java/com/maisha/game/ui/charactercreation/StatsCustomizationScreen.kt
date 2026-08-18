package com.maisha.game.ui.charactercreation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.model.Stats
import com.maisha.game.ui.components.StatBar
import com.maisha.game.ui.components.StatType
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.InkPrimary
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.TealPrimary
import kotlin.math.max
import kotlin.random.Random

private const val MIN_STAT = 40
private const val MAX_STAT = 100
private const val EXTRA_BUDGET = 140
private const val CREATION_STEPS = 3

private enum class StatField(val type: StatType, val hintRes: Int) {
    HEALTH(StatType.HEALTH, R.string.stats_hint_health),
    HAPPINESS(StatType.HAPPINESS, R.string.stats_hint_happiness),
    SMARTS(StatType.SMARTS, R.string.stats_hint_smarts),
    LOOKS(StatType.LOOKS, R.string.stats_hint_looks);

    fun read(stats: Stats): Int = when (this) {
        HEALTH -> stats.health
        HAPPINESS -> stats.happiness
        SMARTS -> stats.smarts
        LOOKS -> stats.looks
    }

    fun write(stats: Stats, value: Int): Stats = when (this) {
        HEALTH -> stats.copy(health = value)
        HAPPINESS -> stats.copy(happiness = value)
        SMARTS -> stats.copy(smarts = value)
        LOOKS -> stats.copy(looks = value)
    }
}

private data class StatPreset(val labelRes: Int, val health: Int, val happiness: Int, val smarts: Int, val looks: Int)

private val STAT_PRESETS = listOf(
    StatPreset(R.string.stats_preset_balanced, 75, 75, 75, 75),
    StatPreset(R.string.stats_preset_brainy, 60, 65, 95, 80),
    StatPreset(R.string.stats_preset_fit, 95, 70, 55, 80),
    StatPreset(R.string.stats_preset_charming, 65, 85, 60, 90)
)

@Composable
fun StatsCustomizationScreen(
    uiState: CharacterCreationUiState,
    onStatsChange: (Stats) -> Unit,
    onStartLife: () -> Unit
) {
    val selected = uiState.selectedStats
    val extraUsed = StatField.entries.sumOf { (it.read(selected) - MIN_STAT).coerceAtLeast(0) }
    val pointsRemaining = max(0, EXTRA_BUDGET - extraUsed)
    val budgetProgress by animateFloatAsState(
        targetValue = extraUsed.toFloat() / EXTRA_BUDGET,
        animationSpec = tween(durationMillis = 350, easing = EaseOutCubic),
        label = "budgetProgress"
    )
    var selectedPresetRes by remember { mutableStateOf<Int?>(null) }

    fun applyStats(stats: Stats) {
        onStatsChange(
            stats.copy(
                health = stats.health.coerceIn(MIN_STAT, MAX_STAT),
                happiness = stats.happiness.coerceIn(MIN_STAT, MAX_STAT),
                smarts = stats.smarts.coerceIn(MIN_STAT, MAX_STAT),
                looks = stats.looks.coerceIn(MIN_STAT, MAX_STAT),
                money = 0,
                karma = 50
            )
        )
    }

    fun updateStat(field: StatField, delta: Int) {
        val current = field.read(selected)
        val next = (current + delta).coerceIn(MIN_STAT, MAX_STAT)
        if (next == current) return

        val draft = field.write(selected, next)
        val newExtraUsed = StatField.entries.sumOf { (it.read(draft) - MIN_STAT).coerceAtLeast(0) }
        if (newExtraUsed > EXTRA_BUDGET) return

        selectedPresetRes = null
        applyStats(draft)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CreationStepIndicator(activeStep = 3, totalSteps = CREATION_STEPS)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.stats_picker_title),
                style = MaterialTheme.typography.headlineSmall,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.stats_picker_subtitle, EXTRA_BUDGET),
                style = MaterialTheme.typography.bodyMedium,
                color = InkPrimary.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            PointsBudgetCard(
                pointsRemaining = pointsRemaining,
                budgetProgress = budgetProgress
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.stats_quick_start),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelLarge,
                color = InkPrimary.copy(alpha = 0.55f),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(STAT_PRESETS, key = { it.labelRes }) { preset ->
                    FilterChip(
                        selected = selectedPresetRes == preset.labelRes,
                        onClick = {
                            selectedPresetRes = preset.labelRes
                            applyStats(
                                Stats(
                                    health = preset.health,
                                    happiness = preset.happiness,
                                    smarts = preset.smarts,
                                    looks = preset.looks,
                                    money = 0,
                                    karma = 50
                                )
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(preset.labelRes),
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAccent.copy(alpha = 0.25f),
                            selectedLabelColor = InkPrimary,
                            containerColor = Color.White,
                            labelColor = InkPrimary.copy(alpha = 0.75f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedPresetRes == preset.labelRes,
                            borderColor = InkPrimary.copy(alpha = 0.12f),
                            selectedBorderColor = GoldAccent
                        )
                    )
                }
                item(key = "random") {
                    FilterChip(
                        selected = selectedPresetRes == R.string.stats_preset_random,
                        onClick = {
                            selectedPresetRes = R.string.stats_preset_random
                            applyStats(randomValidStats())
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.stats_preset_random),
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = InkPrimary,
                            containerColor = Color.White,
                            labelColor = InkPrimary.copy(alpha = 0.75f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedPresetRes == R.string.stats_preset_random,
                            borderColor = InkPrimary.copy(alpha = 0.12f),
                            selectedBorderColor = TealPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            StatField.entries.forEach { field ->
                StatAdjustCard(
                    field = field,
                    value = field.read(selected),
                    canDecrease = field.read(selected) > MIN_STAT,
                    canIncrease = pointsRemaining > 0 && field.read(selected) < MAX_STAT,
                    onDecrease = { updateStat(field, -1) },
                    onIncrease = { updateStat(field, +1) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Button(
            onClick = onStartLife,
            enabled = !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NavyDeep)
        ) {
            Text(
                text = if (uiState.isSaving) {
                    stringResource(R.string.btn_starting)
                } else {
                    stringResource(R.string.btn_start_life)
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CreationStepIndicator(activeStep: Int, totalSteps: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.stats_picker_step),
            style = MaterialTheme.typography.labelMedium,
            color = TealPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { index ->
                val step = index + 1
                val active = step <= activeStep
                val current = step == activeStep
                Box(
                    modifier = Modifier
                        .then(
                            if (current) Modifier.width(28.dp) else Modifier.size(8.dp)
                        )
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                current -> GoldAccent
                                active -> TealPrimary.copy(alpha = 0.45f)
                                else -> InkPrimary.copy(alpha = 0.12f)
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun PointsBudgetCard(pointsRemaining: Int, budgetProgress: Float) {
    val pointsColor by animateColorAsState(
        targetValue = when {
            pointsRemaining == 0 -> TealPrimary
            pointsRemaining <= 30 -> GoldAccent
            else -> InkPrimary
        },
        animationSpec = tween(250),
        label = "pointsColor"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(InkPrimary.copy(alpha = 0.04f))
            .border(1.dp, GoldAccent.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (pointsRemaining > 0) {
                    stringResource(R.string.stats_points_remaining, pointsRemaining)
                } else {
                    stringResource(R.string.stats_points_all_used)
                },
                style = MaterialTheme.typography.titleMedium,
                color = pointsColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${EXTRA_BUDGET - pointsRemaining} / $EXTRA_BUDGET",
                style = MaterialTheme.typography.labelMedium,
                color = InkPrimary.copy(alpha = 0.5f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(InkPrimary.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(budgetProgress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(GoldAccent)
            )
        }
    }
}

@Composable
private fun StatAdjustCard(
    field: StatField,
    value: Int,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val label = field.type.defaultLabel()
    val decreaseDescription = stringResource(R.string.stats_decrease, label)
    val increaseDescription = stringResource(R.string.stats_increase, label)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, InkPrimary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(field.hintRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkPrimary.copy(alpha = 0.55f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatCircleButton(
                    icon = Icons.Filled.Remove,
                    enabled = canDecrease,
                    contentDescription = decreaseDescription,
                    onClick = onDecrease
                )
                Text(
                    text = value.toString(),
                    modifier = Modifier.width(36.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                StatCircleButton(
                    icon = Icons.Filled.Add,
                    enabled = canIncrease,
                    contentDescription = increaseDescription,
                    filled = true,
                    onClick = onIncrease
                )
            }
        }
        StatBar(
            type = field.type,
            value = value,
            showIcon = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    contentDescription: String,
    filled: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .semantics { this.contentDescription = contentDescription }
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = when {
                    !enabled -> InkPrimary.copy(alpha = 0.08f)
                    filled -> GoldAccent
                    else -> InkPrimary.copy(alpha = 0.15f)
                },
                shape = CircleShape
            )
            .background(
                when {
                    !enabled -> Color.Transparent
                    filled -> GoldAccent.copy(alpha = 0.18f)
                    else -> Color.White
                }
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) InkPrimary else InkPrimary.copy(alpha = 0.25f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StatType.defaultLabel(): String = when (this) {
    StatType.HEALTH -> stringResource(R.string.stat_health)
    StatType.HAPPINESS -> stringResource(R.string.stat_happiness)
    StatType.SMARTS -> stringResource(R.string.stat_smarts)
    StatType.LOOKS -> stringResource(R.string.stat_looks)
    else -> ""
}

private fun randomValidStats(): Stats {
    val values = IntArray(4) { MIN_STAT }
    var remaining = EXTRA_BUDGET
    repeat(EXTRA_BUDGET) {
        if (remaining <= 0) return@repeat
        val candidates = values.indices.filter { values[it] < MAX_STAT }
        if (candidates.isEmpty()) return@repeat
        val pick = candidates[Random.nextInt(candidates.size)]
        values[pick]++
        remaining--
    }
    return Stats(
        health = values[0],
        happiness = values[1],
        smarts = values[2],
        looks = values[3],
        money = 0,
        karma = 50
    )
}
