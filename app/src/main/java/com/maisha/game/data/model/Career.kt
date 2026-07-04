// app/src/main/java/com/maisha/game/data/model/Career.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

/**
 * Employment snapshot. [Job.performanceScore] is 0–100 and drives promotion/firing.
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
 * Career history for a [Character]: current job, past titles, tenure.
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
    /** True during a year the character is on active deployment (hazard pay + combat risk). */
    val isDeployed: Boolean = false,
    /** If true, the next [com.maisha.game.domain.CareerEngine.workYear] will be a deployment. */
    val pendingDeployment: Boolean = false
)

@Serializable
enum class WorkEffort {
    COAST,
    NORMAL,
    GRIND
}

@Serializable
enum class HustleType {
    RIDE_SHARE,
    FREELANCE_CODING,
    TUTORING,
    FOOD_DELIVERY,
    RESELLING
}
