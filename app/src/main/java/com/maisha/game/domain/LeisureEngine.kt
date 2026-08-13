package com.maisha.game.domain

import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.Character
import com.maisha.game.util.formatMoney
import javax.inject.Inject
import javax.inject.Singleton

enum class LeisureActivity {
    NIGHT_OUT,
    NATURE_DAY,
    CITY_SHOW,
    SPA_DAY
}

sealed class LeisureResult {
    data class Success(val character: Character, val activity: LeisureActivity) : LeisureResult()
    data object InsufficientFunds : LeisureResult()
    data object Ineligible : LeisureResult()
}

/**
 * Cash-only leisure spends — happiness/health tradeoffs, no gambling.
 * Costs are Kenya-baseline, scaled via [EconomyScaler].
 */
@Singleton
class LeisureEngine @Inject constructor() {

    fun cost(activity: LeisureActivity, countryCode: String): Int =
        EconomyScaler.scaleAmount(baseCostKenya(activity), countryCode)

    fun perform(character: Character, activity: LeisureActivity): LeisureResult {
        if (!character.alive ||
            character.criminalRecord.currentlyIncarcerated ||
            character.criminalRecord.awaitingTrial
        ) {
            return LeisureResult.Ineligible
        }
        val fee = cost(activity, character.countryCode)
        if (character.stats.money < fee) return LeisureResult.InsufficientFunds

        val (happinessDelta, healthDelta) = deltas(activity)
        val updated = character.copy(
            stats = character.stats.copy(
                money = character.stats.money - fee,
                happiness = (character.stats.happiness + happinessDelta).coerceIn(0, 100),
                health = (character.stats.health + healthDelta).coerceIn(0, 100)
            ),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                logLine(activity, fee, character.countryCode)
            )
        )
        return LeisureResult.Success(updated, activity)
    }

    fun allActivities(): List<LeisureActivity> = LeisureActivity.entries.toList()

    private fun baseCostKenya(activity: LeisureActivity): Int = when (activity) {
        LeisureActivity.NIGHT_OUT -> NIGHT_OUT_COST_KENYA
        LeisureActivity.NATURE_DAY -> NATURE_DAY_COST_KENYA
        LeisureActivity.CITY_SHOW -> CITY_SHOW_COST_KENYA
        LeisureActivity.SPA_DAY -> SPA_DAY_COST_KENYA
    }

    private fun deltas(activity: LeisureActivity): Pair<Int, Int> = when (activity) {
        LeisureActivity.NIGHT_OUT -> 10 to -3
        LeisureActivity.NATURE_DAY -> 8 to 4
        LeisureActivity.CITY_SHOW -> 9 to 0
        LeisureActivity.SPA_DAY -> 7 to 6
    }

    private fun logLine(activity: LeisureActivity, fee: Int, countryCode: String): String {
        val money = formatMoney(fee, countryCode)
        return when (activity) {
            LeisureActivity.NIGHT_OUT -> "You spent $money on a night out."
            LeisureActivity.NATURE_DAY -> "You spent $money on a day in nature."
            LeisureActivity.CITY_SHOW -> "You spent $money on tickets to a city show."
            LeisureActivity.SPA_DAY -> "You spent $money on a spa day."
        }
    }

    companion object {
        const val NIGHT_OUT_COST_KENYA = 8_000
        const val NATURE_DAY_COST_KENYA = 3_500
        const val CITY_SHOW_COST_KENYA = 6_000
        const val SPA_DAY_COST_KENYA = 15_000
        const val MIN_LEISURE_AGE = 14
    }
}
