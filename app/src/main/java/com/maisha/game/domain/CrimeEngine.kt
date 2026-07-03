// app/src/main/java/com/maisha/game/domain/CrimeEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.model.Character
import com.maisha.game.data.model.CrimeType
import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.util.clampStat
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

sealed class CrimeResult {
    data class Success(val character: Character, val moneyGained: Int) : CrimeResult()
    data class Caught(val character: Character) : CrimeResult()
}

@Singleton
class CrimeEngine @Inject constructor() {

    /**
     * Attempts a crime if the character is at least [MIN_CRIME_AGE] and not incarcerated.
     *
     * Success chance scales with smarts; failure routes through [processArrest].
     *
     * @return [CrimeResult.Success] with cash gained, or [CrimeResult.Caught] with updated record.
     */
    fun attemptCrime(character: Character, crimeType: CrimeType): CrimeResult {
        if (character.criminalRecord.currentlyIncarcerated) {
            return CrimeResult.Caught(character)
        }
        if (character.age < MIN_CRIME_AGE) {
            return CrimeResult.Caught(character)
        }

        val successChance = successChance(character, crimeType)
        return if (Random.nextFloat() < successChance) {
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
    }

    /**
     * Records arrest, assigns sentence (repeat offenses lengthen it), fires current job, and logs the outcome.
     *
     * @return Character with [CriminalRecord] updated and optional career cleared.
     */
    fun processArrest(character: Character, crimeType: CrimeType): Character {
        val record = character.criminalRecord
        val newTimesArrested = record.timesArrested + 1
        val sentence = sentenceYears(crimeType, newTimesArrested)
        val updatedRecord = record.copy(
            hasRecord = true,
            timesArrested = newTimesArrested,
            currentlyIncarcerated = sentence > 0,
            yearsRemaining = sentence,
            lastArrestAge = character.age
        )
        val logEntry = when (crimeType) {
            CrimeType.PICKPOCKET -> "Caught pickpocketing. Sentenced to $sentence year(s)."
            CrimeType.SHOPLIFT -> "Caught shoplifting. Sentenced to $sentence year(s)."
            CrimeType.FRAUD -> "Caught committing fraud. Sentenced to $sentence year(s)."
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

    /**
     * Advances incarceration by one year: decrements [CriminalRecord.yearsRemaining], applies happiness penalty,
     * and prepends a release log entry when the sentence ends.
     *
     * No-op if not [CriminalRecord.currentlyIncarcerated].
     */
    fun serveYear(character: Character): Character {
        val record = character.criminalRecord
        if (!record.currentlyIncarcerated) return character

        val remaining = (record.yearsRemaining - 1).coerceAtLeast(0)
        val stillInside = remaining > 0
        val updatedRecord = record.copy(
            yearsRemaining = remaining,
            currentlyIncarcerated = stillInside
        )
        val releaseLog = if (!stillInside) {
            EventLogCap.prepend(character.eventLog, "Released from prison.")
        } else {
            character.eventLog
        }
        return character.copy(
            criminalRecord = updatedRecord,
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness - INCARCERATION_HAPPINESS_PENALTY)
            ),
            eventLog = releaseLog
        )
    }

    private fun successChance(character: Character, crimeType: CrimeType): Float {
        val smartsFactor = character.stats.smarts / 100f
        return when (crimeType) {
            CrimeType.PICKPOCKET -> (0.35f + smartsFactor * 0.35f).coerceIn(0.15f, 0.75f)
            CrimeType.SHOPLIFT -> (0.28f + smartsFactor * 0.40f).coerceIn(0.12f, 0.70f)
            CrimeType.FRAUD -> (0.20f + smartsFactor * 0.50f).coerceIn(0.10f, 0.65f)
        }
    }

    private fun moneyReward(crimeType: CrimeType): Int = when (crimeType) {
        CrimeType.PICKPOCKET -> Random.nextInt(500, 2_501)
        CrimeType.SHOPLIFT -> Random.nextInt(2_000, 8_001)
        CrimeType.FRAUD -> Random.nextInt(15_000, 45_001)
    }

    private fun sentenceYears(crimeType: CrimeType, timesArrested: Int): Int {
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
        private const val MAX_SENTENCE_YEARS = 6
        private const val INCARCERATION_HAPPINESS_PENALTY = 6
    }
}
