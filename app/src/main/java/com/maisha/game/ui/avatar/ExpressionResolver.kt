// app/src/main/java/com/maisha/game/ui/avatar/ExpressionResolver.kt (modified — Prompt 26: looser thresholds so expressions read at card size)
package com.maisha.game.ui.avatar

import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Expression
import com.maisha.game.data.model.Person

sealed class EventOutcome {
    data class Positive(val magnitude: Int = 1) : EventOutcome()
    data class Negative(val severity: Int = 1, val isConflict: Boolean = false) : EventOutcome()
    data class Surprising(val magnitude: Int = 1) : EventOutcome()
    data object Neutral : EventOutcome()
}

object ExpressionResolver {

    fun resolveExpression(character: Character, recentEventOutcome: EventOutcome?): Expression {
        recentEventOutcome?.let { outcome ->
            when (outcome) {
                is EventOutcome.Positive -> if (outcome.magnitude >= 1) return Expression.HAPPY
                is EventOutcome.Negative -> return when {
                    outcome.isConflict -> Expression.ANGRY
                    outcome.severity >= 2 -> Expression.SAD
                    else -> Expression.SURPRISED
                }
                is EventOutcome.Surprising -> return Expression.SURPRISED
                EventOutcome.Neutral -> Unit
            }
        }

        return when {
            character.stats.happiness <= 30 -> Expression.SAD
            character.stats.happiness >= 70 -> Expression.HAPPY
            else -> Expression.NEUTRAL
        }
    }

    fun resolvePersonExpression(person: Person): Expression = when {
        person.relationshipLevel >= 65 -> Expression.HAPPY
        person.relationshipLevel <= 35 -> Expression.SAD
        else -> Expression.NEUTRAL
    }

    fun outcomeFromChoiceEffects(happinessDelta: Int, moneyDelta: Int = 0): EventOutcome {
        return when {
            happinessDelta >= 6 || moneyDelta >= 3_000 -> EventOutcome.Positive(2)
            happinessDelta >= 2 || moneyDelta > 0 -> EventOutcome.Positive(1)
            happinessDelta <= -6 -> EventOutcome.Negative(severity = 2)
            happinessDelta <= -2 -> EventOutcome.Negative(severity = 1)
            moneyDelta <= -2_000 -> EventOutcome.Surprising(2)
            else -> EventOutcome.Neutral
        }
    }

    fun outcomeFromInteraction(type: com.maisha.game.domain.InteractionType): EventOutcome = when (type) {
        com.maisha.game.domain.InteractionType.ARGUE,
        com.maisha.game.domain.InteractionType.INSULT -> EventOutcome.Negative(isConflict = true)
        com.maisha.game.domain.InteractionType.GIFT,
        com.maisha.game.domain.InteractionType.TRAVEL_TOGETHER,
        com.maisha.game.domain.InteractionType.SPEND_TIME -> EventOutcome.Positive(1)
        com.maisha.game.domain.InteractionType.PRANK -> EventOutcome.Surprising(1)
        else -> EventOutcome.Neutral
    }
}
