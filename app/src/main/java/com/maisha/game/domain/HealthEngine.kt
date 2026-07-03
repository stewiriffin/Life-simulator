// app/src/main/java/com/maisha/game/domain/HealthEngine.kt (modified — yearsUntreated increment)
package com.maisha.game.domain

import com.maisha.game.data.model.Character
import com.maisha.game.data.model.HealthCondition
import com.maisha.game.data.model.Job
import com.maisha.game.data.model.LifestyleOption
import com.maisha.game.data.model.LifestyleState
import com.maisha.game.data.model.WorkEffort
import java.util.UUID
import com.maisha.game.util.clampStat
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
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
        val updatedHealth = clampStat(character.stats.health - totalDrain)
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

    /**
     * Yearly health loop: illness roll, untreated drain, lifestyle billing/benefits, chronic stress.
     */
    fun processHealthProgression(character: Character): Character {
        var updated = character
        rollForIllness(updated)?.let { condition ->
            updated = addCondition(updated, condition)
        }
        updated = applyUntreatedConditions(updated)
        updated = applyLifestyleCosts(updated)
        updated = applyLifestyleBenefits(updated)
        updated = applyChronicStress(updated)
        return updated
    }

    /**
     * Work-effort stress from [CareerEngine.workYear] or [CareerEngine.applyWorkEffort].
     * [WorkEffort.GRIND] doubles the penalty versus normal effort.
     */
    fun applyWorkEffortStress(character: Character, effort: WorkEffort): Character {
        if (character.career.currentJob == null || effort == WorkEffort.COAST) return character
        val multiplier = when (effort) {
            WorkEffort.GRIND -> 2f
            WorkEffort.NORMAL -> 1f
            WorkEffort.COAST -> 0f
        }
        val healthDrain = (WORK_EFFORT_STRESS_HEALTH * multiplier).roundToInt()
        val happinessDrain = (WORK_EFFORT_STRESS_HAPPINESS * multiplier).roundToInt()
        return character.copy(
            stats = character.stats.copy(
                health = clampStat(character.stats.health - healthDrain),
                happiness = clampStat(character.stats.happiness - happinessDrain)
            )
        )
    }

    /** Enables or disables a recurring lifestyle subscription. */
    fun setLifestyleOption(
        character: Character,
        option: LifestyleOption,
        enabled: Boolean
    ): Character {
        val lifestyle = when (option) {
            LifestyleOption.GYM -> character.lifestyle.copy(hasGymMembership = enabled)
            LifestyleOption.DIET -> character.lifestyle.copy(isVegan = enabled)
            LifestyleOption.THERAPIST -> character.lifestyle.copy(hasTherapist = enabled)
        }
        val logLine = when (option) {
            LifestyleOption.GYM -> if (enabled) "Joined a gym membership." else "Cancelled gym membership."
            LifestyleOption.DIET -> if (enabled) "Started a premium diet plan." else "Stopped premium diet plan."
            LifestyleOption.THERAPIST -> if (enabled) "Started seeing a therapist." else "Stopped therapy sessions."
        }
        return character.copy(
            lifestyle = lifestyle,
            eventLog = EventLogCap.prepend(character.eventLog, logLine)
        )
    }

    fun yearlyLifestyleCost(lifestyle: LifestyleState): Int {
        var total = 0
        if (lifestyle.hasGymMembership) total += GYM_YEARLY_COST
        if (lifestyle.isVegan) total += DIET_YEARLY_COST
        if (lifestyle.hasTherapist) total += THERAPIST_YEARLY_COST
        return total
    }

    private fun applyLifestyleCosts(character: Character): Character {
        val lifestyle = character.lifestyle
        if (!lifestyle.hasGymMembership && !lifestyle.isVegan && !lifestyle.hasTherapist) {
            return character
        }

        var updated = character
        var remaining = character.stats.money
        var nextLifestyle = lifestyle
        val cancelled = mutableListOf<String>()

        if (lifestyle.hasGymMembership) {
            if (remaining >= GYM_YEARLY_COST) {
                remaining -= GYM_YEARLY_COST
            } else {
                nextLifestyle = nextLifestyle.copy(hasGymMembership = false)
                cancelled += "gym membership"
            }
        }
        if (nextLifestyle.isVegan) {
            if (remaining >= DIET_YEARLY_COST) {
                remaining -= DIET_YEARLY_COST
            } else {
                nextLifestyle = nextLifestyle.copy(isVegan = false)
                cancelled += "premium diet"
            }
        }
        if (nextLifestyle.hasTherapist) {
            if (remaining >= THERAPIST_YEARLY_COST) {
                remaining -= THERAPIST_YEARLY_COST
            } else {
                nextLifestyle = nextLifestyle.copy(hasTherapist = false)
                cancelled += "therapy"
            }
        }

        updated = updated.copy(
            stats = updated.stats.copy(money = remaining.coerceAtLeast(0)),
            lifestyle = nextLifestyle
        )
        if (cancelled.isNotEmpty()) {
            updated = updated.copy(
                eventLog = EventLogCap.prepend(
                    updated.eventLog,
                    "Could not afford ${cancelled.joinToString(" and ")}; subscription ended."
                )
            )
        }
        return updated
    }

    private fun applyLifestyleBenefits(character: Character): Character {
        val lifestyle = character.lifestyle
        var healthBonus = 0
        var looksBonus = 0
        var happinessBonus = 0
        if (lifestyle.hasGymMembership) {
            healthBonus += GYM_HEALTH_BONUS
            looksBonus += GYM_LOOKS_BONUS
        }
        if (lifestyle.isVegan) {
            healthBonus += DIET_HEALTH_BONUS
            looksBonus += DIET_LOOKS_BONUS
        }
        if (lifestyle.hasTherapist) {
            happinessBonus += THERAPIST_HAPPINESS_BONUS
        }
        if (healthBonus == 0 && looksBonus == 0 && happinessBonus == 0) return character

        return character.copy(
            stats = character.stats.copy(
                health = clampStat(character.stats.health + healthBonus),
                looks = clampStat(character.stats.looks + looksBonus),
                happiness = clampStat(character.stats.happiness + happinessBonus)
            )
        )
    }

    private fun applyChronicStress(character: Character): Character {
        var healthDrain = 0
        var happinessDrain = 0

        if (character.stats.money <= HIGH_DEBT_MONEY_THRESHOLD) {
            healthDrain += DEBT_STRESS_HEALTH
            happinessDrain += DEBT_STRESS_HAPPINESS
        }

        character.career.currentJob?.let { job ->
            if (isDemandingJob(job)) {
                healthDrain += DEMANDING_JOB_STRESS_HEALTH
                happinessDrain += DEMANDING_JOB_STRESS_HAPPINESS
            }
        }

        if (character.lifestyle.hasTherapist) {
            healthDrain = (healthDrain * THERAPIST_STRESS_RELIEF).roundToInt()
            happinessDrain = (happinessDrain * THERAPIST_STRESS_RELIEF).roundToInt()
        }

        if (healthDrain == 0 && happinessDrain == 0) return character

        return character.copy(
            stats = character.stats.copy(
                health = clampStat(character.stats.health - healthDrain),
                happiness = clampStat(character.stats.happiness - happinessDrain)
            )
        )
    }

    private fun isDemandingJob(job: Job): Boolean {
        if (job.level >= DEMANDING_JOB_LEVEL) return true
        return job.id in DEMANDING_JOB_IDS ||
            job.title.contains("CEO", ignoreCase = true) ||
            job.title.contains("Surgeon", ignoreCase = true)
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

        const val GYM_YEARLY_COST = 24_000
        const val DIET_YEARLY_COST = 42_000
        const val THERAPIST_YEARLY_COST = 60_000

        private const val GYM_HEALTH_BONUS = 3
        private const val GYM_LOOKS_BONUS = 2
        private const val DIET_HEALTH_BONUS = 2
        private const val DIET_LOOKS_BONUS = 1
        private const val THERAPIST_HAPPINESS_BONUS = 3
        private const val THERAPIST_STRESS_RELIEF = 0.5f

        private const val HIGH_DEBT_MONEY_THRESHOLD = 10_000
        private const val DEBT_STRESS_HEALTH = 2
        private const val DEBT_STRESS_HAPPINESS = 3
        private const val DEMANDING_JOB_LEVEL = 3
        private const val DEMANDING_JOB_STRESS_HEALTH = 2
        private const val DEMANDING_JOB_STRESS_HAPPINESS = 2
        private const val WORK_EFFORT_STRESS_HEALTH = 2
        private const val WORK_EFFORT_STRESS_HAPPINESS = 1

        private val DEMANDING_JOB_IDS = setOf(
            "software_developer",
            "engineer",
            "nurse"
        )
    }
}
