package com.maisha.game.domain

import com.maisha.game.data.model.CareerTrack
import com.maisha.game.data.model.PrisonActivity
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.SchoolClub
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.CriminalRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Wave2EnginesTest {
    private val educationEngine = EducationEngine(RelocationEngine())
    private val careerEngine = CareerEngine(HealthEngine(), RelocationEngine())
    private val crimeEngine = CrimeEngine()

    @Test
    fun joinSchoolClub_appliesForSecondaryStudent() {
        val student = TestFixtures.character(
            age = 14,
            education = EducationState(stage = SchoolStage.SECONDARY, currentGrade = 1, gpa = 2.5f)
        )
        val updated = educationEngine.joinSchoolClub(student, SchoolClub.DEBATE)
        assertEquals(SchoolClub.DEBATE, updated.education.schoolClub)
    }

    @Test
    fun applySchoolClubYear_boostsStats() {
        val student = TestFixtures.character(
            age = 15,
            stats = Stats(smarts = 50, happiness = 50, health = 70),
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 2,
                gpa = 2.0f,
                schoolClub = SchoolClub.CODING
            )
        )
        val after = educationEngine.applySchoolClubYear(student)
        assertTrue(after.stats.smarts > student.stats.smarts)
    }

    @Test
    fun startCareerTrack_entertainment() {
        val adult = TestFixtures.character(age = 18)
        val updated = careerEngine.startCareerTrack(adult, CareerTrack.ENTERTAINMENT)
        assertEquals(CareerTrack.ENTERTAINMENT, updated.career.careerTrack)
    }

    @Test
    fun practiceCareerTrack_increasesProgress() {
        val adult = careerEngine.startCareerTrack(
            TestFixtures.character(age = 18),
            CareerTrack.ENTERTAINMENT
        )
        val result = careerEngine.practiceCareerTrack(adult)
        assertTrue(result is CareerTrackPracticeResult.Success)
        assertTrue((result as CareerTrackPracticeResult.Success).character.career.trackProgress > 0)
    }

    @Test
    fun performPrisonActivity_libraryBoostsSmarts() {
        val inmate = TestFixtures.character(
            age = 25,
            stats = Stats(smarts = 40, happiness = 30, health = 60, money = 0),
            criminalRecord = CriminalRecord(currentlyIncarcerated = true, yearsRemaining = 2)
        )
        val result = crimeEngine.performPrisonActivity(inmate, PrisonActivity.LIBRARY)
        assertTrue(result is PrisonActivityResult.Success)
        val after = (result as PrisonActivityResult.Success).character
        assertTrue(after.stats.smarts > inmate.stats.smarts)
    }

    @Test
    fun lifeArchetype_scholarForGraduate() {
        val grad = TestFixtures.character(
            age = 45,
            education = EducationState(stage = SchoolStage.GRADUATED, gpa = 3.8f)
        )
        assertEquals("archetype_scholar", LifeArchetypeEngine.resolveTitleKey(grad, 100_000))
    }

    @Test
    fun lifeArchetype_patriarchWithChildren() {
        val parent = TestFixtures.character(
            age = 55,
            family = listOf(
                TestFixtures.person(relation = RelationType.CHILD, age = 25),
                TestFixtures.person(relation = RelationType.CHILD, age = 22)
            )
        )
        assertEquals("archetype_patriarch", LifeArchetypeEngine.resolveTitleKey(parent, 50_000))
    }
}
