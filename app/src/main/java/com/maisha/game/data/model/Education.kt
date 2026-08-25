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

/** Secondary-school extracurricular — one active club while enrolled. */
@Serializable
enum class SchoolClub {
    DEBATE,
    FOOTBALL,
    DRAMA,
    CODING,
    MUSIC
}

/** University majors offered after secondary exams. */
@Serializable
enum class UniversityMajor(val courseLabel: String) {
    LAW("Law"),
    MEDICINE("Medicine"),
    COMPUTER_SCIENCE("Computer Science"),
    BUSINESS("Business"),
    ENGINEERING("Engineering"),
    NURSING("Nursing")
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

/** People you meet and interact with while enrolled. */
@Serializable
enum class SchoolRole {
    CLASSMATE,
    BEST_CLASSMATE,
    BULLY,
    TEACHER,
    CRUSH
}

/**
 * A named school NPC (classmate, bully, crush, or teacher).
 * Cleared when you leave school / graduate.
 *
 * @property traits BitLife-style personality tags shown on their profile.
 * @property secret Hidden detail revealed when bond is high enough.
 * @property status Public gossip / role status at school.
 * @property secretRevealed Whether the player has learned [secret].
 */
@Serializable
data class SchoolPerson(
    val id: String,
    val name: String,
    val role: SchoolRole,
    val gender: Gender = Gender.MALE,
    val age: Int,
    val relationshipLevel: Int = 50,
    val subject: String? = null,
    val avatarConfig: AvatarConfig = AvatarConfig.DEFAULT,
    val interactedThisYear: Boolean = false,
    val traits: List<String> = emptyList(),
    val secret: String? = null,
    val status: String? = null,
    val secretRevealed: Boolean = false
)

/** Direct BitLife-style interactions with a [SchoolPerson]. */
@Serializable
enum class SchoolPersonAction {
    CHAT,
    COMPLIMENT,
    INSULT,
    ASK_OUT,
    SPREAD_RUMOR,
    BRIBE_GIFT
}

/** Player-driven school activities (one academic + one social per year). */
@Serializable
enum class SchoolActivity {
    STUDY_GROUP,
    LIBRARY_STUDY,
    ASK_TEACHER_HELP,
    HANG_OUT,
    CONFRONT_BULLY,
    SKIP_CLASS,
    SCHOOL_DANCE,
    CLUB_PRACTICE,
    GROUP_PROJECT
}

/**
 * School progression for a [Character].
 *
 * Internal exam fields use KCPE/KCSE names historically; UI shows country-specific exam names.
 *
 * @property expelled When true, blocks enrollment and grade advance (set by expulsion events).
 * @property droppedOutFrom Stage voluntarily left (SECONDARY or UNIVERSITY); blocks re-enrollment in that tier.
 * @property kcpePassed Primary exit exam pass flag; gates secondary enrollment at age 14.
 * @property kcseGrade Letter grade string; gates university via points threshold.
 * @property schoolPeople Classmates, teachers, bullies, and crushes while enrolled.
 * @property schoolReputation Standing with staff and peers (0–100).
 */
@Serializable
data class EducationState(
    val stage: SchoolStage = SchoolStage.NONE,
    val currentGrade: Int = 0,
    val gpa: Float = 0f,
    val expelled: Boolean = false,
    val droppedOutFrom: SchoolStage? = null,
    val courseOfStudy: String? = null,
    val schoolName: String? = null,
    val kcpePassed: Boolean? = null,
    val kcseGrade: String? = null,
    /** Applied on the next grade advance during Age Up. */
    val plannedStudyEffort: StudyEffort = StudyEffort.NORMAL,
    /** Active extracurricular while in upper primary or secondary. */
    val schoolClub: SchoolClub? = null,
    val schoolPeople: List<SchoolPerson> = emptyList(),
    val schoolReputation: Int = 50,
    /** Academic school activity used this year (study group, library, project…). */
    val academicActionDoneThisYear: Boolean = false,
    /** Social school activity used this year (hang out, dance, confront…). */
    val socialActionDoneThisYear: Boolean = false,
    val detentionYears: Int = 0
)
