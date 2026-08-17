package com.maisha.game.domain

import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.Character
import com.maisha.game.util.formatMoney
import javax.inject.Inject
import javax.inject.Singleton

enum class LeisureActivity {
    /** Toddler: bedtime stories. */
    STORYTIME,
    /** Toddler: outing with a caregiver. */
    PARK_CAREGIVER,
    /** Toddler: nap / routine. */
    NAP_ROUTINE,
    /** Toddler: playdate. */
    PLAYDATE,
    /** Child/teen: outdoor play. */
    PLAYGROUND,
    /** School-age study session. */
    STUDY_BUDDY,
    /** Chores for a small cash tip. */
    CHORES,
    NIGHT_OUT,
    NATURE_DAY,
    CITY_SHOW,
    SPA_DAY,
    /** Mid-life: wellness retreat. */
    WEEKEND_RETREAT,
    /** Mid-life: dinner with old friends. */
    REUNION_DINNER,
    /** Senior: day with grandchildren. */
    GRANDKIDS_DAY,
    /** Senior: community club meetup. */
    COMMUNITY_CLUB,
    /** Senior: memoir / life writing. */
    MEMOIR_WRITING,
    /** Mind & body: library reading. */
    LIBRARY_VISIT,
    /** Mind & body: meditation / calm. */
    MEDITATION,
    /** Mind & body: annual health checkup. */
    ANNUAL_CHECKUP
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
            LeisureActivity.STORYTIME -> age in STORYTIME_MIN_AGE..STORYTIME_MAX_AGE
            LeisureActivity.PARK_CAREGIVER -> age in PARK_MIN_AGE..TODDLER_MAX_AGE
            LeisureActivity.NAP_ROUTINE -> age in NAP_MIN_AGE..NAP_MAX_AGE
            LeisureActivity.PLAYDATE -> age in PLAYDATE_MIN_AGE..TODDLER_MAX_AGE
            LeisureActivity.PLAYGROUND -> age in PLAYGROUND_MIN_AGE..PLAYGROUND_MAX_AGE
            LeisureActivity.STUDY_BUDDY -> age in STUDY_MIN_AGE..STUDY_MAX_AGE
            LeisureActivity.CHORES -> age in CHORES_MIN_AGE..CHORES_MAX_AGE
            LeisureActivity.NIGHT_OUT,
            LeisureActivity.NATURE_DAY,
            LeisureActivity.CITY_SHOW,
            LeisureActivity.SPA_DAY -> age >= ADULT_LEISURE_MIN_AGE
            LeisureActivity.WEEKEND_RETREAT,
            LeisureActivity.REUNION_DINNER -> age in MID_LIFE_LEISURE_MIN_AGE..MID_LIFE_LEISURE_MAX_AGE
            LeisureActivity.GRANDKIDS_DAY,
            LeisureActivity.COMMUNITY_CLUB,
            LeisureActivity.MEMOIR_WRITING -> age >= SENIOR_LEISURE_MIN_AGE
            LeisureActivity.LIBRARY_VISIT -> age >= MIND_BODY_MIN_AGE
            LeisureActivity.MEDITATION -> age >= MIND_BODY_MIN_AGE
            LeisureActivity.ANNUAL_CHECKUP -> age >= CHECKUP_MIN_AGE
        }
    }

    fun activitiesFor(character: Character): List<LeisureActivity> =
        LeisureActivity.entries.filter { isEligible(character, it) }

    fun perform(character: Character, activity: LeisureActivity): LeisureResult {
        if (!isEligible(character, activity)) return LeisureResult.Ineligible
        val fee = cost(activity, character.countryCode)
        if (activity != LeisureActivity.CHORES && fee > 0 && character.stats.money < fee) {
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
        LeisureActivity.STORYTIME -> STORYTIME_COST_KENYA
        LeisureActivity.PARK_CAREGIVER -> PARK_COST_KENYA
        LeisureActivity.NAP_ROUTINE -> 0
        LeisureActivity.PLAYDATE -> PLAYDATE_COST_KENYA
        LeisureActivity.PLAYGROUND -> PLAYGROUND_COST_KENYA
        LeisureActivity.STUDY_BUDDY -> STUDY_COST_KENYA
        LeisureActivity.CHORES -> 0
        LeisureActivity.NIGHT_OUT -> NIGHT_OUT_COST_KENYA
        LeisureActivity.NATURE_DAY -> NATURE_DAY_COST_KENYA
        LeisureActivity.CITY_SHOW -> CITY_SHOW_COST_KENYA
        LeisureActivity.SPA_DAY -> SPA_DAY_COST_KENYA
        LeisureActivity.WEEKEND_RETREAT -> WEEKEND_RETREAT_COST_KENYA
        LeisureActivity.REUNION_DINNER -> REUNION_DINNER_COST_KENYA
        LeisureActivity.GRANDKIDS_DAY -> GRANDKIDS_COST_KENYA
        LeisureActivity.COMMUNITY_CLUB -> COMMUNITY_CLUB_COST_KENYA
        LeisureActivity.MEMOIR_WRITING -> MEMOIR_COST_KENYA
        LeisureActivity.LIBRARY_VISIT -> LIBRARY_COST_KENYA
        LeisureActivity.MEDITATION -> 0
        LeisureActivity.ANNUAL_CHECKUP -> CHECKUP_COST_KENYA
    }

    private fun deltas(
        activity: LeisureActivity,
        fee: Int,
        countryCode: String
    ): Quad = when (activity) {
        LeisureActivity.STORYTIME -> Quad(6, 1, 2, -fee)
        LeisureActivity.PARK_CAREGIVER -> Quad(7, 3, 0, -fee)
        LeisureActivity.NAP_ROUTINE -> Quad(2, 4, 0, 0)
        LeisureActivity.PLAYDATE -> Quad(8, 2, 1, -fee)
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
        LeisureActivity.WEEKEND_RETREAT -> Quad(12, 5, 0, -fee)
        LeisureActivity.REUNION_DINNER -> Quad(10, 0, 1, -fee)
        LeisureActivity.GRANDKIDS_DAY -> Quad(11, 2, 0, -fee)
        LeisureActivity.COMMUNITY_CLUB -> Quad(6, 3, 1, -fee)
        LeisureActivity.MEMOIR_WRITING -> Quad(5, 0, 4, -fee)
        LeisureActivity.LIBRARY_VISIT -> Quad(3, 0, 4, -fee)
        LeisureActivity.MEDITATION -> Quad(4, 2, 0, 0)
        LeisureActivity.ANNUAL_CHECKUP -> Quad(0, 6, 0, -fee)
    }

    private fun logLine(activity: LeisureActivity, fee: Int, countryCode: String): String {
        val money = formatMoney(fee, countryCode)
        val chorePay = formatMoney(
            EconomyScaler.scaleAmount(CHORES_PAY_KENYA, countryCode),
            countryCode
        )
        return when (activity) {
            LeisureActivity.STORYTIME -> "You listened to a bedtime story."
            LeisureActivity.PARK_CAREGIVER -> "You went to the park with your caregiver."
            LeisureActivity.NAP_ROUTINE -> "You rested and recharged with a nap."
            LeisureActivity.PLAYDATE -> "You had a playdate with a friend."
            LeisureActivity.PLAYGROUND -> "You played outside with friends."
            LeisureActivity.STUDY_BUDDY -> "You spent $money on a study session."
            LeisureActivity.CHORES -> "You did chores and earned $chorePay."
            LeisureActivity.NIGHT_OUT -> "You spent $money on a night out."
            LeisureActivity.NATURE_DAY -> "You spent $money on a day in nature."
            LeisureActivity.CITY_SHOW -> "You spent $money on tickets to a city show."
            LeisureActivity.SPA_DAY -> "You spent $money on a spa day."
            LeisureActivity.WEEKEND_RETREAT -> "You spent $money on a weekend retreat."
            LeisureActivity.REUNION_DINNER -> "You spent $money on a reunion dinner."
            LeisureActivity.GRANDKIDS_DAY -> "You spent the day with grandchildren."
            LeisureActivity.COMMUNITY_CLUB -> "You joined a community club meetup ($money)."
            LeisureActivity.MEMOIR_WRITING -> "You worked on your life story ($money)."
            LeisureActivity.LIBRARY_VISIT -> "You spent time reading at the library ($money)."
            LeisureActivity.MEDITATION -> "You meditated and cleared your mind."
            LeisureActivity.ANNUAL_CHECKUP -> "You got an annual checkup ($money)."
        }
    }

    private data class Quad(
        val happiness: Int,
        val health: Int,
        val smarts: Int,
        val money: Int
    )

    companion object {
        const val STORYTIME_MIN_AGE = 0
        const val STORYTIME_MAX_AGE = 4
        const val PARK_MIN_AGE = 1
        const val TODDLER_MAX_AGE = 4
        const val NAP_MIN_AGE = 0
        const val NAP_MAX_AGE = 3
        const val PLAYDATE_MIN_AGE = 2
        const val PLAYGROUND_MIN_AGE = 5
        const val PLAYGROUND_MAX_AGE = 13
        const val STUDY_MIN_AGE = 6
        const val STUDY_MAX_AGE = 17
        const val CHORES_MIN_AGE = 8
        const val CHORES_MAX_AGE = 16
        const val ADULT_LEISURE_MIN_AGE = 14
        const val MID_LIFE_LEISURE_MIN_AGE = 26
        const val MID_LIFE_LEISURE_MAX_AGE = 59
        const val SENIOR_LEISURE_MIN_AGE = 60
        /** @deprecated Use age-gated [activitiesFor]. */
        const val MIN_LEISURE_AGE = 0

        const val STORYTIME_COST_KENYA = 0
        const val PARK_COST_KENYA = 300
        const val PLAYDATE_COST_KENYA = 500
        const val PLAYGROUND_COST_KENYA = 500
        const val STUDY_COST_KENYA = 1_500
        const val CHORES_PAY_KENYA = 2_000
        const val NIGHT_OUT_COST_KENYA = 8_000
        const val NATURE_DAY_COST_KENYA = 3_500
        const val CITY_SHOW_COST_KENYA = 6_000
        const val SPA_DAY_COST_KENYA = 15_000
        const val WEEKEND_RETREAT_COST_KENYA = 25_000
        const val REUNION_DINNER_COST_KENYA = 12_000
        const val GRANDKIDS_COST_KENYA = 2_000
        const val COMMUNITY_CLUB_COST_KENYA = 1_500
        const val MEMOIR_COST_KENYA = 3_000
        const val LIBRARY_COST_KENYA = 500
        const val CHECKUP_COST_KENYA = 4_000
        const val MIND_BODY_MIN_AGE = 8
        const val CHECKUP_MIN_AGE = 14
    }
}
