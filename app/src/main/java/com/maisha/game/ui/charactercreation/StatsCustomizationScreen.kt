package com.maisha.game.ui.charactercreation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.model.Stats
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.InkPrimary
import kotlin.math.max

private const val MIN_STAT = 40
private const val MAX_STAT = 100
private const val EXTRA_BUDGET = 240 // (100-40) * 4
private const val TOTAL_BUDGET = MIN_STAT * 4 + EXTRA_BUDGET // 280

@Composable
fun StatsCustomizationScreen(
    uiState: CharacterCreationUiState,
    onStatsChange: (Stats) -> Unit,
    onStartLife: () -> Unit
) {
    val selected = uiState.selectedStats
    val extraUsed = (selected.health - MIN_STAT) +
        (selected.happiness - MIN_STAT) +
        (selected.smarts - MIN_STAT) +
        (selected.looks - MIN_STAT)
    val pointsRemaining = max(0, EXTRA_BUDGET - extraUsed)

    fun updateStat(which: String, delta: Int) {
        val current = when (which) {
            "health" -> selected.health
            "happiness" -> selected.happiness
            "smarts" -> selected.smarts
            else -> selected.looks
        }

        val next = (current + delta).coerceIn(MIN_STAT, MAX_STAT)
        if (next == current) return

        // Check budget: increasing consumes points, decreasing refunds.
        val newExtraUsed = (if (which == "health") next else selected.health) - MIN_STAT +
            (if (which == "happiness") next else selected.happiness) - MIN_STAT +
            (if (which == "smarts") next else selected.smarts) - MIN_STAT +
            (if (which == "looks") next else selected.looks) - MIN_STAT

        val remainingAfter = EXTRA_BUDGET - newExtraUsed
        if (remainingAfter < 0) return

        val updated = when (which) {
            "health" -> selected.copy(health = next)
            "happiness" -> selected.copy(happiness = next)
            "smarts" -> selected.copy(smarts = next)
            else -> selected.copy(looks = next)
        }
        onStatsChange(updated)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Customize your stats",
            style = MaterialTheme.typography.headlineSmall,
            color = InkPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Budget: $TOTAL_BUDGET total • $pointsRemaining remaining",
            style = MaterialTheme.typography.bodyMedium,
            color = InkPrimary.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(18.dp))

        StatStepper(
            label = stringResource(R.string.stat_health),
            value = selected.health,
            pointsRemaining = pointsRemaining,
            onMinus = { updateStat("health", -1) },
            onPlus = { updateStat("health", +1) }
        )
        StatStepper(
            label = stringResource(R.string.stat_happiness),
            value = selected.happiness,
            pointsRemaining = pointsRemaining,
            onMinus = { updateStat("happiness", -1) },
            onPlus = { updateStat("happiness", +1) }
        )
        StatStepper(
            label = stringResource(R.string.stat_smarts),
            value = selected.smarts,
            pointsRemaining = pointsRemaining,
            onMinus = { updateStat("smarts", -1) },
            onPlus = { updateStat("smarts", +1) }
        )
        StatStepper(
            label = stringResource(R.string.stat_looks),
            value = selected.looks,
            pointsRemaining = pointsRemaining,
            onMinus = { updateStat("looks", -1) },
            onPlus = { updateStat("looks", +1) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onStartLife,
            enabled = !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = InkPrimary)
        ) {
            Text(
                text = if (uiState.isSaving) stringResource(R.string.btn_starting) else stringResource(R.string.btn_start_life),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatStepper(
    label: String,
    value: Int,
    pointsRemaining: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onMinus,
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = InkPrimary),
                elevation = null
            ) {
                Icon(imageVector = Icons.Filled.Remove, contentDescription = null, tint = InkPrimary)
            }
            Button(
                onClick = onPlus,
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = pointsRemaining > 0 && value < MAX_STAT,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = InkPrimary),
                elevation = null
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = InkPrimary)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}

