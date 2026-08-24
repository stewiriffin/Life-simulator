package com.maisha.game.ui.charactercreation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.maisha.game.ui.components.color
import com.maisha.game.ui.components.icon
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.InkPrimary
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.TealPrimary
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay

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

private fun extraUsed(stats: Stats): Int =
    StatField.entries.sumOf { (it.read(stats) - MIN_STAT).coerceAtLeast(0) }

private fun pointsRemaining(stats: Stats): Int = max(0, EXTRA_BUDGET - extraUsed(stats))

private fun maxAllowedValue(field: StatField, stats: Stats): Int {
    val otherExtra = StatField.entries
        .filter { it != field }
        .sumOf { (it.read(stats) - MIN_STAT).coerceAtLeast(0) }
    return (MIN_STAT + (EXTRA_BUDGET - otherExtra)).coerceIn(MIN_STAT, MAX_STAT)
}

@Composable
fun StatsCustomizationScreen(
    uiState: CharacterCreationUiState,
    onStatsChange: (Stats) -> Unit,
    onStartLife: () -> Unit
) {
    val selected = uiState.selectedStats
    val pointsLeft = pointsRemaining(selected)
    val budgetProgress by animateFloatAsState(
        targetValue = extraUsed(selected).toFloat() / EXTRA_BUDGET,
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

    fun setStatValue(field: StatField, targetValue: Int) {
        val clamped = targetValue.coerceIn(MIN_STAT, maxAllowedValue(field, selected))
        if (clamped == field.read(selected)) return
        selectedPresetRes = null
        applyStats(field.write(selected, clamped))
    }

    fun updateStat(field: StatField, delta: Int) {
        setStatValue(field, field.read(selected) + delta)
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
                pointsRemaining = pointsLeft,
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
                val value = field.read(selected)
                StatAdjustCard(
                    field = field,
                    value = value,
                    pointsOnStat = value - MIN_STAT,
                    maxValue = maxAllowedValue(field, selected),
                    canDecrease = value > MIN_STAT,
                    canIncrease = pointsLeft > 0 && value < maxAllowedValue(field, selected),
                    onValueChange = { setStatValue(field, it) },
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
    pointsOnStat: Int,
    maxValue: Int,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onValueChange: (Int) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val label = field.type.defaultLabel()
    val statColor = field.type.color()
    val statEmoji = field.type.emoji()
    val decreaseDescription = stringResource(R.string.stats_decrease, label)
    val increaseDescription = stringResource(R.string.stats_increase, label)
    val sliderEnabled = maxValue > MIN_STAT || value > MIN_STAT

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, InkPrimary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(statColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statEmoji,
                        style = MaterialTheme.typography.titleMedium,
                        color = statColor
                    )
                }
                Column {
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
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (pointsOnStat > 0) {
                    Text(
                        text = stringResource(R.string.stats_points_on_stat, pointsOnStat),
                        style = MaterialTheme.typography.labelSmall,
                        color = statColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatRepeatButton(
                sign = "−",
                enabled = canDecrease,
                contentDescription = decreaseDescription,
                onRepeat = onDecrease
            )
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.roundToInt()) },
                enabled = sliderEnabled,
                valueRange = MIN_STAT.toFloat()..maxValue.toFloat(),
                steps = (maxValue - MIN_STAT - 1).coerceAtLeast(0),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = statColor,
                    activeTrackColor = statColor,
                    inactiveTrackColor = statColor.copy(alpha = 0.18f),
                    disabledThumbColor = InkPrimary.copy(alpha = 0.25f),
                    disabledActiveTrackColor = InkPrimary.copy(alpha = 0.12f),
                    disabledInactiveTrackColor = InkPrimary.copy(alpha = 0.08f)
                )
            )
            StatRepeatButton(
                sign = "+",
                enabled = canIncrease,
                contentDescription = increaseDescription,
                filled = true,
                accentColor = statColor,
                onRepeat = onIncrease
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = MIN_STAT.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = InkPrimary.copy(alpha = 0.4f)
            )
            Text(
                text = maxValue.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = InkPrimary.copy(alpha = 0.4f)
            )
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
private fun StatRepeatButton(
    sign: String,
    enabled: Boolean,
    contentDescription: String,
    filled: Boolean = false,
    accentColor: Color = GoldAccent,
    onRepeat: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(pressed, enabled) {
        if (!pressed || !enabled) return@LaunchedEffect
        onRepeat()
        delay(320)
        var interval = 120L
        while (pressed && enabled) {
            onRepeat()
            delay(interval)
            interval = (interval * 0.88f).toLong().coerceAtLeast(35L)
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription }
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = when {
                    !enabled -> InkPrimary.copy(alpha = 0.08f)
                    filled -> accentColor
                    else -> InkPrimary.copy(alpha = 0.15f)
                },
                shape = CircleShape
            )
            .background(
                when {
                    !enabled -> Color.Transparent
                    filled -> accentColor.copy(alpha = 0.16f)
                    pressed && enabled -> InkPrimary.copy(alpha = 0.06f)
                    else -> Color.White
                }
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        pressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = sign,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) InkPrimary else InkPrimary.copy(alpha = 0.25f),
            fontWeight = FontWeight.Bold
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

@Composable
private fun StatType.emoji(): String = when (this) {
    StatType.HEALTH -> "❤️"
    StatType.HAPPINESS -> "😊"
    StatType.SMARTS -> "🧠"
    StatType.LOOKS -> "✨"
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
