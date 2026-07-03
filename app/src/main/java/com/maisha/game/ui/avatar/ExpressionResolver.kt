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

    /**
     * Expression shown while reading an event prompt, before a choice is made.
     * Uses tags and choice stat effects to infer tone.
     */
    fun expressionForEventPrompt(event: com.maisha.game.data.model.LifeEvent): Expression {
        val tags = event.tags.map { it.lowercase() }
        when {
            tags.any { it in setOf("crime", "prison", "death", "illness", "health") } ->
                return Expression.SAD
            tags.any { it in setOf("requires_incarcerated") } ->
                return Expression.SAD
        }
        val happinessDeltas = event.choices.mapNotNull { it.statEffects["happiness"] }
        val moneyDeltas = event.choices.mapNotNull { it.statEffects["money"] }
        val healthDeltas = event.choices.mapNotNull { it.statEffects["health"] }
        val worstHappiness = happinessDeltas.minOrNull() ?: 0
        val bestHappiness = happinessDeltas.maxOrNull() ?: 0
        val worstMoney = moneyDeltas.minOrNull() ?: 0
        val worstHealth = healthDeltas.minOrNull() ?: 0
        return when {
            worstHealth <= -5 || (worstHappiness <= -8 && bestHappiness <= 2) -> Expression.SAD
            worstHappiness <= -5 && bestHappiness <= 4 -> Expression.ANGRY
            bestHappiness >= 8 && worstHappiness >= 0 && worstMoney >= -5_000 -> Expression.HAPPY
            worstMoney <= -20_000 && bestHappiness <= 2 -> Expression.SURPRISED
            else -> Expression.NEUTRAL
        }
    }

    fun outcomeFromInteraction(type: com.maisha.game.domain.InteractionType): EventOutcome = when (type) {
        com.maisha.game.domain.InteractionType.ARGUE,
        com.maisha.game.domain.InteractionType.INSULT,
        com.maisha.game.domain.InteractionType.DISCIPLINE -> EventOutcome.Negative(isConflict = true)
        com.maisha.game.domain.InteractionType.GIFT,
        com.maisha.game.domain.InteractionType.TRAVEL_TOGETHER,
        com.maisha.game.domain.InteractionType.SPEND_TIME,
        com.maisha.game.domain.InteractionType.HELP_WITH_HOMEWORK,
        com.maisha.game.domain.InteractionType.PAY_ALLOWANCE -> EventOutcome.Positive(1)
        com.maisha.game.domain.InteractionType.PRANK -> EventOutcome.Surprising(1)
        else -> EventOutcome.Neutral
    }
}
