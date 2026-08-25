// app/src/main/java/com/maisha/game/data/model/Career.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

/**
 * Employment snapshot. [performanceScore] (0–100) mirrors [CareerState.performance] for legacy callers.
 */
@Serializable
data class Job(
    val id: String,
    val title: String,
    val minEducation: SchoolStage,
    val baseSalary: Int,
    val level: Int = 1,
    val performanceScore: Int = 50,
    /** Minimum [SocialMediaState.followers] required to apply; 0 means no social gate. */
    val minFollowers: Int = 0,
    /** Optional skill that can substitute for [minEducation] when [minSkillLevel] is met. */
    val skillBypass: SkillType? = null,
    val minSkillLevel: Int = 0,
    /** Military track: education gates are bypassed; enables deployments and combat risk. */
    val isMilitary: Boolean = false,
    /** Requires [Character.hasDrivingLicense] to apply. */
    val requiresDrivingLicense: Boolean = false
)

/**
 * Career history for a [Character]: current job, past titles, tenure, and office politics.
 *
 * [performance] / [stress] are 0–100 floats. [bossRelationship] is −100…100.
 * [colleagueRelationships] maps coworker display names → standing (−100…100).
 */
@Serializable
data class CareerState(
    val currentJob: Job? = null,
    val jobHistory: List<String> = emptyList(),
    val yearsAtCurrentJob: Int = 0,
    val isRetired: Boolean = false,
    val pensionAmount: Int = 0,
    val sideHustleDoneThisYear: Boolean = false,
    val workEffortThisYear: WorkEffort? = null,
    /**
     * Effort chosen for the next Age Up paycheque (Coast / Normal / Grind).
     * Cleared into [workEffortThisYear] when [com.maisha.game.domain.CareerEngine.workYear] runs.
     */
    val plannedWorkEffort: WorkEffort = WorkEffort.NORMAL,
    /** True during a year the character is on active deployment (hazard pay + combat risk). */
    val isDeployed: Boolean = false,
    /** If true, the next [com.maisha.game.domain.CareerEngine.workYear] will be a deployment. */
    val pendingDeployment: Boolean = false,
    val careerTrack: CareerTrack = CareerTrack.NONE,
    /** 0 = entry, 3 = top tier within the track. */
    val trackLevel: Int = 0,
    /** Practice progress toward the next [trackLevel]; resets on level-up. */
    val trackProgress: Int = 0,
    /** Teen part-time job payout claimed this in-game year. */
    val partTimeWorkedThisYear: Boolean = false,
    /** Active student part-time role for study/work balance (cleared on Age Up after school tick). */
    val activePartTimeJob: PartTimeJob? = null,
    /** Student energy pool (0–100); drained by shifts and hustles, recovers yearly. */
    val energyLevel: Int = 100,
    /** Mid-year rest used to recover student energy (once per year). */
    val energyRestedThisYear: Boolean = false,
    /** Employer display name while employed. */
    val companyName: String? = null,
    /** Fine-grained job performance (0–100); kept in sync with [Job.performanceScore]. */
    val performance: Float = 50f,
    /** Workplace stress (0–100). */
    val stress: Float = 25f,
    /** Standing with your manager (−100…100). */
    val bossRelationship: Int = 10,
    /** Named coworker standings (−100…100). */
    val colleagueRelationships: Map<String, Int> = emptyMap(),
    /** Work Harder / Slack Off used this year. */
    val officeWorkActionDoneThisYear: Boolean = false,
    /** Kiss-up / network action used this year. */
    val kissUpDoneThisYear: Boolean = false,
    /** Ask for promotion / raise used this year. */
    val askPromotionDoneThisYear: Boolean = false,
    /** Network / coffee with a coworker used this year. */
    val networkColleagueDoneThisYear: Boolean = false,
    /** Lifetime promotions earned (ladder climbs). */
    val promotionsEarned: Int = 0
)

@Serializable
enum class WorkEffort {
    COAST,
    NORMAL,
    GRIND
}

/** Mid-year office action from the Career dashboard. */
@Serializable
enum class OfficeAction {
    WORK_HARDER,
    SLACK_OFF,
    KISS_UP,
    ASK_PROMOTION,
    REQUEST_RAISE,
    SEARCH_JOBS,
    /** Coffee / lunch with a coworker to raise standing. */
    NETWORK_COLLEAGUE
}

/** Resolution for office-politics Age Up / choice dialogs. */
@Serializable
enum class OfficePoliticsAction {
    CONFRONT_BOSS,
    ACCEPT_PASS_OVER,
    LOOK_FOR_JOB,
    ENGAGE_ROMANCE,
    DECLINE_ROMANCE,
    EMBEZZLE,
    WHISTLEBLOW,
    STAY_SILENT,
    /** Performance-improvement plan: grind harder. */
    ACCEPT_PIP,
    /** Quit rather than stay on a PIP. */
    QUIT_OVER_PIP,
    /** Defend yourself when a coworker takes credit. */
    DEFEND_CREDIT,
    /** Let the credit theft slide. */
    LET_CREDIT_GO
}

/** Long-form career ladder parallel to regular jobs. */
@Serializable
enum class CareerTrack {
    NONE,
    ENTERTAINMENT,
    PRO_SPORTS,
    MEDICAL,
    LEGAL,
    SOFTWARE,
    CORPORATE
}

/**
 * Student / teen part-time roles (secondary + university).
 * [demand] drives study-balance stress when paired with hard studying.
 */
@Serializable
enum class PartTimeJob(
    val displayLabel: String,
    val demand: PartTimeDemand
) {
    FAST_FOOD("Fast Food Crew", PartTimeDemand.HIGH),
    BARISTA("Barista", PartTimeDemand.MEDIUM),
    RETAIL("Retail Associate", PartTimeDemand.MEDIUM),
    BABYSITTING("Babysitter", PartTimeDemand.MEDIUM),
    TUTORING("Tutor", PartTimeDemand.LOW),
    FREELANCE_CODER("Freelance Coder", PartTimeDemand.HIGH)
}

/** How draining a [PartTimeJob] is alongside school. */
@Serializable
enum class PartTimeDemand {
    LOW,
    MEDIUM,
    HIGH
}

@Serializable
enum class HustleType {
    RIDE_SHARE,
    FREELANCE_CODING,
    TUTORING,
    FOOD_DELIVERY,
    RESELLING,
    /** Student-friendly: sell handmade crafts. */
    HANDMADE_CRAFTS,
    /** Student-friendly: stream games / content. */
    STREAMING,
    /** Student-friendly: small paid scripts / freelance micro-gigs. */
    SCRIPT_CODING
}

/**
 * Hierarchical titles per job family. Level 1 = entry; higher levels climb the ladder.
 */
object JobLadder {
    private val SOFTWARE = listOf(
        "Junior Developer",
        "Mid-Level Developer",
        "Senior Developer",
        "Lead Architect",
        "CTO"
    )
    private val MEDICAL = listOf(
        "Junior Clinician",
        "Resident",
        "Attending Physician",
        "Department Head",
        "Chief Medical Officer"
    )
    private val LEGAL = listOf(
        "Junior Associate",
        "Associate",
        "Senior Associate",
        "Partner",
        "Managing Partner"
    )
    private val CORPORATE = listOf(
        "Analyst",
        "Associate",
        "Manager",
        "Director",
        "VP / Executive"
    )
    private val EDUCATION = listOf(
        "Assistant Teacher",
        "Teacher",
        "Senior Teacher",
        "Head of Department",
        "Principal"
    )
    private val MEDIA = listOf(
        "Junior Reporter",
        "Reporter",
        "Senior Correspondent",
        "Editor",
        "Editor-in-Chief"
    )
    private val SERVICE = listOf(
        "Trainee",
        "Staff",
        "Senior Staff",
        "Supervisor",
        "Manager"
    )
    private val MILITARY = listOf(
        "Private",
        "Corporal",
        "Sergeant",
        "Lieutenant",
        "Captain"
    )
    private val GENERAL = listOf(
        "Junior",
        "Associate",
        "Senior",
        "Lead",
        "Head"
    )

    private val COMPANIES = listOf(
        "Horizon Labs",
        "Nexus Group",
        "Summit Holdings",
        "Amani Works",
        "BrightPath Co.",
        "Cedar & Stone",
        "Pulse Media",
        "Atlas Health",
        "Legacy Bank",
        "Cityline Services",
        "Northwind Soft",
        "Sunrise Retail"
    )

    fun ladderForJobId(jobId: String): List<String> {
        val id = jobId.substringBefore('@').lowercase()
        return when {
            id.contains("software") || id.contains("developer") || id.contains("engineer") -> SOFTWARE
            id.contains("doctor") || id.contains("nurse") || id.contains("surgeon") -> MEDICAL
            id.contains("lawyer") || id.contains("attorney") || id.contains("legal") -> LEGAL
            id.contains("accountant") || id.contains("bank") || id.contains("finance") ||
                id.contains("manager") || id.contains("executive") -> CORPORATE
            id.contains("teacher") || id.contains("professor") -> EDUCATION
            id.contains("journalist") || id.contains("reporter") || id.contains("editor") -> MEDIA
            id.contains("soldier") || id.contains("military") || id.contains("army") -> MILITARY
            id.contains("shop") || id.contains("guard") || id.contains("driver") ||
                id.contains("chef") || id.contains("delivery") || id.contains("trucker") -> SERVICE
            else -> GENERAL
        }
    }

    fun titleFor(jobId: String, level: Int, fallbackTitle: String): String {
        val ladder = ladderForJobId(jobId)
        val index = (level - 1).coerceIn(0, ladder.lastIndex)
        val rung = ladder[index]
        // Keep recognizable base role in the title for non-generic ladders.
        return if (ladder === GENERAL || ladder === SERVICE) {
            "$rung $fallbackTitle".replace(Regex("\\s+"), " ").trim()
        } else {
            rung
        }
    }

    fun maxLevel(jobId: String): Int = ladderForJobId(jobId).size

    fun randomCompanyName(seed: Int = kotlin.random.Random.nextInt()): String =
        COMPANIES[seed.mod(COMPANIES.size)]

    fun defaultColleagues(seed: Int = kotlin.random.Random.nextInt()): Map<String, Int> {
        val names = listOf(
            "Alex", "Jordan", "Sam", "Riley", "Casey", "Morgan", "Taylor", "Quinn"
        )
        val start = seed.mod(names.size)
        return listOf(
            names[start] to 35,
            names[(start + 2) % names.size] to 40,
            names[(start + 4) % names.size] to 30
        ).toMap()
    }
}
