package com.maisha.game.domain

import com.maisha.game.data.model.Character

/**
 * Pure helper that turns a before/after Age Up pair into a short list of recap facts.
 * Localization happens in the UI layer.
 */
object YearRecapBuilder {

    enum class FactType {
        CASH_UP,
        CASH_DOWN,
        SAVINGS_INTEREST,
        PORTFOLIO_UP,
        PORTFOLIO_DOWN,
        VISA_TICK,
        CULTURE_SHOCK,
        ILLNESS,
        PROMOTION,
        QUEST_STREAK,
        QUESTS_DONE
    }

    data class Fact(
        val type: FactType,
        /** Absolute cash / count value used for formatting. */
        val value: Int = 0
    )

    fun build(
        before: Character,
        after: Character,
        promoted: Boolean = false,
        questsCompleted: Int = 0,
        questStreak: Int = 0,
        maxLines: Int = 4
    ): List<Fact> {
        val facts = mutableListOf<Fact>()

        val moneyDelta = after.stats.money - before.stats.money
        when {
            moneyDelta > 0 -> facts += Fact(FactType.CASH_UP, moneyDelta)
            moneyDelta < 0 -> facts += Fact(FactType.CASH_DOWN, -moneyDelta)
        }

        val savingsDelta = after.savingsBalance - before.savingsBalance
        if (savingsDelta > 0) {
            facts += Fact(FactType.SAVINGS_INTEREST, savingsDelta)
        }

        val portfolioDelta = after.investmentPortfolioValue - before.investmentPortfolioValue
        when {
            portfolioDelta > 0 -> facts += Fact(FactType.PORTFOLIO_UP, portfolioDelta)
            portfolioDelta < 0 -> facts += Fact(FactType.PORTFOLIO_DOWN, -portfolioDelta)
        }

        if (after.isLivingAbroad()) {
            if (before.visaYearsRemaining > after.visaYearsRemaining) {
                facts += Fact(FactType.VISA_TICK, after.visaYearsRemaining.coerceAtLeast(0))
            }
            if (after.stats.happiness < before.stats.happiness) {
                facts += Fact(FactType.CULTURE_SHOCK)
            }
        }

        val newIllness = after.activeConditions.any { condition ->
            before.activeConditions.none { it.id == condition.id }
        }
        if (newIllness || after.stats.health <= before.stats.health - 8) {
            facts += Fact(FactType.ILLNESS)
        }

        if (promoted) {
            facts += Fact(FactType.PROMOTION)
        }

        if (questStreak > 0 && questsCompleted > 0) {
            facts += Fact(FactType.QUEST_STREAK, questStreak)
        } else if (questsCompleted > 0) {
            facts += Fact(FactType.QUESTS_DONE, questsCompleted)
        }

        return facts.take(maxLines)
    }
}
