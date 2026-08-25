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

/**
 * Progression rank inside a [SchoolClub].
 * Display titles vary by club (e.g. Starter vs Treasurer, Captain vs President).
 */
@Serializable
enum class ClubRank {
    MEMBER,
    OFFICER,
    CAPTAIN
}

/** How hard the player practices during a club activity this year. */
@Serializable
enum class ClubPracticeIntensity {
    LIGHT,
    NORMAL,
    INTENSE
}

/** University majors offered after secondary exams. */
@Serializable
enum class UniversityMajor(
    val courseLabel: String,
    /** Typical program length in years. */
    val programYears: Int,
    /** Career track this major unlocks more easily after graduation. */
    val careerTrack: CareerTrack
) {
    COMPUTER_SCIENCE("Computer Science", 4, CareerTrack.SOFTWARE),
    LAW("Law", 4, CareerTrack.LEGAL),
    MEDICINE("Medicine", 5, CareerTrack.MEDICAL),
    BUSINESS("Business", 3, CareerTrack.CORPORATE),
    COMMUNICATIONS("Communications", 3, CareerTrack.ENTERTAINMENT),
    ENGINEERING("Engineering", 4, CareerTrack.SOFTWARE),
    NURSING("Nursing", 4, CareerTrack.MEDICAL)
}

/** How the player finances university tuition. */
@Serializable
enum class UniversityFunding {
    CASH,
    LOAN,
    SCHOLARSHIP
}

/** Graduation distinction based on cumulative university GPA. */
@Serializable
enum class GraduationHonors {
    NONE,
    PASS,
    CUM_LAUDE,
    MAGNA_CUM_LAUDE,
    SUMMA_CUM_LAUDE,
    FIRST_CLASS
}

@Serializable
enum class ExamType {
    KCPE,
    KCSE
}

/** Kind of school exam on the yearly schedule. */
@Serializable
enum class ExamKind {
    MIDTERM,
    FINALS,
    NATIONAL_EXIT
}

/**
 * A scheduled exam the player can prepare for.
 *
 * @property yearsUntilDue 0 = imminent / this school year.
 * @property preparedness How ready the player is (0–100), raised by prep actions.
 */
@Serializable
data class ExamSchedule(
    val id: String,
    val kind: ExamKind,
    val title: String,
    val yearsUntilDue: Int = 0,
    val preparedness: Int = 40
)

/** Player choice when an exam prompt fires (or from the School card). */
@Serializable
enum class ExamPrepChoice {
    STUDY_HARD,
    STUDY_NORMAL,
    CRAM,
    CHEAT
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
    START_FIGHT,
    SKIP_CLASS,
    PULL_PRANK,
    TALK_BACK,
    SCHOOL_DANCE,
    CLUB_PRACTICE,
    GROUP_PROJECT
}

/** Choice at an expulsion hearing after repeated detentions. */
@Serializable
enum class ExpulsionHearingChoice {
    MERCY,
    DEFIANT,
    /** Leave for another school — keep enrolled, reset roster, start on probation. */
    TRANSFER
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
 * @property pendingExams Upcoming midterms, finals, and national exit exams.
 * @property examStress Pressure from hard studying and imminent exams (0–100).
 */
@Serializable
data class EducationState(
    val stage: SchoolStage = SchoolStage.NONE,
    val currentGrade: Int = 0,
    val gpa: Float = 0f,
    val expelled: Boolean = false,
    val droppedOutFrom: SchoolStage? = null,
    val courseOfStudy: String? = null,
    /** Structured major while in / after university (preferred over free-text [courseOfStudy]). */
    val universityMajor: UniversityMajor? = null,
    /** How tuition is being paid. */
    val universityFunding: UniversityFunding? = null,
    /** Outstanding student loan principal (integer currency units). */
    val studentLoanBalance: Int = 0,
    /** Annual tuition billed while enrolled (0 if scholarship covers it). */
    val tuitionPerYear: Int = 0,
    /** True when a merit scholarship waived tuition. */
    val scholarshipActive: Boolean = false,
    /** Honors awarded on graduation. */
    val graduationHonors: GraduationHonors = GraduationHonors.NONE,
    /** Campus work-study shift claimed this year while at university. */
    val campusJobDoneThisYear: Boolean = false,
    /** Major internship claimed this year while at university. */
    val internshipDoneThisYear: Boolean = false,
    /** Cumulative internship seasons completed (resume signal). */
    val internshipYearsCompleted: Int = 0,
    /** After graduating, offer matching career track once on Age Up / Career tab. */
    val pendingCareerTrackOffer: Boolean = false,
    val schoolName: String? = null,
    val kcpePassed: Boolean? = null,
    val kcseGrade: String? = null,
    /** Applied on the next grade advance during Age Up. */
    val plannedStudyEffort: StudyEffort = StudyEffort.NORMAL,
    /** Active extracurricular while in upper primary or secondary. */
    val schoolClub: SchoolClub? = null,
    val clubRank: ClubRank = ClubRank.MEMBER,
    /** Ability / performance inside the active club (0–100). */
    val clubSkill: Int = 0,
    /** Standing of the club / your contribution (0–100). */
    val clubPrestige: Int = 0,
    /** Club practice / activity used this year. */
    val clubActivityDoneThisYear: Boolean = false,
    /** Captain/President unlocked a major yearly showcase event. */
    val clubMajorEventReady: Boolean = false,
    /** Championships / fairs won — persists after leaving school for scholarships & careers. */
    val clubAwardsWon: Int = 0,
    /** Years spent as Team Captain / President (cumulative). */
    val clubYearsAsCaptain: Int = 0,
    /** Last club remembered on the resume after membership ends. */
    val clubResumeClub: SchoolClub? = null,
    /** Officer+ fundraiser used this year. */
    val clubFundraiserDoneThisYear: Boolean = false,
    /** Inter-school rivalry / scrimmage used this year. */
    val clubRivalryDoneThisYear: Boolean = false,
    /** Extracurricular fame from wins and showcases (0–100); persists on resume. */
    val clubFame: Int = 0,
    /** Letter jacket / varsity mark earned through strong club performance. */
    val clubLetterJacket: Boolean = false,
    val schoolPeople: List<SchoolPerson> = emptyList(),
    val schoolReputation: Int = 50,
    /** Academic school activity used this year (study group, library, project…). */
    val academicActionDoneThisYear: Boolean = false,
    /** Social school activity used this year (hang out, dance, confront…). */
    val socialActionDoneThisYear: Boolean = false,
    /** Lifetime detentions served (resume / legacy). */
    val detentionYears: Int = 0,
    /** Detentions earned in the current school year — hearing triggers at threshold. */
    val detentionCountThisYear: Int = 0,
    /** Summoned to an expulsion hearing after too many detentions. */
    val pendingExpulsionHearing: Boolean = false,
    /** On probation after begging for mercy at a hearing. */
    val onProbation: Boolean = false,
    /** Served detention hall this year (clears one strike). */
    val detentionServedThisYear: Boolean = false,
    /** Appealed / apologized to the principal this year. */
    val principalAppealDoneThisYear: Boolean = false,
    /** Consecutive failed school exams (year finals / national). */
    val failedExamStreak: Int = 0,
    val pendingExams: List<ExamSchedule> = emptyList(),
    val examStress: Int = 0,
    /** School-card exam prep action used this year. */
    val examPrepDoneThisYear: Boolean = false,
    /** Set when cheat prep succeeds unnoticed — raises catch risk on exam day. */
    val plannedCheatOnExam: Boolean = false,
    /** Short summary of the last exam outcome for the School card. */
    val lastExamSummary: String? = null
)
