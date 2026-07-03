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
    val performanceScore: Int = 50
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
    val pensionAmount: Int = 0
)

@Serializable
enum class WorkEffort {
    COAST,
    NORMAL,
    GRIND
}
