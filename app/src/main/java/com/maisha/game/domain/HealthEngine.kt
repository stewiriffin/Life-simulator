// app/src/main/java/com/maisha/game/domain/HealthEngine.kt (modified — yearsUntreated increment)
package com.maisha.game.domain

import com.maisha.game.data.model.Character
import com.maisha.game.data.model.HealthCondition
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

sealed class DoctorResult {
    data class Treated(val character: Character) : DoctorResult()
    data class Failed(val character: Character) : DoctorResult()
}

@Singleton
class HealthEngine @Inject constructor() {

    /**
     * Random new illness if health is low enough and no untreated condition exists.
     *
     * @return A new [HealthCondition] or null if roll fails or character already has untreated illness.
     */
    fun rollForIllness(character: Character): HealthCondition? {
        if (character.activeConditions.any { !it.treated }) {
            return null
        }
        val chance = illnessChance(character.stats.health)
        if (Random.nextFloat() >= chance) return null

        val severity = when {
            character.stats.health < 30 -> if (Random.nextFloat() < 0.35f) 2 else 3
            character.stats.health < 55 -> if (Random.nextFloat() < 0.70f) 1 else 2
            else -> if (Random.nextFloat() < 0.82f) 1 else 2
        }.coerceIn(1, 3)

        return HealthCondition(
            id = UUID.randomUUID().toString(),
            name = illnessName(severity),
            severity = severity
        )
    }

    /** Appends [condition] to [Character.activeConditions] and logs onset; no-op if id already present. */
    fun addCondition(character: Character, condition: HealthCondition): Character {
        if (character.activeConditions.any { it.id == condition.id }) return character
        return character.copy(
            activeConditions = character.activeConditions + condition,
            eventLog = EventLogCap.prepend(character.eventLog, "Fell ill with ${condition.name}.")
        )
    }

    /**
     * Yearly drain from untreated conditions: reduces health and increments [HealthCondition.yearsUntreated].
     */
    fun applyUntreatedConditions(character: Character): Character {
        val untreated = character.activeConditions.filter { !it.treated }
        if (untreated.isEmpty()) return character

        val totalDrain = untreated.sumOf { severityDrain(it.severity) }
        val updatedHealth = (character.stats.health - totalDrain).coerceIn(0, 100)
        val updatedConditions = character.activeConditions.map { condition ->
            if (!condition.treated) {
                condition.copy(yearsUntreated = condition.yearsUntreated + 1)
            } else {
                condition
            }
        }
        return character.copy(
            stats = character.stats.copy(health = updatedHealth),
            activeConditions = updatedConditions
        )
    }

    /**
     * Pays for treatment of [conditionId]; deducts cost first, then rolls success by severity and care tier.
     *
     * @return [DoctorResult.Failed] if condition missing, already treated, or insufficient funds.
     */
    fun visitDoctor(
        character: Character,
        conditionId: String,
        usePrivateCare: Boolean
    ): DoctorResult {
        val index = character.activeConditions.indexOfFirst { it.id == conditionId }
        if (index == -1) return DoctorResult.Failed(character)

        val condition = character.activeConditions[index]
        if (condition.treated) return DoctorResult.Failed(character)

        val cost = treatmentCost(condition.severity, usePrivateCare)
        if (character.stats.money < cost) {
            return DoctorResult.Failed(character)
        }

        val successChance = treatmentSuccessChance(condition.severity, usePrivateCare)
        val afterPayment = character.copy(
            stats = character.stats.copy(money = character.stats.money - cost)
        )

        return if (Random.nextFloat() < successChance) {
            val treatedCondition = condition.copy(treated = true)
            val updatedConditions = afterPayment.activeConditions.toMutableList().apply {
                this[index] = treatedCondition
            }
            val facility = if (usePrivateCare) "Nairobi Hospital" else "public clinic"
            DoctorResult.Treated(
                afterPayment.copy(
                    activeConditions = updatedConditions,
                    eventLog = EventLogCap.prepend(
                        afterPayment.eventLog,
                        "Recovered from ${condition.name} after treatment at $facility."
                    )
                )
            )
        } else {
            DoctorResult.Failed(
                afterPayment.copy(
                    eventLog = EventLogCap.prepend(
                        afterPayment.eventLog,
                        "Treatment for ${condition.name} at the " +
                            (if (usePrivateCare) "hospital" else "clinic") +
                            " did not fully work."
                    )
                )
            )
        }
    }

    /** Convenience wrapper: treats the first untreated condition, if any. */
    fun visitFirstUntreatedCondition(
        character: Character,
        usePrivateCare: Boolean
    ): DoctorResult {
        val condition = character.activeConditions.firstOrNull { !it.treated }
            ?: return DoctorResult.Failed(character)
        return visitDoctor(character, condition.id, usePrivateCare)
    }

    private fun illnessChance(health: Int): Float {
        val normalized = (100 - health).coerceIn(0, 100) / 100f
        return (BASE_ILLNESS_CHANCE + normalized * 0.22f).coerceIn(0.02f, 0.28f)
    }

    private fun severityDrain(severity: Int): Int = when (severity) {
        1 -> Random.nextInt(2, 5)
        2 -> Random.nextInt(4, 8)
        else -> Random.nextInt(6, 12)
    }

    private fun treatmentCost(severity: Int, privateCare: Boolean): Int {
        val base = when (severity) {
            1 -> if (privateCare) 8_000 else 2_000
            2 -> if (privateCare) 20_000 else 6_000
            else -> if (privateCare) 45_000 else 12_000
        }
        return base
    }

    private fun treatmentSuccessChance(severity: Int, privateCare: Boolean): Float {
        val base = when (severity) {
            1 -> if (privateCare) 0.93f else 0.60f
            2 -> if (privateCare) 0.86f else 0.45f
            else -> if (privateCare) 0.78f else 0.32f
        }
        return base
    }

    private fun illnessName(severity: Int): String {
        val minor = listOf("Common cold", "Mild flu", "Stomach bug")
        val moderate = listOf("Typhoid", "Malaria", "Persistent infection")
        val serious = listOf("Severe pneumonia", "Complicated malaria", "Major infection")
        return when (severity) {
            1 -> minor.random()
            2 -> moderate.random()
            else -> serious.random()
        }
    }

    companion object {
        const val HEALTH_TAG = "health"
        const val CRIME_TAG = "crime"
        private const val BASE_ILLNESS_CHANCE = 0.035f
    }
}
