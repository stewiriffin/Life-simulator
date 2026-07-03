// app/src/main/java/com/maisha/game/domain/EducationEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.ExamResult
import com.maisha.game.data.model.ExamType
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.StudyEffort
import com.maisha.game.util.clampGpa
import com.maisha.game.util.clampStat
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.random.Random

@Singleton
class EducationEngine @Inject constructor() {

    /**
     * Auto-enrolls at primary (age 6) or secondary (age 14 after KCPE pass). Skips if [EducationState.expelled].
     */
    fun enrollIfEligible(character: Character): Character {
        val education = character.education
        if (education.expelled) return character

        return when {
            character.age >= PRIMARY_ENROLL_AGE &&
                education.stage == SchoolStage.NONE -> {
                character.copy(
                    education = education.copy(
                        stage = SchoolStage.PRIMARY,
                        currentGrade = 1,
                        gpa = 2.0f,
                        schoolName = randomPrimarySchool(character.countryCode)
                    )
                )
            }

            character.age >= SECONDARY_ENROLL_AGE &&
                education.kcpePassed == true &&
                education.stage == SchoolStage.PRIMARY &&
                education.droppedOutFrom != SchoolStage.SECONDARY -> {
                character.copy(
                    education = education.copy(
                        stage = SchoolStage.SECONDARY,
                        currentGrade = 1,
                        schoolName = randomSecondarySchool(character.countryCode)
                    )
                )
            }

            else -> character
        }
    }

    /**
     * Increments grade in primary/secondary and applies [studyChoice] GPA/smarts effects.
     * No-op if not in primary/secondary or already at max grade for stage.
     */
    fun advanceGrade(character: Character, studyChoice: StudyEffort): Character {
        val education = character.education
        if (education.expelled || education.droppedOutFrom != null) return character
        if (education.stage != SchoolStage.PRIMARY && education.stage != SchoolStage.SECONDARY) {
            return character
        }

        val maxGrade = if (education.stage == SchoolStage.PRIMARY) {
            PRIMARY_MAX_GRADE
        } else {
            SECONDARY_MAX_GRADE
        }
        if (education.currentGrade >= maxGrade) return character

        return applyStudyEffort(
            character = character,
            studyChoice = studyChoice,
            incrementGrade = true
        )
    }

    /** Applies study effort without advancing grade — used by study-tagged event choices. */
    fun applyStudyEffort(character: Character, studyChoice: StudyEffort): Character {
        return applyStudyEffort(character, studyChoice, incrementGrade = false)
    }

    private fun applyStudyEffort(
        character: Character,
        studyChoice: StudyEffort,
        incrementGrade: Boolean
    ): Character {
        val education = character.education
        if (education.expelled || education.droppedOutFrom != null) return character
        if (education.stage != SchoolStage.PRIMARY && education.stage != SchoolStage.SECONDARY) {
            return character
        }

        val maxGrade = if (education.stage == SchoolStage.PRIMARY) {
            PRIMARY_MAX_GRADE
        } else {
            SECONDARY_MAX_GRADE
        }
        if (incrementGrade && education.currentGrade >= maxGrade) return character

        val gpaDelta = EffortResolver.studyGpaDelta(studyChoice)
        val smartsDelta = EffortResolver.studySmartsDelta(studyChoice)
        val happinessDelta = EffortResolver.studyHappinessDelta(studyChoice)

        val newGpa = clampGpa(education.gpa + gpaDelta)
        val newStats = character.stats.copy(
            smarts = clampStat(character.stats.smarts + smartsDelta),
            happiness = clampStat(character.stats.happiness + happinessDelta)
        )

        return character.copy(
            stats = newStats,
            education = education.copy(
                currentGrade = if (incrementGrade) education.currentGrade + 1 else education.currentGrade,
                gpa = newGpa
            )
        )
    }

    /** Increments university year or graduates when [UNIVERSITY_YEARS] completed. */
    fun advanceUniversityYear(character: Character): Character {
        val education = character.education
        if (education.expelled || education.droppedOutFrom == SchoolStage.UNIVERSITY) return character
        if (education.stage != SchoolStage.UNIVERSITY) return character

        val nextGrade = education.currentGrade + 1
        return if (nextGrade > UNIVERSITY_YEARS) {
            character.copy(
                education = education.copy(
                    stage = SchoolStage.GRADUATED,
                    currentGrade = UNIVERSITY_YEARS
                )
            )
        } else {
            character.copy(
                education = education.copy(currentGrade = nextGrade)
            )
        }
    }

    /**
     * Scores KCPE/KCSE from GPA, smarts, and randomness; updates pass flags on [EducationState].
     *
     * @return Updated character and [ExamResult] for UI/system events.
     */
    fun takeExam(character: Character, examType: ExamType): Pair<Character, ExamResult> {
        val education = character.education
        val randomFactor = Random.nextFloat() * 15f
        val score = (
            education.gpa * 15f +
                character.stats.smarts * 0.5f +
                randomFactor
            ).coerceIn(0f, 100f)

        val grade = scoreToLetterGrade(score)
        val passed = when (examType) {
            ExamType.KCPE -> score >= KCPE_PASS_SCORE
            ExamType.KCSE -> score >= KCSE_PASS_SCORE
        }

        val updatedEducation = when (examType) {
            ExamType.KCPE -> education.copy(kcpePassed = passed)
            ExamType.KCSE -> if (passed) education.copy(kcseGrade = grade) else education
        }

        val updatedCharacter = character.copy(education = updatedEducation)
        return updatedCharacter to ExamResult(passed = passed, grade = grade, score = score)
    }

    /** Enrolls in university with [course] if [isEligibleForUniversity]; otherwise returns character unchanged. */
    fun applyToUniversity(character: Character, course: String): Character {
        if (!isEligibleForUniversity(character)) return character
        if (character.education.droppedOutFrom == SchoolStage.UNIVERSITY) return character
        return character.copy(
            education = character.education.copy(
                stage = SchoolStage.UNIVERSITY,
                currentGrade = 1,
                courseOfStudy = course,
                schoolName = universityNameFor(character.countryCode)
            )
        )
    }

    /** True when KCSE letter grade maps to at least [UNIVERSITY_MIN_POINTS]. */
    fun isEligibleForUniversity(character: Character): Boolean {
        val grade = character.education.kcseGrade ?: return false
        return gradeToPoints(grade) >= UNIVERSITY_MIN_POINTS
    }

    /** Primary exit exam due: final primary grade, age threshold, not yet passed. Country-agnostic; display localized via [ExamNames]. */
    fun shouldTriggerPrimaryExam(character: Character): Boolean {
        val education = character.education
        return education.stage == SchoolStage.PRIMARY &&
            education.currentGrade >= PRIMARY_MAX_GRADE &&
            character.age >= PRIMARY_EXIT_EXAM_AGE &&
            education.kcpePassed != true
    }

    /** Secondary exit exam due: final secondary grade, age threshold, no grade recorded yet. Country-agnostic; display localized via [ExamNames]. */
    fun shouldTriggerSecondaryExam(character: Character): Boolean {
        val education = character.education
        return education.stage == SchoolStage.SECONDARY &&
            education.currentGrade >= SECONDARY_MAX_GRADE &&
            character.age >= SECONDARY_EXIT_EXAM_AGE &&
            education.kcseGrade == null
    }

    /**
     * System result event after [takeExam]; exam name is localized via [ExamNames] for [character.countryCode].
     */
    fun buildExamResultEvent(
        examType: ExamType,
        result: ExamResult,
        character: Character
    ): LifeEvent {
        val examName = if (examType == ExamType.KCPE) {
            com.maisha.game.data.ExamNames.primaryExamName(character.countryCode)
        } else {
            com.maisha.game.data.ExamNames.secondaryExamName(character.countryCode)
        }
        val outcome = if (result.passed) "passed" else "did not pass"
        val text = buildString {
            append("$examName results are out! You $outcome with a grade of ${result.grade}. ")
            append("Your score was ${result.score.roundToInt()}%. ")
            if (examType == ExamType.KCPE) {
                if (result.passed) {
                    append("You qualify for secondary school next year.")
                } else {
                    append("You can re-sit next year or consider other paths.")
                }
            } else {
                if (result.passed) {
                    append(
                        if (isEligibleForUniversity(character)) {
                            "University doors are open to you."
                        } else {
                            "You passed, but university may require a stronger grade."
                        }
                    )
                } else {
                    append("You can re-sit next year.")
                }
            }
        }

        val choices = buildExamChoices(examType, result, character)
        return LifeEvent(
            id = if (examType == ExamType.KCPE) KCPE_RESULT_EVENT_ID else KCSE_RESULT_EVENT_ID,
            minAge = character.age,
            maxAge = character.age,
            text = text,
            choices = choices,
            tags = listOf(EXAM_SYSTEM_TAG, "one_time")
        )
    }

    /** Adds [gpaEffect] to [EducationState.gpa], clamped 0–4. No-op when effect is 0. */
    fun applyGpaEffect(character: Character, gpaEffect: Float): Character {
        if (gpaEffect == 0f) return character
        return character.copy(
            education = character.education.copy(
                gpa = clampGpa(character.education.gpa + gpaEffect)
            )
        )
    }

    /**
     * Voluntary leave from secondary or university. Records [EducationState.droppedOutFrom] so the same
     * tier cannot be re-entered; preserves exam flags already earned.
     */
    fun processDropout(character: Character): Character {
        val education = character.education
        if (education.expelled) return character
        val stage = education.stage
        if (stage != SchoolStage.SECONDARY && stage != SchoolStage.UNIVERSITY) return character

        return character.copy(
            education = education.copy(
                droppedOutFrom = stage,
                stage = SchoolStage.NONE,
                currentGrade = 0,
                schoolName = null,
                courseOfStudy = null
            )
        )
    }

    /** Forced removal from school — blocks all future enrollment and progression. */
    fun processExpulsion(character: Character): Character {
        val education = character.education
        if (education.expelled) return character

        return character.copy(
            education = education.copy(
                expelled = true,
                stage = SchoolStage.NONE,
                currentGrade = 0,
                schoolName = null,
                courseOfStudy = null
            )
        )
    }

    /** True when still enrolled in secondary or university and eligible to leave voluntarily. */
    fun canVoluntarilyDropOut(character: Character): Boolean {
        val education = character.education
        if (education.expelled) return false
        return education.stage == SchoolStage.SECONDARY || education.stage == SchoolStage.UNIVERSITY
    }

    private fun buildExamChoices(
        examType: ExamType,
        result: ExamResult,
        character: Character
    ): List<EventChoice> {
        val choices = mutableListOf<EventChoice>()

        if (result.passed) {
            choices += EventChoice(
                label = "Celebrate with family",
                statEffects = mapOf("happiness" to 8),
                resultText = "A family meal at home. Everyone is proud of you."
            )
        } else {
            choices += EventChoice(
                label = "Study harder for the resit",
                statEffects = mapOf("smarts" to 3, "happiness" to -2),
                gpaEffect = 0.2f,
                resultText = "You hit the books hard. Next year will be different."
            )
            choices += EventChoice(
                label = "Take a break and regroup",
                statEffects = mapOf("happiness" to 4),
                resultText = "You cleared your head. Failure isn't the end."
            )
        }

        if (examType == ExamType.KCSE && result.passed && isEligibleForUniversity(character)) {
            choices += EventChoice(
                label = "Apply for Law at university",
                statEffects = mapOf("smarts" to 2, "happiness" to 5),
                universityCourse = "Law",
                resultText = "You enrolled in Law at ${universityNameFor(character.countryCode)}."
            )
            choices += EventChoice(
                label = "Apply for Medicine at university",
                statEffects = mapOf("smarts" to 3, "happiness" to 4),
                universityCourse = "Medicine",
                resultText = "You enrolled in Medicine at ${universityNameFor(character.countryCode)}."
            )
            choices += EventChoice(
                label = "Apply for Computer Science",
                statEffects = mapOf("smarts" to 3, "happiness" to 5),
                universityCourse = "Computer Science",
                resultText = "You enrolled in Computer Science at ${universityNameFor(character.countryCode)}."
            )
        }

        if (examType == ExamType.KCSE && result.passed && !isEligibleForUniversity(character)) {
            choices += EventChoice(
                label = "Look for college options",
                statEffects = mapOf("happiness" to 2),
                resultText = "You research diploma courses for the next chapter."
            )
        }

        return choices
    }

    private fun scoreToLetterGrade(score: Float): String = when {
        score >= 90 -> "A"
        score >= 85 -> "A-"
        score >= 80 -> "B+"
        score >= 75 -> "B"
        score >= 70 -> "B-"
        score >= 65 -> "C+"
        score >= 60 -> "C"
        score >= 55 -> "C-"
        score >= 50 -> "D+"
        score >= 45 -> "D"
        score >= 40 -> "D-"
        else -> "E"
    }

    private fun gradeToPoints(grade: String): Int = when (grade) {
        "A" -> 12
        "A-" -> 11
        "B+" -> 10
        "B" -> 9
        "B-" -> 8
        "C+" -> 7
        "C" -> 6
        "C-" -> 5
        "D+" -> 4
        "D" -> 3
        "D-" -> 2
        else -> 1
    }

    private fun randomPrimarySchool(countryCode: String): String =
        if (countryCode == "KE") PRIMARY_SCHOOLS_KE.random() else PRIMARY_SCHOOLS_WORLD.random()

    private fun randomSecondarySchool(countryCode: String): String =
        if (countryCode == "KE") SECONDARY_SCHOOLS_KE.random() else SECONDARY_SCHOOLS_WORLD.random()

    private fun universityNameFor(countryCode: String): String = when (countryCode) {
        "KE" -> "University of Nairobi"
        "NG" -> "University of Lagos"
        "ZA" -> "University of Cape Town"
        "IN" -> "Delhi University"
        "MX" -> "UNAM"
        else -> "the local university"
    }

    companion object {
        const val KCPE_RESULT_EVENT_ID = "kcpe_results_system"
        const val KCSE_RESULT_EVENT_ID = "kcse_results_system"
        const val EXAM_SYSTEM_TAG = "exam_system"

        private const val PRIMARY_ENROLL_AGE = 6
        private const val SECONDARY_ENROLL_AGE = 14
        private const val PRIMARY_EXIT_EXAM_AGE = 13
        private const val SECONDARY_EXIT_EXAM_AGE = 17
        private const val PRIMARY_MAX_GRADE = 8
        private const val SECONDARY_MAX_GRADE = 4
        private const val UNIVERSITY_YEARS = 4
        private const val KCPE_PASS_SCORE = 50f
        private const val KCSE_PASS_SCORE = 45f
        private const val UNIVERSITY_MIN_POINTS = 7 // C+ equivalent

        private val PRIMARY_SCHOOLS_KE = listOf(
            "Mwiki Primary",
            "Kawangware Primary",
            "Buruburu Primary",
            "Kibera Primary",
            "Westlands Primary"
        )

        private val PRIMARY_SCHOOLS_WORLD = listOf(
            "Central Primary",
            "Hillview Primary",
            "Riverside Primary",
            "Oakwood Primary",
            "Greenfield Primary"
        )

        private val SECONDARY_SCHOOLS_KE = listOf(
            "Nairobi Secondary School",
            "Alliance High School",
            "Mang'u High School",
            "Lenana School",
            "Kenya High School"
        )

        private val SECONDARY_SCHOOLS_WORLD = listOf(
            "Central Secondary",
            "Hillview High",
            "Riverside Academy",
            "Oakwood High",
            "Greenfield Secondary"
        )
    }
}
