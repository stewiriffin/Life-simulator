package com.maisha.game.domain

import com.maisha.game.domain.ClubActivityResult
import com.maisha.game.domain.ExamPrepResult
import com.maisha.game.domain.SchoolActionResult
import com.maisha.game.domain.SchoolDisciplineResult
import com.maisha.game.domain.SchoolInteractionResult
import com.maisha.game.data.model.ClubRank
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.ExamType
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.SchoolActivity
import com.maisha.game.data.model.SchoolClub
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import com.maisha.game.data.model.VisaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EducationEngineTest {

    private val engine = EducationEngine(RelocationEngine())

    @Test
    fun enrollIfEligible_enrollsPrimaryAtAgeSix() {
        val character = TestFixtures.character(
            age = 6,
            education = EducationState(stage = SchoolStage.NONE)
        )
        val enrolled = engine.enrollIfEligible(character)
        assertEquals(SchoolStage.PRIMARY, enrolled.education.stage)
        assertEquals(1, enrolled.education.currentGrade)
    }

    @Test
    fun enrollIfEligible_enrollsSecondaryAtFourteenWhenKcpePassed() {
        val character = TestFixtures.character(
            age = 14,
            education = EducationState(
                stage = SchoolStage.PRIMARY,
                currentGrade = 8,
                kcpePassed = true
            )
        )
        val enrolled = engine.enrollIfEligible(character)
        assertEquals(SchoolStage.SECONDARY, enrolled.education.stage)
    }

    @Test
    fun enrollIfEligible_doesNotEnrollSecondaryWithoutKcpePass() {
        val character = TestFixtures.character(
            age = 14,
            education = EducationState(
                stage = SchoolStage.PRIMARY,
                currentGrade = 8,
                kcpePassed = false
            )
        )
        val enrolled = engine.enrollIfEligible(character)
        assertEquals(SchoolStage.PRIMARY, enrolled.education.stage)
    }

    @Test
    fun takeExam_highStatsPassMoreOftenThanLowStats() {
        val highStatChar = TestFixtures.character(
            stats = Stats(smarts = 95),
            education = EducationState(stage = SchoolStage.PRIMARY, gpa = 3.9f)
        )
        val lowStatChar = TestFixtures.character(
            stats = Stats(smarts = 20),
            education = EducationState(stage = SchoolStage.PRIMARY, gpa = 1.0f)
        )
        var highPasses = 0
        var lowPasses = 0
        repeat(300) {
            if (engine.takeExam(highStatChar, ExamType.KCPE).second.passed) highPasses++
            if (engine.takeExam(lowStatChar, ExamType.KCPE).second.passed) lowPasses++
        }
        assertTrue("High-stat pass rate should exceed low-stat", highPasses > lowPasses)
    }

    @Test
    fun applyToUniversity_rejectsBelowMinimumGrade() {
        val character = TestFixtures.character(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                kcseGrade = "D"
            )
        )
        assertFalse(engine.isEligibleForUniversity(character))
        val after = engine.applyToUniversity(character, "Law")
        assertEquals(SchoolStage.SECONDARY, after.education.stage)
    }

    @Test
    fun applyToUniversity_acceptsStrongGrade() {
        val character = TestFixtures.character(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                kcseGrade = "B+"
            )
        )
        assertTrue(engine.isEligibleForUniversity(character))
        val after = engine.applyToUniversity(character, "Law")
        assertEquals(SchoolStage.UNIVERSITY, after.education.stage)
        assertEquals("Law", after.education.courseOfStudy)
    }

    @Test
    fun shouldTriggerPrimaryExam_trueAtPrimaryExitBeforePass() {
        val character = TestFixtures.character(
            age = 13,
            education = EducationState(
                stage = SchoolStage.PRIMARY,
                currentGrade = 8,
                kcpePassed = false
            )
        )
        assertTrue(engine.shouldTriggerPrimaryExam(character))
    }

    @Test
    fun shouldTriggerPrimaryExam_falseAfterAlreadyPassed() {
        val character = TestFixtures.character(
            age = 14,
            education = EducationState(
                stage = SchoolStage.PRIMARY,
                currentGrade = 8,
                kcpePassed = true
            )
        )
        assertFalse(engine.shouldTriggerPrimaryExam(character))
    }

    @Test
    fun shouldTriggerPrimaryExam_trueForMultipleCountriesAtExitAge() {
        listOf("KE", "NG", "US").forEach { countryCode ->
            val character = TestFixtures.character(
                age = 13,
                countryCode = countryCode,
                education = EducationState(
                    stage = SchoolStage.PRIMARY,
                    currentGrade = 8,
                    kcpePassed = false
                )
            )
            assertTrue(
                "Primary exam should trigger for $countryCode at exit grade/age",
                engine.shouldTriggerPrimaryExam(character)
            )
        }
    }

    @Test
    fun shouldTriggerSecondaryExam_trueAtSecondaryExitBeforeGradeRecorded() {
        val character = TestFixtures.character(
            age = 18,
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 4,
                kcseGrade = null
            )
        )
        assertTrue(engine.shouldTriggerSecondaryExam(character))
    }

    @Test
    fun shouldTriggerSecondaryExam_falseAfterGradeRecorded() {
        val character = TestFixtures.character(
            age = 18,
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 4,
                kcseGrade = "C+"
            )
        )
        assertFalse(engine.shouldTriggerSecondaryExam(character))
    }

    @Test
    fun shouldTriggerSecondaryExam_trueForMultipleCountriesAtExitAge() {
        listOf("KE", "NG", "GB").forEach { countryCode ->
            val character = TestFixtures.character(
                age = 17,
                countryCode = countryCode,
                education = EducationState(
                    stage = SchoolStage.SECONDARY,
                    currentGrade = 4,
                    kcseGrade = null
                )
            )
            assertTrue(
                "Secondary exam should trigger for $countryCode at exit grade/age",
                engine.shouldTriggerSecondaryExam(character)
            )
        }
    }

    @Test
    fun processDropout_capsEducationLevelAndPreventsProgression() {
        val inSecondary = TestFixtures.character(
            age = 16,
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 2,
                gpa = 2.5f,
                schoolName = "Test High",
                kcpePassed = true
            )
        )
        val afterDropout = engine.processDropout(inSecondary)
        assertEquals(SchoolStage.NONE, afterDropout.education.stage)
        assertEquals(SchoolStage.SECONDARY, afterDropout.education.droppedOutFrom)
        assertEquals(true, afterDropout.education.kcpePassed)

        val advanced = engine.advanceGrade(afterDropout, com.maisha.game.data.model.StudyEffort.NORMAL)
        assertEquals(SchoolStage.NONE, advanced.education.stage)

        val reEnrollAttempt = engine.enrollIfEligible(
            afterDropout.copy(
                age = 15,
                education = afterDropout.education.copy(
                    stage = SchoolStage.PRIMARY,
                    currentGrade = 8,
                    kcpePassed = true
                )
            )
        )
        assertEquals(SchoolStage.PRIMARY, reEnrollAttempt.education.stage)
        assertEquals(SchoolStage.SECONDARY, reEnrollAttempt.education.droppedOutFrom)
    }

    @Test
    fun processExpulsion_updatesStateAndTriggersFamilyPenalty() {
        val relationshipEngine = RelationshipEngine(FinanceEngine())
        val mother = TestFixtures.person(
            id = "mom",
            relation = RelationType.MOTHER,
            relationshipLevel = 80
        )
        val father = TestFixtures.person(
            id = "dad",
            relation = RelationType.FATHER,
            relationshipLevel = 75
        )
        val student = TestFixtures.character(
            age = 15,
            family = listOf(mother, father),
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 2,
                schoolName = "Test High"
            )
        )

        val expelled = engine.processExpulsion(student)
        assertTrue(expelled.education.expelled)
        assertEquals(SchoolStage.NONE, expelled.education.stage)

        val withFamilyPenalty = relationshipEngine.applyExpulsionFamilyEffect(expelled)
        assertEquals(50, withFamilyPenalty.family.first { it.relation == RelationType.MOTHER }.relationshipLevel)
        assertEquals(45, withFamilyPenalty.family.first { it.relation == RelationType.FATHER }.relationshipLevel)
    }

    @Test
    fun applyToUniversity_grantsStudentVisaForForeignAdmissions() {
        val character = TestFixtures.character(
            age = 18,
            countryCode = "KE",
            stats = Stats(health = 80, happiness = 70, smarts = 80, looks = 50, money = 500_000),
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 4,
                kcseGrade = "A",
                kcpePassed = true
            )
        )
        val enrolled = engine.applyToUniversity(
            character,
            course = "Computer Science",
            universityCountryCode = "US"
        )

        assertEquals("US", enrolled.countryCode)
        assertEquals(VisaType.STUDENT, enrolled.currentVisa)
        assertTrue(enrolled.visaYearsRemaining > 0)
        assertEquals(SchoolStage.UNIVERSITY, enrolled.education.stage)
        assertEquals("Computer Science", enrolled.education.courseOfStudy)
        assertTrue(enrolled.stats.money < character.stats.money)
    }

    @Test
    fun careerEngine_rejectsDropoutForDegreeRequiredJobs() {
        val careerEngine = CareerEngine(HealthEngine(), RelocationEngine())
        val secondaryDropout = TestFixtures.character(
            age = 20,
            education = EducationState(
                stage = SchoolStage.NONE,
                droppedOutFrom = SchoolStage.SECONDARY,
                kcpePassed = true,
                kcseGrade = null
            )
        )
        // Incomplete secondary dropouts fail the general education gate…
        assertFalse(careerEngine.isJobEligible(secondaryDropout))
        val secondaryJobs = careerEngine.getEligibleJobs(secondaryDropout)
        // …but military roles intentionally bypass education (age + not expelled only).
        assertTrue(secondaryJobs.isNotEmpty())
        assertTrue(secondaryJobs.all { it.isMilitary })
        assertTrue(secondaryJobs.none { it.minEducation == SchoolStage.GRADUATED })

        val universityDropout = TestFixtures.character(
            age = 22,
            education = EducationState(
                stage = SchoolStage.NONE,
                droppedOutFrom = SchoolStage.UNIVERSITY,
                kcseGrade = "B",
                kcpePassed = true
            )
        )
        assertTrue(careerEngine.isJobEligible(universityDropout))
        val graduatedJobs = careerEngine.getEligibleJobs(universityDropout)
            .filter { it.minEducation == SchoolStage.GRADUATED }
        assertTrue(graduatedJobs.isEmpty())
    }

    @Test
    fun setPlannedStudyEffort_persistsOnEducationState() {
        val student = TestFixtures.character(
            age = 12,
            education = EducationState(stage = SchoolStage.PRIMARY, currentGrade = 5, gpa = 2.0f)
        )
        val updated = engine.setPlannedStudyEffort(student, com.maisha.game.data.model.StudyEffort.HARD)
        assertEquals(com.maisha.game.data.model.StudyEffort.HARD, updated.education.plannedStudyEffort)
    }

    @Test
    fun enrollIfEligible_createsSchoolRosterWithClassmatesAndTeachers() {
        val character = TestFixtures.character(
            age = 6,
            education = EducationState(stage = SchoolStage.NONE)
        )
        val enrolled = engine.enrollIfEligible(character)
        assertTrue(enrolled.education.schoolPeople.isNotEmpty())
        assertTrue(engine.classmates(enrolled).isNotEmpty())
        assertTrue(engine.teachers(enrolled).isNotEmpty())
    }

    @Test
    fun performSchoolActivity_libraryStudyRaisesGpaAndBlocksSecondAcademic() {
        val character = engine.ensureSchoolRoster(
            TestFixtures.character(
                age = 12,
                education = EducationState(
                    stage = SchoolStage.PRIMARY,
                    currentGrade = 6,
                    gpa = 2.5f,
                    schoolReputation = 50
                )
            )
        )
        val first = engine.performSchoolActivity(
            character,
            com.maisha.game.data.model.SchoolActivity.LIBRARY_STUDY
        )
        assertTrue(first is SchoolActionResult.Success)
        val after = (first as SchoolActionResult.Success).character
        assertTrue(after.education.gpa > character.education.gpa)
        assertTrue(after.education.academicActionDoneThisYear)

        val second = engine.performSchoolActivity(
            after,
            com.maisha.game.data.model.SchoolActivity.STUDY_GROUP
        )
        assertEquals(SchoolActionResult.AlreadyDone, second)
    }

    @Test
    fun performSchoolActivity_hangOutUsesSocialSlot() {
        val character = engine.ensureSchoolRoster(
            TestFixtures.character(
                age = 15,
                education = EducationState(
                    stage = SchoolStage.SECONDARY,
                    currentGrade = 2,
                    gpa = 2.8f,
                    kcpePassed = true
                )
            )
        )
        val result = engine.performSchoolActivity(
            character,
            com.maisha.game.data.model.SchoolActivity.HANG_OUT
        )
        assertTrue(result is SchoolActionResult.Success)
        val after = (result as SchoolActionResult.Success).character
        assertTrue(after.education.socialActionDoneThisYear)
        assertFalse(after.education.academicActionDoneThisYear)
    }

    @Test
    fun ensureSchoolRoster_assignsTraitsAndSecrets() {
        val enrolled = engine.enrollIfEligible(
            TestFixtures.character(age = 6, education = EducationState(stage = SchoolStage.NONE))
        )
        assertTrue(enrolled.education.schoolPeople.isNotEmpty())
        assertTrue(enrolled.education.schoolPeople.any { it.traits.isNotEmpty() })
        assertTrue(enrolled.education.schoolPeople.any { !it.secret.isNullOrBlank() || it.status != null })
    }

    @Test
    fun handleSchoolPersonInteraction_chatRaisesBond() {
        val character = engine.ensureSchoolRoster(
            TestFixtures.character(
                age = 14,
                education = EducationState(
                    stage = SchoolStage.SECONDARY,
                    currentGrade = 1,
                    kcpePassed = true,
                    gpa = 2.5f
                )
            )
        )
        val peer = engine.classmates(character).first()
        val before = peer.relationshipLevel
        val result = engine.handleSchoolPersonInteraction(
            character,
            peer.id,
            com.maisha.game.data.model.SchoolPersonAction.CHAT
        )
        assertTrue(result is SchoolInteractionResult.Success)
        val afterPerson = (result as SchoolInteractionResult.Success).character.education.schoolPeople
            .first { it.id == peer.id }
        assertTrue(afterPerson.relationshipLevel > before)
    }

    @Test
    fun handleSchoolPersonInteraction_giftRequiresMoney() {
        val character = engine.ensureSchoolRoster(
            TestFixtures.character(
                age = 15,
                stats = com.maisha.game.data.model.Stats(money = 0),
                education = EducationState(
                    stage = SchoolStage.SECONDARY,
                    currentGrade = 2,
                    kcpePassed = true
                )
            )
        )
        val peer = engine.classmates(character).first()
        val result = engine.handleSchoolPersonInteraction(
            character,
            peer.id,
            com.maisha.game.data.model.SchoolPersonAction.BRIBE_GIFT
        )
        assertEquals(SchoolInteractionResult.InsufficientFunds, result)
    }

    @Test
    fun refreshExamSchedule_addsImminentExamsWhileEnrolled() {
        val character = engine.refreshExamSchedule(
            TestFixtures.character(
                age = 12,
                education = EducationState(
                    stage = SchoolStage.PRIMARY,
                    currentGrade = 5,
                    gpa = 2.5f
                )
            )
        )
        assertTrue(character.education.pendingExams.isNotEmpty())
        assertTrue(character.education.pendingExams.any { it.yearsUntilDue <= 0 })
    }

    @Test
    fun performExamPrepAction_studyHardRaisesPreparedness() {
        val base = engine.refreshExamSchedule(
            TestFixtures.character(
                age = 12,
                education = EducationState(
                    stage = SchoolStage.PRIMARY,
                    currentGrade = 6,
                    gpa = 2.4f,
                    examStress = 40
                )
            )
        )
        val before = base.education.pendingExams.first { it.yearsUntilDue <= 0 }.preparedness
        val result = engine.performExamPrepAction(
            base,
            com.maisha.game.data.model.ExamPrepChoice.STUDY_HARD
        )
        assertTrue(result is ExamPrepResult.Success)
        val after = (result as ExamPrepResult.Success).character
        val prep = after.education.pendingExams.first { it.yearsUntilDue <= 0 }.preparedness
        assertTrue(prep > before)
        assertTrue(after.education.examPrepDoneThisYear)
        assertTrue(after.education.examStress < base.education.examStress)
    }

    @Test
    fun calculateExamPassChance_hardStudyBeatsSlack() {
        val hard = engine.refreshExamSchedule(
            TestFixtures.character(
                age = 13,
                stats = Stats(smarts = 70),
                education = EducationState(
                    stage = SchoolStage.PRIMARY,
                    currentGrade = 8,
                    gpa = 3.2f,
                    schoolReputation = 70,
                    plannedStudyEffort = com.maisha.game.data.model.StudyEffort.HARD,
                    examStress = 20
                )
            )
        )
        val slack = hard.copy(
            education = hard.education.copy(
                plannedStudyEffort = com.maisha.game.data.model.StudyEffort.SLACK,
                examStress = 80
            )
        )
        assertTrue(engine.calculateExamPassChance(hard) > engine.calculateExamPassChance(slack))
    }

    @Test
    fun clubRankTitle_mapsFootballAndAcademicTitles() {
        assertEquals("Starter", engine.clubRankTitle(SchoolClub.FOOTBALL, ClubRank.OFFICER))
        assertEquals("Team Captain", engine.clubRankTitle(SchoolClub.FOOTBALL, ClubRank.CAPTAIN))
        assertEquals("Treasurer", engine.clubRankTitle(SchoolClub.DEBATE, ClubRank.OFFICER))
        assertEquals("President", engine.clubRankTitle(SchoolClub.CODING, ClubRank.CAPTAIN))
    }

    @Test
    fun joinSchoolClub_startsAsMemberWithSkill() {
        val student = secondaryStudent()
        val joined = engine.joinSchoolClub(student, SchoolClub.MUSIC)
        assertEquals(SchoolClub.MUSIC, joined.education.schoolClub)
        assertEquals(ClubRank.MEMBER, joined.education.clubRank)
        assertTrue(joined.education.clubSkill in 8..17)
        assertTrue(joined.education.clubPrestige in 5..14)
    }

    @Test
    fun performClubActivity_raisesSkillAndCanPromoteToOfficer() {
        var character = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 1,
                kcpePassed = true,
                gpa = 2.8f,
                schoolClub = SchoolClub.DEBATE,
                clubRank = ClubRank.MEMBER,
                clubSkill = 35,
                clubPrestige = 20,
                clubActivityDoneThisYear = false
            ),
            stats = Stats(happiness = 50, health = 70, smarts = 70)
        )
        var reachedOfficer = false
        repeat(10) {
            character = character.copy(
                education = character.education.copy(clubActivityDoneThisYear = false)
            )
            when (val result = engine.performClubActivity(character)) {
                is ClubActivityResult.Success -> {
                    character = result.character
                    if (character.education.clubRank.ordinal >= ClubRank.OFFICER.ordinal) {
                        reachedOfficer = true
                    }
                }
                is ClubActivityResult.Dropped -> return@repeat
                else -> return@repeat
            }
        }
        assertTrue(character.education.clubSkill > 35 || reachedOfficer)
        assertTrue(
            "Expected Treasurer promotion or skill past officer threshold",
            reachedOfficer || character.education.clubSkill >= 40
        )
    }

    @Test
    fun performClubActivity_promotesCaptainAndUnlocksMajorEvent() {
        val captainTrack = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 2,
                kcpePassed = true,
                gpa = 3.0f,
                schoolClub = SchoolClub.CODING,
                clubRank = ClubRank.OFFICER,
                clubSkill = 68,
                clubPrestige = 50,
                clubActivityDoneThisYear = false
            ),
            stats = Stats(happiness = 60, health = 70, smarts = 80)
        )
        var unlocked = false
        repeat(12) {
            val base = captainTrack.copy(
                education = captainTrack.education.copy(
                    clubSkill = (68 + it * 2).coerceAtMost(95),
                    clubPrestige = 50,
                    clubRank = ClubRank.OFFICER,
                    clubActivityDoneThisYear = false,
                    clubMajorEventReady = false
                )
            )
            when (val result = engine.performClubActivity(base)) {
                is ClubActivityResult.Success -> {
                    if (result.character.education.clubRank == ClubRank.CAPTAIN) {
                        assertTrue(result.character.education.clubMajorEventReady)
                        unlocked = true
                    }
                }
                else -> Unit
            }
        }
        assertTrue("Officer with high skill/prestige should reach President", unlocked)
    }

    @Test
    fun claimClubMajorEvent_awardsCashWhenCaptain() {
        val captain = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 3,
                kcpePassed = true,
                gpa = 3.2f,
                schoolClub = SchoolClub.FOOTBALL,
                clubRank = ClubRank.CAPTAIN,
                clubSkill = 85,
                clubPrestige = 70,
                clubMajorEventReady = true
            ),
            stats = Stats(money = 1_000, happiness = 50, health = 80)
        )
        val result = engine.claimClubMajorEvent(captain)
        assertTrue(result is ClubActivityResult.Success)
        val after = (result as ClubActivityResult.Success).character
        assertTrue(after.stats.money > captain.stats.money)
        assertFalse(after.education.clubMajorEventReady)
    }

    @Test
    fun leaveSchoolClub_firedLowersHappinessAndReputation() {
        val member = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 1,
                kcpePassed = true,
                gpa = 2.5f,
                schoolClub = SchoolClub.DRAMA,
                clubRank = ClubRank.MEMBER,
                clubSkill = 20,
                clubPrestige = 10,
                schoolReputation = 60
            ),
            stats = Stats(happiness = 55)
        )
        val fired = engine.leaveSchoolClub(member, fired = true)
        assertNull(fired.education.schoolClub)
        assertTrue(fired.stats.happiness < member.stats.happiness)
        assertTrue(fired.education.schoolReputation < member.education.schoolReputation)
        assertEquals(0, fired.education.clubSkill)
    }

    @Test
    fun performClubActivity_alreadyDoneReturnsAlreadyDone() {
        val student = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 1,
                kcpePassed = true,
                gpa = 2.8f,
                schoolClub = SchoolClub.MUSIC,
                clubSkill = 40,
                clubActivityDoneThisYear = true
            )
        )
        assertEquals(ClubActivityResult.AlreadyDone, engine.performClubActivity(student))
    }

    @Test
    fun performClubActivity_intenseGivesMoreSkillThanLight() {
        val base = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 1,
                kcpePassed = true,
                gpa = 2.8f,
                schoolClub = SchoolClub.MUSIC,
                clubSkill = 20,
                clubPrestige = 20,
                clubActivityDoneThisYear = false
            ),
            stats = Stats(happiness = 50, health = 80, smarts = 60)
        )
        var lightGain = 0
        var intenseGain = 0
        repeat(40) {
            when (
                val light = engine.performClubActivity(
                    base,
                    com.maisha.game.data.model.ClubPracticeIntensity.LIGHT
                )
            ) {
                is ClubActivityResult.Success ->
                    lightGain += light.character.education.clubSkill - base.education.clubSkill
                else -> Unit
            }
            when (
                val intense = engine.performClubActivity(
                    base,
                    com.maisha.game.data.model.ClubPracticeIntensity.INTENSE
                )
            ) {
                is ClubActivityResult.Success ->
                    intenseGain += intense.character.education.clubSkill - base.education.clubSkill
                else -> Unit
            }
        }
        assertTrue("Intense practice should outpace light on average", intenseGain > lightGain)
    }

    @Test
    fun hostClubFundraiser_officerRaisesCash() {
        val officer = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 2,
                kcpePassed = true,
                gpa = 3.0f,
                schoolClub = SchoolClub.DEBATE,
                clubRank = ClubRank.OFFICER,
                clubSkill = 50,
                clubPrestige = 40
            ),
            stats = Stats(money = 500, happiness = 50)
        )
        val result = engine.hostClubFundraiser(officer)
        assertTrue(result is ClubActivityResult.Success)
        val after = (result as ClubActivityResult.Success).character
        assertTrue(after.stats.money > officer.stats.money)
        assertTrue(after.education.clubFundraiserDoneThisYear)
        assertEquals(ClubActivityResult.AlreadyDone, engine.hostClubFundraiser(after))
    }

    @Test
    fun claimClubMajorEvent_incrementsAwardsAndScholarshipEstimate() {
        val captain = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 3,
                kcpePassed = true,
                gpa = 3.2f,
                schoolClub = SchoolClub.CODING,
                clubRank = ClubRank.CAPTAIN,
                clubSkill = 90,
                clubPrestige = 80,
                clubMajorEventReady = true,
                clubYearsAsCaptain = 1
            ),
            stats = Stats(money = 1_000, happiness = 50, health = 70, smarts = 80)
        )
        var awards = 0
        repeat(20) {
            val attempt = captain.copy(
                education = captain.education.copy(clubMajorEventReady = true, clubAwardsWon = 0)
            )
            when (val result = engine.claimClubMajorEvent(attempt)) {
                is ClubActivityResult.Success -> {
                    if (result.character.education.clubAwardsWon > 0) {
                        awards = result.character.education.clubAwardsWon
                        assertTrue(engine.clubScholarshipEstimate(result.character) > 0)
                    }
                }
                else -> Unit
            }
        }
        assertTrue("High-skill captain should win awards sometimes", awards > 0)
    }

    @Test
    fun leaveSchoolClub_preservesAwardsOnResume() {
        val member = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 1,
                kcpePassed = true,
                gpa = 2.5f,
                schoolClub = SchoolClub.FOOTBALL,
                clubAwardsWon = 2,
                clubYearsAsCaptain = 1,
                schoolReputation = 60
            ),
            stats = Stats(happiness = 55, health = 70)
        )
        val left = engine.leaveSchoolClub(member, fired = false)
        assertNull(left.education.schoolClub)
        assertEquals(2, left.education.clubAwardsWon)
        assertEquals(SchoolClub.FOOTBALL, left.education.clubResumeClub)
        assertTrue(engine.clubScholarshipEstimate(left) > 0)
    }

    @Test
    fun challengeRivalSchool_requiresSkillAndMarksDone() {
        val weak = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 1,
                kcpePassed = true,
                gpa = 2.8f,
                schoolClub = SchoolClub.FOOTBALL,
                clubSkill = 10
            ),
            stats = Stats(happiness = 50, health = 70)
        )
        assertEquals(ClubActivityResult.Ineligible, engine.challengeRivalSchool(weak))

        val ready = weak.copy(
            education = weak.education.copy(clubSkill = 40, clubPrestige = 30, clubFame = 20)
        )
        val result = engine.challengeRivalSchool(ready)
        assertTrue(result is ClubActivityResult.Success)
        val after = (result as ClubActivityResult.Success).character
        assertTrue(after.education.clubRivalryDoneThisYear)
        assertEquals(ClubActivityResult.AlreadyDone, engine.challengeRivalSchool(after))
    }

    @Test
    fun performClubActivity_canEarnLetterJacketAtOfficerSkill() {
        val officer = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 2,
                kcpePassed = true,
                gpa = 3.0f,
                schoolClub = SchoolClub.MUSIC,
                clubRank = ClubRank.OFFICER,
                clubSkill = 52,
                clubPrestige = 40,
                clubLetterJacket = false,
                clubActivityDoneThisYear = false
            ),
            stats = Stats(happiness = 50, health = 70, looks = 40, smarts = 60)
        )
        var earned = false
        repeat(8) {
            val base = officer.copy(
                education = officer.education.copy(
                    clubSkill = 52 + it,
                    clubActivityDoneThisYear = false,
                    clubLetterJacket = false
                )
            )
            when (val result = engine.performClubActivity(base)) {
                is ClubActivityResult.Success -> {
                    if (result.character.education.clubLetterJacket) earned = true
                }
                else -> Unit
            }
        }
        assertTrue("Officer near letter threshold should earn jacket", earned)
    }

    @Test
    fun recordDetention_queuesHearingAtThreshold() {
        var student = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 1,
                kcpePassed = true,
                gpa = 2.5f,
                schoolReputation = 50
            ),
            stats = Stats(happiness = 60)
        )
        repeat(3) {
            student = engine.recordDetention(student, reason = "Test offense ${it + 1}.")
        }
        assertEquals(3, student.education.detentionCountThisYear)
        assertTrue(student.education.pendingExpulsionHearing)
        assertTrue(student.education.schoolReputation < 50)
    }

    @Test
    fun resolveExpulsionHearing_mercySetsProbation() {
        val hearing = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 2,
                kcpePassed = true,
                gpa = 2.8f,
                detentionCountThisYear = 3,
                pendingExpulsionHearing = true,
                schoolReputation = 40
            ),
            stats = Stats(happiness = 50)
        )
        val after = engine.resolveExpulsionHearing(
            hearing,
            com.maisha.game.data.model.ExpulsionHearingChoice.MERCY
        )
        assertTrue(after.education.onProbation)
        assertFalse(after.education.pendingExpulsionHearing)
        assertTrue(after.education.gpa < hearing.education.gpa)
        assertFalse(after.education.expelled)
    }

    @Test
    fun resolveExpulsionHearing_defiantExpels() {
        val hearing = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 2,
                kcpePassed = true,
                gpa = 2.8f,
                detentionCountThisYear = 3,
                pendingExpulsionHearing = true
            )
        )
        val after = engine.resolveExpulsionHearing(
            hearing,
            com.maisha.game.data.model.ExpulsionHearingChoice.DEFIANT
        )
        assertTrue(after.education.expelled)
        assertFalse(after.education.pendingExpulsionHearing)
    }

    @Test
    fun skipClass_whenCaughtIncreasesDetentionCount() {
        var caught = false
        repeat(40) {
            val student = secondaryStudent().copy(
                education = EducationState(
                    stage = SchoolStage.SECONDARY,
                    currentGrade = 1,
                    kcpePassed = true,
                    gpa = 2.5f,
                    schoolReputation = 55
                ),
                stats = Stats(happiness = 50, smarts = 50, health = 70)
            )
            when (val result = engine.performSchoolActivity(student, SchoolActivity.SKIP_CLASS)) {
                is SchoolActionResult.Success -> {
                    if (result.character.education.detentionCountThisYear > 0) {
                        caught = true
                        assertTrue(result.character.education.socialActionDoneThisYear)
                    }
                }
                else -> Unit
            }
        }
        assertTrue("Skip class should sometimes earn detention", caught)
    }

    @Test
    fun pullPrank_isAvailableSocialActivity() {
        val student = secondaryStudent()
        assertTrue(engine.availableSchoolActivities(student).contains(SchoolActivity.PULL_PRANK))
        assertTrue(engine.availableSchoolActivities(student).contains(SchoolActivity.TALK_BACK))
    }

    @Test
    fun serveDetention_clearsOneStrike() {
        val student = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 1,
                kcpePassed = true,
                gpa = 2.5f,
                detentionCountThisYear = 2,
                schoolReputation = 40
            ),
            stats = Stats(happiness = 50, smarts = 50)
        )
        val result = engine.serveDetention(student)
        assertTrue(result is SchoolDisciplineResult.Success)
        val after = (result as SchoolDisciplineResult.Success).character
        assertEquals(1, after.education.detentionCountThisYear)
        assertTrue(after.education.detentionServedThisYear)
        assertEquals(SchoolDisciplineResult.AlreadyDone, engine.serveDetention(after))
    }

    @Test
    fun resolveExpulsionHearing_transferKeepsEnrolledOnProbation() {
        val hearing = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 2,
                kcpePassed = true,
                gpa = 2.8f,
                detentionCountThisYear = 3,
                pendingExpulsionHearing = true,
                schoolName = "Old School"
            )
        )
        val after = engine.resolveExpulsionHearing(
            hearing,
            com.maisha.game.data.model.ExpulsionHearingChoice.TRANSFER
        )
        assertFalse(after.education.expelled)
        assertTrue(after.education.onProbation)
        assertFalse(after.education.pendingExpulsionHearing)
        assertEquals(SchoolStage.SECONDARY, after.education.stage)
        assertTrue(after.education.schoolName != "Old School")
    }

    @Test
    fun apologizeToPrincipal_requiresFunds() {
        val student = secondaryStudent().copy(
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 1,
                kcpePassed = true,
                gpa = 2.5f,
                detentionCountThisYear = 1
            ),
            stats = Stats(money = 0, happiness = 50)
        )
        assertEquals(SchoolDisciplineResult.InsufficientFunds, engine.apologizeToPrincipal(student))
    }

    private fun secondaryStudent() = TestFixtures.character(
        age = 15,
        education = EducationState(
            stage = SchoolStage.SECONDARY,
            currentGrade = 1,
            kcpePassed = true,
            gpa = 2.8f
        ),
        stats = Stats(happiness = 50, health = 70, smarts = 60)
    )
}
