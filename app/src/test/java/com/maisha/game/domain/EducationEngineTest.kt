package com.maisha.game.domain

import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.ExamType
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EducationEngineTest {

    private val engine = EducationEngine()

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
        val relationshipEngine = RelationshipEngine()
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
    fun careerEngine_rejectsDropoutForDegreeRequiredJobs() {
        val careerEngine = CareerEngine(HealthEngine())
        val secondaryDropout = TestFixtures.character(
            age = 20,
            education = EducationState(
                stage = SchoolStage.NONE,
                droppedOutFrom = SchoolStage.SECONDARY,
                kcpePassed = true,
                kcseGrade = null
            )
        )
        assertFalse(careerEngine.isJobEligible(secondaryDropout))
        assertTrue(careerEngine.getEligibleJobs(secondaryDropout).isEmpty())

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
}
