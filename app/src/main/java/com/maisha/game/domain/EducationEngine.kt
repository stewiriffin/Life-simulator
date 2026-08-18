// app/src/main/java/com/maisha/game/domain/EducationEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.CountryCatalog
import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.ExamResult
import com.maisha.game.data.model.ExamType
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.SchoolClub
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.StudyEffort
import com.maisha.game.data.model.UniversityMajor
import com.maisha.game.data.model.VisaType
import com.maisha.game.util.clampGpa
import com.maisha.game.util.clampStat
import com.maisha.game.util.formatMoney
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.random.Random

sealed class DrivingTestResult {
    data class Passed(val character: Character) : DrivingTestResult()
    data class Failed(val character: Character) : DrivingTestResult()
    data object TooYoung : DrivingTestResult()
    data object AlreadyLicensed : DrivingTestResult()
    data object InsufficientFunds : DrivingTestResult()
}

@Singleton
class EducationEngine @Inject constructor(
    private val relocationEngine: RelocationEngine
) {

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

    /** Sets the effort applied on the next grade advance during Age Up. */
    fun setPlannedStudyEffort(character: Character, effort: StudyEffort): Character {
        val education = character.education
        if (education.expelled || education.droppedOutFrom != null) return character
        if (education.stage != SchoolStage.PRIMARY &&
            education.stage != SchoolStage.SECONDARY &&
            education.stage != SchoolStage.UNIVERSITY
        ) {
            return character
        }
        return character.copy(education = education.copy(plannedStudyEffort = effort))
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
        val healthStudyPenalty = when (studyChoice) {
            StudyEffort.HARD -> if (character.stats.health < 45) -2 else -1
            StudyEffort.NORMAL -> 0
            StudyEffort.SLACK -> 0
        }
        val effectiveSmartsDelta = if (character.stats.happiness < 35) {
            (smartsDelta - 1).coerceAtLeast(0)
        } else {
            smartsDelta
        }
        val effectiveGpaDelta = if (character.stats.health < 35) gpaDelta - 0.08f else gpaDelta

        val newGpa = clampGpa(education.gpa + effectiveGpaDelta)
        val newStats = character.stats.copy(
            smarts = clampStat(character.stats.smarts + effectiveSmartsDelta),
            happiness = clampStat(character.stats.happiness + happinessDelta),
            health = clampStat(character.stats.health + healthStudyPenalty)
        )

        return character.copy(
            stats = newStats,
            education = education.copy(
                currentGrade = if (incrementGrade) education.currentGrade + 1 else education.currentGrade,
                gpa = newGpa
            )
        ).let { applySchoolClubYear(it) }
    }

    fun isSchoolClubEligible(character: Character): Boolean {
        if (!character.alive || character.criminalRecord.currentlyIncarcerated) return false
        val education = character.education
        if (education.expelled || education.droppedOutFrom != null) return false
        if (character.age !in SCHOOL_CLUB_MIN_AGE..SCHOOL_CLUB_MAX_AGE) return false
        return education.stage == SchoolStage.SECONDARY ||
            (education.stage == SchoolStage.PRIMARY && education.currentGrade >= 6)
    }

    fun joinSchoolClub(character: Character, club: SchoolClub): Character {
        if (!isSchoolClubEligible(character)) return character
        if (club == SchoolClub.FOOTBALL && character.stats.health < FOOTBALL_MIN_HEALTH) {
            return character
        }
        if (club == SchoolClub.DEBATE && character.education.gpa < DEBATE_MIN_GPA && character.education.gpa > 0f) {
            return character
        }
        return character.copy(
            education = character.education.copy(schoolClub = club),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "You joined the ${club.name.lowercase().replace('_', ' ')} club at school."
            )
        )
    }

    fun applySchoolClubYear(character: Character): Character {
        val club = character.education.schoolClub ?: return character
        if (!isSchoolClubEligible(character)) {
            return character.copy(education = character.education.copy(schoolClub = null))
        }
        val (happiness, health, smarts, gpaDelta) = when (club) {
            SchoolClub.DEBATE -> Quad(2, 0, 3, 0.08f)
            SchoolClub.FOOTBALL -> Quad(4, 3, 0, 0.03f)
            SchoolClub.DRAMA -> Quad(5, 0, 1, 0.04f)
            SchoolClub.CODING -> Quad(1, 0, 4, 0.06f)
            SchoolClub.MUSIC -> Quad(4, 0, 2, 0.05f)
        }
        return character.copy(
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness + happiness),
                health = clampStat(character.stats.health + health),
                smarts = clampStat(character.stats.smarts + smarts)
            ),
            education = character.education.copy(
                gpa = if (character.education.gpa > 0f) {
                    clampGpa(character.education.gpa + gpaDelta)
                } else {
                    character.education.gpa
                }
            )
        )
    }

    private data class Quad(val happiness: Int, val health: Int, val smarts: Int, val gpa: Float)

    /** Increments university year or graduates when [UNIVERSITY_YEARS] completed. Bills domestic tuition. */
    fun advanceUniversityYear(character: Character): Character {
        val education = character.education
        if (education.expelled || education.droppedOutFrom == SchoolStage.UNIVERSITY) return character
        if (education.stage != SchoolStage.UNIVERSITY) return character

        val tuition = domesticUniversityTuition(character)
        var updated = character
        if (tuition > 0) {
            val paid = minOf(tuition, character.stats.money)
            val shortfall = tuition - paid
            updated = character.copy(
                stats = character.stats.copy(
                    money = (character.stats.money - paid).coerceAtLeast(0),
                    happiness = if (shortfall > 0) {
                        clampStat(character.stats.happiness - 4)
                    } else {
                        character.stats.happiness
                    }
                ),
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    if (shortfall > 0) {
                        "University fees ${formatMoney(tuition, character.countryCode)} — " +
                            "short by ${formatMoney(shortfall, character.countryCode)}."
                    } else {
                        "Paid ${formatMoney(tuition, character.countryCode)} in university tuition."
                    }
                )
            )
        }

        val nextGrade = updated.education.currentGrade + 1
        return if (nextGrade > UNIVERSITY_YEARS) {
            updated.copy(
                education = updated.education.copy(
                    stage = SchoolStage.GRADUATED,
                    currentGrade = UNIVERSITY_YEARS
                )
            )
        } else {
            updated.copy(
                education = updated.education.copy(currentGrade = nextGrade)
            )
        }
    }

    private fun domesticUniversityTuition(character: Character): Int {
        // International enrollments already paid a large upfront fee.
        if (character.currentVisa == VisaType.STUDENT && character.isLivingAbroad()) return 0
        return EconomyScaler.scaleAmount(DOMESTIC_UNIVERSITY_TUITION_KENYA, character.countryCode)
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
                character.stats.happiness * 0.08f +
                character.stats.health * 0.06f +
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

    /**
     * Driving test for adults (18+). Fee is economy-scaled; pass chance is driven mainly by smarts.
     */
    fun takeDrivingTest(character: Character): DrivingTestResult {
        if (character.hasDrivingLicense) return DrivingTestResult.AlreadyLicensed
        if (character.age < MIN_DRIVING_AGE) return DrivingTestResult.TooYoung
        val fee = drivingTestFee(character.countryCode)
        if (character.stats.money < fee) return DrivingTestResult.InsufficientFunds

        val afterFee = character.copy(
            stats = character.stats.copy(money = character.stats.money - fee)
        )
        val passChance = (0.25f + character.stats.smarts / 100f * 0.65f).coerceIn(0.20f, 0.95f)
        return if (Random.nextFloat() < passChance) {
            DrivingTestResult.Passed(
                afterFee.copy(
                    hasDrivingLicense = true,
                    eventLog = EventLogCap.prepend(
                        afterFee.eventLog,
                        "You passed the driving test (${formatMoney(fee, character.countryCode)} fee)."
                    )
                )
            )
        } else {
            DrivingTestResult.Failed(
                afterFee.copy(
                    eventLog = EventLogCap.prepend(
                        afterFee.eventLog,
                        "You failed the driving test. The ${formatMoney(fee, character.countryCode)} fee is gone."
                    )
                )
            )
        }
    }

    fun drivingTestFee(countryCode: String): Int =
        EconomyScaler.scaleAmount(DRIVING_TEST_FEE_KENYA, countryCode)

    fun drivingTestPassChance(character: Character): Float =
        (0.25f + character.stats.smarts / 100f * 0.65f).coerceIn(0.20f, 0.95f)

    /**
     * Enrolls in university with [course] if [isEligibleForUniversity].
     * When [universityCountryCode] differs from residence and the character is not a citizen there,
     * relocates with a [VisaType.STUDENT] visa and charges international tuition.
     */
    fun applyToUniversity(
        character: Character,
        course: String,
        universityCountryCode: String = character.countryCode
    ): Character {
        if (!isEligibleForUniversity(character)) return character
        if (character.education.droppedOutFrom == SchoolStage.UNIVERSITY) return character

        val studyAbroad = universityCountryCode != character.countryCode &&
            !character.holdsCitizenship(universityCountryCode)

        var enrolled = character
        if (studyAbroad) {
            val destination = CountryCatalog.getCountry(universityCountryCode)
            enrolled = relocationEngine.relocate(enrolled, destination, VisaType.STUDENT)
            val tuition = internationalTuitionCost(universityCountryCode)
            enrolled = enrolled.copy(
                stats = enrolled.stats.copy(
                    money = enrolled.stats.money - tuition
                ),
                eventLog = listOf(
                    "You enrolled as an international student in ${destination.displayName}. " +
                        "Tuition and visa fees hit hard."
                ) + enrolled.eventLog
            )
        }

        return enrolled.copy(
            education = enrolled.education.copy(
                stage = SchoolStage.UNIVERSITY,
                currentGrade = 1,
                courseOfStudy = course,
                schoolName = universityNameFor(universityCountryCode)
            )
        )
    }

    fun internationalTuitionCost(universityCountryCode: String): Int {
        val base = EconomyScaler.scaleAmount(DOMESTIC_TUITION_KENYA, universityCountryCode)
        return (base * INTERNATIONAL_STUDENT_MULTIPLIER).roundToInt()
    }

    /** True when KCSE letter grade maps to at least [UNIVERSITY_MIN_POINTS]. */
    fun isEligibleForUniversity(character: Character): Boolean {
        val grade = character.education.kcseGrade ?: return false
        if (gradeToPoints(grade) < UNIVERSITY_MIN_POINTS) return false
        if (character.stats.smarts < UNIVERSITY_MIN_SMARTS) return false
        return character.stats.happiness >= UNIVERSITY_MIN_HAPPINESS
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
            UniversityMajor.entries.forEach { major ->
                val majorEligible = when (major) {
                    UniversityMajor.MEDICINE -> character.stats.smarts >= MAJOR_MEDICINE_MIN_SMARTS &&
                        character.stats.health >= MAJOR_MEDICINE_MIN_HEALTH
                    UniversityMajor.LAW -> character.stats.smarts >= MAJOR_LAW_MIN_SMARTS
                    UniversityMajor.NURSING -> character.stats.health >= MAJOR_NURSING_MIN_HEALTH
                    else -> true
                }
                if (!majorEligible) return@forEach
                choices += EventChoice(
                    label = "Apply for ${major.courseLabel} at university",
                    statEffects = mapOf(
                        "smarts" to if (major == UniversityMajor.MEDICINE) 3 else 2,
                        "happiness" to 4
                    ),
                    universityCourse = major.courseLabel,
                    resultText = "You enrolled in ${major.courseLabel} at " +
                        "${universityNameFor(character.countryCode)}."
                )
            }
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

    private fun randomPrimarySchool(countryCode: String): String = when (countryCode) {
        "KE" -> PRIMARY_SCHOOLS_KE.random()
        "NG" -> listOf("Surulere Primary", "Ikeja Primary", "Yaba Primary", "Garki Primary", "Bodija Primary").random()
        "US", "CA" -> listOf("Lincoln Elementary", "Washington Elementary", "Maple Elementary", "Riverdale Primary", "Cedar Grove School").random()
        "GB" -> listOf("St. Mary's Primary", "Greenwood Primary", "Hillcrest Primary", "Ashfield Primary", "Riverside Primary").random()
        "IN" -> listOf("Delhi Public School", "Kendriya Vidyalaya", "St. Xavier's Primary", "Modern School", "City Montessori").random()
        "JP" -> listOf("Sakura Elementary", "Midori Elementary", "Aoi Primary", "Hikari Elementary", "Umi Primary").random()
        "BR" -> listOf("Escola Municipal Central", "Colégio São Paulo", "Escola Verde", "Escola do Sol", "Colégio Horizonte").random()
        else -> PRIMARY_SCHOOLS_WORLD.random()
    }

    private fun randomSecondarySchool(countryCode: String): String = when (countryCode) {
        "KE" -> SECONDARY_SCHOOLS_KE.random()
        "NG" -> listOf("King's College Lagos", "Federal Government College", "Queen's College", "Government College Ibadan", "St. Gregory's College").random()
        "US", "CA" -> listOf("Lincoln High School", "Westfield High", "Riverdale Academy", "Central High", "Northview Secondary").random()
        "GB" -> listOf("King's Secondary", "Greenwood Academy", "Ashfield Comprehensive", "St. Helen's High", "Riverside College").random()
        "IN" -> listOf("Delhi Public School Secondary", "St. Xavier's High", "Modern School Senior", "National Public School", "City Academy").random()
        "JP" -> listOf("Sakura High School", "Tokyo Metropolitan High", "Midori Senior High", "Kaisei Academy", "Umi High").random()
        "BR" -> listOf("Colégio Objetivo", "Escola Estadual Central", "Colégio Santa Maria", "Instituto Horizonte", "Colégio do Sol").random()
        else -> SECONDARY_SCHOOLS_WORLD.random()
    }

    private fun universityNameFor(countryCode: String): String = when (countryCode) {
        "KE" -> "University of Nairobi"
        "NG" -> "University of Lagos"
        "ZA" -> "University of Cape Town"
        "EG" -> "Cairo University"
        "US" -> "State University"
        "CA" -> "University of Toronto"
        "GB" -> "University of Manchester"
        "FR" -> "Sorbonne University"
        "DE" -> "Technical University of Munich"
        "IN" -> "Delhi University"
        "JP" -> "University of Tokyo"
        "PH" -> "University of the Philippines"
        "ID" -> "University of Indonesia"
        "BR" -> "University of São Paulo"
        "MX" -> "UNAM"
        else -> "the local university"
    }

    companion object {
        const val KCPE_RESULT_EVENT_ID = "kcpe_results_system"
        const val KCSE_RESULT_EVENT_ID = "kcse_results_system"
        const val EXAM_SYSTEM_TAG = "exam_system"
        const val MIN_DRIVING_AGE = 18
        const val DRIVING_TEST_FEE_KENYA = 12_000

        private const val PRIMARY_ENROLL_AGE = 6
        private const val SECONDARY_ENROLL_AGE = 14
        private const val PRIMARY_EXIT_EXAM_AGE = 13
        private const val SECONDARY_EXIT_EXAM_AGE = 17
        private const val PRIMARY_MAX_GRADE = 8
        private const val SECONDARY_MAX_GRADE = 4
        private const val UNIVERSITY_YEARS = 4
        private const val DOMESTIC_UNIVERSITY_TUITION_KENYA = 45_000
        private const val KCPE_PASS_SCORE = 50f
        private const val KCSE_PASS_SCORE = 45f
        private const val UNIVERSITY_MIN_POINTS = 7 // C+ equivalent
        private const val UNIVERSITY_MIN_SMARTS = 45
        private const val UNIVERSITY_MIN_HAPPINESS = 35
        private const val MAJOR_MEDICINE_MIN_SMARTS = 75
        private const val MAJOR_MEDICINE_MIN_HEALTH = 55
        private const val MAJOR_LAW_MIN_SMARTS = 68
        private const val MAJOR_NURSING_MIN_HEALTH = 50
        private const val DOMESTIC_TUITION_KENYA = 80_000
        /** International students pay this multiplier on local tuition. */
        const val INTERNATIONAL_STUDENT_MULTIPLIER = 3.5

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

        const val SCHOOL_CLUB_MIN_AGE = 12
        const val SCHOOL_CLUB_MAX_AGE = 17
        private const val FOOTBALL_MIN_HEALTH = 45
        private const val DEBATE_MIN_GPA = 1.8f
    }
}
