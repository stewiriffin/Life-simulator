// app/src/main/java/com/maisha/game/ui/components/StatBar.kt (modified — AppIcons for stat glyphs)
package com.maisha.game.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.ui.theme.AppIcons
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.StatCondition
import com.maisha.game.ui.theme.StatHappiness
import com.maisha.game.ui.theme.StatHealth
import com.maisha.game.ui.theme.StatLooks
import com.maisha.game.ui.theme.StatMoney
import com.maisha.game.ui.theme.StatPerformance
import com.maisha.game.ui.theme.StatRelationship
import com.maisha.game.ui.theme.StatSmarts
import com.maisha.game.ui.theme.SuccessGreen
import com.maisha.game.util.formatMoney
import kotlinx.coroutines.delay

enum class StatType {
    HEALTH,
    HAPPINESS,
    SMARTS,
    LOOKS,
    MONEY,
    NET_WORTH,
    RELATIONSHIP,
    CONDITION,
    PERFORMANCE,
    FOLLOWERS,
    SKILL
}

fun StatType.color(): Color = when (this) {
    StatType.HEALTH -> StatHealth
    StatType.HAPPINESS -> StatHappiness
    StatType.SMARTS -> StatSmarts
    StatType.LOOKS -> StatLooks
    StatType.MONEY -> StatMoney
    StatType.NET_WORTH -> StatMoney
    StatType.RELATIONSHIP -> StatRelationship
    StatType.CONDITION -> StatCondition
    StatType.PERFORMANCE -> StatPerformance
    StatType.FOLLOWERS -> StatLooks
    StatType.SKILL -> StatSmarts
}

fun StatType.icon(): ImageVector = AppIcons.forStat(this)

@Composable
private fun StatType.defaultLabel(): String = when (this) {
    StatType.HEALTH -> stringResource(R.string.stat_health)
    StatType.HAPPINESS -> stringResource(R.string.stat_happiness)
    StatType.SMARTS -> stringResource(R.string.stat_smarts)
    StatType.LOOKS -> stringResource(R.string.stat_looks)
    StatType.MONEY -> stringResource(R.string.stat_money)
    StatType.NET_WORTH -> stringResource(R.string.label_net_worth)
    StatType.RELATIONSHIP -> stringResource(R.string.stat_relationship)
    StatType.CONDITION -> stringResource(R.string.stat_condition)
    StatType.PERFORMANCE -> stringResource(R.string.stat_performance)
    StatType.FOLLOWERS -> stringResource(R.string.stat_followers)
    StatType.SKILL -> stringResource(R.string.stat_skill)
}

@Composable
fun StatBar(
    type: StatType,
    value: Int,
    modifier: Modifier = Modifier,
    label: String? = null,
    maxValue: Int = 100,
    showIcon: Boolean = true,
    showBar: Boolean = true,
    displayValue: String? = null,
    barColorOverride: Color? = null
) {
    val progress = (value.toFloat() / maxValue).coerceIn(0f, 1f)
    val statAnimationSpec = tween<Float>(durationMillis = 600, easing = EaseOutCubic)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = statAnimationSpec,
        label = "statProgress"
    )

    val normalBarColor = barColorOverride ?: type.color()
    val criticalBarColor = if (barColorOverride == null && type in CRITICAL_STAT_TYPES && value < CRITICAL_THRESHOLD) {
        CoralNegative
    } else {
        normalBarColor
    }
    val barColor by animateColorAsState(
        targetValue = criticalBarColor,
        animationSpec = tween(durationMillis = 600, easing = EaseOutCubic),
        label = "statBarColor"
    )
    val resolvedLabel = label ?: type.defaultLabel()
    val resolvedValue = displayValue ?: value.toString()
    val stateDescriptionText = if (showBar && type !in VALUE_STAT_TYPES) {
        stringResource(R.string.a11y_stat_percent, resolvedLabel, value.coerceIn(0, maxValue))
    } else {
        stringResource(R.string.a11y_stat_value, resolvedLabel, resolvedValue)
    }

    var previousValue by remember { mutableIntStateOf(value) }
    var flashAlpha by remember { mutableStateOf(0f) }
    var flashTint by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(value) {
        if (value != previousValue) {
            flashTint = if (value > previousValue) SuccessGreen else CoralNegative
            flashAlpha = 0.55f
            previousValue = value
            delay(350)
            flashAlpha = 0f
            delay(100)
            flashTint = null
        }
    }

    val animatedFlashAlpha by animateFloatAsState(
        targetValue = flashAlpha,
        animationSpec = tween(durationMillis = 300, easing = EaseOutCubic),
        label = "statFlash"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                stateDescription = stateDescriptionText
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (showIcon) {
                    Icon(
                        imageVector = type.icon(),
                        contentDescription = null,
                        tint = barColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = resolvedLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = resolvedValue,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (type == StatType.MONEY) StatMoney else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
        if (showBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor)
                )
                flashTint?.let { tint ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(tint.copy(alpha = animatedFlashAlpha))
                    )
                }
            }
        }
    }
}

private val VALUE_STAT_TYPES = setOf(StatType.MONEY, StatType.NET_WORTH)

private val CRITICAL_STAT_TYPES = setOf(
    StatType.HEALTH,
    StatType.HAPPINESS,
    StatType.SMARTS,
    StatType.LOOKS,
    StatType.CONDITION,
    StatType.PERFORMANCE,
    StatType.RELATIONSHIP
)

private const val CRITICAL_THRESHOLD = 30

@Composable
fun MoneyStatRow(
    amount: Int,
    modifier: Modifier = Modifier,
    label: String? = null,
    countryCode: String = "KE"
) {
    StatBar(
        type = StatType.MONEY,
        value = 0,
        label = label ?: stringResource(R.string.stat_money),
        displayValue = formatMoney(amount, countryCode),
        showBar = false,
        modifier = modifier
    )
}
