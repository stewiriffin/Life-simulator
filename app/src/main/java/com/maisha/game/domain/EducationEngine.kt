// app/src/main/java/com/maisha/game/domain/EducationEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.CountryCatalog
import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.NamePool
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.ClubPracticeIntensity
import com.maisha.game.data.model.ClubRank
import com.maisha.game.data.model.ExamKind
import com.maisha.game.data.model.ExamPrepChoice
import com.maisha.game.data.model.ExamResult
import com.maisha.game.data.model.ExamSchedule
import com.maisha.game.data.model.ExamType
import com.maisha.game.data.model.ExpulsionHearingChoice
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.SchoolActivity
import com.maisha.game.data.model.SchoolClub
import com.maisha.game.data.model.SchoolPerson
import com.maisha.game.data.model.SchoolPersonAction
import com.maisha.game.data.model.SchoolRole
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.StudyEffort
import com.maisha.game.data.model.UniversityMajor
import com.maisha.game.data.model.VisaType
import com.maisha.game.util.clampGpa
import com.maisha.game.util.clampRelationshipLevel
import com.maisha.game.util.clampStat
import com.maisha.game.util.formatMoney
import java.util.UUID
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

sealed class SchoolActionResult {
    data class Success(val character: Character, val message: String) : SchoolActionResult()
    data object Ineligible : SchoolActionResult()
    data object AlreadyDone : SchoolActionResult()
    data object PersonNotFound : SchoolActionResult()
}

sealed class SchoolInteractionResult {
    data class Success(val character: Character, val message: String) : SchoolInteractionResult()
    data object Ineligible : SchoolInteractionResult()
    data object PersonNotFound : SchoolInteractionResult()
    data object InsufficientFunds : SchoolInteractionResult()
}

sealed class ExamPrepResult {
    data class Success(val character: Character, val message: String) : ExamPrepResult()
    data object Ineligible : ExamPrepResult()
    data object AlreadyDone : ExamPrepResult()
}

sealed class ClubActivityResult {
    data class Success(
        val character: Character,
        val message: String,
        val promoted: Boolean = false
    ) : ClubActivityResult()
    data class Dropped(val character: Character, val message: String) : ClubActivityResult()
    data object Ineligible : ClubActivityResult()
    data object AlreadyDone : ClubActivityResult()
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
                val enrolled = character.copy(
                    education = education.copy(
                        stage = SchoolStage.PRIMARY,
                        currentGrade = 1,
                        gpa = 2.0f,
                        schoolName = randomPrimarySchool(character.countryCode),
                        schoolReputation = 50,
                        academicActionDoneThisYear = false,
                        socialActionDoneThisYear = false
                    )
                )
                ensureSchoolRoster(enrolled)
            }

            character.age >= SECONDARY_ENROLL_AGE &&
                education.kcpePassed == true &&
                education.stage == SchoolStage.PRIMARY &&
                education.droppedOutFrom != SchoolStage.SECONDARY -> {
                val enrolled = character.copy(
                    education = education.copy(
                        stage = SchoolStage.SECONDARY,
                        currentGrade = 1,
                        schoolName = randomSecondarySchool(character.countryCode),
                        schoolReputation = (education.schoolReputation + 5).coerceIn(0, 100),
                        academicActionDoneThisYear = false,
                        socialActionDoneThisYear = false
                    )
                )
                ensureSchoolRoster(enrolled, forceRefresh = true)
            }

            else -> {
                if (isEnrolled(character) && education.schoolPeople.isEmpty()) {
                    ensureSchoolRoster(character)
                } else {
                    character
                }
            }
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
        val stressDelta = when (effort) {
            StudyEffort.HARD -> 8
            StudyEffort.NORMAL -> -2
            StudyEffort.SLACK -> -6
        }
        return character.copy(
            education = education.copy(
                plannedStudyEffort = effort,
                examStress = (education.examStress + stressDelta).coerceIn(0, 100)
            )
        ).let { refreshExamSchedule(it) }
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
        val sameClub = character.education.schoolClub == club
        return character.copy(
            education = character.education.copy(
                schoolClub = club,
                clubRank = if (sameClub) character.education.clubRank else ClubRank.MEMBER,
                clubSkill = if (sameClub) character.education.clubSkill else Random.nextInt(8, 18),
                clubPrestige = if (sameClub) character.education.clubPrestige else Random.nextInt(5, 15),
                clubActivityDoneThisYear = if (sameClub) character.education.clubActivityDoneThisYear else false,
                clubMajorEventReady = if (sameClub) character.education.clubMajorEventReady else false,
                clubFundraiserDoneThisYear = if (sameClub) character.education.clubFundraiserDoneThisYear else false,
                clubRivalryDoneThisYear = if (sameClub) character.education.clubRivalryDoneThisYear else false,
                clubResumeClub = club
            ),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                if (sameClub) {
                    "You recommitted to the ${clubDisplayName(club)}."
                } else {
                    "You joined the ${clubDisplayName(club)} as a ${clubRankTitle(club, ClubRank.MEMBER)}."
                }
            )
        )
    }

    fun leaveSchoolClub(character: Character, fired: Boolean = false): Character {
        val club = character.education.schoolClub ?: return character
        val happinessHit = if (fired) -8 else -3
        val repHit = if (fired) -10 else -2
        val message = if (fired) {
            "You were dropped from the ${clubDisplayName(club)}. Humiliating."
        } else {
            "You left the ${clubDisplayName(club)}."
        }
        return character.copy(
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness + happinessHit)
            ),
            education = clearActiveClubMembership(
                character.education,
                schoolReputation = (character.education.schoolReputation + repHit).coerceIn(0, 100),
                rememberResume = !fired || character.education.clubAwardsWon > 0
            ),
            eventLog = EventLogCap.prepend(character.eventLog, message)
        )
    }

    /** Clears active membership but keeps awards / captain years for scholarships & career tracks. */
    private fun clearActiveClubMembership(
        education: EducationState,
        schoolReputation: Int = education.schoolReputation,
        rememberResume: Boolean = true
    ): EducationState {
        val resume = when {
            !rememberResume -> education.clubResumeClub
            education.schoolClub != null -> education.schoolClub
            else -> education.clubResumeClub
        }
        return education.copy(
            schoolClub = null,
            clubRank = ClubRank.MEMBER,
            clubSkill = 0,
            clubPrestige = 0,
            clubActivityDoneThisYear = false,
            clubMajorEventReady = false,
            clubFundraiserDoneThisYear = false,
            clubRivalryDoneThisYear = false,
            clubResumeClub = resume,
            schoolReputation = schoolReputation
        )
    }

    fun clubRankTitle(club: SchoolClub, rank: ClubRank): String = when (club) {
        SchoolClub.FOOTBALL -> when (rank) {
            ClubRank.MEMBER -> "Member"
            ClubRank.OFFICER -> "Starter"
            ClubRank.CAPTAIN -> "Team Captain"
        }
        else -> when (rank) {
            ClubRank.MEMBER -> "Member"
            ClubRank.OFFICER -> "Treasurer"
            ClubRank.CAPTAIN -> "President"
        }
    }

    fun clubDisplayName(club: SchoolClub): String =
        club.name.lowercase().replace('_', ' ') + " club"

    fun clubMajorEventTitle(club: SchoolClub): String = when (club) {
        SchoolClub.FOOTBALL -> "State Championship Match"
        SchoolClub.DEBATE -> "National Debate Finals"
        SchoolClub.DRAMA -> "Regional Drama Festival"
        SchoolClub.CODING -> "National Science & Coding Fair"
        SchoolClub.MUSIC -> "Inter-School Music Showcase"
    }

    fun clubRivalryTitle(club: SchoolClub): String = when (club) {
        SchoolClub.FOOTBALL -> "Rival School Derby"
        SchoolClub.DEBATE -> "City Debate Dual"
        SchoolClub.DRAMA -> "Rival Drama Night"
        SchoolClub.CODING -> "Hackathon Showdown"
        SchoolClub.MUSIC -> "Battle of the Bands"
    }

    // ── Misbehavior & detention ──────────────────────────────────────────

    /** Threshold of detentions in one year before an expulsion hearing. Lower while on probation. */
    fun detentionHearingThreshold(character: Character): Int =
        if (character.education.onProbation) DETENTION_HEARING_THRESHOLD_PROBATION
        else DETENTION_HEARING_THRESHOLD

    fun disciplineStandingLabel(character: Character): String = when {
        character.education.pendingExpulsionHearing -> "Expulsion hearing pending"
        character.education.onProbation -> "On probation"
        character.education.detentionCountThisYear >= 2 -> "At risk"
        character.education.detentionCountThisYear >= 1 -> "Detention on record"
        character.education.schoolReputation < 30 -> "Poor standing"
        else -> "Clear"
    }

    fun hasDisciplineWarning(character: Character): Boolean =
        character.education.pendingExpulsionHearing ||
            character.education.onProbation ||
            character.education.detentionCountThisYear > 0 ||
            character.education.schoolReputation < 35

    /**
     * Records a detention from misbehavior. May queue an expulsion hearing.
     */
    fun recordDetention(
        character: Character,
        reason: String,
        reputationHit: Int = 8,
        happinessHit: Int = 3
    ): Character {
        if (!isEnrolled(character) || character.education.expelled) return character
        val count = character.education.detentionCountThisYear + 1
        val threshold = detentionHearingThreshold(character)
        val hearing = character.education.pendingExpulsionHearing || count >= threshold
        val message = buildString {
            append(reason)
            append(" Detention ($count/${threshold} this year).")
            if (hearing && !character.education.pendingExpulsionHearing) {
                append(" You've been summoned to an expulsion hearing.")
            }
        }
        return character.copy(
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness - happinessHit)
            ),
            education = character.education.copy(
                detentionYears = character.education.detentionYears + 1,
                detentionCountThisYear = count,
                pendingExpulsionHearing = hearing,
                schoolReputation = (character.education.schoolReputation - reputationHit)
                    .coerceIn(0, 100)
            ),
            eventLog = EventLogCap.prepend(character.eventLog, message)
        )
    }

    fun buildExpulsionHearingEvent(character: Character): LifeEvent? {
        if (!character.education.pendingExpulsionHearing) return null
        if (!isEnrolled(character) || character.education.expelled) return null
        val count = character.education.detentionCountThisYear
        return LifeEvent(
            id = "expulsion_hearing_${character.age}_$count",
            minAge = character.age,
            maxAge = character.age,
            text = "The principal calls you in. Your file shows $count detention(s) this year " +
                "and a school reputation of ${character.education.schoolReputation}. " +
                "This is an expulsion hearing. How do you face it?",
            choices = listOf(
                EventChoice(
                    label = "Beg for mercy",
                    resultText = "You apologize, promise to change, and hope for probation.",
                    expulsionHearingAction = ExpulsionHearingChoice.MERCY.name
                ),
                EventChoice(
                    label = "Act defiant",
                    resultText = "You refuse to apologize. The board has had enough.",
                    expulsionHearingAction = ExpulsionHearingChoice.DEFIANT.name,
                    triggersExpulsion = true
                )
            ),
            tags = listOf(EXPULSION_HEARING_TAG, "one_time", "education"),
            weight = 12
        )
    }

    fun resolveExpulsionHearing(
        character: Character,
        choice: ExpulsionHearingChoice
    ): Character {
        if (!character.education.pendingExpulsionHearing) return character
        return when (choice) {
            ExpulsionHearingChoice.MERCY -> {
                character.copy(
                    stats = character.stats.copy(
                        happiness = clampStat(character.stats.happiness - 4)
                    ),
                    education = character.education.copy(
                        pendingExpulsionHearing = false,
                        onProbation = true,
                        detentionCountThisYear = 0,
                        gpa = clampGpa(character.education.gpa - 0.35f),
                        schoolReputation = (character.education.schoolReputation - 5)
                            .coerceIn(0, 100)
                    ),
                    eventLog = EventLogCap.prepend(
                        character.eventLog,
                        "You begged for mercy. Probation granted — GPA takes a hit. One more strike and you're out."
                    )
                )
            }
            ExpulsionHearingChoice.DEFIANT -> {
                val expelled = processExpulsion(
                    character.copy(
                        education = character.education.copy(pendingExpulsionHearing = false)
                    )
                )
                expelled.copy(
                    eventLog = EventLogCap.prepend(
                        expelled.eventLog,
                        "You acted defiant at the hearing. Expulsion is immediate. Find another path — or drop out of the system entirely."
                    )
                )
            }
        }
    }

    /** Minimum club skill required to challenge a rival school. */
    fun canChallengeRivalSchool(character: Character): Boolean =
        isSchoolClubEligible(character) &&
            character.education.schoolClub != null &&
            !character.education.clubRivalryDoneThisYear &&
            character.education.clubSkill >= CLUB_RIVALRY_MIN_SKILL

    /** Estimated scholarship cash from club awards / captain years (0 if none). */
    fun clubScholarshipEstimate(character: Character): Int {
        val awards = character.education.clubAwardsWon
        val captainYears = character.education.clubYearsAsCaptain
        val fameBonus = character.education.clubFame / 20
        val jacketBonus = if (character.education.clubLetterJacket) 6_000 else 0
        if (awards <= 0 && captainYears <= 0 && !character.education.clubLetterJacket) return 0
        val base = 8_000 + awards * 12_000 + captainYears * 4_000 + jacketBonus + fameBonus * 1_000
        return EconomyScaler.scaleAmount(base, character.countryCode)
    }

    /**
     * Practice / compete with the active club.
     * [intensity] trades skill gain vs injury / drop risk (BitLife-style training).
     */
    fun performClubActivity(
        character: Character,
        intensity: ClubPracticeIntensity = ClubPracticeIntensity.NORMAL
    ): ClubActivityResult {
        if (!isSchoolClubEligible(character)) return ClubActivityResult.Ineligible
        val club = character.education.schoolClub ?: return ClubActivityResult.Ineligible
        if (character.education.clubActivityDoneThisYear) return ClubActivityResult.AlreadyDone

        val intensityDropBonus = when (intensity) {
            ClubPracticeIntensity.LIGHT -> -0.04f
            ClubPracticeIntensity.NORMAL -> 0f
            ClubPracticeIntensity.INTENSE -> 0.08f
        }
        val dropChance = when {
            club == SchoolClub.FOOTBALL && character.stats.health < 40 -> 0.35f
            character.education.clubSkill < 15 && character.education.clubRank == ClubRank.MEMBER -> 0.12f
            character.education.plannedStudyEffort == StudyEffort.SLACK &&
                character.education.clubSkill < 30 -> 0.18f
            else -> 0.04f
        } + intensityDropBonus
        if (Random.nextFloat() < dropChance.coerceIn(0.01f, 0.55f)) {
            val dropped = leaveSchoolClub(character, fired = true)
            return ClubActivityResult.Dropped(dropped, "Dropped from the ${clubDisplayName(club)}.")
        }

        val skillGain = when (intensity) {
            ClubPracticeIntensity.LIGHT -> Random.nextInt(4, 9)
            ClubPracticeIntensity.NORMAL -> Random.nextInt(8, 16)
            ClubPracticeIntensity.INTENSE -> Random.nextInt(14, 22)
        } + if (character.education.plannedStudyEffort == StudyEffort.HARD) 2 else 0
        val prestigeGain = when (intensity) {
            ClubPracticeIntensity.LIGHT -> Random.nextInt(1, 3)
            ClubPracticeIntensity.NORMAL -> Random.nextInt(2, 6)
            ClubPracticeIntensity.INTENSE -> Random.nextInt(4, 9)
        }
        val newSkill = (character.education.clubSkill + skillGain).coerceIn(0, 100)
        val newPrestige = (character.education.clubPrestige + prestigeGain).coerceIn(0, 100)
        val (happiness, health, smarts) = when (club) {
            SchoolClub.DEBATE -> Triple(2, 0, 3)
            SchoolClub.FOOTBALL -> Triple(3, 2, 0)
            SchoolClub.DRAMA -> Triple(4, 0, 1)
            SchoolClub.CODING -> Triple(1, 0, 4)
            SchoolClub.MUSIC -> Triple(3, 0, 2)
        }
        val intensityHappiness = when (intensity) {
            ClubPracticeIntensity.LIGHT -> 1
            ClubPracticeIntensity.NORMAL -> 0
            ClubPracticeIntensity.INTENSE -> -2
        }

        // Intense football (and sometimes drama stage work) can injure you.
        val injuryChance = when {
            intensity != ClubPracticeIntensity.INTENSE -> 0f
            club == SchoolClub.FOOTBALL -> 0.22f
            club == SchoolClub.DRAMA -> 0.06f
            else -> 0.03f
        }
        val injured = injuryChance > 0f && Random.nextFloat() < injuryChance
        val injuryHealth = if (injured) -Random.nextInt(8, 16) else 0

        var rank = character.education.clubRank
        var promoted = false
        var majorReady = character.education.clubMajorEventReady
        if (rank == ClubRank.MEMBER && newSkill >= CLUB_OFFICER_SKILL) {
            rank = ClubRank.OFFICER
            promoted = true
        } else if (rank == ClubRank.OFFICER && newSkill >= CLUB_CAPTAIN_SKILL && newPrestige >= 45) {
            rank = ClubRank.CAPTAIN
            promoted = true
            majorReady = true
        } else if (rank == ClubRank.CAPTAIN) {
            majorReady = true
        }

        val earnedJacket = !character.education.clubLetterJacket &&
            rank.ordinal >= ClubRank.OFFICER.ordinal &&
            newSkill >= CLUB_LETTER_JACKET_SKILL

        val title = clubRankTitle(club, rank)
        val intensityLabel = when (intensity) {
            ClubPracticeIntensity.LIGHT -> "Light practice"
            ClubPracticeIntensity.NORMAL -> "Club practice"
            ClubPracticeIntensity.INTENSE -> "Intense training"
        }
        val message = buildString {
            append("$intensityLabel paid off (+$skillGain skill).")
            if (promoted) append(" Promoted to $title!")
            if (earnedJacket) append(" You earned your letter jacket!")
            if (injured) append(" You picked up an injury on the way.")
            if (rank == ClubRank.CAPTAIN && majorReady) {
                append(" ${clubMajorEventTitle(club)} is on the calendar.")
            }
        }

        val fameGain = when {
            earnedJacket -> 12
            promoted -> 4
            intensity == ClubPracticeIntensity.INTENSE -> 2
            else -> 1
        }

        val updated = character.copy(
            stats = character.stats.copy(
                happiness = clampStat(
                    character.stats.happiness + happiness + intensityHappiness + if (earnedJacket) 5 else 0
                ),
                health = clampStat(character.stats.health + health + injuryHealth),
                smarts = clampStat(character.stats.smarts + smarts),
                looks = clampStat(character.stats.looks + if (earnedJacket) 4 else 0)
            ),
            education = character.education.copy(
                clubSkill = newSkill,
                clubPrestige = newPrestige,
                clubRank = rank,
                clubActivityDoneThisYear = true,
                clubMajorEventReady = majorReady,
                clubResumeClub = club,
                clubLetterJacket = character.education.clubLetterJacket || earnedJacket,
                clubFame = (character.education.clubFame + fameGain).coerceIn(0, 100),
                schoolReputation = (character.education.schoolReputation + if (promoted) 4 else 1)
                    .coerceIn(0, 100),
                gpa = if (club == SchoolClub.DEBATE || club == SchoolClub.CODING) {
                    clampGpa(character.education.gpa + 0.04f)
                } else {
                    character.education.gpa
                }
            ),
            eventLog = EventLogCap.prepend(character.eventLog, message)
        )
        return ClubActivityResult.Success(updated, message, promoted = promoted)
    }

    /**
     * Captain/President showcase: championship, science fair, etc. High cash / scholarship reward.
     */
    fun claimClubMajorEvent(character: Character): ClubActivityResult {
        if (!isSchoolClubEligible(character)) return ClubActivityResult.Ineligible
        val club = character.education.schoolClub ?: return ClubActivityResult.Ineligible
        if (character.education.clubRank != ClubRank.CAPTAIN) return ClubActivityResult.Ineligible
        if (!character.education.clubMajorEventReady) return ClubActivityResult.AlreadyDone

        val won = Random.nextFloat() < clubMajorWinChance(character)
        val baseReward = when (club) {
            SchoolClub.FOOTBALL -> 18_000
            SchoolClub.CODING, SchoolClub.DEBATE -> 22_000
            SchoolClub.DRAMA, SchoolClub.MUSIC -> 15_000
        }
        val reward = EconomyScaler.scaleAmount(baseReward, character.countryCode)
        val eventName = clubMajorEventTitle(club)

        return if (won) {
            val updated = character.copy(
                stats = character.stats.copy(
                    money = character.stats.money + reward,
                    happiness = clampStat(character.stats.happiness + 10),
                    looks = clampStat(character.stats.looks + 2)
                ),
                education = character.education.copy(
                    clubMajorEventReady = false,
                    clubPrestige = (character.education.clubPrestige + 12).coerceIn(0, 100),
                    clubSkill = (character.education.clubSkill + 5).coerceIn(0, 100),
                    clubAwardsWon = character.education.clubAwardsWon + 1,
                    clubFame = (character.education.clubFame + 18).coerceIn(0, 100),
                    clubResumeClub = club,
                    schoolReputation = (character.education.schoolReputation + 10).coerceIn(0, 100),
                    gpa = clampGpa(character.education.gpa + 0.1f)
                ),
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    "You led the ${clubDisplayName(club)} to glory at the $eventName. " +
                        "Scholarship / prize: ${formatMoney(reward, character.countryCode)}."
                )
            )
            ClubActivityResult.Success(
                updated,
                "Won the $eventName! ${formatMoney(reward, character.countryCode)} prize."
            )
        } else {
            val updated = character.copy(
                stats = character.stats.copy(
                    happiness = clampStat(character.stats.happiness - 4),
                    money = character.stats.money + reward / 5
                ),
                education = character.education.copy(
                    clubMajorEventReady = false,
                    clubPrestige = (character.education.clubPrestige + 3).coerceIn(0, 100),
                    schoolReputation = (character.education.schoolReputation + 2).coerceIn(0, 100)
                ),
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    "The $eventName was tough. You didn't take the trophy, but you still earned a small stipend."
                )
            )
            ClubActivityResult.Success(
                updated,
                "Competed at $eventName. Consolation stipend awarded."
            )
        }
    }

    /**
     * Officer / Captain fundraiser — cash for the club and a prestige bump (once per year).
     */
    fun hostClubFundraiser(character: Character): ClubActivityResult {
        if (!isSchoolClubEligible(character)) return ClubActivityResult.Ineligible
        val club = character.education.schoolClub ?: return ClubActivityResult.Ineligible
        if (character.education.clubRank == ClubRank.MEMBER) return ClubActivityResult.Ineligible
        if (character.education.clubFundraiserDoneThisYear) return ClubActivityResult.AlreadyDone

        val raised = EconomyScaler.scaleAmount(
            when (character.education.clubRank) {
                ClubRank.CAPTAIN -> Random.nextInt(4_000, 9_000)
                ClubRank.OFFICER -> Random.nextInt(2_000, 5_000)
                ClubRank.MEMBER -> 0
            },
            character.countryCode
        )
        val message = "Your ${clubDisplayName(club)} fundraiser raised ${formatMoney(raised, character.countryCode)}."
        val updated = character.copy(
            stats = character.stats.copy(
                money = character.stats.money + raised,
                happiness = clampStat(character.stats.happiness + 3),
                looks = clampStat(character.stats.looks + 1)
            ),
            education = character.education.copy(
                clubFundraiserDoneThisYear = true,
                clubPrestige = (character.education.clubPrestige + 6).coerceIn(0, 100),
                schoolReputation = (character.education.schoolReputation + 3).coerceIn(0, 100)
            ),
            eventLog = EventLogCap.prepend(character.eventLog, message)
        )
        return ClubActivityResult.Success(updated, message)
    }

    /**
     * Inter-school rivalry / scrimmage — available once skill is solid enough (BitLife derby energy).
     */
    fun challengeRivalSchool(character: Character): ClubActivityResult {
        if (!canChallengeRivalSchool(character)) {
            return if (character.education.clubRivalryDoneThisYear) {
                ClubActivityResult.AlreadyDone
            } else {
                ClubActivityResult.Ineligible
            }
        }
        val club = character.education.schoolClub ?: return ClubActivityResult.Ineligible
        val eventName = clubRivalryTitle(club)
        val winChance = (
            0.22f +
                character.education.clubSkill / 100f * 0.45f +
                character.education.clubPrestige / 100f * 0.2f +
                character.education.clubFame / 100f * 0.15f +
                if (character.education.clubLetterJacket) 0.05f else 0f
            ).coerceIn(0.12f, 0.88f)
        val won = Random.nextFloat() < winChance
        val prize = EconomyScaler.scaleAmount(
            when (club) {
                SchoolClub.FOOTBALL -> 6_000
                SchoolClub.CODING, SchoolClub.DEBATE -> 5_000
                SchoolClub.DRAMA, SchoolClub.MUSIC -> 4_500
            },
            character.countryCode
        )

        return if (won) {
            val message = "You crushed the $eventName! Prize: ${formatMoney(prize, character.countryCode)}."
            val updated = character.copy(
                stats = character.stats.copy(
                    money = character.stats.money + prize,
                    happiness = clampStat(character.stats.happiness + 7),
                    looks = clampStat(character.stats.looks + 1),
                    health = clampStat(
                        character.stats.health + if (club == SchoolClub.FOOTBALL) -2 else 0
                    )
                ),
                education = character.education.copy(
                    clubRivalryDoneThisYear = true,
                    clubSkill = (character.education.clubSkill + 4).coerceIn(0, 100),
                    clubPrestige = (character.education.clubPrestige + 5).coerceIn(0, 100),
                    clubFame = (character.education.clubFame + 8).coerceIn(0, 100),
                    schoolReputation = (character.education.schoolReputation + 5).coerceIn(0, 100)
                ),
                eventLog = EventLogCap.prepend(character.eventLog, message)
            )
            ClubActivityResult.Success(updated, message)
        } else {
            val message = "The $eventName went sideways. Rival school took the bragging rights."
            val updated = character.copy(
                stats = character.stats.copy(
                    happiness = clampStat(character.stats.happiness - 5),
                    health = clampStat(
                        character.stats.health + if (club == SchoolClub.FOOTBALL) -3 else 0
                    )
                ),
                education = character.education.copy(
                    clubRivalryDoneThisYear = true,
                    clubSkill = (character.education.clubSkill + 1).coerceIn(0, 100),
                    clubPrestige = (character.education.clubPrestige - 2).coerceIn(0, 100),
                    clubFame = (character.education.clubFame - 2).coerceIn(0, 100),
                    schoolReputation = (character.education.schoolReputation - 2).coerceIn(0, 100)
                ),
                eventLog = EventLogCap.prepend(character.eventLog, message)
            )
            ClubActivityResult.Success(updated, message)
        }
    }

    private fun clubMajorWinChance(character: Character): Float {
        val skill = character.education.clubSkill / 100f
        val prestige = character.education.clubPrestige / 100f
        return (0.28f + skill * 0.4f + prestige * 0.25f).coerceIn(0.15f, 0.85f)
    }

    fun applySchoolClubYear(character: Character): Character {
        val club = character.education.schoolClub ?: return character
        if (!isSchoolClubEligible(character)) {
            return leaveSchoolClub(character, fired = true)
        }
        // Passive year tick if they never practiced — small skill decay / drop risk.
        if (!character.education.clubActivityDoneThisYear && character.education.clubSkill < 25) {
            if (Random.nextFloat() < 0.2f) {
                return leaveSchoolClub(character, fired = true)
            }
        }
        val (happiness, health, smarts, gpaDelta) = when (club) {
            SchoolClub.DEBATE -> Quad(2, 0, 3, 0.08f)
            SchoolClub.FOOTBALL -> Quad(4, 3, 0, 0.03f)
            SchoolClub.DRAMA -> Quad(5, 0, 1, 0.04f)
            SchoolClub.CODING -> Quad(1, 0, 4, 0.06f)
            SchoolClub.MUSIC -> Quad(4, 0, 2, 0.05f)
        }
        val rankBonus = when (character.education.clubRank) {
            ClubRank.MEMBER -> 0
            ClubRank.OFFICER -> 1
            ClubRank.CAPTAIN -> 2
        }
        val captainYears = if (character.education.clubRank == ClubRank.CAPTAIN) {
            character.education.clubYearsAsCaptain + 1
        } else {
            character.education.clubYearsAsCaptain
        }
        val fameDelta = when {
            character.education.clubActivityDoneThisYear -> 2
            character.education.clubLetterJacket -> 0
            else -> -1
        }
        val looksBonus = when {
            character.education.clubLetterJacket -> 1
            character.education.clubFame >= 60 -> 1
            else -> 0
        }
        // High fame can draw a scout / agent whisper for sports & arts.
        val scoutLog = if (
            character.education.clubFame >= 55 &&
            character.education.clubRank.ordinal >= ClubRank.OFFICER.ordinal &&
            Random.nextFloat() < 0.18f
        ) {
            when (club) {
                SchoolClub.FOOTBALL -> "A talent scout watched practice and took notes on you."
                SchoolClub.DRAMA, SchoolClub.MUSIC -> "An arts recruiter asked for your contact after the showcase."
                SchoolClub.CODING -> "A tech internship recruiter liked your club project reel."
                SchoolClub.DEBATE -> "A university debate coach sent you a recruitment flyer."
            }
        } else {
            null
        }
        val withStats = character.copy(
            stats = character.stats.copy(
                happiness = clampStat(
                    character.stats.happiness + happiness + rankBonus + if (scoutLog != null) 3 else 0
                ),
                health = clampStat(character.stats.health + health),
                smarts = clampStat(character.stats.smarts + smarts),
                looks = clampStat(character.stats.looks + looksBonus)
            ),
            education = character.education.copy(
                gpa = if (character.education.gpa > 0f) {
                    clampGpa(character.education.gpa + gpaDelta)
                } else {
                    character.education.gpa
                },
                clubSkill = (character.education.clubSkill + 2).coerceIn(0, 100),
                clubPrestige = (character.education.clubPrestige + 1).coerceIn(0, 100),
                clubFame = (character.education.clubFame + fameDelta).coerceIn(0, 100),
                clubActivityDoneThisYear = false,
                clubFundraiserDoneThisYear = false,
                clubRivalryDoneThisYear = false,
                clubYearsAsCaptain = captainYears,
                clubResumeClub = club,
                clubMajorEventReady = character.education.clubRank == ClubRank.CAPTAIN
            ),
            eventLog = if (scoutLog != null) {
                EventLogCap.prepend(character.eventLog, scoutLog)
            } else {
                character.eventLog
            }
        )
        return withStats
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
        val withEffort = applyUniversityStudyEffort(updated)
        return if (nextGrade > UNIVERSITY_YEARS) {
            withEffort.copy(
                education = clearActiveClubMembership(withEffort.education).copy(
                    stage = SchoolStage.GRADUATED,
                    currentGrade = UNIVERSITY_YEARS,
                    schoolPeople = emptyList()
                ),
                eventLog = EventLogCap.prepend(
                    withEffort.eventLog,
                    "You graduated from university."
                )
            )
        } else {
            withEffort.copy(
                education = withEffort.education.copy(currentGrade = nextGrade)
            )
        }
    }

    private fun applyUniversityStudyEffort(character: Character): Character {
        val effort = character.education.plannedStudyEffort
        val smartsDelta = EffortResolver.studySmartsDelta(effort)
        val happinessDelta = EffortResolver.studyHappinessDelta(effort)
        val gpaDelta = EffortResolver.studyGpaDelta(effort)
        return character.copy(
            stats = character.stats.copy(
                smarts = clampStat(character.stats.smarts + smartsDelta),
                happiness = clampStat(character.stats.happiness + happinessDelta)
            ),
            education = character.education.copy(
                gpa = if (character.education.gpa > 0f) {
                    clampGpa(character.education.gpa + gpaDelta)
                } else {
                    clampGpa(2.0f + gpaDelta)
                }
            )
        )
    }

    private fun domesticUniversityTuition(character: Character): Int {
        // International enrollments already paid a large upfront fee.
        if (character.currentVisa == VisaType.STUDENT && character.isLivingAbroad()) return 0
        return EconomyScaler.scaleAmount(DOMESTIC_UNIVERSITY_TUITION_KENYA, character.countryCode)
    }

    /**
     * Scores national exams from GPA, study effort, reputation, stress, preparedness, and luck.
     *
     * @return Updated character and [ExamResult] for UI/system events.
     */
    fun takeExam(character: Character, examType: ExamType): Pair<Character, ExamResult> {
        val education = character.education
        val passChance = calculateExamPassChance(character)
        val randomFactor = Random.nextFloat() * 12f
        val prepBoost = imminentPreparedness(character) * 0.25f
        val stressDrag = education.examStress * 0.12f
        val effortBoost = when (education.plannedStudyEffort) {
            StudyEffort.HARD -> 8f
            StudyEffort.NORMAL -> 3f
            StudyEffort.SLACK -> -6f
        }
        val score = (
            education.gpa * 14f +
                character.stats.smarts * 0.4f +
                character.stats.happiness * 0.05f +
                character.stats.health * 0.05f +
                education.schoolReputation * 0.12f +
                prepBoost +
                effortBoost -
                stressDrag +
                randomFactor
            ).coerceIn(0f, 100f)

        val grade = scoreToLetterGrade(score)
        val passedByScore = when (examType) {
            ExamType.KCPE -> score >= KCPE_PASS_SCORE
            ExamType.KCSE -> score >= KCSE_PASS_SCORE
        }
        val passed = if (Random.nextFloat() < passChance) {
            passedByScore || score >= (if (examType == ExamType.KCPE) KCPE_PASS_SCORE - 5f else KCSE_PASS_SCORE - 5f)
        } else {
            false
        }
        val finalGrade = if (passed) grade else if (score >= 45f) grade else "F"

        val updatedEducation = when (examType) {
            ExamType.KCPE -> education.copy(
                kcpePassed = passed,
                examStress = (education.examStress - 15).coerceAtLeast(0),
                plannedCheatOnExam = false,
                lastExamSummary = "National exit: ${if (passed) "Passed" else "Failed"} $finalGrade (${score.roundToInt()}%)",
                pendingExams = education.pendingExams.filterNot {
                    it.kind == ExamKind.NATIONAL_EXIT && it.yearsUntilDue <= 0
                }
            )
            ExamType.KCSE -> if (passed) {
                education.copy(
                    kcseGrade = finalGrade,
                    examStress = (education.examStress - 15).coerceAtLeast(0),
                    plannedCheatOnExam = false,
                    lastExamSummary = "National exit: Passed $finalGrade (${score.roundToInt()}%)",
                    pendingExams = education.pendingExams.filterNot {
                        it.kind == ExamKind.NATIONAL_EXIT && it.yearsUntilDue <= 0
                    }
                )
            } else {
                education.copy(
                    examStress = (education.examStress + 5).coerceIn(0, 100),
                    plannedCheatOnExam = false,
                    lastExamSummary = "National exit: Failed $finalGrade (${score.roundToInt()}%)"
                )
            }
        }

        val updatedCharacter = character.copy(education = updatedEducation)
        return updatedCharacter to ExamResult(passed = passed, grade = finalGrade, score = score)
    }

    /** Composite 0–100 readiness shown on the School exam banner. */
    fun examPreparednessPercent(character: Character): Int {
        if (!isEnrolled(character)) return 0
        val education = character.education
        val gpaPart = ((education.gpa / 4f) * 35f)
        val effortPart = when (education.plannedStudyEffort) {
            StudyEffort.HARD -> 20f
            StudyEffort.NORMAL -> 12f
            StudyEffort.SLACK -> 3f
        }
        val repPart = education.schoolReputation * 0.15f
        val prepPart = imminentPreparedness(character) * 0.25f
        val stressPenalty = education.examStress * 0.22f
        return (gpaPart + effortPart + repPart + prepPart - stressPenalty)
            .roundToInt()
            .coerceIn(0, 100)
    }

    fun calculateExamPassChance(character: Character): Float {
        val education = character.education
        val gpaFactor = (education.gpa / 4f) * 0.32f
        val effortFactor = when (education.plannedStudyEffort) {
            StudyEffort.HARD -> 0.18f
            StudyEffort.NORMAL -> 0.11f
            StudyEffort.SLACK -> 0.02f
        }
        val repFactor = (education.schoolReputation / 100f) * 0.14f
        val prepFactor = (imminentPreparedness(character) / 100f) * 0.22f
        val smartsFactor = (character.stats.smarts / 100f) * 0.12f
        val stressPenalty = (education.examStress / 100f) * 0.28f
        return (0.22f + gpaFactor + effortFactor + repFactor + prepFactor + smartsFactor - stressPenalty)
            .coerceIn(0.05f, 0.95f)
    }

    fun hasImminentExam(character: Character): Boolean =
        character.education.pendingExams.any { it.yearsUntilDue <= 0 }

    fun imminentExams(character: Character): List<ExamSchedule> =
        character.education.pendingExams.filter { it.yearsUntilDue <= 0 }

    /**
     * School-card prep before Age Up. One prep action per year.
     * Raises preparedness / adjusts stress; does not sit the exam yet.
     */
    fun performExamPrepAction(character: Character, choice: ExamPrepChoice): ExamPrepResult {
        var working = character
        if (isEnrolled(working) && working.education.pendingExams.isEmpty()) {
            working = refreshExamSchedule(working)
        }
        if (!isEnrolled(working) || !working.alive) return ExamPrepResult.Ineligible
        if (working.criminalRecord.currentlyIncarcerated) return ExamPrepResult.Ineligible
        if (!hasImminentExam(working)) return ExamPrepResult.Ineligible
        if (working.education.examPrepDoneThisYear) return ExamPrepResult.AlreadyDone

        return when (choice) {
            ExamPrepChoice.STUDY_HARD -> ExamPrepResult.Success(
                applyPrepAdjustments(
                    character = working,
                    prepDelta = 18,
                    stressDelta = -10,
                    gpaDelta = 0.12f,
                    happinessDelta = -3,
                    healthDelta = -1,
                    message = "You studied hard. Stress eased and you feel sharper for the exam."
                ),
                "You studied hard. Preparedness up, stress down."
            )
            ExamPrepChoice.STUDY_NORMAL -> ExamPrepResult.Success(
                applyPrepAdjustments(
                    character = working,
                    prepDelta = 10,
                    stressDelta = -2,
                    gpaDelta = 0.06f,
                    happinessDelta = -1,
                    healthDelta = 0,
                    message = "You put in a normal study session. Solid progress."
                ),
                "Balanced study session. Steady progress."
            )
            ExamPrepChoice.CRAM -> ExamPrepResult.Success(
                applyPrepAdjustments(
                    character = working,
                    prepDelta = 14,
                    stressDelta = 16,
                    gpaDelta = 0.10f,
                    happinessDelta = -2,
                    healthDelta = -3,
                    message = "You crammed all night. Knowledge stuck — so did the headache."
                ),
                "All-nighter cram. Preparedness up, stress spiked."
            )
            ExamPrepChoice.CHEAT -> {
                val caught = Random.nextFloat() < CHEAT_CATCH_CHANCE
                if (!caught) {
                    ExamPrepResult.Success(
                        applyPrepAdjustments(
                            character = working,
                            prepDelta = 28,
                            stressDelta = 8,
                            gpaDelta = 0.05f,
                            happinessDelta = 1,
                            healthDelta = 0,
                            karmaDelta = -4,
                            plannedCheat = true,
                            message = "You lined up a cheat sheet. Risky — but you feel 'ready'."
                        ),
                        "Cheat prep ready. Catch risk rises on exam day."
                    )
                } else {
                    val expel = Random.nextFloat() < CHEAT_EXPEL_CHANCE
                    val after = if (expel) {
                        processExpulsion(
                            working.copy(
                                stats = working.stats.copy(
                                    karma = clampStat(working.stats.karma - 8),
                                    happiness = clampStat(working.stats.happiness - 10)
                                ),
                                education = working.education.copy(
                                    examPrepDoneThisYear = true,
                                    examStress = (working.education.examStress + 20).coerceIn(0, 100),
                                    detentionYears = working.education.detentionYears + 1
                                )
                            )
                        )
                    } else {
                        working.copy(
                            stats = working.stats.copy(
                                karma = clampStat(working.stats.karma - 6),
                                happiness = clampStat(working.stats.happiness - 6)
                            ),
                            education = working.education.copy(
                                examPrepDoneThisYear = true,
                                examStress = (working.education.examStress + 15).coerceIn(0, 100),
                                schoolReputation = (working.education.schoolReputation - 12)
                                    .coerceIn(0, 100),
                                detentionYears = working.education.detentionYears + 1,
                                gpa = clampGpa(working.education.gpa - 0.2f)
                            ),
                            eventLog = EventLogCap.prepend(
                                working.eventLog,
                                "Caught cheating while preparing. Detention."
                            )
                        )
                    }
                    ExamPrepResult.Success(
                        after,
                        if (expel) "Caught cheating — expelled." else "Caught cheating. Detention."
                    )
                }
            }
        }
    }

    /**
     * Resolves an age-up exam prompt choice: applies prep effects, then sits the exam.
     * Cheat on the day (or prior planned cheat) can yield an instant top grade — or detention/expulsion.
     */
    fun resolveExamWithPrepChoice(character: Character, choice: ExamPrepChoice): Character {
        var working = if (isEnrolled(character) && character.education.pendingExams.isEmpty()) {
            refreshExamSchedule(character)
        } else {
            character
        }

        // Exam-day cheat is resolved here (not via prep-only path).
        if (choice == ExamPrepChoice.CHEAT || working.education.plannedCheatOnExam) {
            val dayCatchChance = if (working.education.plannedCheatOnExam) {
                CHEAT_DAY_CATCH_CHANCE_PLANNED
            } else {
                CHEAT_DAY_CATCH_CHANCE
            }
            if (Random.nextFloat() < dayCatchChance) {
                return resolveCaughtCheatingOnExamDay(working)
            }
            return resolveSuccessfulCheatExam(working)
        }

        val afterPrep = when (
            val prep = performExamPrepAction(
                working.copy(education = working.education.copy(examPrepDoneThisYear = false)),
                choice
            )
        ) {
            is ExamPrepResult.Success -> prep.character
            else -> working
        }
        if (afterPrep.education.expelled) return afterPrep

        return sitScheduledExam(afterPrep, choice)
    }

    private fun resolveCaughtCheatingOnExamDay(character: Character): Character {
        val expel = Random.nextFloat() < CHEAT_EXPEL_CHANCE
        val base = character.copy(
            stats = character.stats.copy(
                karma = clampStat(character.stats.karma - 10),
                happiness = clampStat(character.stats.happiness - 12)
            ),
            education = character.education.copy(
                plannedCheatOnExam = false,
                examPrepDoneThisYear = true,
                examStress = (character.education.examStress + 25).coerceIn(0, 100),
                schoolReputation = (character.education.schoolReputation - 18).coerceIn(0, 100),
                detentionYears = character.education.detentionYears + 1,
                gpa = clampGpa(character.education.gpa - 0.35f),
                lastExamSummary = if (expel) {
                    "Caught cheating — expelled"
                } else {
                    "Caught cheating — detention"
                }
            )
        )
        val after = if (expel) processExpulsion(base) else base
        return after.copy(
            eventLog = EventLogCap.prepend(
                after.eventLog,
                if (expel) {
                    "Invigilators caught you cheating. You were expelled."
                } else {
                    "Caught cheating in the exam hall. Detention and a ruined grade."
                }
            )
        )
    }

    private fun resolveSuccessfulCheatExam(character: Character): Character {
        val boosted = character.copy(
            stats = character.stats.copy(
                karma = clampStat(character.stats.karma - 5),
                happiness = clampStat(character.stats.happiness + 4)
            ),
            education = character.education.copy(
                plannedCheatOnExam = false,
                examPrepDoneThisYear = true,
                examStress = (character.education.examStress - 8).coerceAtLeast(0),
                gpa = clampGpa(character.education.gpa + 0.4f),
                schoolReputation = (character.education.schoolReputation + 2).coerceIn(0, 100)
            )
        )
        return when {
            shouldTriggerPrimaryExam(boosted) -> {
                val forced = takeExamWithForcedResult(boosted, ExamType.KCPE, passed = true, grade = "A", score = 92f)
                annotateExamResult(forced.first, forced.second, com.maisha.game.data.ExamNames.primaryExamName(boosted.countryCode))
            }
            shouldTriggerSecondaryExam(boosted) -> {
                val forced = takeExamWithForcedResult(boosted, ExamType.KCSE, passed = true, grade = "A", score = 90f)
                annotateExamResult(forced.first, forced.second, com.maisha.game.data.ExamNames.secondaryExamName(boosted.countryCode))
            }
            else -> {
                val summary = "Cheated through finals — A (undetected)"
                boosted.copy(
                    education = boosted.education.copy(lastExamSummary = summary),
                    eventLog = EventLogCap.prepend(
                        boosted.eventLog,
                        "You cheated through year exams and walked out with top marks. For now."
                    )
                )
            }
        }
    }

    private fun sitScheduledExam(character: Character, choice: ExamPrepChoice): Character {
        return when {
            shouldTriggerPrimaryExam(character) -> {
                val (scored, result) = takeExam(character, ExamType.KCPE)
                annotateExamResult(
                    scored,
                    result,
                    com.maisha.game.data.ExamNames.primaryExamName(scored.countryCode)
                )
            }
            shouldTriggerSecondaryExam(character) -> {
                val (scored, result) = takeExam(character, ExamType.KCSE)
                annotateExamResult(
                    scored,
                    result,
                    com.maisha.game.data.ExamNames.secondaryExamName(scored.countryCode)
                )
            }
            else -> resolveYearFinals(character, choice)
        }
    }

    private fun takeExamWithForcedResult(
        character: Character,
        examType: ExamType,
        passed: Boolean,
        grade: String,
        score: Float
    ): Pair<Character, ExamResult> {
        val education = character.education
        val updatedEducation = when (examType) {
            ExamType.KCPE -> education.copy(
                kcpePassed = passed,
                examStress = (education.examStress - 15).coerceAtLeast(0),
                pendingExams = education.pendingExams.filterNot {
                    it.kind == ExamKind.NATIONAL_EXIT && it.yearsUntilDue <= 0
                }
            )
            ExamType.KCSE -> education.copy(
                kcseGrade = if (passed) grade else education.kcseGrade,
                examStress = (education.examStress - 15).coerceAtLeast(0),
                pendingExams = education.pendingExams.filterNot {
                    it.kind == ExamKind.NATIONAL_EXIT && it.yearsUntilDue <= 0
                }
            )
        }
        return character.copy(education = updatedEducation) to
            ExamResult(passed = passed, grade = grade, score = score)
    }

    private fun annotateExamResult(
        character: Character,
        result: ExamResult,
        examName: String
    ): Character {
        val summary = "$examName: ${if (result.passed) "Passed" else "Failed"} ${result.grade} (${result.score.roundToInt()}%)"
        return character.copy(
            education = character.education.copy(
                lastExamSummary = summary,
                plannedCheatOnExam = false
            ),
            eventLog = EventLogCap.prepend(character.eventLog, examOutcomeLog(examName, result))
        )
    }

    /** Prompt event with Study hard / normal / Cram / Cheat — replaces auto exam sit. */
    fun buildExamPromptEvent(character: Character): LifeEvent? {
        if (!isEnrolled(character) || character.education.expelled) return null
        val nationalDue = shouldTriggerPrimaryExam(character) || shouldTriggerSecondaryExam(character)
        val finalsDue = character.education.pendingExams.any {
            it.kind == ExamKind.FINALS && it.yearsUntilDue <= 0
        }
        if (!nationalDue && !finalsDue) return null
        val title = when {
            shouldTriggerPrimaryExam(character) ->
                com.maisha.game.data.ExamNames.primaryExamName(character.countryCode)
            shouldTriggerSecondaryExam(character) ->
                com.maisha.game.data.ExamNames.secondaryExamName(character.countryCode)
            else -> character.education.pendingExams.firstOrNull {
                it.kind == ExamKind.FINALS && it.yearsUntilDue <= 0
            }?.title ?: "Year exams"
        }
        val prep = examPreparednessPercent(character)
        val stress = character.education.examStress
        val chance = (calculateExamPassChance(character) * 100).roundToInt()
        val outlook = examOutlookTip(prep, stress, character.education.plannedCheatOnExam)
        return LifeEvent(
            id = "exam_prompt_${character.age}_${title.replace(' ', '_').lowercase()}",
            minAge = character.age,
            maxAge = character.age,
            text = "$title are here. Preparedness $prep%. Stress $stress. Pass chance ~$chance%. $outlook How do you play it?",
            choices = listOf(
                EventChoice(
                    label = "Study hard",
                    resultText = "You lock in and give it everything.",
                    examPrepAction = ExamPrepChoice.STUDY_HARD.name
                ),
                EventChoice(
                    label = "Study normally",
                    resultText = "You stick to a balanced plan.",
                    examPrepAction = ExamPrepChoice.STUDY_NORMAL.name
                ),
                EventChoice(
                    label = "Cram all night",
                    resultText = "Coffee, notes, sunrise.",
                    examPrepAction = ExamPrepChoice.CRAM.name
                ),
                EventChoice(
                    label = "Cheat on the exam",
                    resultText = "You gamble on a shortcut.",
                    examPrepAction = ExamPrepChoice.CHEAT.name
                )
            ),
            tags = listOf(EXAM_SYSTEM_TAG, EXAM_PROMPT_TAG, "one_time"),
            weight = 10
        )
    }

    fun examOutlookTip(preparedness: Int, stress: Int, plannedCheat: Boolean): String {
        if (plannedCheat) return "A cheat plan is in motion — high reward, high risk."
        return when {
            preparedness >= 75 && stress < 50 -> "You look ready."
            preparedness >= 55 -> "You're in the mix if you stay focused."
            stress >= 70 -> "Stress is eating your focus."
            else -> "You are underprepared."
        }
    }

    fun refreshExamSchedule(character: Character): Character {
        if (!isEnrolled(character) || character.education.expelled) {
            return if (character.education.pendingExams.isEmpty()) {
                character
            } else {
                character.copy(education = character.education.copy(pendingExams = emptyList()))
            }
        }
        val education = character.education
        val priorPrep = education.pendingExams.associate { it.kind to it.preparedness }
        val exams = mutableListOf<ExamSchedule>()
        val stage = education.stage

        fun prepFor(kind: ExamKind, default: Int = 40) =
            priorPrep[kind] ?: default

        when (stage) {
            SchoolStage.PRIMARY -> {
                exams += ExamSchedule(
                    id = "midterm_primary_${education.currentGrade}",
                    kind = ExamKind.MIDTERM,
                    title = "Midterms",
                    yearsUntilDue = 0,
                    preparedness = prepFor(ExamKind.MIDTERM)
                )
                if (education.currentGrade >= PRIMARY_MAX_GRADE - 1 && education.kcpePassed != true) {
                    val due = if (shouldTriggerPrimaryExam(character) ||
                        education.currentGrade >= PRIMARY_MAX_GRADE
                    ) {
                        0
                    } else {
                        1
                    }
                    exams += ExamSchedule(
                        id = "national_primary",
                        kind = ExamKind.NATIONAL_EXIT,
                        title = com.maisha.game.data.ExamNames.primaryExamName(character.countryCode),
                        yearsUntilDue = due,
                        preparedness = prepFor(ExamKind.NATIONAL_EXIT, 35)
                    )
                } else {
                    exams += ExamSchedule(
                        id = "finals_primary_${education.currentGrade}",
                        kind = ExamKind.FINALS,
                        title = "End-of-year finals",
                        yearsUntilDue = 0,
                        preparedness = prepFor(ExamKind.FINALS)
                    )
                }
            }
            SchoolStage.SECONDARY -> {
                exams += ExamSchedule(
                    id = "midterm_secondary_${education.currentGrade}",
                    kind = ExamKind.MIDTERM,
                    title = "Midterms",
                    yearsUntilDue = 0,
                    preparedness = prepFor(ExamKind.MIDTERM)
                )
                if (education.currentGrade >= SECONDARY_MAX_GRADE - 1 && education.kcseGrade == null) {
                    val due = if (shouldTriggerSecondaryExam(character) ||
                        education.currentGrade >= SECONDARY_MAX_GRADE
                    ) {
                        0
                    } else {
                        1
                    }
                    exams += ExamSchedule(
                        id = "national_secondary",
                        kind = ExamKind.NATIONAL_EXIT,
                        title = com.maisha.game.data.ExamNames.secondaryExamName(character.countryCode),
                        yearsUntilDue = due,
                        preparedness = prepFor(ExamKind.NATIONAL_EXIT, 35)
                    )
                } else {
                    exams += ExamSchedule(
                        id = "finals_secondary_${education.currentGrade}",
                        kind = ExamKind.FINALS,
                        title = "End-of-year finals",
                        yearsUntilDue = 0,
                        preparedness = prepFor(ExamKind.FINALS)
                    )
                }
            }
            SchoolStage.UNIVERSITY -> {
                exams += ExamSchedule(
                    id = "midterm_uni_${education.currentGrade}",
                    kind = ExamKind.MIDTERM,
                    title = "Midterm papers",
                    yearsUntilDue = 0,
                    preparedness = prepFor(ExamKind.MIDTERM)
                )
                exams += ExamSchedule(
                    id = "finals_uni_${education.currentGrade}",
                    kind = ExamKind.FINALS,
                    title = "Final exams",
                    yearsUntilDue = 0,
                    preparedness = prepFor(ExamKind.FINALS)
                )
            }
            else -> Unit
        }

        var stress = education.examStress
        if (exams.any { it.yearsUntilDue <= 0 }) {
            stress = (stress + 4).coerceIn(0, 100)
        }
        if (education.plannedStudyEffort == StudyEffort.HARD) {
            stress = (stress + 2).coerceIn(0, 100)
        }

        return character.copy(
            education = education.copy(
                pendingExams = exams,
                examStress = stress
            )
        )
    }

    private fun resolveYearFinals(character: Character, choice: ExamPrepChoice): Character {
        val chance = calculateExamPassChance(character)
        val passed = Random.nextFloat() < chance
        val gpaDelta = when {
            choice == ExamPrepChoice.CHEAT && passed -> 0.35f
            passed -> 0.18f
            else -> -0.15f
        }
        val summary = if (passed) {
            "Year finals: Passed (~${(chance * 100).roundToInt()}% chance)"
        } else {
            "Year finals: Failed"
        }
        val message = if (passed) {
            "You passed your year exams."
        } else {
            "You underperformed on year exams. GPA takes a hit."
        }
        val streak = if (passed) 0 else character.education.failedExamStreak + 1
        var updated = character.copy(
            education = character.education.copy(
                gpa = clampGpa(character.education.gpa + gpaDelta),
                examStress = (character.education.examStress - 10).coerceAtLeast(0),
                schoolReputation = (character.education.schoolReputation + if (passed) 3 else -4)
                    .coerceIn(0, 100),
                lastExamSummary = summary,
                plannedCheatOnExam = false,
                failedExamStreak = streak,
                pendingExams = character.education.pendingExams.map {
                    if (it.yearsUntilDue <= 0 && it.kind != ExamKind.NATIONAL_EXIT) {
                        it.copy(preparedness = 40, yearsUntilDue = 1)
                    } else {
                        it
                    }
                }
            ),
            eventLog = EventLogCap.prepend(character.eventLog, message)
        )
        if (streak >= 2) {
            updated = recordDetention(
                updated,
                reason = "Repeated exam failures finally caught the principal's eye.",
                reputationHit = 6,
                happinessHit = 2
            )
        }
        return updated
    }

    private fun applyPrepAdjustments(
        character: Character,
        prepDelta: Int,
        stressDelta: Int,
        gpaDelta: Float,
        happinessDelta: Int,
        healthDelta: Int,
        message: String,
        karmaDelta: Int = 0,
        plannedCheat: Boolean = false
    ): Character {
        val updatedExams = character.education.pendingExams.map { exam ->
            if (exam.yearsUntilDue <= 0) {
                exam.copy(preparedness = (exam.preparedness + prepDelta).coerceIn(0, 100))
            } else {
                exam
            }
        }
        return character.copy(
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness + happinessDelta),
                health = clampStat(character.stats.health + healthDelta),
                karma = clampStat(character.stats.karma + karmaDelta),
                smarts = clampStat(
                    character.stats.smarts + if (prepDelta >= 14) 2 else if (prepDelta >= 8) 1 else 0
                )
            ),
            education = character.education.copy(
                pendingExams = updatedExams,
                examStress = (character.education.examStress + stressDelta).coerceIn(0, 100),
                gpa = clampGpa(character.education.gpa + gpaDelta),
                examPrepDoneThisYear = true,
                plannedCheatOnExam = character.education.plannedCheatOnExam || plannedCheat
            ),
            eventLog = EventLogCap.prepend(character.eventLog, message)
        )
    }

    private fun imminentPreparedness(character: Character): Float {
        val imminent = imminentExams(character)
        if (imminent.isEmpty()) return 40f
        return imminent.map { it.preparedness }.average().toFloat()
    }

    private fun examOutcomeLog(examName: String, result: ExamResult): String {
        val outcome = if (result.passed) "passed" else "did not pass"
        return "$examName results: you $outcome with ${result.grade} (${result.score.roundToInt()}%)."
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

        val scholarship = clubScholarshipEstimate(enrolled)
        val withScholarship = if (scholarship > 0 && !studyAbroad) {
            enrolled.copy(
                stats = enrolled.stats.copy(
                    money = enrolled.stats.money + scholarship,
                    happiness = clampStat(enrolled.stats.happiness + 4)
                ),
                eventLog = EventLogCap.prepend(
                    enrolled.eventLog,
                    "Your extracurricular resume unlocked a ${formatMoney(scholarship, enrolled.countryCode)} scholarship."
                )
            )
        } else {
            enrolled
        }

        return ensureSchoolRoster(
            withScholarship.copy(
                education = clearActiveClubMembership(withScholarship.education).copy(
                    stage = SchoolStage.UNIVERSITY,
                    currentGrade = 1,
                    courseOfStudy = course,
                    schoolName = universityNameFor(universityCountryCode),
                    academicActionDoneThisYear = false,
                    socialActionDoneThisYear = false
                )
            ),
            forceRefresh = true
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
            education = clearActiveClubMembership(education).copy(
                droppedOutFrom = stage,
                stage = SchoolStage.NONE,
                currentGrade = 0,
                schoolName = null,
                courseOfStudy = null,
                schoolPeople = emptyList(),
                academicActionDoneThisYear = false,
                socialActionDoneThisYear = false,
                pendingExpulsionHearing = false,
                onProbation = false,
                detentionCountThisYear = 0
            )
        )
    }

    /** Forced removal from school — blocks all future enrollment and progression. */
    fun processExpulsion(character: Character): Character {
        val education = character.education
        if (education.expelled) return character

        return character.copy(
            education = clearActiveClubMembership(education).copy(
                expelled = true,
                stage = SchoolStage.NONE,
                currentGrade = 0,
                schoolName = null,
                courseOfStudy = null,
                schoolPeople = emptyList(),
                pendingExams = emptyList(),
                examStress = 0,
                schoolReputation = (education.schoolReputation - 20).coerceIn(0, 100),
                academicActionDoneThisYear = false,
                socialActionDoneThisYear = false,
                pendingExpulsionHearing = false,
                onProbation = false,
                detentionCountThisYear = 0
            )
        )
    }

    /** True when still enrolled in secondary or university and eligible to leave voluntarily. */
    fun canVoluntarilyDropOut(character: Character): Boolean {
        val education = character.education
        if (education.expelled) return false
        return education.stage == SchoolStage.SECONDARY || education.stage == SchoolStage.UNIVERSITY
    }

    fun isEnrolled(character: Character): Boolean {
        val stage = character.education.stage
        return !character.education.expelled &&
            (stage == SchoolStage.PRIMARY ||
                stage == SchoolStage.SECONDARY ||
                stage == SchoolStage.UNIVERSITY)
    }

    /** Age school NPCs, decay unattended relationships, reset yearly school action flags. */
    fun tickSchoolYear(character: Character): Character {
        if (!isEnrolled(character)) {
            return if (character.education.schoolPeople.isNotEmpty()) {
                character.copy(
                    education = character.education.copy(
                        schoolPeople = emptyList(),
                        academicActionDoneThisYear = false,
                        socialActionDoneThisYear = false
                    )
                )
            } else {
                character
            }
        }
        val people = character.education.schoolPeople.map { person ->
            val enriched = enrichSchoolPersonIfNeeded(person)
            val aged = enriched.copy(age = enriched.age + 1)
            if (aged.interactedThisYear) {
                aged.copy(interactedThisYear = false)
            } else {
                aged.copy(
                    relationshipLevel = clampRelationshipLevel(aged.relationshipLevel - 1)
                )
            }
        }
        val withPeople = character.copy(
            education = character.education.copy(
                schoolPeople = people,
                academicActionDoneThisYear = false,
                socialActionDoneThisYear = false,
                examPrepDoneThisYear = false,
                // Reset yearly detention tally only when no hearing is queued.
                detentionCountThisYear = if (character.education.pendingExpulsionHearing) {
                    character.education.detentionCountThisYear
                } else {
                    0
                },
                // Clean year clears probation.
                onProbation = if (
                    !character.education.pendingExpulsionHearing &&
                    character.education.detentionCountThisYear == 0
                ) {
                    false
                } else {
                    character.education.onProbation
                }
            )
        )
        return refreshExamSchedule(resolveMidtermsQuietly(withPeople))
    }

    /** Quiet midterm resolution — feeds GPA/stress without a dialog. */
    private fun resolveMidtermsQuietly(character: Character): Character {
        val midterms = character.education.pendingExams.filter {
            it.kind == ExamKind.MIDTERM && it.yearsUntilDue <= 0
        }
        if (midterms.isEmpty()) return character
        val avgPrep = midterms.map { it.preparedness }.average().toFloat()
        val passed = avgPrep + (100 - character.education.examStress) * 0.25f +
            character.education.gpa * 8f >= 55f
        val gpaDelta = if (passed) 0.08f else -0.1f
        val stressDelta = if (passed) -4 else 6
        val note = if (passed) {
            "Midterms went fine."
        } else {
            "Midterms stung. More revision needed before finals."
        }
        return character.copy(
            education = character.education.copy(
                gpa = clampGpa(character.education.gpa + gpaDelta),
                examStress = (character.education.examStress + stressDelta).coerceIn(0, 100),
                pendingExams = character.education.pendingExams.map { exam ->
                    if (exam.kind == ExamKind.MIDTERM && exam.yearsUntilDue <= 0) {
                        exam.copy(yearsUntilDue = 1, preparedness = 45)
                    } else {
                        exam
                    }
                }
            ),
            eventLog = EventLogCap.prepend(character.eventLog, note)
        )
    }

    fun ensureSchoolRoster(character: Character, forceRefresh: Boolean = false): Character {
        if (!isEnrolled(character)) return character
        if (!forceRefresh && character.education.schoolPeople.isNotEmpty()) {
            val enriched = character.education.schoolPeople.map { enrichSchoolPersonIfNeeded(it) }
            val withPeople = if (enriched == character.education.schoolPeople) {
                character
            } else {
                character.copy(education = character.education.copy(schoolPeople = enriched))
            }
            return refreshExamSchedule(withPeople)
        }
        val people = generateSchoolRoster(character)
        return refreshExamSchedule(
            character.copy(
                education = character.education.copy(schoolPeople = people),
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    "You met new people at ${character.education.schoolName ?: "school"}."
                )
            )
        )
    }

    fun availableSchoolActivities(character: Character): List<SchoolActivity> {
        if (!isEnrolled(character) || !character.alive) return emptyList()
        if (character.criminalRecord.currentlyIncarcerated) return emptyList()
        val list = mutableListOf(
            SchoolActivity.LIBRARY_STUDY,
            SchoolActivity.STUDY_GROUP,
            SchoolActivity.GROUP_PROJECT,
            SchoolActivity.ASK_TEACHER_HELP,
            SchoolActivity.HANG_OUT,
            SchoolActivity.SKIP_CLASS,
            SchoolActivity.PULL_PRANK,
            SchoolActivity.TALK_BACK
        )
        if (character.education.schoolPeople.any { it.role == SchoolRole.BULLY }) {
            list += SchoolActivity.CONFRONT_BULLY
            list += SchoolActivity.START_FIGHT
        }
        if (character.education.stage == SchoolStage.SECONDARY &&
            character.age in 15..18
        ) {
            list += SchoolActivity.SCHOOL_DANCE
        }
        if (character.education.schoolClub != null && isSchoolClubEligible(character)) {
            list += SchoolActivity.CLUB_PRACTICE
        }
        return list
    }

    fun performSchoolActivity(
        character: Character,
        activity: SchoolActivity,
        targetPersonId: String? = null
    ): SchoolActionResult {
        if (!isEnrolled(character) || !character.alive) return SchoolActionResult.Ineligible
        if (character.criminalRecord.currentlyIncarcerated) return SchoolActionResult.Ineligible
        if (activity !in availableSchoolActivities(character)) return SchoolActionResult.Ineligible

        val isSocial = activity in SOCIAL_ACTIVITIES
        if (isSocial && character.education.socialActionDoneThisYear) {
            return SchoolActionResult.AlreadyDone
        }
        if (!isSocial && character.education.academicActionDoneThisYear) {
            return SchoolActionResult.AlreadyDone
        }

        val target = targetPersonId?.let { id ->
            character.education.schoolPeople.find { it.id == id }
        }

        return when (activity) {
            SchoolActivity.LIBRARY_STUDY -> succeedAcademic(
                character,
                gpaDelta = 0.12f,
                smarts = 3,
                happiness = -1,
                reputation = 1,
                message = "You spent hours in the library and actually understood the material."
            )
            SchoolActivity.STUDY_GROUP -> {
                val partner = target ?: classmates(character).randomOrNull()
                    ?: return SchoolActionResult.PersonNotFound
                succeedWithPerson(
                    character = character,
                    person = partner,
                    academic = true,
                    gpaDelta = 0.18f,
                    smarts = 2,
                    happiness = 2,
                    relationshipDelta = 8,
                    reputation = 2,
                    message = "You studied with ${partner.name}. The quiz suddenly feels doable."
                )
            }
            SchoolActivity.GROUP_PROJECT -> {
                val partner = target ?: classmates(character).randomOrNull()
                    ?: return SchoolActionResult.PersonNotFound
                succeedWithPerson(
                    character = character,
                    person = partner,
                    academic = true,
                    gpaDelta = 0.15f,
                    smarts = 2,
                    happiness = 1,
                    relationshipDelta = 6,
                    reputation = 3,
                    message = "Your group project with ${partner.name} impressed the teacher."
                )
            }
            SchoolActivity.ASK_TEACHER_HELP -> {
                val teacher = target?.takeIf { it.role == SchoolRole.TEACHER }
                    ?: teachers(character).randomOrNull()
                    ?: return SchoolActionResult.PersonNotFound
                succeedWithPerson(
                    character = character,
                    person = teacher,
                    academic = true,
                    gpaDelta = 0.22f,
                    smarts = 3,
                    happiness = 1,
                    relationshipDelta = 10,
                    reputation = 4,
                    message = "${teacher.name} stayed after class to help you. It clicked."
                )
            }
            SchoolActivity.HANG_OUT -> {
                val friend = target ?: classmates(character).randomOrNull()
                    ?: return SchoolActionResult.PersonNotFound
                succeedWithPerson(
                    character = character,
                    person = friend,
                    academic = false,
                    gpaDelta = -0.02f,
                    smarts = 0,
                    happiness = 5,
                    relationshipDelta = 12,
                    reputation = 1,
                    message = "You hung out with ${friend.name} after school. Best part of the day."
                )
            }
            SchoolActivity.CONFRONT_BULLY -> {
                val bully = target?.takeIf { it.role == SchoolRole.BULLY }
                    ?: character.education.schoolPeople.find { it.role == SchoolRole.BULLY }
                    ?: return SchoolActionResult.PersonNotFound
                val won = character.stats.health >= 55 || Random.nextFloat() < 0.45f
                if (won) {
                    succeedWithPerson(
                        character = character,
                        person = bully,
                        academic = false,
                        gpaDelta = 0f,
                        smarts = 0,
                        happiness = 4,
                        relationshipDelta = -15,
                        reputation = 6,
                        message = "You stood up to ${bully.name}. Word spreads that you won't be pushed around."
                    )
                } else {
                    val bruised = succeedWithPerson(
                        character = character,
                        person = bully,
                        academic = false,
                        gpaDelta = -0.05f,
                        smarts = 0,
                        happiness = -6,
                        relationshipDelta = -8,
                        reputation = -4,
                        healthDelta = -4,
                        message = "Confronting ${bully.name} went badly. You got shoved and sent home early."
                    )
                    if (bruised is SchoolActionResult.Success && Random.nextFloat() < 0.4f) {
                        val detained = recordDetention(
                            bruised.character,
                            reason = "Staff broke up the hallway scene with ${bully.name}.",
                            reputationHit = 6
                        )
                        SchoolActionResult.Success(detained, "Fight fallout — detention.")
                    } else {
                        bruised
                    }
                }
            }
            SchoolActivity.START_FIGHT -> {
                val bully = target?.takeIf { it.role == SchoolRole.BULLY }
                    ?: character.education.schoolPeople.find { it.role == SchoolRole.BULLY }
                    ?: return SchoolActionResult.PersonNotFound
                val power = character.stats.health * 0.55f + character.stats.looks * 0.15f
                val winChance = (0.25f + power / 100f * 0.55f).coerceIn(0.15f, 0.85f)
                val won = Random.nextFloat() < winChance
                val caught = Random.nextFloat() < 0.55f
                val base = if (won) {
                    succeedWithPerson(
                        character = character,
                        person = bully,
                        academic = false,
                        gpaDelta = -0.04f,
                        smarts = 0,
                        happiness = 6,
                        relationshipDelta = -20,
                        reputation = if (caught) -2 else 4,
                        healthDelta = -Random.nextInt(2, 6),
                        message = "You threw hands with ${bully.name} and won the scrap."
                    )
                } else {
                    succeedWithPerson(
                        character = character,
                        person = bully,
                        academic = false,
                        gpaDelta = -0.08f,
                        smarts = 0,
                        happiness = -8,
                        relationshipDelta = -10,
                        reputation = -6,
                        healthDelta = -Random.nextInt(6, 12),
                        message = "${bully.name} flattened you. Everyone saw it."
                    )
                }
                if (base !is SchoolActionResult.Success) return base
                if (caught || !won) {
                    val detained = recordDetention(
                        base.character.copy(
                            education = base.character.education.copy(socialActionDoneThisYear = true)
                        ),
                        reason = "Fighting ${bully.name} on school grounds.",
                        reputationHit = 10,
                        happinessHit = 2
                    )
                    SchoolActionResult.Success(
                        detained,
                        if (won) "Won the fight — still got detention." else "Lost the fight and earned detention."
                    )
                } else {
                    base
                }
            }
            SchoolActivity.SKIP_CLASS -> {
                val caught = Random.nextFloat() < if (character.education.onProbation) 0.55f else 0.38f
                if (caught) {
                    val marked = character.copy(
                        stats = character.stats.copy(
                            happiness = clampStat(character.stats.happiness + 2),
                            smarts = clampStat(character.stats.smarts - 2),
                            health = clampStat(character.stats.health + 1)
                        ),
                        education = character.education.copy(
                            gpa = clampGpa(character.education.gpa - 0.12f),
                            socialActionDoneThisYear = true
                        )
                    )
                    val detained = recordDetention(
                        marked,
                        reason = "You skipped class and got caught.",
                        reputationHit = 8
                    )
                    SchoolActionResult.Success(detained, "Caught skipping. Detention and a lecture.")
                } else {
                    succeedSocial(
                        character,
                        gpaDelta = -0.08f,
                        happiness = 5,
                        reputation = -3,
                        smartsDelta = -1,
                        message = "You skipped class and got away with it — this time."
                    )
                }
            }
            SchoolActivity.PULL_PRANK -> {
                val caught = Random.nextFloat() < if (character.education.onProbation) 0.6f else 0.42f
                if (caught) {
                    val entertained = character.copy(
                        stats = character.stats.copy(
                            happiness = clampStat(character.stats.happiness + 3),
                            karma = clampStat(character.stats.karma - 3)
                        ),
                        education = character.education.copy(socialActionDoneThisYear = true)
                    )
                    val detained = recordDetention(
                        entertained,
                        reason = "Your prank blew up — staff identified you immediately.",
                        reputationHit = 12,
                        happinessHit = 4
                    )
                    SchoolActionResult.Success(detained, "Prank backfired. Detention.")
                } else {
                    succeedSocial(
                        character,
                        gpaDelta = -0.02f,
                        happiness = 8,
                        reputation = 2,
                        karmaDelta = -2,
                        message = "The prank was legendary. Hallways still buzzing."
                    )
                }
            }
            SchoolActivity.TALK_BACK -> {
                val teacher = target?.takeIf { it.role == SchoolRole.TEACHER }
                    ?: teachers(character).randomOrNull()
                    ?: return SchoolActionResult.PersonNotFound
                val detained = Random.nextFloat() < 0.7f
                if (detained) {
                    val snark = succeedWithPerson(
                        character = character,
                        person = teacher,
                        academic = false,
                        gpaDelta = -0.1f,
                        smarts = 0,
                        happiness = 3,
                        relationshipDelta = -14,
                        reputation = -6,
                        message = "You talked back to ${teacher.name}."
                    )
                    if (snark is SchoolActionResult.Success) {
                        val after = recordDetention(
                            snark.character,
                            reason = "Talking back to ${teacher.name}.",
                            reputationHit = 7
                        )
                        SchoolActionResult.Success(after, "Mouthing off earned detention.")
                    } else {
                        snark
                    }
                } else {
                    succeedWithPerson(
                        character = character,
                        person = teacher,
                        academic = false,
                        gpaDelta = -0.05f,
                        smarts = 0,
                        happiness = 2,
                        relationshipDelta = -8,
                        reputation = -3,
                        message = "You talked back to ${teacher.name} and somehow avoided detention — for now."
                    )
                }
            }
            SchoolActivity.SCHOOL_DANCE -> {
                val crush = character.education.schoolPeople.find { it.role == SchoolRole.CRUSH }
                val partner = crush ?: classmates(character).maxByOrNull { it.relationshipLevel }
                if (partner != null) {
                    succeedWithPerson(
                        character = character,
                        person = partner,
                        academic = false,
                        gpaDelta = 0f,
                        smarts = 0,
                        happiness = 8,
                        relationshipDelta = 14,
                        reputation = 3,
                        message = "You danced with ${partner.name}. The night felt endless in the best way."
                    )
                } else {
                    succeedSocial(
                        character,
                        gpaDelta = 0f,
                        happiness = 5,
                        reputation = 2,
                        message = "The school dance was loud, awkward, and somehow perfect."
                    )
                }
            }
            SchoolActivity.CLUB_PRACTICE -> {
                when (val result = performClubActivity(character)) {
                    is ClubActivityResult.Success -> SchoolActionResult.Success(result.character, result.message)
                    is ClubActivityResult.Dropped -> SchoolActionResult.Success(result.character, result.message)
                    ClubActivityResult.AlreadyDone -> SchoolActionResult.AlreadyDone
                    ClubActivityResult.Ineligible -> SchoolActionResult.Ineligible
                }
            }
        }
    }

    fun classmates(character: Character): List<SchoolPerson> =
        character.education.schoolPeople.filter {
            it.role == SchoolRole.CLASSMATE ||
                it.role == SchoolRole.BEST_CLASSMATE ||
                it.role == SchoolRole.CRUSH ||
                it.role == SchoolRole.BULLY
        }

    fun teachers(character: Character): List<SchoolPerson> =
        character.education.schoolPeople.filter { it.role == SchoolRole.TEACHER }

    fun availableSchoolPersonActions(
        character: Character,
        personId: String
    ): List<SchoolPersonAction> {
        if (!isEnrolled(character) || !character.alive) return emptyList()
        if (character.criminalRecord.currentlyIncarcerated) return emptyList()
        val person = character.education.schoolPeople.find { it.id == personId } ?: return emptyList()
        val actions = mutableListOf(
            SchoolPersonAction.CHAT,
            SchoolPersonAction.COMPLIMENT,
            SchoolPersonAction.INSULT,
            SchoolPersonAction.SPREAD_RUMOR,
            SchoolPersonAction.BRIBE_GIFT
        )
        if (canAskOut(character, person)) {
            actions += SchoolPersonAction.ASK_OUT
        }
        return actions
    }

    fun schoolGiftCost(character: Character): Int =
        EconomyScaler.scaleRelationshipCost(SCHOOL_GIFT_BASE_COST_KENYA, character.countryCode, character.age)

    /**
     * BitLife-style one-on-one action with a school NPC.
     * High bond (>80) can reveal their secret.
     */
    fun handleSchoolPersonInteraction(
        character: Character,
        personId: String,
        actionType: SchoolPersonAction
    ): SchoolInteractionResult {
        if (!isEnrolled(character) || !character.alive) return SchoolInteractionResult.Ineligible
        if (character.criminalRecord.currentlyIncarcerated) return SchoolInteractionResult.Ineligible
        val person = character.education.schoolPeople.find { it.id == personId }
            ?: return SchoolInteractionResult.PersonNotFound
        if (actionType !in availableSchoolPersonActions(character, personId)) {
            return SchoolInteractionResult.Ineligible
        }

        return when (actionType) {
            SchoolPersonAction.CHAT -> applyPersonInteraction(
                character = character,
                person = person,
                relationshipDelta = Random.nextInt(3, 7),
                happinessDelta = 1,
                reputationDelta = 0,
                karmaDelta = 0,
                message = "You caught up with ${person.name}. Easy conversation."
            )
            SchoolPersonAction.COMPLIMENT -> {
                val awkward = character.stats.looks < 35 && character.stats.smarts < 40
                if (awkward && Random.nextFloat() < 0.45f) {
                    applyPersonInteraction(
                        character = character,
                        person = person,
                        relationshipDelta = -4,
                        happinessDelta = -2,
                        reputationDelta = -1,
                        karmaDelta = 0,
                        message = "Your compliment to ${person.name} landed weird. Awkward silence."
                    )
                } else {
                    applyPersonInteraction(
                        character = character,
                        person = person,
                        relationshipDelta = Random.nextInt(6, 12),
                        happinessDelta = 3,
                        reputationDelta = 1,
                        karmaDelta = 1,
                        message = "${person.name} smiled. The compliment worked."
                    )
                }
            }
            SchoolPersonAction.INSULT -> {
                val becomesBully = person.role != SchoolRole.BULLY &&
                    person.role != SchoolRole.TEACHER &&
                    Random.nextFloat() < 0.28f
                val updatedRole = if (becomesBully) SchoolRole.BULLY else person.role
                val status = when {
                    becomesBully -> "Now targeting you"
                    person.role == SchoolRole.TEACHER -> person.status ?: "Annoyed"
                    else -> "Feuding with you"
                }
                val insulted = applyPersonInteraction(
                    character = character,
                    person = person,
                    relationshipDelta = -Random.nextInt(10, 18),
                    happinessDelta = if (Random.nextBoolean()) 2 else -3,
                    reputationDelta = -3,
                    karmaDelta = -3,
                    roleOverride = updatedRole,
                    statusOverride = status,
                    traitsOverride = if (becomesBully && "Bully" !in person.traits) {
                        (person.traits + "Bully").distinct().take(3)
                    } else {
                        null
                    },
                    message = if (becomesBully) {
                        "You insulted ${person.name}. They turn on you — school just got harder."
                    } else {
                        "You insulted ${person.name}. The hallway goes quiet."
                    }
                )
                if (person.role == SchoolRole.TEACHER &&
                    insulted is SchoolInteractionResult.Success &&
                    Random.nextFloat() < 0.65f
                ) {
                    val detained = recordDetention(
                        insulted.character,
                        reason = "Insulting teacher ${person.name}.",
                        reputationHit = 9
                    )
                    SchoolInteractionResult.Success(detained, "Talking trash to a teacher — detention.")
                } else {
                    insulted
                }
            }
            SchoolPersonAction.ASK_OUT -> {
                val chance = askOutSuccessChance(character, person)
                if (Random.nextFloat() < chance) {
                    applyPersonInteraction(
                        character = character,
                        person = person,
                        relationshipDelta = Random.nextInt(12, 20),
                        happinessDelta = 8,
                        reputationDelta = 2,
                        karmaDelta = 1,
                        roleOverride = if (person.role == SchoolRole.CLASSMATE ||
                            person.role == SchoolRole.BEST_CLASSMATE
                        ) {
                            SchoolRole.CRUSH
                        } else {
                            null
                        },
                        statusOverride = "Dating you (sort of)",
                        message = "${person.name} said yes. Your stomach does a full flip."
                    )
                } else {
                    applyPersonInteraction(
                        character = character,
                        person = person,
                        relationshipDelta = -Random.nextInt(6, 12),
                        happinessDelta = -6,
                        reputationDelta = -2,
                        karmaDelta = 0,
                        statusOverride = person.status ?: "Not interested",
                        message = "${person.name} turned you down. Everyone somehow knows."
                    )
                }
            }
            SchoolPersonAction.SPREAD_RUMOR -> {
                val caught = Random.nextFloat() < 0.32f
                if (caught) {
                    val base = character.copy(
                        stats = character.stats.copy(
                            happiness = clampStat(character.stats.happiness - 4),
                            karma = clampStat(character.stats.karma - 4)
                        ),
                        education = character.education.copy(
                            schoolPeople = character.education.schoolPeople.map { p ->
                                if (p.id == person.id) {
                                    p.copy(
                                        relationshipLevel = clampRelationshipLevel(
                                            p.relationshipLevel - 12
                                        ),
                                        status = "Knows you started a rumor",
                                        interactedThisYear = true
                                    )
                                } else {
                                    p
                                }
                            }
                        )
                    )
                    val detained = recordDetention(
                        base,
                        reason = "You spread a rumor about ${person.name} and got caught.",
                        reputationHit = 10
                    )
                    SchoolInteractionResult.Success(
                        detained,
                        "Caught spreading rumors about ${person.name}. Detention."
                    )
                } else {
                    applyPersonInteraction(
                        character = character,
                        person = person,
                        relationshipDelta = -8,
                        happinessDelta = 2,
                        reputationDelta = 1,
                        karmaDelta = -3,
                        statusOverride = listOf(
                            "Subject of hallway gossip",
                            "Rumored to be failing",
                            "Trending for the wrong reasons"
                        ).random(),
                        message = "The rumor about ${person.name} sticks. Your conscience does not."
                    )
                }
            }
            SchoolPersonAction.BRIBE_GIFT -> {
                val cost = schoolGiftCost(character)
                if (character.stats.money < cost) return SchoolInteractionResult.InsufficientFunds
                val spent = character.copy(
                    stats = character.stats.copy(money = character.stats.money - cost)
                )
                applyPersonInteraction(
                    character = spent,
                    person = person,
                    relationshipDelta = Random.nextInt(10, 18),
                    happinessDelta = 4,
                    reputationDelta = 1,
                    karmaDelta = 0,
                    message = "You gifted ${person.name} something nice (${formatMoney(cost, character.countryCode)}). Bond secured."
                )
            }
        }
    }

    private fun canAskOut(character: Character, person: SchoolPerson): Boolean {
        if (person.role == SchoolRole.TEACHER || person.role == SchoolRole.BULLY) return false
        if (person.role != SchoolRole.CRUSH &&
            person.role != SchoolRole.CLASSMATE &&
            person.role != SchoolRole.BEST_CLASSMATE
        ) {
            return false
        }
        return character.age >= ASK_OUT_MIN_AGE && person.age >= ASK_OUT_MIN_AGE
    }

    private fun askOutSuccessChance(character: Character, person: SchoolPerson): Float {
        val looksFactor = character.stats.looks / 100f
        val bondFactor = person.relationshipLevel / 100f
        val smartsBoost = if (person.traits.any { it == "Studious" || it == "Teacher's Pet" }) {
            character.stats.smarts / 200f
        } else {
            0f
        }
        val crushBonus = if (person.role == SchoolRole.CRUSH) 0.12f else 0f
        return (0.18f + looksFactor * 0.35f + bondFactor * 0.4f + smartsBoost + crushBonus)
            .coerceIn(0.08f, 0.88f)
    }

    private fun applyPersonInteraction(
        character: Character,
        person: SchoolPerson,
        relationshipDelta: Int,
        happinessDelta: Int,
        reputationDelta: Int,
        karmaDelta: Int,
        message: String,
        roleOverride: SchoolRole? = null,
        statusOverride: String? = null,
        traitsOverride: List<String>? = null
    ): SchoolInteractionResult {
        val newBond = clampRelationshipLevel(person.relationshipLevel + relationshipDelta)
        val revealSecret = !person.secretRevealed &&
            !person.secret.isNullOrBlank() &&
            newBond > SECRET_REVEAL_BOND
        val updatedPerson = person.copy(
            relationshipLevel = newBond,
            interactedThisYear = true,
            role = roleOverride ?: person.role,
            status = statusOverride ?: person.status,
            traits = traitsOverride ?: person.traits,
            secretRevealed = person.secretRevealed || revealSecret
        )
        val fullMessage = if (revealSecret) {
            "$message You learn their secret: ${person.secret}."
        } else {
            message
        }
        val updatedPeople = character.education.schoolPeople.map { p ->
            if (p.id == person.id) updatedPerson else p
        }
        val updated = character.copy(
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness + happinessDelta),
                karma = clampStat(character.stats.karma + karmaDelta)
            ),
            education = character.education.copy(
                schoolPeople = updatedPeople,
                schoolReputation = (character.education.schoolReputation + reputationDelta)
                    .coerceIn(0, 100)
            ),
            eventLog = EventLogCap.prepend(character.eventLog, fullMessage)
        )
        return SchoolInteractionResult.Success(updated, fullMessage)
    }

    private fun generateSchoolRoster(character: Character): List<SchoolPerson> {
        val country = character.countryCode
        val stage = character.education.stage
        val peerAge = when (stage) {
            SchoolStage.PRIMARY -> character.age.coerceIn(6, 13)
            SchoolStage.SECONDARY -> character.age.coerceIn(14, 18)
            SchoolStage.UNIVERSITY -> character.age.coerceIn(18, 24)
            else -> character.age
        }
        val teacherAge = when (stage) {
            SchoolStage.UNIVERSITY -> Random.nextInt(28, 55)
            else -> Random.nextInt(26, 50)
        }
        val classmates = (1..CLASSMATE_COUNT).map {
            buildSchoolPerson(
                role = SchoolRole.CLASSMATE,
                country = country,
                age = (peerAge + Random.nextInt(-1, 2)).coerceAtLeast(6),
                relationship = Random.nextInt(35, 65)
            )
        }
        val best = buildSchoolPerson(
            role = SchoolRole.BEST_CLASSMATE,
            country = country,
            age = peerAge,
            relationship = Random.nextInt(65, 85)
        )
        val bully = buildSchoolPerson(
            role = SchoolRole.BULLY,
            country = country,
            age = peerAge + Random.nextInt(0, 2),
            relationship = Random.nextInt(10, 30)
        )
        val crush = if (character.age >= 12 && stage != SchoolStage.PRIMARY) {
            listOf(
                buildSchoolPerson(
                    role = SchoolRole.CRUSH,
                    country = country,
                    age = peerAge,
                    relationship = Random.nextInt(40, 60)
                )
            )
        } else {
            emptyList()
        }
        val subjects = when (stage) {
            SchoolStage.UNIVERSITY -> listOf("Professor", "Advisor", "Lab Instructor")
            SchoolStage.SECONDARY -> listOf("Math", "Science", "Literature", "History")
            else -> listOf("Homeroom", "Math", "Reading")
        }
        val teacherList = subjects.take(TEACHER_COUNT).map { subject ->
            buildSchoolPerson(
                role = SchoolRole.TEACHER,
                country = country,
                age = teacherAge + Random.nextInt(-3, 4),
                relationship = Random.nextInt(40, 70),
                subject = subject
            )
        }
        return classmates + best + bully + crush + teacherList
    }

    private fun buildSchoolPerson(
        role: SchoolRole,
        country: String,
        age: Int,
        relationship: Int,
        subject: String? = null
    ): SchoolPerson {
        val gender = if (Random.nextBoolean()) Gender.MALE else Gender.FEMALE
        val traits = randomTraitsFor(role)
        return SchoolPerson(
            id = UUID.randomUUID().toString(),
            name = NamePool.randomFullName(gender, country),
            role = role,
            gender = gender,
            age = age,
            relationshipLevel = relationship,
            subject = subject,
            avatarConfig = AvatarConfig.random(),
            traits = traits,
            secret = randomSecretFor(role, traits),
            status = randomStatusFor(role),
            secretRevealed = false
        )
    }

    private fun enrichSchoolPersonIfNeeded(person: SchoolPerson): SchoolPerson {
        if (person.traits.isNotEmpty() || person.secret != null || person.status != null) {
            return person
        }
        val traits = randomTraitsFor(person.role)
        return person.copy(
            traits = traits,
            secret = randomSecretFor(person.role, traits),
            status = randomStatusFor(person.role)
        )
    }

    private fun randomTraitsFor(role: SchoolRole): List<String> {
        val pool = when (role) {
            SchoolRole.TEACHER -> TEACHER_TRAITS
            SchoolRole.BULLY -> listOf("Bully", "Rebellious", "Popular", "Jock")
            SchoolRole.CRUSH -> listOf("Popular", "Studious", "Jock", "Teacher's Pet", "Rebellious")
            SchoolRole.BEST_CLASSMATE -> listOf("Studious", "Popular", "Teacher's Pet", "Jock")
            SchoolRole.CLASSMATE -> STUDENT_TRAITS
        }
        val count = Random.nextInt(1, 3)
        return pool.shuffled().take(count)
    }

    private fun randomSecretFor(role: SchoolRole, traits: List<String>): String? {
        if (Random.nextFloat() < 0.18f) return null
        val pool = when (role) {
            SchoolRole.TEACHER -> listOf(
                "Plays favorites and pretends not to",
                "Grades late every weekend",
                "Once failed the subject they teach",
                "Has a soft spot for rebellious kids"
            )
            SchoolRole.BULLY -> listOf(
                "Gets bullied at home",
                "Cheating on exams",
                "Skips class behind the gym",
                "Afraid of being ignored"
            )
            SchoolRole.CRUSH -> listOf(
                "Crushing on the player",
                "Writes your name in their notebook",
                "Practices conversations before talking to you",
                "Jealous of your closest classmate"
            )
            else -> listOf(
                "Cheating on exams",
                "Skips class behind the gym",
                "Crushing on the player",
                "Hides failing grades from family",
                "Sneaks out during lunch",
                "Stole a test answer key once"
            )
        }
        val traitLinked = when {
            "Studious" in traits -> "Secretly terrified of getting a B"
            "Popular" in traits -> "Cares more about followers than friends"
            "Rebellious" in traits -> "Has a fake ID"
            else -> null
        }
        return traitLinked ?: pool.random()
    }

    private fun randomStatusFor(role: SchoolRole): String? {
        if (Random.nextFloat() < 0.25f) return null
        return when (role) {
            SchoolRole.TEACHER -> listOf(
                "Department favorite",
                "Strict on punctuality",
                "Always on duty"
            ).random()
            SchoolRole.BULLY -> listOf(
                "Hallway menace",
                "Suspended once",
                "Dating someone else"
            ).random()
            SchoolRole.CRUSH -> listOf(
                "Class crush",
                "Dating someone else",
                "Single and noticed"
            ).random()
            SchoolRole.BEST_CLASSMATE -> listOf(
                "Your locker neighbor",
                "Group project legend",
                "Always has notes"
            ).random()
            SchoolRole.CLASSMATE -> listOf(
                "Class President",
                "Dating someone else",
                "On the honor roll",
                "Club captain",
                "Quiet in back row",
                "Suspended"
            ).random()
        }
    }

    private fun succeedAcademic(
        character: Character,
        gpaDelta: Float,
        smarts: Int,
        happiness: Int,
        reputation: Int,
        message: String,
        health: Int = 0,
        examPrepDelta: Int = 6
    ): SchoolActionResult {
        val updatedExams = character.education.pendingExams.map { exam ->
            if (exam.yearsUntilDue <= 0) {
                exam.copy(preparedness = (exam.preparedness + examPrepDelta).coerceIn(0, 100))
            } else {
                exam
            }
        }
        val updated = character.copy(
            stats = character.stats.copy(
                smarts = clampStat(character.stats.smarts + smarts),
                happiness = clampStat(character.stats.happiness + happiness),
                health = clampStat(character.stats.health + health)
            ),
            education = character.education.copy(
                gpa = clampGpa(character.education.gpa + gpaDelta),
                schoolReputation = (character.education.schoolReputation + reputation)
                    .coerceIn(0, 100),
                pendingExams = updatedExams,
                examStress = (character.education.examStress - 2).coerceAtLeast(0),
                academicActionDoneThisYear = true
            ),
            eventLog = EventLogCap.prepend(character.eventLog, message)
        )
        return SchoolActionResult.Success(updated, message)
    }

    private fun succeedSocial(
        character: Character,
        gpaDelta: Float,
        happiness: Int,
        reputation: Int,
        message: String,
        smartsDelta: Int = 0,
        karmaDelta: Int = 0
    ): SchoolActionResult {
        val updated = character.copy(
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness + happiness),
                smarts = clampStat(character.stats.smarts + smartsDelta),
                karma = clampStat(character.stats.karma + karmaDelta)
            ),
            education = character.education.copy(
                gpa = clampGpa(character.education.gpa + gpaDelta),
                schoolReputation = (character.education.schoolReputation + reputation)
                    .coerceIn(0, 100),
                socialActionDoneThisYear = true
            ),
            eventLog = EventLogCap.prepend(character.eventLog, message)
        )
        return SchoolActionResult.Success(updated, message)
    }

    private fun succeedWithPerson(
        character: Character,
        person: SchoolPerson,
        academic: Boolean,
        gpaDelta: Float,
        smarts: Int,
        happiness: Int,
        relationshipDelta: Int,
        reputation: Int,
        message: String,
        healthDelta: Int = 0
    ): SchoolActionResult {
        val updatedPeople = character.education.schoolPeople.map { p ->
            if (p.id == person.id) {
                p.copy(
                    relationshipLevel = clampRelationshipLevel(
                        p.relationshipLevel + relationshipDelta
                    ),
                    interactedThisYear = true
                )
            } else {
                p
            }
        }
        val updated = character.copy(
            stats = character.stats.copy(
                smarts = clampStat(character.stats.smarts + smarts),
                happiness = clampStat(character.stats.happiness + happiness),
                health = clampStat(character.stats.health + healthDelta)
            ),
            education = character.education.copy(
                schoolPeople = updatedPeople,
                gpa = clampGpa(character.education.gpa + gpaDelta),
                schoolReputation = (character.education.schoolReputation + reputation)
                    .coerceIn(0, 100),
                academicActionDoneThisYear = character.education.academicActionDoneThisYear || academic,
                socialActionDoneThisYear = character.education.socialActionDoneThisYear || !academic
            ),
            eventLog = EventLogCap.prepend(character.eventLog, message)
        )
        return SchoolActionResult.Success(updated, message)
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
        const val EXAM_PROMPT_TAG = "exam_prompt"
        const val MIN_DRIVING_AGE = 18
        const val DRIVING_TEST_FEE_KENYA = 12_000
        private const val CHEAT_CATCH_CHANCE = 0.35f
        private const val CHEAT_DAY_CATCH_CHANCE = 0.40f
        private const val CHEAT_DAY_CATCH_CHANCE_PLANNED = 0.55f
        private const val CHEAT_EXPEL_CHANCE = 0.12f

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
        private const val CLUB_OFFICER_SKILL = 40
        private const val CLUB_CAPTAIN_SKILL = 70
        private const val CLUB_LETTER_JACKET_SKILL = 55
        private const val CLUB_RIVALRY_MIN_SKILL = 25
        private const val CLASSMATE_COUNT = 5
        private const val TEACHER_COUNT = 3

        private val SOCIAL_ACTIVITIES = setOf(
            SchoolActivity.HANG_OUT,
            SchoolActivity.CONFRONT_BULLY,
            SchoolActivity.START_FIGHT,
            SchoolActivity.SKIP_CLASS,
            SchoolActivity.PULL_PRANK,
            SchoolActivity.TALK_BACK,
            SchoolActivity.SCHOOL_DANCE
        )

        const val SCHOOL_GIFT_BASE_COST_KENYA = 800
        const val SECRET_REVEAL_BOND = 80
        private const val ASK_OUT_MIN_AGE = 13
        private const val DETENTION_HEARING_THRESHOLD = 3
        private const val DETENTION_HEARING_THRESHOLD_PROBATION = 2
        const val EXPULSION_HEARING_TAG = "expulsion_hearing"

        private val STUDENT_TRAITS = listOf(
            "Studious",
            "Bully",
            "Jock",
            "Popular",
            "Rebellious",
            "Teacher's Pet"
        )
        private val TEACHER_TRAITS = listOf(
            "Strict",
            "Inspirational",
            "Fair",
            "Demanding",
            "Softie"
        )
    }
}
