// app/src/main/java/com/maisha/game/domain/MortalityEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.model.Character
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

enum class DeathCause {
    OLD_AGE,
    HEALTH_FAILURE,
    ACCIDENT,
    ILLNESS
}

sealed class DeathResult {
    data object Alive : DeathResult()
    data class Died(val cause: DeathCause, val ageAtDeath: Int) : DeathResult()
}

@Singleton
class MortalityEngine @Inject constructor() {

    /**
     * Rolls death for a living character. Evaluated in order: accident → critical health → low health illness
     * → chronic untreated conditions → age curve. Already-dead characters return [DeathResult.Alive] immediately.
     *
     * Called last in [GameEngine.finalizeYear] so other yearly hooks run before death.
     */
    fun checkDeath(character: Character): DeathResult {
        if (!character.alive) return DeathResult.Alive

        val age = character.age
        val health = character.stats.health
        val deployed = character.career.isDeployed

        if (Random.nextFloat() < accidentChance(character)) {
            return DeathResult.Died(DeathCause.ACCIDENT, age)
        }

        if (health < CRITICAL_HEALTH_THRESHOLD) {
            val healthDeathChance = healthFailureChance(health, deployed)
            if (Random.nextFloat() < healthDeathChance && !tryKarmaMiracle(character)) {
                return DeathResult.Died(DeathCause.HEALTH_FAILURE, age)
            }
        }

        if (health < LOW_HEALTH_THRESHOLD &&
            Random.nextFloat() < illnessChance(character) &&
            !tryKarmaMiracle(character)
        ) {
            return DeathResult.Died(DeathCause.ILLNESS, age)
        }

        val chronicUntreated = character.activeConditions.any {
            !it.treated && it.severity >= 2 && it.yearsUntreated >= 3
        }
        if (chronicUntreated &&
            Random.nextFloat() < chronicIllnessChance(character) &&
            !tryKarmaMiracle(character)
        ) {
            return DeathResult.Died(DeathCause.ILLNESS, age)
        }

        val ageChance = ageDeathProbability(age, deployed)
        if (Random.nextFloat() < ageChance && !tryKarmaMiracle(character)) {
            return DeathResult.Died(DeathCause.OLD_AGE, age)
        }

        return DeathResult.Alive
    }

    /**
     * High-karma characters get a small chance to survive an otherwise fatal roll.
     * Exposed for tests via [karmaMiracleChance].
     */
    fun tryKarmaMiracle(character: Character): Boolean {
        val chance = karmaMiracleChance(character)
        if (chance <= 0f) return false
        return Random.nextFloat() < chance
    }

    fun karmaMiracleChance(character: Character): Float {
        val karma = character.stats.karma
        if (karma <= HIGH_KARMA_THRESHOLD) return 0f
        return ((karma - HIGH_KARMA_THRESHOLD) / 20f * MAX_KARMA_MIRACLE_CHANCE)
            .coerceIn(0f, MAX_KARMA_MIRACLE_CHANCE)
    }

    /** Baseline accident probability; elevated during active military deployment. */
    fun accidentChance(character: Character): Float =
        if (character.career.isDeployed) {
            ACCIDENT_CHANCE * DEPLOYMENT_MORTALITY_MULTIPLIER
        } else {
            ACCIDENT_CHANCE
        }

    fun illnessChance(character: Character): Float =
        if (character.career.isDeployed) {
            ILLNESS_CHANCE * DEPLOYMENT_MORTALITY_MULTIPLIER
        } else {
            ILLNESS_CHANCE
        }

    fun chronicIllnessChance(character: Character): Float =
        if (character.career.isDeployed) {
            CHRONIC_ILLNESS_DEATH_CHANCE * DEPLOYMENT_MORTALITY_MULTIPLIER
        } else {
            CHRONIC_ILLNESS_DEATH_CHANCE
        }

    fun healthFailureChance(health: Int, deployed: Boolean): Float {
        val base = ((CRITICAL_HEALTH_THRESHOLD - health) / CRITICAL_HEALTH_THRESHOLD.toFloat())
            .coerceIn(0f, 1f) * 0.2f + 0.08f
        return if (deployed) {
            (base * DEPLOYMENT_MORTALITY_MULTIPLIER).coerceIn(0f, 0.95f)
        } else {
            base
        }
    }

    /** Combined one-year death probability estimate for tests (independent rolls approximated). */
    fun estimatedDeathProbability(character: Character): Float {
        if (!character.alive) return 0f
        val accident = accidentChance(character)
        val health = character.stats.health
        val healthFail = if (health < CRITICAL_HEALTH_THRESHOLD) {
            healthFailureChance(health, character.career.isDeployed)
        } else {
            0f
        }
        val illness = if (health < LOW_HEALTH_THRESHOLD) illnessChance(character) else 0f
        val chronic = if (character.activeConditions.any { !it.treated && it.severity >= 2 && it.yearsUntreated >= 3 }) {
            chronicIllnessChance(character)
        } else {
            0f
        }
        val age = ageDeathProbability(character.age, character.career.isDeployed)
        val miracle = karmaMiracleChance(character)
        val healthFailEff = healthFail * (1f - miracle)
        val illnessEff = illness * (1f - miracle)
        val chronicEff = chronic * (1f - miracle)
        val ageEff = age * (1f - miracle)
        // P(any) ≈ 1 - Π(1 - p_i) for independent rolls
        return (1f - (1f - accident) * (1f - healthFailEff) * (1f - illnessEff) *
            (1f - chronicEff) * (1f - ageEff))
            .coerceIn(0f, 1f)
    }

    /**
     * Marks the character dead, sets [Character.age] to [ageAtDeath], and prepends a structured death marker
     * on [Character.eventLog] for later parsing.
     */
    fun applyDeath(character: Character, cause: DeathCause, ageAtDeath: Int): Character {
        val flavor = flavorText(cause)
        val marker = "${EventLogCap.DEATH_MARKER_PREFIX}${cause.name}::$flavor"
        return character.copy(
            alive = false,
            age = ageAtDeath,
            eventLog = EventLogCap.prepend(character.eventLog, marker)
        )
    }

    /** Reads [DeathCause] from the newest death-marker log line; defaults to [DeathCause.OLD_AGE] if missing. */
    fun parseDeathCause(character: Character): DeathCause? {
        if (character.alive) return null
        val line = character.eventLog.firstOrNull() ?: return DeathCause.OLD_AGE
        if (!line.startsWith(EventLogCap.DEATH_MARKER_PREFIX)) return DeathCause.OLD_AGE
        val causeName = line.removePrefix(EventLogCap.DEATH_MARKER_PREFIX).substringBefore("::")
        return runCatching { DeathCause.valueOf(causeName) }.getOrElse { DeathCause.OLD_AGE }
    }

    /** Human-readable death sentence for summary UI; falls back to [gentleCauseLabel] if log format is unexpected. */
    fun deathFlavorText(character: Character): String {
        val line = character.eventLog.firstOrNull() ?: return gentleCauseLabel(DeathCause.OLD_AGE)
        return if (line.startsWith(EventLogCap.DEATH_MARKER_PREFIX)) {
            line.substringAfter("::", gentleCauseLabel(parseDeathCause(character) ?: DeathCause.OLD_AGE))
        } else {
            line
        }
    }

    /** Short cause label for ancestry and life-summary screens (no log parsing). */
    fun gentleCauseLabel(cause: DeathCause): String = when (cause) {
        DeathCause.OLD_AGE -> "Passed away peacefully in old age"
        DeathCause.HEALTH_FAILURE -> "Passed away due to failing health"
        DeathCause.ACCIDENT -> "Died in an unexpected accident"
        DeathCause.ILLNESS -> "Passed away after a prolonged illness"
    }

    private fun flavorText(cause: DeathCause): String = when (cause) {
        DeathCause.OLD_AGE -> "Passed away peacefully at home in your sleep."
        DeathCause.HEALTH_FAILURE -> "Your health gave out. Family gathered to say goodbye."
        DeathCause.ACCIDENT -> "Life ended suddenly in an unexpected accident."
        DeathCause.ILLNESS -> "After a long illness, you passed away surrounded by loved ones."
    }

    private fun ageDeathProbability(age: Int, deployed: Boolean = false): Float {
        val probability = when {
            age < 60 -> 0.001f
            age < 70 -> 0.01f + (age - 60) * 0.004f
            age < 80 -> 0.05f + (age - 70) * 0.02f
            age < 90 -> 0.25f + (age - 80) * 0.045f
            age < 95 -> 0.70f + (age - 90) * 0.05f
            else -> 0.95f + (age - 95).coerceAtMost(5) * 0.01f
        }
        val scaled = if (deployed) probability * DEPLOYMENT_MORTALITY_MULTIPLIER else probability
        return scaled.coerceIn(0f, 0.99f)
    }

    companion object {
        private const val ACCIDENT_CHANCE = 0.004f
        private const val CRITICAL_HEALTH_THRESHOLD = 10
        private const val LOW_HEALTH_THRESHOLD = 25
        private const val ILLNESS_CHANCE = 0.025f
        private const val CHRONIC_ILLNESS_DEATH_CHANCE = 0.018f
        /** Combat deployments multiply baseline mortality rolls. */
        const val DEPLOYMENT_MORTALITY_MULTIPLIER = 8f
        const val HIGH_KARMA_THRESHOLD = 80
        private const val MAX_KARMA_MIRACLE_CHANCE = 0.35f
    }
}
