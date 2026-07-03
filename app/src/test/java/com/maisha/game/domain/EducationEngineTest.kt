package com.maisha.game.domain

import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.ExamType
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
}
