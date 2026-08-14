package com.maisha.game.domain

import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.Character
import com.maisha.game.util.formatMoney
import javax.inject.Inject
import javax.inject.Singleton

enum class LeisureActivity {
    /** Child/teen: outdoor play. */
    PLAYGROUND,
    /** School-age study session. */
    STUDY_BUDDY,
    /** Chores for a small cash tip. */
    CHORES,
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
 * Cash-only leisure / childhood activities — happiness/health/smarts tradeoffs, no gambling.
 * Costs are Kenya-baseline, scaled via [EconomyScaler].
 */
@Singleton
class LeisureEngine @Inject constructor() {

    fun cost(activity: LeisureActivity, countryCode: String): Int =
        EconomyScaler.scaleAmount(baseCostKenya(activity), countryCode)

    fun isEligible(character: Character, activity: LeisureActivity): Boolean {
        if (!character.alive ||
            character.criminalRecord.currentlyIncarcerated ||
            character.criminalRecord.awaitingTrial
        ) {
            return false
        }
        val age = character.age
        return when (activity) {
            LeisureActivity.PLAYGROUND -> age in PLAYGROUND_MIN_AGE..PLAYGROUND_MAX_AGE
            LeisureActivity.STUDY_BUDDY -> age in STUDY_MIN_AGE..STUDY_MAX_AGE
            LeisureActivity.CHORES -> age in CHORES_MIN_AGE..CHORES_MAX_AGE
            LeisureActivity.NIGHT_OUT,
            LeisureActivity.NATURE_DAY,
            LeisureActivity.CITY_SHOW,
            LeisureActivity.SPA_DAY -> age >= ADULT_LEISURE_MIN_AGE
        }
    }

    fun activitiesFor(character: Character): List<LeisureActivity> =
        LeisureActivity.entries.filter { isEligible(character, it) }

    fun perform(character: Character, activity: LeisureActivity): LeisureResult {
        if (!isEligible(character, activity)) return LeisureResult.Ineligible
        val fee = cost(activity, character.countryCode)
        // Chores pay the player (negative fee stored as payout).
        if (activity != LeisureActivity.CHORES && character.stats.money < fee) {
            return LeisureResult.InsufficientFunds
        }

        val (happinessDelta, healthDelta, smartsDelta, moneyDelta) =
            deltas(activity, fee, character.countryCode)
        val updated = character.copy(
            stats = character.stats.copy(
                money = (character.stats.money + moneyDelta).coerceAtLeast(0),
                happiness = (character.stats.happiness + happinessDelta).coerceIn(0, 100),
                health = (character.stats.health + healthDelta).coerceIn(0, 100),
                smarts = (character.stats.smarts + smartsDelta).coerceIn(0, 100)
            ),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                logLine(activity, fee, character.countryCode)
            )
        )
        return LeisureResult.Success(updated, activity)
    }

    /** @deprecated Prefer [activitiesFor]. */
    fun allActivities(): List<LeisureActivity> = LeisureActivity.entries.toList()

    private fun baseCostKenya(activity: LeisureActivity): Int = when (activity) {
        LeisureActivity.PLAYGROUND -> PLAYGROUND_COST_KENYA
        LeisureActivity.STUDY_BUDDY -> STUDY_COST_KENYA
        LeisureActivity.CHORES -> 0
        LeisureActivity.NIGHT_OUT -> NIGHT_OUT_COST_KENYA
        LeisureActivity.NATURE_DAY -> NATURE_DAY_COST_KENYA
        LeisureActivity.CITY_SHOW -> CITY_SHOW_COST_KENYA
        LeisureActivity.SPA_DAY -> SPA_DAY_COST_KENYA
    }

    private fun deltas(
        activity: LeisureActivity,
        fee: Int,
        countryCode: String
    ): Quad = when (activity) {
        LeisureActivity.PLAYGROUND -> Quad(8, 3, 0, -fee)
        LeisureActivity.STUDY_BUDDY -> Quad(2, 0, 5, -fee)
        LeisureActivity.CHORES -> Quad(
            -2,
            0,
            0,
            EconomyScaler.scaleAmount(CHORES_PAY_KENYA, countryCode)
        )
        LeisureActivity.NIGHT_OUT -> Quad(10, -3, 0, -fee)
        LeisureActivity.NATURE_DAY -> Quad(8, 4, 0, -fee)
        LeisureActivity.CITY_SHOW -> Quad(9, 0, 0, -fee)
        LeisureActivity.SPA_DAY -> Quad(7, 6, 0, -fee)
    }

    private fun logLine(activity: LeisureActivity, fee: Int, countryCode: String): String {
        val money = formatMoney(fee, countryCode)
        val chorePay = formatMoney(
            EconomyScaler.scaleAmount(CHORES_PAY_KENYA, countryCode),
            countryCode
        )
        return when (activity) {
            LeisureActivity.PLAYGROUND -> "You played outside with friends."
            LeisureActivity.STUDY_BUDDY -> "You spent $money on a study session."
            LeisureActivity.CHORES -> "You did chores and earned $chorePay."
            LeisureActivity.NIGHT_OUT -> "You spent $money on a night out."
            LeisureActivity.NATURE_DAY -> "You spent $money on a day in nature."
            LeisureActivity.CITY_SHOW -> "You spent $money on tickets to a city show."
            LeisureActivity.SPA_DAY -> "You spent $money on a spa day."
        }
    }

    private data class Quad(
        val happiness: Int,
        val health: Int,
        val smarts: Int,
        val money: Int
    )

    companion object {
        const val PLAYGROUND_MIN_AGE = 5
        const val PLAYGROUND_MAX_AGE = 13
        const val STUDY_MIN_AGE = 6
        const val STUDY_MAX_AGE = 17
        const val CHORES_MIN_AGE = 8
        const val CHORES_MAX_AGE = 16
        const val ADULT_LEISURE_MIN_AGE = 14
        /** @deprecated Use age-gated [activitiesFor]. */
        const val MIN_LEISURE_AGE = 5

        const val PLAYGROUND_COST_KENYA = 500
        const val STUDY_COST_KENYA = 1_500
        const val CHORES_PAY_KENYA = 2_000
        const val NIGHT_OUT_COST_KENYA = 8_000
        const val NATURE_DAY_COST_KENYA = 3_500
        const val CITY_SHOW_COST_KENYA = 6_000
        const val SPA_DAY_COST_KENYA = 15_000
    }
}
