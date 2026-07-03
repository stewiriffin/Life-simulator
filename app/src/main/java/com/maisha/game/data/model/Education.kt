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

/**
 * School progression for a [Character].
 *
 * Internal exam fields use KCPE/KCSE names historically; UI shows country-specific exam names.
 *
 * @property expelled When true, blocks enrollment and grade advance. **Note:** no gameplay path currently sets this.
 * @property kcpePassed Primary exit exam pass flag; gates secondary enrollment at age 14.
 * @property kcseGrade Letter grade string; gates university via points threshold.
 */
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
