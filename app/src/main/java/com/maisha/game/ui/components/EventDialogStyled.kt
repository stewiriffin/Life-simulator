// app/src/main/java/com/maisha/game/ui/components/EventDialogStyled.kt
package com.maisha.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maisha.game.R
import com.maisha.game.data.FlavorInterpolator
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.Expression
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.Stats
import com.maisha.game.feedback.HapticType
import com.maisha.game.ui.avatar.AvatarImage
import com.maisha.game.ui.avatar.EventNpcResolver
import com.maisha.game.ui.avatar.ExpressionResolver
import com.maisha.game.ui.feedback.LocalFeedbackManager
import com.maisha.game.ui.theme.AccentPink
import com.maisha.game.ui.theme.AppIcons
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.CreamMuted
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.InkSecondary
import com.maisha.game.ui.theme.LifeGreen
import com.maisha.game.ui.theme.MaishaTheme
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.NavyElevated
import com.maisha.game.ui.theme.NavySurface
import com.maisha.game.ui.theme.StatHappiness
import com.maisha.game.ui.theme.StatHealth
import com.maisha.game.ui.theme.StatMoney
import com.maisha.game.ui.theme.StatRelationship
import com.maisha.game.ui.theme.StatSmarts
import com.maisha.game.ui.theme.TealLight
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.formatMoney
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val CHOICE_FLASH_MS = 140L
private const val CHOICE_STAGGER_MS = 55L

private enum class EventTone { POSITIVE, NEGATIVE, HOLIDAY, NEUTRAL }

private data class EventCategoryVisual(
    val titleRes: Int,
    val icon: ImageVector,
    val accent: Color
)

private data class ChoiceStatHint(
    val type: StatType,
    val delta: Int,
    val label: String
)

@Composable
fun EventDialogStyled(
    event: LifeEvent,
    character: Character,
    playerAvatarConfig: AvatarConfig,
    playerAge: Int,
    playerExpression: Expression = Expression.NEUTRAL,
    eventQueueIndex: Int = 1,
    eventQueueTotal: Int = 1,
    onChoiceSelected: (EventChoice) -> Unit
) {
    var visible by remember(event.id) { mutableStateOf(false) }
    var selectionLocked by remember(event.id) { mutableStateOf(false) }
    val view = LocalView.current
    val feedbackManager = LocalFeedbackManager.current

    LaunchedEffect(event.id) {
        visible = true
        feedbackManager.triggerHaptic(view, HapticType.LIGHT_TAP)
    }

    val npc: Person? = remember(event.id, character) {
        EventNpcResolver.resolveNpc(character, event)
    }
    val displayConfig = npc?.avatarConfig ?: playerAvatarConfig
    val displayAge = npc?.age ?: playerAge
    val promptExpression = remember(event.id) {
        ExpressionResolver.expressionForEventPrompt(event)
    }
    val displayExpression = npc?.let { ExpressionResolver.resolvePersonExpression(it) }
        ?: promptExpression.takeUnless { it == Expression.NEUTRAL }
        ?: playerExpression

    val category = remember(event.tags) { categoryVisualFor(event) }
    val tone = remember(event.tags) { toneFor(event) }
    val accentBarColor = remember(tone, category) { accentColorFor(tone, category) }
    val showQueue = eventQueueTotal > 1

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyDeep.copy(alpha = 0.62f))
                .padding(horizontal = 12.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(animationSpec = tween(240))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 620.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        EventDialogHeroHeader(
                            category = category,
                            tone = tone,
                            playerAge = playerAge,
                            showQueue = showQueue,
                            eventQueueIndex = eventQueueIndex,
                            eventQueueTotal = eventQueueTotal
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            EventAvatarScene(
                                config = displayConfig,
                                age = displayAge,
                                expression = displayExpression,
                                seed = npc?.id ?: character.name,
                                speakerName = npc?.name ?: character.name,
                                isPlayer = npc == null,
                                accent = category.accent
                            )
                        }

                        EventNarrativePanel(
                            text = event.text,
                            accentBarColor = accentBarColor,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        )

                        Text(
                            text = stringResource(R.string.event_choose_prompt),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = InkSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            event.choices.forEachIndexed { index, choice ->
                                EventChoiceCard(
                                    index = index + 1,
                                    choice = choice,
                                    countryCode = character.countryCode,
                                    accent = category.accent,
                                    enabled = !selectionLocked,
                                    staggerIndex = index,
                                    eventId = event.id,
                                    onSelectStart = {
                                        selectionLocked = true
                                        feedbackManager.triggerHaptic(view, HapticType.LIGHT_TAP)
                                    },
                                    onClick = { onChoiceSelected(choice) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EventDialogHeroHeader(
    category: EventCategoryVisual,
    tone: EventTone,
    playerAge: Int,
    showQueue: Boolean,
    eventQueueIndex: Int,
    eventQueueTotal: Int
) {
    val headerBrush = when (tone) {
        EventTone.HOLIDAY -> Brush.verticalGradient(
            colors = listOf(NavyElevated, NavySurface, Color(0xFF1A2840))
        )
        EventTone.NEGATIVE -> Brush.verticalGradient(
            colors = listOf(Color(0xFF2A1520), NavySurface, NavyDeep)
        )
        else -> Brush.verticalGradient(
            colors = listOf(NavyElevated, NavySurface, NavyDeep)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBrush)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(category.accent.copy(alpha = 0.18f))
                            .border(1.dp, category.accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        MaishaIcon(
                            icon = category.icon,
                            size = 22.dp,
                            tint = category.accent
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(category.titleRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = category.accent,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.event_dialog_subtitle),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.event_age_badge, playerAge),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (showQueue) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.event_queue_progress,
                            eventQueueIndex,
                            eventQueueTotal
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Text(
                        text = "${((eventQueueIndex.toFloat() / eventQueueTotal) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldAccent.copy(alpha = 0.9f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { eventQueueIndex.toFloat() / eventQueueTotal.coerceAtLeast(1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = GoldAccent,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
private fun EventAvatarScene(
    config: AvatarConfig,
    age: Int,
    expression: Expression,
    seed: String,
    speakerName: String,
    isPlayer: Boolean,
    accent: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-12).dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(NavyDeep)
                    .border(3.dp, accent.copy(alpha = 0.65f), CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                AvatarImage(
                    config = config,
                    size = 76.dp,
                    age = age,
                    expression = expression,
                    seed = seed
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (isPlayer) {
                    stringResource(R.string.event_speaker_you, speakerName)
                } else {
                    speakerName
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EventNarrativePanel(
    text: String,
    accentBarColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CreamMuted)
            .border(1.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .heightIn(min = 80.dp)
                .background(accentBarColor)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "\u201C",
                style = MaterialTheme.typography.headlineSmall,
                color = accentBarColor.copy(alpha = 0.45f),
                fontSize = 28.sp,
                lineHeight = 20.sp
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface,
                fontStyle = FontStyle.Normal
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventChoiceCard(
    index: Int,
    choice: EventChoice,
    countryCode: String,
    accent: Color,
    enabled: Boolean,
    staggerIndex: Int,
    eventId: String,
    onSelectStart: () -> Unit,
    onClick: () -> Unit
) {
    var visible by remember(eventId, index) { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    var flashing by remember { mutableStateOf(false) }
    var pendingClick by remember { mutableStateOf(false) }

    LaunchedEffect(eventId, index) {
        delay(staggerIndex * CHOICE_STAGGER_MS)
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = when {
            !visible -> 0.94f
            pressed || flashing -> 0.97f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "eventChoiceScale"
    )
    val backgroundColor by animateColorAsState(
        targetValue = when {
            flashing -> accent.copy(alpha = 0.22f)
            pressed -> accent.copy(alpha = 0.10f)
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(90),
        label = "eventChoiceBg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            flashing -> accent
            pressed -> accent.copy(alpha = 0.55f)
            else -> Color.Black.copy(alpha = 0.08f)
        },
        animationSpec = tween(90),
        label = "eventChoiceBorder"
    )

    val hints = remember(choice, countryCode) { buildChoiceStatHints(choice, countryCode) }

    LaunchedEffect(pendingClick) {
        if (pendingClick) {
            flashing = true
            delay(CHOICE_FLASH_MS)
            onClick()
            pendingClick = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .semantics { role = Role.Button }
            .pointerInput(enabled, eventId, index) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    try {
                        val up = waitForUpOrCancellation()
                        if (up != null && enabled && !pendingClick) {
                            onSelectStart()
                            pendingClick = true
                        }
                    } finally {
                        pressed = false
                    }
                }
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (flashing) 0.35f else 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = choice.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (hints.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    hints.forEach { hint ->
                        ChoiceStatChip(hint = hint)
                    }
                }
            }
        }
        Text(
            text = "\u203A",
            style = MaterialTheme.typography.headlineSmall,
            color = accent.copy(alpha = 0.65f),
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}

@Composable
private fun ChoiceStatChip(hint: ChoiceStatHint) {
    val chipColor = hint.type.color()
    val isPositive = hint.delta > 0
    val bg = if (isPositive) chipColor.copy(alpha = 0.14f) else CoralNegative.copy(alpha = 0.12f)
    val fg = if (isPositive) chipColor else CoralNegative

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MaishaStatIcon(type = hint.type, size = 14.dp)
        Text(
            text = hint.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

private fun buildChoiceStatHints(choice: EventChoice, countryCode: String): List<ChoiceStatHint> {
    val hints = mutableListOf<ChoiceStatHint>()

    fun addStat(type: StatType, delta: Int, format: (Int) -> String = ::formatStatDelta) {
        if (delta != 0) hints += ChoiceStatHint(type, delta, format(delta))
    }

    choice.statEffects.forEach { (key, delta) ->
        when (key) {
            "health" -> addStat(StatType.HEALTH, delta)
            "happiness" -> addStat(StatType.HAPPINESS, delta)
            "smarts" -> addStat(StatType.SMARTS, delta)
            "looks" -> addStat(StatType.LOOKS, delta)
            "money" -> addStat(StatType.MONEY, delta) { formatMoneyDelta(it, countryCode) }
            "karma" -> addStat(StatType.KARMA, delta)
        }
    }

    val relationshipDelta = listOf(
        choice.familyRelationshipEffect,
        choice.spouseRelationshipEffect,
        choice.childRelationshipEffect,
        choice.siblingRelationshipEffect
    ).firstOrNull { it != 0 } ?: 0
    addStat(StatType.RELATIONSHIP, relationshipDelta)

    addStat(StatType.PERFORMANCE, choice.performanceEffect)
    addStat(StatType.CONDITION, choice.conditionEffect)

    return hints
        .sortedByDescending { abs(it.delta) }
        .take(4)
}

private fun formatStatDelta(delta: Int): String = when {
    delta > 0 -> "+$delta"
    else -> delta.toString()
}

private fun formatMoneyDelta(delta: Int, countryCode: String): String {
    val formatted = formatMoney(abs(delta), countryCode)
    return if (delta > 0) "+$formatted" else "-$formatted"
}

private fun categoryVisualFor(event: LifeEvent): EventCategoryVisual {
    val tags = event.tags.map { it.lowercase() }
    return when {
        "education" in tags -> EventCategoryVisual(
            R.string.event_category_education,
            AppIcons.Education,
            StatSmarts
        )
        "career" in tags -> EventCategoryVisual(
            R.string.event_category_career,
            AppIcons.Career,
            TealPrimary
        )
        "finance" in tags -> EventCategoryVisual(
            R.string.event_category_finance,
            AppIcons.Wealth,
            StatMoney
        )
        "relationship" in tags -> EventCategoryVisual(
            R.string.event_category_relationships,
            AppIcons.Relationship,
            AccentPink
        )
        "family" in tags -> EventCategoryVisual(
            R.string.event_category_family,
            AppIcons.Family,
            StatRelationship
        )
        "crime" in tags || "prison" in tags -> EventCategoryVisual(
            R.string.event_category_crime,
            AppIcons.Mischief,
            CoralNegative
        )
        else -> EventCategoryVisual(
            R.string.event_category_life,
            AppIcons.NavLife,
            GoldAccent
        )
    }
}

private fun toneFor(event: LifeEvent): EventTone {
    val tags = event.tags.map { it.lowercase() }
    return when {
        FlavorInterpolator.HOLIDAY_TAG in tags -> EventTone.HOLIDAY
        "negative" in tags || "crime" in tags || "prison" in tags || "illness" in tags ->
            EventTone.NEGATIVE
        "death" in tags -> EventTone.NEGATIVE
        else -> {
            val happiness = event.choices.mapNotNull { it.statEffects["happiness"] }
            val avgBest = happiness.maxOrNull() ?: 0
            val avgWorst = happiness.minOrNull() ?: 0
            if (avgBest >= 6 && avgWorst >= -2) EventTone.POSITIVE else EventTone.NEUTRAL
        }
    }
}

private fun accentColorFor(tone: EventTone, category: EventCategoryVisual): Color = when (tone) {
    EventTone.NEGATIVE -> CoralNegative
    EventTone.HOLIDAY -> GoldAccent
    EventTone.POSITIVE -> LifeGreen
    EventTone.NEUTRAL -> category.accent
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720, name = "Event dialog — finance")
@Composable
private fun EventDialogStyledPreview() {
    val event = LifeEvent(
        id = "preview_negative",
        minAge = 18,
        maxAge = 80,
        text = "Your landlord raises the rent without warning and gives you one week to decide. " +
            "Friends offer advice, but none of it is free. The market is brutal and your savings " +
            "are thinner than you hoped.",
        choices = listOf(
            EventChoice(
                label = "Pay the increase",
                resultText = "You stay, poorer.",
                statEffects = mapOf("money" to -25000, "happiness" to -6)
            ),
            EventChoice(
                label = "Argue and risk eviction",
                resultText = "Things escalate.",
                statEffects = mapOf("happiness" to -10, "health" to -2)
            ),
            EventChoice(
                label = "Move in with family",
                resultText = "Pride takes a hit.",
                statEffects = mapOf("happiness" to -4, "money" to -2000),
                familyRelationshipEffect = 8
            )
        ),
        tags = listOf("finance", "negative")
    )
    val character = Character(
        name = "Amina",
        age = 32,
        gender = Gender.FEMALE,
        stats = Stats(happiness = 40),
        birthYear = 1994,
        countryCode = "US",
        avatarConfig = AvatarConfig.DEFAULT.copy(skinTone = 4, hairStyle = 2)
    )
    MaishaTheme {
        EventDialogStyled(
            event = event,
            character = character,
            playerAvatarConfig = character.avatarConfig,
            playerAge = character.age,
            playerExpression = Expression.ANGRY,
            eventQueueIndex = 2,
            eventQueueTotal = 3,
            onChoiceSelected = {}
        )
    }
}
