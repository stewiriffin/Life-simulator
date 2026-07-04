// app/src/main/java/com/maisha/game/domain/CrimeEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.model.Character
import com.maisha.game.data.model.CrimeType
import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.LawyerTier
import com.maisha.game.util.clampStat
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

sealed class CrimeResult {
    data class Success(val character: Character, val moneyGained: Int) : CrimeResult()
    data class Caught(val character: Character) : CrimeResult()
}

sealed class TrialResult {
    data class Sentenced(val character: Character, val sentenceYears: Int) : TrialResult()
    data class Acquitted(val character: Character) : TrialResult()
    data object Ineligible : TrialResult()
}

@Singleton
class CrimeEngine @Inject constructor() {

    /**
     * Attempts a crime if the character is at least [MIN_CRIME_AGE], not incarcerated, and not awaiting trial.
     *
     * Success chance scales with smarts; failure routes through [processArrest].
     */
    fun attemptCrime(character: Character, crimeType: CrimeType): CrimeResult {
        val record = character.criminalRecord
        if (record.currentlyIncarcerated || record.awaitingTrial) {
            return CrimeResult.Caught(character)
        }
        if (character.age < MIN_CRIME_AGE) {
            return CrimeResult.Caught(character)
        }

        val attemptsSoFar = record.crimeAttemptsThisYear
        val chance = successChance(character, crimeType, attemptsSoFar)
        val result = if (Random.nextFloat() < chance) {
            val moneyGained = moneyReward(crimeType)
            val updated = character.copy(
                stats = character.stats.copy(
                    money = character.stats.money + moneyGained
                )
            )
            CrimeResult.Success(updated, moneyGained)
        } else {
            CrimeResult.Caught(processArrest(character, crimeType))
        }
        return recordCrimeAttempt(result)
    }

    private fun recordCrimeAttempt(result: CrimeResult): CrimeResult {
        val bumped = { character: Character ->
            character.copy(
                criminalRecord = character.criminalRecord.copy(
                    crimeAttemptsThisYear = character.criminalRecord.crimeAttemptsThisYear + 1
                )
            )
        }
        return when (result) {
            is CrimeResult.Success -> CrimeResult.Success(bumped(result.character), result.moneyGained)
            is CrimeResult.Caught -> CrimeResult.Caught(bumped(result.character))
        }
    }

    /**
     * Records arrest and enters [CriminalRecord.awaitingTrial]; sentencing is deferred to [goToTrial].
     */
    fun processArrest(character: Character, crimeType: CrimeType): Character {
        val record = character.criminalRecord
        val newTimesArrested = record.timesArrested + 1
        val updatedRecord = record.copy(
            hasRecord = true,
            timesArrested = newTimesArrested,
            awaitingTrial = true,
            pendingCrimeType = crimeType.name,
            currentlyIncarcerated = false,
            yearsRemaining = 0,
            totalSentenceYears = 0,
            yearsServed = 0,
            negativePrisonEvents = 0,
            paroleBonus = 0,
            lastArrestAge = character.age
        )
        val logEntry = when (crimeType) {
            CrimeType.PICKPOCKET -> "Caught pickpocketing. Awaiting trial."
            CrimeType.SHOPLIFT -> "Caught shoplifting. Awaiting trial."
            CrimeType.FRAUD -> "Caught committing fraud. Awaiting trial."
        }
        return character.copy(
            criminalRecord = updatedRecord,
            career = character.career.currentJob?.let { job ->
                character.career.copy(
                    currentJob = null,
                    yearsAtCurrentJob = 0,
                    jobHistory = character.career.jobHistory + job.title
                )
            } ?: character.career,
            eventLog = EventLogCap.prepend(character.eventLog, logEntry),
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness - 12)
            )
        )
    }

    fun lawyerFee(lawyerTier: LawyerTier, netWorth: Int): Int = when (lawyerTier) {
        LawyerTier.PUBLIC_DEFENDER -> 0
        LawyerTier.AVERAGE -> (netWorth * AVERAGE_LAWYER_NET_WORTH_RATE).roundToInt().coerceAtLeast(AVERAGE_LAWYER_MIN_FEE)
        LawyerTier.EXPENSIVE -> (netWorth * EXPENSIVE_LAWYER_NET_WORTH_RATE).roundToInt().coerceAtLeast(EXPENSIVE_LAWYER_MIN_FEE)
    }

    fun canAffordLawyer(character: Character, lawyerTier: LawyerTier, netWorth: Int): Boolean =
        character.stats.money >= lawyerFee(lawyerTier, netWorth)

    /**
     * Resolves a pending trial with the chosen [lawyerTier].
     * Public defenders are free but usually yield the maximum sentence; expensive counsel can acquit or sharply reduce time.
     */
    fun goToTrial(character: Character, lawyerTier: LawyerTier, netWorth: Int): TrialResult {
        val record = character.criminalRecord
        if (!record.awaitingTrial || record.pendingCrimeType == null) return TrialResult.Ineligible

        val crimeType = runCatching {
            CrimeType.valueOf(record.pendingCrimeType)
        }.getOrNull() ?: return TrialResult.Ineligible

        val fee = lawyerFee(lawyerTier, netWorth)
        if (fee > character.stats.money) return TrialResult.Ineligible

        val maxSentence = maxSentenceYears(crimeType, record.timesArrested)
        val roll = Random.nextFloat()
        val outcome = resolveTrialOutcome(lawyerTier, roll, maxSentence)

        val afterFee = character.copy(
            stats = character.stats.copy(money = character.stats.money - fee)
        )

        return when (outcome) {
            TrialOutcome.Acquitted -> TrialResult.Acquitted(
                afterFee.copy(
                    criminalRecord = clearedTrialState(record),
                    eventLog = EventLogCap.prepend(
                        afterFee.eventLog,
                        "Acquitted at trial with ${lawyerLabel(lawyerTier)}."
                    ),
                    stats = afterFee.stats.copy(
                        happiness = clampStat(afterFee.stats.happiness + 6)
                    )
                )
            )
            is TrialOutcome.Sentenced -> {
                val sentence = outcome.years
                val incarcerated = sentence > 0
                TrialResult.Sentenced(
                    afterFee.copy(
                        criminalRecord = record.copy(
                            awaitingTrial = false,
                            pendingCrimeType = null,
                            currentlyIncarcerated = incarcerated,
                            yearsRemaining = sentence,
                            totalSentenceYears = sentence,
                            yearsServed = 0,
                            negativePrisonEvents = 0,
                            paroleBonus = 0
                        ),
                        eventLog = EventLogCap.prepend(
                            afterFee.eventLog,
                            trialSentenceLog(crimeType, sentence, lawyerTier)
                        ),
                        stats = afterFee.stats.copy(
                            happiness = clampStat(afterFee.stats.happiness - if (incarcerated) 10 else 4)
                        )
                    ),
                    sentenceYears = sentence
                )
            }
        }
    }

    /** Applies prison-event choice effects to parole eligibility and roll bonus. */
    fun applyPrisonChoiceEffect(character: Character, paroleEffect: Int): Character {
        if (!character.criminalRecord.currentlyIncarcerated || paroleEffect == 0) return character
        val record = character.criminalRecord
        return character.copy(
            criminalRecord = record.copy(
                negativePrisonEvents = if (paroleEffect < 0) {
                    record.negativePrisonEvents + 1
                } else {
                    record.negativePrisonEvents
                },
                paroleBonus = (record.paroleBonus + paroleEffect).coerceAtLeast(0)
            )
        )
    }

    /**
     * Advances incarceration by one year: parole roll, sentence decrement, happiness penalty, release log.
     */
    fun serveYear(character: Character): Character {
        val record = character.criminalRecord
        if (!record.currentlyIncarcerated) return character

        val yearsServed = record.yearsServed + 1
        val minServedForParole = (record.totalSentenceYears * PAROLE_MIN_SERVED_FRACTION)
            .roundToInt()
            .coerceAtLeast(1)
        val paroleEligible = yearsServed >= minServedForParole &&
            record.negativePrisonEvents == 0 &&
            record.totalSentenceYears > 0

        if (paroleEligible && Random.nextFloat() < paroleChance(record)) {
            return releaseFromPrison(
                character = character.copy(
                    criminalRecord = record.copy(yearsServed = yearsServed)
                ),
                logMessage = "Released on parole after $yearsServed year(s) served."
            )
        }

        val remaining = (record.yearsRemaining - 1).coerceAtLeast(0)
        val stillInside = remaining > 0
        val updatedRecord = record.copy(
            yearsServed = yearsServed,
            yearsRemaining = remaining,
            currentlyIncarcerated = stillInside
        )

        if (!stillInside) {
            return releaseFromPrison(
                character.copy(criminalRecord = updatedRecord),
                logMessage = "Released from prison."
            )
        }

        return character.copy(
            criminalRecord = updatedRecord,
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness - INCARCERATION_HAPPINESS_PENALTY)
            )
        )
    }

    fun paroleEligible(record: CriminalRecord): Boolean {
        if (!record.currentlyIncarcerated || record.totalSentenceYears <= 0) return false
        val minServed = (record.totalSentenceYears * PAROLE_MIN_SERVED_FRACTION)
            .roundToInt()
            .coerceAtLeast(1)
        return record.yearsServed >= minServed && record.negativePrisonEvents == 0
    }

    fun paroleChance(record: CriminalRecord): Float =
        (PAROLE_BASE_CHANCE + record.paroleBonus * PAROLE_BONUS_PER_POINT).coerceAtMost(PAROLE_MAX_CHANCE)

    private fun releaseFromPrison(character: Character, logMessage: String): Character {
        val record = character.criminalRecord
        return character.copy(
            criminalRecord = record.copy(
                currentlyIncarcerated = false,
                yearsRemaining = 0,
                totalSentenceYears = 0,
                yearsServed = 0,
                negativePrisonEvents = 0,
                paroleBonus = 0
            ),
            eventLog = EventLogCap.prepend(character.eventLog, logMessage),
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness + 4)
            )
        )
    }

    private fun clearedTrialState(record: CriminalRecord): CriminalRecord =
        record.copy(
            awaitingTrial = false,
            pendingCrimeType = null
        )

    private sealed class TrialOutcome {
        data object Acquitted : TrialOutcome()
        data class Sentenced(val years: Int) : TrialOutcome()
    }

    private fun resolveTrialOutcome(
        lawyerTier: LawyerTier,
        roll: Float,
        maxSentence: Int
    ): TrialOutcome {
        if (maxSentence <= 0) {
            return when (lawyerTier) {
                LawyerTier.PUBLIC_DEFENDER -> TrialOutcome.Sentenced(0)
                LawyerTier.AVERAGE -> if (roll < 0.35f) TrialOutcome.Acquitted else TrialOutcome.Sentenced(0)
                LawyerTier.EXPENSIVE -> if (roll < 0.65f) TrialOutcome.Acquitted else TrialOutcome.Sentenced(0)
            }
        }

        return when (lawyerTier) {
            LawyerTier.PUBLIC_DEFENDER -> when {
                roll < 0.05f -> TrialOutcome.Acquitted
                roll < 0.15f -> TrialOutcome.Sentenced((maxSentence * 0.75f).roundToInt().coerceAtLeast(1))
                else -> TrialOutcome.Sentenced(maxSentence)
            }
            LawyerTier.AVERAGE -> when {
                roll < 0.25f -> TrialOutcome.Acquitted
                roll < 0.55f -> TrialOutcome.Sentenced((maxSentence * 0.5f).roundToInt().coerceAtLeast(1))
                roll < 0.80f -> TrialOutcome.Sentenced((maxSentence * 0.75f).roundToInt().coerceAtLeast(1))
                else -> TrialOutcome.Sentenced(maxSentence)
            }
            LawyerTier.EXPENSIVE -> when {
                roll < 0.45f -> TrialOutcome.Acquitted
                roll < 0.75f -> TrialOutcome.Sentenced((maxSentence * 0.35f).roundToInt().coerceAtLeast(1))
                roll < 0.90f -> TrialOutcome.Sentenced((maxSentence * 0.6f).roundToInt().coerceAtLeast(1))
                else -> TrialOutcome.Sentenced(maxSentence)
            }
        }
    }

    private fun trialSentenceLog(
        crimeType: CrimeType,
        sentence: Int,
        lawyerTier: LawyerTier
    ): String {
        val crimeLabel = when (crimeType) {
            CrimeType.PICKPOCKET -> "pickpocketing"
            CrimeType.SHOPLIFT -> "shoplifting"
            CrimeType.FRAUD -> "fraud"
        }
        return if (sentence > 0) {
            "Found guilty of $crimeLabel. Sentenced to $sentence year(s) with ${lawyerLabel(lawyerTier)}."
        } else {
            "Found guilty of $crimeLabel but received no prison time with ${lawyerLabel(lawyerTier)}."
        }
    }

    private fun lawyerLabel(tier: LawyerTier): String = when (tier) {
        LawyerTier.PUBLIC_DEFENDER -> "a public defender"
        LawyerTier.AVERAGE -> "average legal counsel"
        LawyerTier.EXPENSIVE -> "expensive legal counsel"
    }

    private fun successChance(
        character: Character,
        crimeType: CrimeType,
        priorAttemptsThisYear: Int = character.criminalRecord.crimeAttemptsThisYear
    ): Float {
        val smartsFactor = character.stats.smarts / 100f
        val base = when (crimeType) {
            CrimeType.PICKPOCKET -> 0.25f + smartsFactor * 0.45f
            CrimeType.SHOPLIFT -> 0.18f + smartsFactor * 0.50f
            CrimeType.FRAUD -> 0.12f + smartsFactor * 0.55f
        }
        val karmaPenalty = when {
            character.stats.karma < LOW_KARMA_THRESHOLD -> LOW_KARMA_CATCH_PENALTY
            character.stats.karma < MID_KARMA_THRESHOLD -> MID_KARMA_CATCH_PENALTY
            else -> 0f
        }
        val repeatRisk = REPEAT_ATTEMPT_RISK_BASE.pow(priorAttemptsThisYear)
        return (base / repeatRisk - karmaPenalty).coerceIn(MIN_SUCCESS_CHANCE, MAX_SUCCESS_CHANCE)
    }

    private fun moneyReward(crimeType: CrimeType): Int = when (crimeType) {
        CrimeType.PICKPOCKET -> Random.nextInt(500, 2_501)
        CrimeType.SHOPLIFT -> Random.nextInt(2_000, 8_001)
        CrimeType.FRAUD -> Random.nextInt(15_000, 45_001)
    }

    /** Maximum sentence before lawyer mitigation (used at trial). */
    fun maxSentenceYears(crimeType: CrimeType, timesArrested: Int): Int {
        val base = when (crimeType) {
            CrimeType.PICKPOCKET -> Random.nextInt(0, 2)
            CrimeType.SHOPLIFT -> Random.nextInt(1, 4)
            CrimeType.FRAUD -> Random.nextInt(3, 7)
        }
        val repeatBonus = (timesArrested - 1).coerceAtLeast(0)
        return (base + repeatBonus).coerceAtMost(MAX_SENTENCE_YEARS)
    }

    companion object {
        const val MIN_CRIME_AGE = 14
        const val REQUIRES_INCARCERATED_TAG = "requires_incarcerated"
        const val PRISON_TAG = "prison"

        private const val MAX_SENTENCE_YEARS = 6
        private const val INCARCERATION_HAPPINESS_PENALTY = 6
        private const val REPEAT_ATTEMPT_RISK_BASE = 1.4f
        private const val MIN_SUCCESS_CHANCE = 0.08f
        private const val MAX_SUCCESS_CHANCE = 0.80f
        private const val LOW_KARMA_THRESHOLD = 20
        private const val MID_KARMA_THRESHOLD = 40
        private const val LOW_KARMA_CATCH_PENALTY = 0.15f
        private const val MID_KARMA_CATCH_PENALTY = 0.08f
        private const val AVERAGE_LAWYER_NET_WORTH_RATE = 0.08
        private const val EXPENSIVE_LAWYER_NET_WORTH_RATE = 0.20
        private const val AVERAGE_LAWYER_MIN_FEE = 5_000
        private const val EXPENSIVE_LAWYER_MIN_FEE = 25_000
        private const val PAROLE_MIN_SERVED_FRACTION = 0.30
        private const val PAROLE_BASE_CHANCE = 0.12f
        private const val PAROLE_BONUS_PER_POINT = 0.04f
        private const val PAROLE_MAX_CHANCE = 0.50f
    }
}
