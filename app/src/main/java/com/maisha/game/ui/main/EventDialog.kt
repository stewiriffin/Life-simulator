// app/src/main/java/com/maisha/game/ui/main/EventDialog.kt
package com.maisha.game.ui.main

import androidx.compose.runtime.Composable
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.Expression
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.ui.components.EventDialogStyled

@Composable
fun EventDialog(
    event: LifeEvent,
    character: Character,
    expression: Expression = Expression.NEUTRAL,
    onChoiceSelected: (EventChoice) -> Unit
) {
    EventDialogStyled(
        event = event,
        character = character,
        playerAvatarConfig = character.avatarConfig,
        playerAge = character.age,
        playerExpression = expression,
        onChoiceSelected = onChoiceSelected
    )
}
