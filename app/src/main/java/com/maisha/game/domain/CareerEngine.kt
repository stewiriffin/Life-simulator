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
import com.maisha.game.util.clampPerformanceScore
import com.maisha.game.util.clampStat
import com.maisha.game.util.formatMoney
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.random.Random

sealed class CareerResult {
    data class Hired(val job: Job) : CareerResult()
    data object Rejected : CareerResult()
}

sealed class RetirementResult {
    data class Success(val character: Character) : RetirementResult()
    data object Ineligible : RetirementResult()
}

@Singleton
class CareerEngine @Inject constructor(
    private val healthEngine: HealthEngine
) {

    /** Minimum age and completed-or-enrolled secondary+ education; rejects expelled and incomplete dropouts. */
    fun isJobEligible(character: Character): Boolean {
        if (character.age < MIN_JOB_AGE) return false
        if (character.education.expelled) return false
        return meetsEducationRequirement(character, SchoolStage.SECONDARY)
    }

    /** Country-scoped job list filtered by education; empty if already employed, retired, or ineligible. */
    fun getEligibleJobs(character: Character): List<Job> {
        if (character.career.isRetired) return emptyList()
        if (!isJobEligible(character) || character.career.currentJob != null) return emptyList()

        return JobPool.getJobsForCountry(character.countryCode).filter { job ->
            meetsEducationRequirement(character, job.minEducation)
        }
    }

    /**
     * Hire roll using smarts, GPA, and criminal-record penalty (reduced after clean years since last arrest).
     *
     * @return [CareerResult.Hired] with salary scaled to country, or [CareerResult.Rejected].
     */
    fun applyForJob(character: Character, jobId: String): Pair<Character, CareerResult> {
        if (character.criminalRecord.awaitingTrial ||
            character.career.isRetired ||
            character.career.currentJob != null
        ) {
            return character to CareerResult.Rejected
        }

        val jobTemplate = JobPool.findById(jobId) ?: return character to CareerResult.Rejected
        if (!meetsEducationRequirement(character, jobTemplate.minEducation)) {
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

    /**
     * Annual work simulation: pays [calculateAnnualSalary], adjusts performance/happiness/health by [effort],
     * increments [CareerState.yearsAtCurrentJob].
     */
    fun workYear(character: Character, effort: WorkEffort): Character {
        val job = character.career.currentJob ?: return character

        val performanceDelta = EffortResolver.workYearPerformanceDelta(effort)
        val happinessDelta = EffortResolver.workYearHappinessDelta(effort)

        val annualPay = calculateAnnualSalary(job)
        val newPerformance = clampPerformanceScore(job.performanceScore + performanceDelta)
        val updatedJob = job.copy(performanceScore = newPerformance)
        val updatedStats = character.stats.copy(
            money = character.stats.money + annualPay,
            happiness = clampStat(character.stats.happiness + happinessDelta)
        )

        return healthEngine.applyWorkEffortStress(
            character.copy(
                stats = updatedStats,
                career = character.career.copy(
                    currentJob = updatedJob,
                    yearsAtCurrentJob = character.career.yearsAtCurrentJob + 1
                )
            ),
            effort
        )
    }

    /** Event-choice variant of work effort — performance/happiness only, no salary or tenure tick. */
    fun applyWorkEffort(character: Character, effort: WorkEffort): Character {
        val job = character.career.currentJob ?: return character

        val performanceDelta = EffortResolver.workEventPerformanceDelta(effort)
        val happinessDelta = EffortResolver.workEventHappinessDelta(effort)
        val newPerformance = clampPerformanceScore(job.performanceScore + performanceDelta)
        val updatedStats = character.stats.copy(
            happiness = clampStat(character.stats.happiness + happinessDelta)
        )

        return healthEngine.applyWorkEffortStress(
            character.copy(
                stats = updatedStats,
                career = character.career.copy(
                    currentJob = job.copy(performanceScore = newPerformance)
                )
            ),
            effort
        )
    }

    /**
     * Promotion check on interval years when performance ≥ threshold; bumps level and salary.
     *
     * @return Pair of updated character and whether promotion occurred.
     */
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

    /** Fires employee when [Job.performanceScore] falls below threshold; title moves to [CareerState.jobHistory]. */
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

    /** Voluntary resignation; clears [CareerState.currentJob] and appends title to history. */
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

    fun canRetire(character: Character): Boolean =
        character.age >= MIN_RETIREMENT_AGE &&
            character.career.currentJob != null &&
            !character.career.isRetired

    /** Mid-point pension quote for confirmation UI (actual rate is rolled at retirement). */
    fun estimateRetirementPension(character: Character): Int {
        val job = character.career.currentJob ?: return 0
        return calculatePensionAmount(
            annualSalary = calculateAnnualSalary(job),
            pensionRate = PENSION_RATE_MIDPOINT
        )
    }

    /**
     * Retires a character age 60+ with an active job: clears employment and sets yearly pension
     * to 40–60% of final salary (economy-scaled at retirement).
     */
    fun retire(character: Character): RetirementResult {
        if (!canRetire(character)) return RetirementResult.Ineligible

        val job = character.career.currentJob!!
        val pensionRate = Random.nextDouble(PENSION_RATE_MIN, PENSION_RATE_MAX)
        val pension = calculatePensionAmount(
            annualSalary = calculateAnnualSalary(job),
            pensionRate = pensionRate
        )

        return RetirementResult.Success(
            character.copy(
                career = character.career.copy(
                    isRetired = true,
                    pensionAmount = pension,
                    currentJob = null,
                    yearsAtCurrentJob = 0,
                    jobHistory = character.career.jobHistory + job.title
                ),
                stats = character.stats.copy(
                    happiness = clampStat(character.stats.happiness + RETIREMENT_HAPPINESS_BONUS)
                ),
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    "Retired from ${job.title} with a pension of ${formatMoney(pension, character.countryCode)} per year."
                )
            )
        )
    }

    /** Event-driven delta to [Job.performanceScore], clamped 0–100. */
    fun applyPerformanceEffect(character: Character, delta: Int): Character {
        val job = character.career.currentJob ?: return character
        val newScore = clampPerformanceScore(job.performanceScore + delta)
        return character.copy(
            career = character.career.copy(
                currentJob = job.copy(performanceScore = newScore)
            )
        )
    }

    /** One-time system event after [evaluatePromotion] succeeds. Assumes [character.career.currentJob] is set. */
    fun buildPromotionEvent(character: Character): LifeEvent {
        val job = character.career.currentJob!!
        return LifeEvent(
            id = PROMOTION_EVENT_ID,
            minAge = character.age,
            maxAge = character.age,
            text = "Your manager calls you in. After a strong review, you have been promoted to " +
                "${job.title} — Level ${job.level}. Your salary increases to " +
                "${formatMoney(job.baseSalary, character.countryCode)} per year.",
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

    /** One-time system event after [evaluateFiring]. */
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

    /** One-time layoff event (random downsizing, not performance-based). */
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

    /** Random layoff roll while employed and past minimum job age. */
    fun shouldTriggerDownsizing(character: Character): Boolean {
        if (character.career.currentJob == null) return false
        return character.age >= MIN_JOB_AGE && Random.nextFloat() < DOWNSIZING_CHANCE
    }

    /** Clears job without performance check; applies happiness penalty and logs layoff. */
    fun applyDownsizing(character: Character): Pair<Character, String> {
        val job = character.career.currentJob ?: return character to ""
        val title = job.title
        val updated = character.copy(
            career = character.career.copy(
                currentJob = null,
                yearsAtCurrentJob = 0,
                jobHistory = character.career.jobHistory + title
            ),
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness - DOWNSIZING_HAPPINESS_PENALTY)
            ),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "Laid off from $title during company downsizing."
            )
        )
        return updated to title
    }

    /** UI helper: job title and level, or "Unemployed". */
    fun formatCareerStatus(career: CareerState): String {
        val job = career.currentJob ?: return "Unemployed"
        return "${job.title} — Level ${job.level}"
    }

    /** Whether the character is still in the culture-shock window after moving abroad. */
    fun isCultureShockActive(character: Character): Boolean =
        character.birthCountryCode != character.countryCode &&
            character.yearsInCurrentCountry < CULTURE_SHOCK_YEARS

    /** Exposed for tests and UI hire previews. */
    fun hireSuccessChance(character: Character): Float = calculateHireChance(character)

    /** Annual gross pay equals [Job.baseSalary] (already economy-scaled at hire). */
    fun calculateAnnualSalary(job: Job): Int = job.baseSalary

    private fun calculatePensionAmount(
        annualSalary: Int,
        pensionRate: Double
    ): Int = (annualSalary * pensionRate).roundToInt()

    private fun calculateHireChance(character: Character): Float {
        val smartsFactor = character.stats.smarts / 100f * 0.35f
        val gpaFactor = (character.education.gpa / 4f).coerceIn(0f, 1f) * 0.25f
        val recordPenalty = criminalRecordHirePenalty(character)
        val cultureShockPenalty = if (isCultureShockActive(character)) {
            CULTURE_SHOCK_HIRE_PENALTY
        } else {
            0f
        }
        val base = 0.35f
        return (base + smartsFactor + gpaFactor - recordPenalty - cultureShockPenalty)
            .coerceIn(0.1f, 0.95f)
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
        character: Character,
        required: SchoolStage
    ): Boolean {
        val education = character.education
        if (education.expelled) return false

        return when (required) {
            SchoolStage.SECONDARY -> {
                if (education.droppedOutFrom == SchoolStage.SECONDARY && education.kcseGrade == null) {
                    return false
                }
                when (education.stage) {
                    SchoolStage.SECONDARY,
                    SchoolStage.UNIVERSITY,
                    SchoolStage.GRADUATED -> true
                    SchoolStage.NONE -> education.kcseGrade != null
                    else -> false
                }
            }
            SchoolStage.GRADUATED -> {
                if (education.droppedOutFrom == SchoolStage.UNIVERSITY) return false
                education.stage == SchoolStage.GRADUATED
            }
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
        private const val MIN_RETIREMENT_AGE = 60
        private const val PENSION_RATE_MIN = 0.40
        private const val PENSION_RATE_MAX = 0.60
        private const val PENSION_RATE_MIDPOINT = 0.50
        private const val RETIREMENT_HAPPINESS_BONUS = 8
        private const val PROMOTION_THRESHOLD = 65
        private const val FIRING_THRESHOLD = 20
        private const val PROMOTION_INTERVAL_YEARS = 3
        private const val DOWNSIZING_CHANCE = 0.04f
        private const val DOWNSIZING_HAPPINESS_PENALTY = 15
        private const val CRIMINAL_RECORD_HIRE_PENALTY = 0.15f
        const val CULTURE_SHOCK_YEARS = 3
        const val CULTURE_SHOCK_HIRE_PENALTY = 0.10f
    }
}
