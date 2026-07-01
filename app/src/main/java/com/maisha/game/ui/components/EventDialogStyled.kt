// app/src/main/java/com/maisha/game/ui/components/EventDialogStyled.kt
package com.maisha.game.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maisha.game.R
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.Expression
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.Person
import com.maisha.game.ui.avatar.AvatarImage
import com.maisha.game.ui.avatar.EventNpcResolver
import com.maisha.game.ui.avatar.ExpressionResolver
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.TealPrimary

@Composable
fun EventDialogStyled(
    event: LifeEvent,
    character: Character,
    playerAvatarConfig: com.maisha.game.data.model.AvatarConfig,
    playerAge: Int,
    playerExpression: Expression = Expression.NEUTRAL,
    onChoiceSelected: (EventChoice) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
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

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .scale(scale)
                .alpha(alpha),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                EventDialogHeader(event = event)

                val npc: Person? = EventNpcResolver.resolveNpc(character, event)
                val displayConfig = npc?.avatarConfig ?: playerAvatarConfig
                val displayAge = npc?.age ?: playerAge
                val displayExpression = npc?.let { ExpressionResolver.resolvePersonExpression(it) }
                    ?: playerExpression

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    AvatarImage(
                        config = displayConfig,
                        size = 48.dp,
                        age = displayAge,
                        expression = displayExpression
                    )
                    Text(
                        text = event.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    event.choices.forEach { choice ->
                        Button(
                            onClick = { onChoiceSelected(choice) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(
                                text = choice.label,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
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
