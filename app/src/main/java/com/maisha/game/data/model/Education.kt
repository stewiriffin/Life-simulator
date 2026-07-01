// app/src/main/java/com/maisha/game/data/model/Education.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class SchoolStage {
    NONE,
    PRIMARY,
    SECONDARY,
    UNIVERSITY,
    GRADUATED
}

@Serializable
enum class StudyEffort {
    SLACK,
    NORMAL,
    HARD
}

@Serializable
enum class ExamType {
    KCPE,
    KCSE
}

@Serializable
data class ExamResult(
    val passed: Boolean,
    val grade: String,
    val score: Float
)

@Serializable
data class EducationState(
    val stage: SchoolStage = SchoolStage.NONE,
    val currentGrade: Int = 0,
    val gpa: Float = 0f,
    val expelled: Boolean = false,
    val courseOfStudy: String? = null,
    val schoolName: String? = null,
    val kcpePassed: Boolean? = null,
    val kcseGrade: String? = null
)
