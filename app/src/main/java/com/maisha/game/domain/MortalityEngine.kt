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

    fun checkDeath(character: Character): DeathResult {
        if (!character.alive) return DeathResult.Alive

        val age = character.age
        val health = character.stats.health

        if (Random.nextFloat() < ACCIDENT_CHANCE) {
            return DeathResult.Died(DeathCause.ACCIDENT, age)
        }

        if (health < CRITICAL_HEALTH_THRESHOLD) {
            val healthDeathChance = ((CRITICAL_HEALTH_THRESHOLD - health) / CRITICAL_HEALTH_THRESHOLD.toFloat())
                .coerceIn(0f, 1f) * 0.2f + 0.08f
            if (Random.nextFloat() < healthDeathChance) {
                return DeathResult.Died(DeathCause.HEALTH_FAILURE, age)
            }
        }

        if (health < LOW_HEALTH_THRESHOLD && Random.nextFloat() < ILLNESS_CHANCE) {
            return DeathResult.Died(DeathCause.ILLNESS, age)
        }

        val chronicUntreated = character.activeConditions.any {
            !it.treated && it.severity >= 2 && it.yearsUntreated >= 3
        }
        if (chronicUntreated && Random.nextFloat() < CHRONIC_ILLNESS_DEATH_CHANCE) {
            return DeathResult.Died(DeathCause.ILLNESS, age)
        }

        val ageChance = ageDeathProbability(age)
        if (Random.nextFloat() < ageChance) {
            return DeathResult.Died(DeathCause.OLD_AGE, age)
        }

        return DeathResult.Alive
    }

    fun applyDeath(character: Character, cause: DeathCause, ageAtDeath: Int): Character {
        val flavor = flavorText(cause)
        val marker = "$DEATH_MARKER_PREFIX${cause.name}::$flavor"
        return character.copy(
            alive = false,
            age = ageAtDeath,
            eventLog = listOf(marker) + character.eventLog
        )
    }

    fun parseDeathCause(character: Character): DeathCause? {
        if (character.alive) return null
        val line = character.eventLog.firstOrNull() ?: return DeathCause.OLD_AGE
        if (!line.startsWith(DEATH_MARKER_PREFIX)) return DeathCause.OLD_AGE
        val causeName = line.removePrefix(DEATH_MARKER_PREFIX).substringBefore("::")
        return runCatching { DeathCause.valueOf(causeName) }.getOrElse { DeathCause.OLD_AGE }
    }

    fun deathFlavorText(character: Character): String {
        val line = character.eventLog.firstOrNull() ?: return gentleCauseLabel(DeathCause.OLD_AGE)
        return if (line.startsWith(DEATH_MARKER_PREFIX)) {
            line.substringAfter("::", gentleCauseLabel(parseDeathCause(character) ?: DeathCause.OLD_AGE))
        } else {
            line
        }
    }

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

    private fun ageDeathProbability(age: Int): Float {
        val probability = when {
            age < 60 -> 0.001f
            age < 70 -> 0.01f + (age - 60) * 0.004f
            age < 80 -> 0.05f + (age - 70) * 0.02f
            age < 90 -> 0.25f + (age - 80) * 0.045f
            age < 95 -> 0.70f + (age - 90) * 0.05f
            else -> 0.95f + (age - 95).coerceAtMost(5) * 0.01f
        }
        return probability.coerceIn(0f, 0.99f)
    }

    companion object {
        private const val DEATH_MARKER_PREFIX = "::DEATH:"
        private const val ACCIDENT_CHANCE = 0.004f
        private const val CRITICAL_HEALTH_THRESHOLD = 10
        private const val LOW_HEALTH_THRESHOLD = 25
        private const val ILLNESS_CHANCE = 0.025f
        private const val CHRONIC_ILLNESS_DEATH_CHANCE = 0.018f
    }
}
