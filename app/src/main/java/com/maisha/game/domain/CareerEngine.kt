// app/src/main/java/com/maisha/game/domain/CareerEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.JobPool
import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.Job
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.WorkEffort
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.random.Random

sealed class CareerResult {
    data class Hired(val job: Job) : CareerResult()
    data object Rejected : CareerResult()
}

@Singleton
class CareerEngine @Inject constructor() {

    fun isJobEligible(character: Character): Boolean {
        if (character.age < MIN_JOB_AGE) return false
        return when (character.education.stage) {
            SchoolStage.SECONDARY,
            SchoolStage.UNIVERSITY,
            SchoolStage.GRADUATED -> true
            else -> false
        }
    }

    fun getEligibleJobs(character: Character): List<Job> {
        if (!isJobEligible(character) || character.career.currentJob != null) return emptyList()

        return JobPool.getJobsForCountry(character.countryCode).filter { job ->
            meetsEducationRequirement(character.education.stage, job.minEducation)
        }
    }

    fun applyForJob(character: Character, jobId: String): Pair<Character, CareerResult> {
        if (character.career.currentJob != null) {
            return character to CareerResult.Rejected
        }

        val jobTemplate = JobPool.findById(jobId) ?: return character to CareerResult.Rejected
        if (!meetsEducationRequirement(character.education.stage, jobTemplate.minEducation)) {
            return character to CareerResult.Rejected
        }

        val successChance = calculateHireChance(character)
        if (Random.nextFloat() > successChance) {
            return character to CareerResult.Rejected
        }

        val hiredJob = jobTemplate.copy(
            baseSalary = EconomyScaler.scaleAmount(jobTemplate.baseSalary, character.countryCode),
            performanceScore = 50
        )
        val updatedCareer = character.career.copy(
            currentJob = hiredJob,
            yearsAtCurrentJob = 0
        )
        return character.copy(career = updatedCareer) to CareerResult.Hired(hiredJob)
    }

    fun workYear(character: Character, effort: WorkEffort): Character {
        val job = character.career.currentJob ?: return character

        val performanceDelta = when (effort) {
            WorkEffort.COAST -> -Random.nextInt(5, 16)
            WorkEffort.NORMAL -> Random.nextInt(-2, 6)
            WorkEffort.GRIND -> Random.nextInt(5, 16)
        }

        val happinessDelta = when (effort) {
            WorkEffort.COAST -> Random.nextInt(1, 4)
            WorkEffort.NORMAL -> Random.nextInt(0, 2)
            WorkEffort.GRIND -> -Random.nextInt(2, 6)
        }

        val healthDelta = when (effort) {
            WorkEffort.GRIND -> -Random.nextInt(1, 4)
            else -> 0
        }

        val annualPay = calculateAnnualSalary(job)
        val newPerformance = (job.performanceScore + performanceDelta).coerceIn(0, 100)
        val updatedJob = job.copy(performanceScore = newPerformance)
        val updatedStats = character.stats.copy(
            money = character.stats.money + annualPay,
            happiness = (character.stats.happiness + happinessDelta).coerceIn(0, 100),
            health = (character.stats.health + healthDelta).coerceIn(0, 100)
        )

        return character.copy(
            stats = updatedStats,
            career = character.career.copy(
                currentJob = updatedJob,
                yearsAtCurrentJob = character.career.yearsAtCurrentJob + 1
            )
        )
    }

    fun applyWorkEffort(character: Character, effort: WorkEffort): Character {
        val job = character.career.currentJob ?: return character

        val performanceDelta = when (effort) {
            WorkEffort.COAST -> -Random.nextInt(8, 18)
            WorkEffort.NORMAL -> Random.nextInt(0, 6)
            WorkEffort.GRIND -> Random.nextInt(8, 18)
        }

        val happinessDelta = when (effort) {
            WorkEffort.COAST -> Random.nextInt(2, 6)
            WorkEffort.NORMAL -> 0
            WorkEffort.GRIND -> -Random.nextInt(4, 10)
        }

        val healthDelta = if (effort == WorkEffort.GRIND) -Random.nextInt(2, 6) else 0
        val newPerformance = (job.performanceScore + performanceDelta).coerceIn(0, 100)
        val updatedStats = character.stats.copy(
            happiness = (character.stats.happiness + happinessDelta).coerceIn(0, 100),
            health = (character.stats.health + healthDelta).coerceIn(0, 100)
        )

        return character.copy(
            stats = updatedStats,
            career = character.career.copy(
                currentJob = job.copy(performanceScore = newPerformance)
            )
        )
    }

    fun evaluatePromotion(character: Character): Pair<Character, Boolean> {
        val job = character.career.currentJob ?: return character to false
        if (character.career.yearsAtCurrentJob == 0 ||
            character.career.yearsAtCurrentJob % PROMOTION_INTERVAL_YEARS != 0
        ) {
            return character to false
        }
        if (job.performanceScore < PROMOTION_THRESHOLD) return character to false

        val salaryBump = 1.15 + Random.nextDouble(0.0, 0.06)
        val promotedJob = job.copy(
            level = job.level + 1,
            baseSalary = (job.baseSalary * salaryBump).roundToInt(),
            performanceScore = 55
        )
        return character.copy(
            career = character.career.copy(currentJob = promotedJob)
        ) to true
    }

    fun evaluateFiring(character: Character): Pair<Character, Boolean> {
        val job = character.career.currentJob ?: return character to false
        if (job.performanceScore >= FIRING_THRESHOLD) return character to false

        return character.copy(
            career = character.career.copy(
                currentJob = null,
                yearsAtCurrentJob = 0,
                jobHistory = character.career.jobHistory + job.title
            )
        ) to true
    }

    fun quitJob(character: Character): Character {
        val job = character.career.currentJob ?: return character
        return character.copy(
            career = character.career.copy(
                currentJob = null,
                yearsAtCurrentJob = 0,
                jobHistory = character.career.jobHistory + job.title
            )
        )
    }

    fun applyPerformanceEffect(character: Character, delta: Int): Character {
        val job = character.career.currentJob ?: return character
        val newScore = (job.performanceScore + delta).coerceIn(0, 100)
        return character.copy(
            career = character.career.copy(
                currentJob = job.copy(performanceScore = newScore)
            )
        )
    }

    fun buildPromotionEvent(character: Character): LifeEvent {
        val job = character.career.currentJob!!
        return LifeEvent(
            id = PROMOTION_EVENT_ID,
            minAge = character.age,
            maxAge = character.age,
            text = "Your manager calls you in. After a strong review, you have been promoted to " +
                "${job.title} — Level ${job.level}. Your salary increases to KSh ${
                formatSalary(job.baseSalary)
                } per year.",
            choices = listOf(
                EventChoice(
                    label = "Celebrate with colleagues",
                    statEffects = mapOf("happiness" to 8),
                    resultText = "You bought sodas for the team. Morale is high."
                ),
                EventChoice(
                    label = "Stay humble and keep grinding",
                    statEffects = mapOf("smarts" to 2),
                    performanceEffect = 5,
                    resultText = "You thanked your boss and doubled down on your goals."
                )
            ),
            tags = listOf(CAREER_SYSTEM_TAG, ONE_TIME_TAG)
        )
    }

    fun buildFiringEvent(character: Character, formerTitle: String): LifeEvent {
        return LifeEvent(
            id = FIRING_EVENT_ID,
            minAge = character.age,
            maxAge = character.age,
            text = "Your employer lets you go from your role as $formerTitle due to poor performance. " +
                "It is a tough day, but you can look for something new.",
            choices = listOf(
                EventChoice(
                    label = "Take it on the chin",
                    statEffects = mapOf("happiness" to -8),
                    resultText = "You cleared your desk and headed home quietly."
                ),
                EventChoice(
                    label = "Promise yourself a comeback",
                    statEffects = mapOf("happiness" to -3, "smarts" to 2),
                    resultText = "You updated your CV that same evening."
                )
            ),
            tags = listOf(CAREER_SYSTEM_TAG, ONE_TIME_TAG)
        )
    }

    fun buildDownsizingEvent(character: Character, formerTitle: String): LifeEvent {
        return LifeEvent(
            id = DOWNSIZING_EVENT_ID,
            minAge = character.age,
            maxAge = character.age,
            text = "The company announces layoffs. Despite your efforts as $formerTitle, your position " +
                "is cut in a restructuring.",
            choices = listOf(
                EventChoice(
                    label = "Accept the severance",
                    statEffects = mapOf("money" to 50_000, "happiness" to -5),
                    resultText = "You received a modest package and moved on."
                ),
                EventChoice(
                    label = "Appeal the decision",
                    statEffects = mapOf("happiness" to -8),
                    resultText = "HR said the decision was final. At least you tried."
                )
            ),
            tags = listOf(CAREER_SYSTEM_TAG, ONE_TIME_TAG)
        )
    }

    fun shouldTriggerDownsizing(character: Character): Boolean {
        if (character.career.currentJob == null) return false
        return character.age >= MIN_JOB_AGE && Random.nextFloat() < DOWNSIZING_CHANCE
    }

    fun applyDownsizing(character: Character): Pair<Character, String> {
        val job = character.career.currentJob ?: return character to ""
        val title = job.title
        val updated = character.copy(
            career = character.career.copy(
                currentJob = null,
                yearsAtCurrentJob = 0,
                jobHistory = character.career.jobHistory + title
            )
        )
        return updated to title
    }

    fun formatCareerStatus(career: CareerState): String {
        val job = career.currentJob ?: return "Unemployed"
        return "${job.title} — Level ${job.level}"
    }

    fun calculateAnnualSalary(job: Job): Int = job.baseSalary

    private fun calculateHireChance(character: Character): Float {
        val smartsFactor = character.stats.smarts / 100f * 0.35f
        val gpaFactor = (character.education.gpa / 4f).coerceIn(0f, 1f) * 0.25f
        val recordPenalty = criminalRecordHirePenalty(character)
        val base = 0.35f
        return (base + smartsFactor + gpaFactor - recordPenalty).coerceIn(0.1f, 0.95f)
    }

    private fun criminalRecordHirePenalty(character: Character): Float {
        if (!character.criminalRecord.hasRecord) return 0f
        val cleanYears = character.criminalRecord.lastArrestAge?.let { character.age - it } ?: 0
        val multiplier = when {
            cleanYears >= 15 -> 0.25f
            cleanYears >= 10 -> 0.50f
            cleanYears >= 5 -> 0.75f
            else -> 1.0f
        }
        return CRIMINAL_RECORD_HIRE_PENALTY * multiplier
    }

    private fun meetsEducationRequirement(
        currentStage: SchoolStage,
        required: SchoolStage
    ): Boolean {
        return when (required) {
            SchoolStage.SECONDARY -> currentStage in setOf(
                SchoolStage.SECONDARY,
                SchoolStage.UNIVERSITY,
                SchoolStage.GRADUATED
            )
            SchoolStage.GRADUATED -> currentStage == SchoolStage.GRADUATED
            else -> false
        }
    }

    private fun formatSalary(amount: Int): String =
        "%,d".format(amount)

    companion object {
        const val CAREER_SYSTEM_TAG = "career_system"
        const val PROMOTION_EVENT_ID = "career_promotion_system"
        const val FIRING_EVENT_ID = "career_firing_system"
        const val DOWNSIZING_EVENT_ID = "career_downsizing_system"
        private const val ONE_TIME_TAG = "one_time"

        private const val MIN_JOB_AGE = 18
        private const val PROMOTION_THRESHOLD = 65
        private const val FIRING_THRESHOLD = 20
        private const val PROMOTION_INTERVAL_YEARS = 3
        private const val DOWNSIZING_CHANCE = 0.04f
        private const val CRIMINAL_RECORD_HIRE_PENALTY = 0.15f
    }
}
