// app/src/main/java/com/maisha/game/ui/components/EventDialogStyled.kt
package com.maisha.game.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maisha.game.R
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.Expression
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.Stats
import com.maisha.game.ui.avatar.AvatarImage
import com.maisha.game.ui.avatar.EventNpcResolver
import com.maisha.game.ui.avatar.ExpressionResolver
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.MaishaTheme
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.TealPrimary
import kotlinx.coroutines.delay

private const val CHOICE_FLASH_MS = 150L

@Composable
fun EventDialogStyled(
    event: LifeEvent,
    character: Character,
    playerAvatarConfig: AvatarConfig,
    playerAge: Int,
    playerExpression: Expression = Expression.NEUTRAL,
    onChoiceSelected: (EventChoice) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var selectionLocked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = tween(durationMillis = 220),
        label = "eventDialogScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "eventDialogAlpha"
    )

    val npc: Person? = EventNpcResolver.resolveNpc(character, event)
    val displayConfig = npc?.avatarConfig ?: playerAvatarConfig
    val displayAge = npc?.age ?: playerAge
    val promptExpression = remember(event.id) {
        ExpressionResolver.expressionForEventPrompt(event)
    }
    val displayExpression = npc?.let { ExpressionResolver.resolvePersonExpression(it) }
        ?: promptExpression.takeUnless { it == Expression.NEUTRAL }
        ?: playerExpression

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
                .background(NavyDeep.copy(alpha = 0.45f))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .scale(scale)
                    .alpha(alpha),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    EventDialogHeader(event = event)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AvatarImage(
                            config = displayConfig,
                            size = 64.dp,
                            age = displayAge,
                            expression = displayExpression
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = event.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        event.choices.forEach { choice ->
                            EventChoiceButton(
                                label = choice.label,
                                enabled = !selectionLocked,
                                onSelectStart = { selectionLocked = true },
                                onClick = { onChoiceSelected(choice) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventChoiceButton(
    label: String,
    enabled: Boolean,
    onSelectStart: () -> Unit,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var flashing by remember { mutableStateOf(false) }
    var pendingClick by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed || flashing) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "eventChoiceScale"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (flashing) TealPrimary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 80),
        label = "eventChoiceFlash"
    )
    val contentColor by animateColorAsState(
        targetValue = if (flashing) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 80),
        label = "eventChoiceContent"
    )

    LaunchedEffect(pendingClick) {
        if (pendingClick) {
            flashing = true
            delay(CHOICE_FLASH_MS)
            onClick()
            pendingClick = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .semantics { role = Role.Button }
            .pointerInput(enabled) {
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
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun EventDialogHeader(event: LifeEvent) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
            text = eventCategoryTitle(event),
            style = MaterialTheme.typography.labelMedium,
            color = GoldAccent
        )
        Text(
            text = stringResource(R.string.event_dialog_subtitle),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun eventCategoryTitle(event: LifeEvent): String {
    val tags = event.tags
    return when {
        "education" in tags -> stringResource(R.string.event_category_education)
        "career" in tags -> stringResource(R.string.event_category_career)
        "finance" in tags -> stringResource(R.string.event_category_finance)
        "relationship" in tags -> stringResource(R.string.event_category_relationships)
        "family" in tags -> stringResource(R.string.event_category_family)
        else -> stringResource(R.string.event_category_life)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640, name = "Event dialog angry")
@Composable
private fun EventDialogStyledPreview() {
    val event = LifeEvent(
        id = "preview_negative",
        minAge = 18,
        maxAge = 80,
        text = "Your landlord raises the rent without warning and gives you one week to decide. " +
            "Friends offer advice, but none of it is free. The market is brutal and your savings " +
            "are thinner than you hoped. Neighbours whisper about who will be next.",
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
                statEffects = mapOf("happiness" to -4, "money" to -2000)
            )
        ),
        tags = listOf("finance", "general")
    )
    val character = Character(
        name = "Amina",
        age = 32,
        gender = Gender.FEMALE,
        stats = Stats(happiness = 40),
        birthYear = 1994,
        avatarConfig = AvatarConfig.DEFAULT.copy(skinTone = 4, hairStyle = 2)
    )
    MaishaTheme {
        EventDialogStyled(
            event = event,
            character = character,
            playerAvatarConfig = character.avatarConfig,
            playerAge = character.age,
            playerExpression = Expression.ANGRY,
            onChoiceSelected = {}
        )
    }
}
