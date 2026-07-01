// app/src/main/java/com/maisha/game/data/model/Career.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Job(
    val id: String,
    val title: String,
    val minEducation: SchoolStage,
    val baseSalary: Int,
    val level: Int = 1,
    val performanceScore: Int = 50
)

@Serializable
data class CareerState(
    val currentJob: Job? = null,
    val jobHistory: List<String> = emptyList(),
    val yearsAtCurrentJob: Int = 0
)

@Serializable
enum class WorkEffort {
    COAST,
    NORMAL,
    GRIND
}
