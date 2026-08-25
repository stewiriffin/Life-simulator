// app/src/main/java/com/maisha/game/domain/CareerEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.CountryCatalog
import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.JobPool
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.CareerTrack
import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.HustleType
import com.maisha.game.data.model.PartTimeJob
import com.maisha.game.data.model.Job
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.SchoolClub
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.UniversityMajor
import com.maisha.game.data.model.VisaType
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

sealed class SideHustleResult {
    data class Success(val character: Character, val payout: Int) : SideHustleResult()
    data class Failed(val reason: SideHustleFailure) : SideHustleResult()
}

enum class SideHustleFailure {
    PREREQUISITES_NOT_MET,
    ALREADY_DONE_THIS_YEAR,
    INELIGIBLE
}

sealed class CareerTrackPracticeResult {
    data class Success(val character: Character) : CareerTrackPracticeResult()
    data object Ineligible : CareerTrackPracticeResult()
    data object MaxLevel : CareerTrackPracticeResult()
}

sealed class PartTimeJobResult {
    data class Success(val character: Character, val payout: Int) : PartTimeJobResult()
    data object Ineligible : PartTimeJobResult()
    data object AlreadyWorked : PartTimeJobResult()
}

@Singleton
class CareerEngine @Inject constructor(
    private val healthEngine: HealthEngine,
    private val relocationEngine: RelocationEngine
) {

    /** Minimum age and completed-or-enrolled secondary+ education; rejects expelled and incomplete dropouts. */
    fun isJobEligible(character: Character): Boolean {
        if (character.age < MIN_JOB_AGE) return false
        if (character.education.expelled) return false
        return meetsEducationRequirement(character, SchoolStage.SECONDARY)
    }

    /**
     * Per-job eligibility: education (or skill bypass), plus optional [Job.minFollowers] social gate.
     * Military jobs ([Job.isMilitary]) bypass education and only require age.
     * Degree-tier jobs ([offersWorkVisaSponsorship]) are eligible for international sponsorship applications.
     */
    fun isJobEligible(character: Character, job: Job): Boolean {
        if (character.age < MIN_JOB_AGE) return false
        if (character.criminalRecord.currentlyIncarcerated) return false
        if (job.isMilitary) {
            return character.age >= MIN_MILITARY_AGE && !character.education.expelled
        }
        if (!isJobEligible(character)) return false
        if (job.requiresDrivingLicense && !character.hasDrivingLicense) return false
        if (job.minFollowers > 0) {
            if (!character.socialMedia.hasAccount ||
                character.socialMedia.followers < job.minFollowers
            ) {
                return false
            }
        }
        if (meetsEducationRequirement(character, job.minEducation)) return true
        return meetsSkillBypass(character, job)
    }

    /** Degree-tier roles that can sponsor a [VisaType.WORK] visa from abroad. */
    fun offersWorkVisaSponsorship(job: Job): Boolean =
        job.minEducation == SchoolStage.GRADUATED || job.minEducation == SchoolStage.UNIVERSITY

    private fun meetsSkillBypass(character: Character, job: Job): Boolean {
        val skill = job.skillBypass ?: return false
        if (job.minSkillLevel <= 0) return false
        val level = character.skills.find { it.type == skill }?.level ?: 0
        return level >= job.minSkillLevel
    }

    /**
     * Local jobs plus sponsored degree-tier roles abroad (id suffix `@CC` for destination country).
     * Empty if already employed, retired, or ineligible.
     */
    fun getEligibleJobs(character: Character): List<Job> {
        if (character.career.isRetired) return emptyList()
        if (character.career.currentJob != null) return emptyList()
        if (character.age < MIN_JOB_AGE) return emptyList()

        val local = JobPool.getJobsForCountry(character.countryCode).filter { job ->
            isJobEligible(character, job)
        }
        val sponsoredAbroad = CountryCatalog.all()
            .asSequence()
            .filter { it.code != character.countryCode }
            .flatMap { country ->
                JobPool.getJobsForCountry(country.code)
                    .asSequence()
                    .filter { offersWorkVisaSponsorship(it) && isJobEligible(character, it) }
                    .map { job ->
                        job.copy(
                            id = sponsoredJobId(job.id, country.code),
                            title = "${job.title} (${country.displayName} — Work Visa)"
                        )
                    }
            }
            .distinctBy { it.id }
            .toList()
        return local + sponsoredAbroad
    }

    /**
     * Hire roll using smarts, GPA, and criminal-record penalty (reduced after clean years since last arrest).
     * Sponsored foreign listings (`jobId@CC`) relocate with a [VisaType.WORK] visa on success.
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

        val (baseJobId, sponsorCountry) = parseSponsoredJobId(jobId)
        val jobTemplate = JobPool.findById(baseJobId) ?: return character to CareerResult.Rejected
        if (!isJobEligible(character, jobTemplate)) {
            return character to CareerResult.Rejected
        }
        if (sponsorCountry != null) {
            if (!offersWorkVisaSponsorship(jobTemplate)) {
                return character to CareerResult.Rejected
            }
            if (sponsorCountry == character.countryCode) {
                return character to CareerResult.Rejected
            }
        }

        val successChance = calculateHireChance(character)
        if (Random.nextFloat() > successChance) {
            return character to CareerResult.Rejected
        }

        var hiredCharacter = character
        val workCountry = sponsorCountry ?: character.countryCode
        if (sponsorCountry != null) {
            hiredCharacter = relocationEngine.relocate(
                hiredCharacter,
                CountryCatalog.getCountry(sponsorCountry),
                VisaType.WORK
            )
        } else if (
            hiredCharacter.isLivingAbroad() &&
            offersWorkVisaSponsorship(jobTemplate) &&
            hiredCharacter.currentVisa != VisaType.WORK
        ) {
            hiredCharacter = hiredCharacter.copy(
                currentVisa = VisaType.WORK,
                visaYearsRemaining = relocationEngine.defaultVisaYears(VisaType.WORK)
            )
        }

        val hiredJob = jobTemplate.copy(
            baseSalary = EconomyScaler.scaleAmount(jobTemplate.baseSalary, workCountry),
            performanceScore = 50,
            isMilitary = jobTemplate.isMilitary,
            requiresDrivingLicense = jobTemplate.requiresDrivingLicense
        )
        val pendingDeployment = hiredJob.isMilitary && Random.nextFloat() < DEPLOYMENT_CHANCE
        val updatedCareer = hiredCharacter.career.copy(
            currentJob = hiredJob,
            yearsAtCurrentJob = 0,
            isDeployed = false,
            pendingDeployment = pendingDeployment
        )
        return hiredCharacter.copy(career = updatedCareer) to CareerResult.Hired(hiredJob)
    }

    fun sponsoredJobId(jobId: String, countryCode: String): String = "$jobId@$countryCode"

    fun parseSponsoredJobId(jobId: String): Pair<String, String?> {
        val separator = jobId.lastIndexOf('@')
        if (separator <= 0 || separator == jobId.lastIndex) return jobId to null
        val country = jobId.substring(separator + 1)
        if (country.length != 2) return jobId to null
        return jobId.substring(0, separator) to country
    }

    /**
     * Annual work simulation: pays [calculateAnnualSalary], adjusts performance/happiness/health by [effort],
     * increments [CareerState.yearsAtCurrentJob].
     * Military jobs may deploy ([DEPLOYMENT_CHANCE]): double hazard pay and set [CareerState.isDeployed].
     * [CareerState.pendingDeployment] schedules the next year's deployment for UI warning before Age Up.
     */
    fun workYear(character: Character, effort: WorkEffort): Character {
        val job = character.career.currentJob ?: return character

        val performanceDelta = EffortResolver.workYearPerformanceDelta(effort) + workPerformanceModifier(character)
        val happinessDelta = EffortResolver.workYearHappinessDelta(effort)

        val deployed = job.isMilitary && character.career.pendingDeployment
        val annualPay = calculateAnnualSalary(job)
        val effortGross = (annualPay * effortPayMultiplier(effort)).toInt().coerceAtLeast(0)
        val payThisYear = if (deployed) {
            (effortGross * HAZARD_PAY_MULTIPLIER).toInt()
        } else {
            effortGross
        }
        val tax = FinanceEngine.calculateIncomeTax(payThisYear, character.countryCode)
        val netPay = (payThisYear - tax).coerceAtLeast(0)
        val newPerformance = clampPerformanceScore(job.performanceScore + performanceDelta)
        val updatedJob = job.copy(performanceScore = newPerformance)
        val pendingNext = job.isMilitary && Random.nextFloat() < DEPLOYMENT_CHANCE
        var happinessDeltaTotal = happinessDelta
        val transitStress = needsPublicTransitCommute(character, job)
        if (transitStress) {
            happinessDeltaTotal -= PUBLIC_TRANSIT_STRESS
        }
        val updatedStats = character.stats.copy(
            money = character.stats.money + netPay,
            happiness = clampStat(character.stats.happiness + happinessDeltaTotal)
        )

        var updated = applyWorkStress(
            character.copy(
                stats = updatedStats,
                career = character.career.copy(
                    currentJob = updatedJob,
                    yearsAtCurrentJob = character.career.yearsAtCurrentJob + 1,
                    workEffortThisYear = effort,
                    plannedWorkEffort = effort,
                    isDeployed = deployed,
                    pendingDeployment = pendingNext
                )
            ),
            effort
        )
        val payLog = buildString {
            append("Paycheque: ${formatMoney(netPay, character.countryCode)} net")
            if (tax > 0) append(" after ${formatMoney(tax, character.countryCode)} tax")
            append(" (${effort.name.lowercase()} effort")
            if (deployed) append(", hazard pay")
            append(").")
        }
        updated = updated.copy(eventLog = EventLogCap.prepend(updated.eventLog, payLog))
        if (deployed) {
            updated = updated.copy(
                eventLog = EventLogCap.prepend(
                    updated.eventLog,
                    "Deployed on active duty. Hazard pay applied; combat risk is elevated."
                )
            )
        }
        if (transitStress) {
            updated = updated.copy(
                eventLog = EventLogCap.prepend(
                    updated.eventLog,
                    "Commuting without a vehicle wore you down (public transit stress)."
                )
            )
        }
        return updated
    }

    fun setPlannedWorkEffort(character: Character, effort: WorkEffort): Character {
        if (character.career.currentJob == null || character.career.isRetired) return character
        if (character.career.plannedWorkEffort == effort) return character
        return character.copy(
            career = character.career.copy(plannedWorkEffort = effort),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "You plan to ${effort.name.lowercase()} at work this year."
            )
        )
    }

    private fun effortPayMultiplier(effort: WorkEffort): Double = when (effort) {
        WorkEffort.COAST -> COAST_PAY_MULTIPLIER
        WorkEffort.NORMAL -> 1.0
        WorkEffort.GRIND -> GRIND_PAY_MULTIPLIER
    }

    /** High-tier roles without a personal vehicle incur yearly commute stress. */
    fun needsPublicTransitCommute(character: Character, job: Job? = null): Boolean {
        val activeJob = job ?: character.career.currentJob ?: return false
        if (!isHighTierJob(activeJob)) return false
        return !ownsVehicle(character)
    }

    fun isHighTierJob(job: Job): Boolean =
        job.minEducation == SchoolStage.GRADUATED ||
            job.minEducation == SchoolStage.UNIVERSITY ||
            job.level >= HIGH_TIER_JOB_LEVEL

    fun ownsVehicle(character: Character): Boolean =
        character.assets.any {
            it.type == AssetType.CAR || it.type == AssetType.MOTORBIKE
        }

    /**
     * Earns extra cash from a side gig; applies happiness/health burnout.
     * One hustle per in-game year; blocked when incarcerated, retired, or awaiting trial.
     */
    fun executeSideHustle(character: Character, hustleType: HustleType): SideHustleResult {
        if (!canAttemptSideHustle(character)) {
            return SideHustleResult.Failed(SideHustleFailure.INELIGIBLE)
        }
        if (character.career.sideHustleDoneThisYear) {
            return SideHustleResult.Failed(SideHustleFailure.ALREADY_DONE_THIS_YEAR)
        }
        if (!JobPool.meetsSideHustlePrerequisites(character, hustleType)) {
            return SideHustleResult.Failed(SideHustleFailure.PREREQUISITES_NOT_MET)
        }

        val spec = JobPool.getSideHustleSpec(hustleType) ?: return SideHustleResult.Failed(
            SideHustleFailure.PREREQUISITES_NOT_MET
        )
        val payout = calculateSideHustlePayout(character, spec)
        val (happinessPenalty, healthPenalty) = calculateSideHustleBurnout(character)

        val updated = character.copy(
            stats = character.stats.copy(
                money = character.stats.money + payout,
                happiness = clampStat(character.stats.happiness - happinessPenalty),
                health = clampStat(character.stats.health - healthPenalty)
            ),
            career = character.career.copy(sideHustleDoneThisYear = true),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "Side hustle (${hustleLabel(hustleType)}): earned ${formatMoney(payout, character.countryCode)}."
            )
        )
        return SideHustleResult.Success(updated, payout)
    }

    fun canAttemptSideHustle(character: Character): Boolean =
        character.alive &&
            character.age >= MIN_SIDE_HUSTLE_AGE &&
            !character.criminalRecord.currentlyIncarcerated &&
            !character.criminalRecord.awaitingTrial &&
            !character.career.isRetired

    fun isSideHustleAvailable(character: Character, hustleType: HustleType): Boolean =
        canAttemptSideHustle(character) &&
            !character.career.sideHustleDoneThisYear &&
            JobPool.meetsSideHustlePrerequisites(character, hustleType)

    /** Event-choice variant of work effort — performance/happiness only, no salary or tenure tick. */
    fun applyWorkEffort(character: Character, effort: WorkEffort): Character {
        val job = character.career.currentJob ?: return character

        val performanceDelta = EffortResolver.workEventPerformanceDelta(effort) + workPerformanceModifier(character)
        val happinessDelta = EffortResolver.workEventHappinessDelta(effort)
        val newPerformance = clampPerformanceScore(job.performanceScore + performanceDelta)
        val updatedStats = character.stats.copy(
            happiness = clampStat(character.stats.happiness + happinessDelta)
        )

        return applyWorkStress(
            character.copy(
                stats = updatedStats,
                career = character.career.copy(
                    currentJob = job.copy(performanceScore = newPerformance),
                    workEffortThisYear = effort
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
                jobHistory = character.career.jobHistory + job.title,
                isDeployed = false,
                pendingDeployment = false
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
                jobHistory = character.career.jobHistory + title,
                isDeployed = false,
                pendingDeployment = false
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
        character.isLivingAbroad() &&
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
        val looksFactor = character.stats.looks / 100f * LOOKS_HIRE_WEIGHT
        val healthFactor = character.stats.health / 100f * HEALTH_HIRE_WEIGHT
        val recordPenalty = criminalRecordHirePenalty(character)
        val cultureShockPenalty = if (isCultureShockActive(character)) {
            CULTURE_SHOCK_HIRE_PENALTY
        } else {
            0f
        }
        val base = 0.35f
        return (base + smartsFactor + gpaFactor + looksFactor + healthFactor - recordPenalty - cultureShockPenalty)
            .coerceIn(0.1f, 0.95f)
    }

    private fun criminalRecordHirePenalty(character: Character): Float {
        if (character.criminalRecord.recordExpunged) return 0f
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

    private fun applyWorkStress(character: Character, effort: WorkEffort): Character {
        val burnoutMultiplier = if (
            character.career.sideHustleDoneThisYear && effort == WorkEffort.GRIND
        ) {
            SIDE_HUSTLE_BURNOUT_MULTIPLIER
        } else {
            1f
        }
        return healthEngine.applyWorkEffortStress(character, effort, burnoutMultiplier)
    }

    private fun workPerformanceModifier(character: Character): Int {
        var modifier = 0
        if (character.stats.smarts >= 75) modifier += 3
        if (character.stats.happiness >= 70) modifier += 2
        if (character.stats.health >= 70) modifier += 2
        if (character.stats.happiness < 35) modifier -= 4
        if (character.stats.health < 35) modifier -= 4
        if (character.stats.smarts < 35) modifier -= 3
        return modifier
    }

    private fun calculateSideHustlePayout(
        character: Character,
        spec: JobPool.SideHustleSpec
    ): Int {
        val smartsFactor = 0.75f + (character.stats.smarts / 100f) * 0.5f
        val raw = Random.nextInt(spec.basePayoutMin, spec.basePayoutMax + 1)
        return EconomyScaler.scaleAmount((raw * smartsFactor).roundToInt(), character.countryCode)
    }

    private fun calculateSideHustleBurnout(character: Character): Pair<Int, Int> {
        var happinessPenalty = Random.nextInt(SIDE_HUSTLE_HAPPINESS_MIN, SIDE_HUSTLE_HAPPINESS_MAX + 1)
        var healthPenalty = Random.nextInt(SIDE_HUSTLE_HEALTH_MIN, SIDE_HUSTLE_HEALTH_MAX + 1)
        if (character.career.workEffortThisYear == WorkEffort.GRIND) {
            happinessPenalty = (happinessPenalty * SIDE_HUSTLE_BURNOUT_MULTIPLIER).roundToInt()
            healthPenalty = (healthPenalty * SIDE_HUSTLE_BURNOUT_MULTIPLIER).roundToInt()
        }
        return happinessPenalty to healthPenalty
    }

    private fun hustleLabel(type: HustleType): String = when (type) {
        HustleType.RIDE_SHARE -> "ride-share"
        HustleType.FREELANCE_CODING -> "freelance coding"
        HustleType.TUTORING -> "tutoring"
        HustleType.FOOD_DELIVERY -> "food delivery"
        HustleType.RESELLING -> "reselling"
    }

    fun canStartCareerTrack(character: Character, track: CareerTrack): Boolean {
        if (!character.alive || character.criminalRecord.currentlyIncarcerated) return false
        if (character.career.careerTrack != CareerTrack.NONE) return false
        if (character.age < MIN_TRACK_AGE) return false
        return when (track) {
            CareerTrack.ENTERTAINMENT -> true
            CareerTrack.PRO_SPORTS -> character.education.schoolClub == SchoolClub.FOOTBALL ||
                character.education.clubResumeClub == SchoolClub.FOOTBALL ||
                character.stats.health >= PRO_SPORTS_MIN_HEALTH
            CareerTrack.MEDICAL -> character.education.universityMajor == UniversityMajor.MEDICINE ||
                character.education.universityMajor == UniversityMajor.NURSING ||
                character.education.courseOfStudy == "Medicine" ||
                character.stats.smarts >= MEDICAL_MIN_SMARTS
            CareerTrack.LEGAL -> character.education.universityMajor == UniversityMajor.LAW ||
                character.education.courseOfStudy == "Law" ||
                character.stats.smarts >= LEGAL_MIN_SMARTS
            CareerTrack.SOFTWARE ->
                character.education.universityMajor == UniversityMajor.COMPUTER_SCIENCE ||
                    character.education.universityMajor == UniversityMajor.ENGINEERING ||
                    character.education.courseOfStudy == "Computer Science" ||
                    character.stats.smarts >= SOFTWARE_MIN_SMARTS
            CareerTrack.CORPORATE ->
                character.education.universityMajor == UniversityMajor.BUSINESS ||
                    character.education.courseOfStudy == "Business" ||
                    character.stats.smarts >= CORPORATE_MIN_SMARTS
            CareerTrack.NONE -> false
        }
    }

    fun startCareerTrack(character: Character, track: CareerTrack): Character {
        if (!canStartCareerTrack(character, track)) return character
        val label = when (track) {
            CareerTrack.ENTERTAINMENT -> "entertainment"
            CareerTrack.PRO_SPORTS -> "pro sports"
            CareerTrack.MEDICAL -> "medical"
            CareerTrack.LEGAL -> "legal"
            CareerTrack.SOFTWARE -> "software engineering"
            CareerTrack.CORPORATE -> "corporate / banking"
            CareerTrack.NONE -> return character
        }
        return character.copy(
            career = character.career.copy(
                careerTrack = track,
                trackLevel = 0,
                trackProgress = 0
            ),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "You started pursuing a $label career."
            )
        )
    }

    fun practiceCareerTrack(character: Character): CareerTrackPracticeResult {
        val track = character.career.careerTrack
        if (track == CareerTrack.NONE || !character.alive) {
            return CareerTrackPracticeResult.Ineligible
        }
        if (character.career.trackLevel >= MAX_TRACK_LEVEL) {
            return CareerTrackPracticeResult.MaxLevel
        }
        val progressGain = TRACK_PRACTICE_GAIN + Random.nextInt(0, 6)
        val happinessDelta = when (track) {
            CareerTrack.PRO_SPORTS -> -2
            CareerTrack.MEDICAL, CareerTrack.LEGAL, CareerTrack.SOFTWARE -> -2
            else -> -1
        }
        var updated = character.copy(
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness + happinessDelta),
                smarts = when (track) {
                    CareerTrack.ENTERTAINMENT, CareerTrack.MEDICAL, CareerTrack.LEGAL,
                    CareerTrack.SOFTWARE, CareerTrack.CORPORATE ->
                        clampStat(character.stats.smarts + 1)
                    else -> character.stats.smarts
                },
                health = when (track) {
                    CareerTrack.PRO_SPORTS, CareerTrack.MEDICAL ->
                        clampStat(character.stats.health + 1)
                    else -> character.stats.health
                }
            )
        )
        var newProgress = updated.career.trackProgress + progressGain
        var newLevel = updated.career.trackLevel
        if (newProgress >= TRACK_LEVEL_THRESHOLD) {
            newProgress = 0
            newLevel = (newLevel + 1).coerceAtMost(MAX_TRACK_LEVEL)
            updated = updated.copy(
                eventLog = EventLogCap.prepend(
                    updated.eventLog,
                    trackLevelUpMessage(track, newLevel)
                )
            )
            if (track == CareerTrack.ENTERTAINMENT && newLevel >= 2 && !updated.socialMedia.hasAccount) {
                updated = updated.copy(
                    socialMedia = updated.socialMedia.copy(hasAccount = true, followers = 500)
                )
            }
        }
        updated = updated.copy(
            career = updated.career.copy(
                trackProgress = newProgress,
                trackLevel = newLevel
            )
        )
        return CareerTrackPracticeResult.Success(updated)
    }

    fun tickCareerTrackYear(character: Character): Character {
        val track = character.career.careerTrack
        if (track == CareerTrack.NONE || character.career.trackLevel >= MAX_TRACK_LEVEL) {
            return character
        }
        var newProgress = character.career.trackProgress + TRACK_PASSIVE_GAIN
        var newLevel = character.career.trackLevel
        var updated = character
        if (newProgress >= TRACK_LEVEL_THRESHOLD) {
            newProgress = 0
            newLevel = (newLevel + 1).coerceAtMost(MAX_TRACK_LEVEL)
            updated = updated.copy(
                eventLog = EventLogCap.prepend(
                    updated.eventLog,
                    trackLevelUpMessage(track, newLevel)
                )
            )
        }
        return updated.copy(
            career = updated.career.copy(
                trackProgress = newProgress,
                trackLevel = newLevel
            )
        )
    }

    private fun trackLevelUpMessage(track: CareerTrack, level: Int): String = when (track) {
        CareerTrack.ENTERTAINMENT -> when (level) {
            1 -> "You booked your first paid local gig."
            2 -> "Regional promoters started noticing your act."
            3 -> "You signed with a label — you're a rising star."
            else -> "Your entertainment career advanced."
        }
        CareerTrack.PRO_SPORTS -> when (level) {
            1 -> "You made a semi-pro squad."
            2 -> "A pro team offered you a contract."
            3 -> "You're a star athlete with national buzz."
            else -> "Your sports career advanced."
        }
        CareerTrack.MEDICAL -> when (level) {
            1 -> "You completed clinical rotations."
            2 -> "You earned your medical license."
            3 -> "You're a respected specialist in your field."
            else -> "Your medical career advanced."
        }
        CareerTrack.LEGAL -> when (level) {
            1 -> "You passed the bar exam."
            2 -> "You made partner at a regional firm."
            3 -> "You're a sought-after litigator."
            else -> "Your legal career advanced."
        }
        CareerTrack.SOFTWARE -> when (level) {
            1 -> "You shipped your first production feature."
            2 -> "You were promoted to senior engineer."
            3 -> "You're a principal engineer with equity upside."
            else -> "Your software career advanced."
        }
        CareerTrack.CORPORATE -> when (level) {
            1 -> "You closed your first major client deal."
            2 -> "You moved into management."
            3 -> "You're a director with banking connections."
            else -> "Your corporate career advanced."
        }
        CareerTrack.NONE -> "Your career track advanced."
    }

    /** Teen after-school job (once per year, ages 14–17). */
    fun workPartTime(character: Character, job: PartTimeJob): PartTimeJobResult {
        if (!character.alive || character.criminalRecord.currentlyIncarcerated) {
            return PartTimeJobResult.Ineligible
        }
        if (character.age !in MIN_PART_TIME_AGE..MAX_PART_TIME_AGE) {
            return PartTimeJobResult.Ineligible
        }
        if (character.career.partTimeWorkedThisYear) return PartTimeJobResult.AlreadyWorked

        val spec = partTimeSpec(job)
        val smartsBonus = if (job == PartTimeJob.TUTORING) {
            0.85f + character.stats.smarts / 100f * 0.3f
        } else {
            1f
        }
        val payout = EconomyScaler.scaleAmount(
            (Random.nextInt(spec.minPayout, spec.maxPayout + 1) * smartsBonus).roundToInt(),
            character.countryCode
        )
        val happinessDelta = when (job) {
            PartTimeJob.TUTORING -> 2
            PartTimeJob.BABYSITTING -> 1
            PartTimeJob.FAST_FOOD -> -1
            PartTimeJob.RETAIL -> 0
        }
        val updated = character.copy(
            stats = character.stats.copy(
                money = character.stats.money + payout,
                happiness = clampStat(character.stats.happiness + happinessDelta),
                smarts = if (job == PartTimeJob.TUTORING) {
                    clampStat(character.stats.smarts + 1)
                } else {
                    character.stats.smarts
                }
            ),
            career = character.career.copy(partTimeWorkedThisYear = true),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "You worked a ${spec.label} shift and earned ${formatMoney(payout, character.countryCode)}."
            )
        )
        return PartTimeJobResult.Success(updated, payout)
    }

    private data class PartTimeSpec(val label: String, val minPayout: Int, val maxPayout: Int)

    private fun partTimeSpec(job: PartTimeJob): PartTimeSpec = when (job) {
        PartTimeJob.RETAIL -> PartTimeSpec("retail", 800, 2_200)
        PartTimeJob.FAST_FOOD -> PartTimeSpec("fast-food", 700, 1_800)
        PartTimeJob.BABYSITTING -> PartTimeSpec("babysitting", 1_000, 2_800)
        PartTimeJob.TUTORING -> PartTimeSpec("tutoring", 1_500, 4_000)
    }

    companion object {
        const val CAREER_SYSTEM_TAG = "career_system"
        /** Event gate: only eligible when the character has a current job. */
        const val REQUIRES_JOB_TAG = "requires_job"
        const val PROMOTION_EVENT_ID = "career_promotion_system"
        const val FIRING_EVENT_ID = "career_firing_system"
        const val DOWNSIZING_EVENT_ID = "career_downsizing_system"
        private const val ONE_TIME_TAG = "one_time"

        private const val MIN_JOB_AGE = 18
        const val MIN_MILITARY_AGE = 18
        const val DEPLOYMENT_CHANCE = 0.20f
        const val HAZARD_PAY_MULTIPLIER = 2
        const val REQUIRES_MILITARY_TAG = "requires_military"
        const val REQUIRES_VEHICLE_TAG = "requires_vehicle"
        private const val PUBLIC_TRANSIT_STRESS = 4
        private const val HIGH_TIER_JOB_LEVEL = 3
        private const val COAST_PAY_MULTIPLIER = 0.72
        private const val GRIND_PAY_MULTIPLIER = 1.38
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
        private const val LOOKS_HIRE_WEIGHT = 0.08f
        private const val HEALTH_HIRE_WEIGHT = 0.07f
        const val CULTURE_SHOCK_YEARS = 3
        const val CULTURE_SHOCK_HIRE_PENALTY = 0.10f
        private const val MIN_SIDE_HUSTLE_AGE = 16
        private const val SIDE_HUSTLE_HAPPINESS_MIN = 2
        private const val SIDE_HUSTLE_HAPPINESS_MAX = 5
        private const val SIDE_HUSTLE_HEALTH_MIN = 1
        private const val SIDE_HUSTLE_HEALTH_MAX = 3
        const val SIDE_HUSTLE_BURNOUT_MULTIPLIER = 2f
        const val MIN_TRACK_AGE = 16
        private const val PRO_SPORTS_MIN_HEALTH = 55
        private const val TRACK_PRACTICE_GAIN = 18
        private const val TRACK_PASSIVE_GAIN = 8
        private const val TRACK_LEVEL_THRESHOLD = 100
        private const val MAX_TRACK_LEVEL = 3
        private const val MEDICAL_MIN_SMARTS = 65
        private const val LEGAL_MIN_SMARTS = 62
        private const val SOFTWARE_MIN_SMARTS = 60
        private const val CORPORATE_MIN_SMARTS = 55
        const val MIN_PART_TIME_AGE = 14
        const val MAX_PART_TIME_AGE = 17
    }
}
