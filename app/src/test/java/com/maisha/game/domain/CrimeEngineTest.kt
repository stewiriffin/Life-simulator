package com.maisha.game.domain

import com.maisha.game.data.model.CrimeType
import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertTrue
import org.junit.Test

class CrimeEngineTest {

    private val engine = CrimeEngine()
    private val careerEngine = CareerEngine()

    @Test
    fun crimeTypes_produceDifferentSentenceLengthsWhenCaught() {
        val pickpocketSentences = mutableListOf<Int>()
        val fraudSentences = mutableListOf<Int>()
        repeat(200) {
            val base = TestFixtures.character(age = 20)
            val pickCaught = engine.processArrest(base, CrimeType.PICKPOCKET)
            val fraudCaught = engine.processArrest(base, CrimeType.FRAUD)
            pickpocketSentences += pickCaught.criminalRecord.yearsRemaining
            fraudSentences += fraudCaught.criminalRecord.yearsRemaining
        }
        val pickAvg = pickpocketSentences.average()
        val fraudAvg = fraudSentences.average()
        assertTrue("Fraud sentences should average longer than pickpocket", fraudAvg > pickAvg)
    }

    @Test
    fun cleanStreak_reducesHirePenaltyAfterFiveYears() {
        val fresh = TestFixtures.character(
            age = 25,
            stats = Stats(smarts = 65),
            education = EducationState(stage = SchoolStage.GRADUATED, gpa = 3.0f),
            criminalRecord = CriminalRecord(hasRecord = true, timesArrested = 1, lastArrestAge = 20)
        )
        val beforeThreshold = fresh.copy(age = 24)
        val afterThreshold = fresh.copy(age = 26)
        var hiresBefore = 0
        var hiresAfter = 0
        repeat(400) {
            if (careerEngine.applyForJob(beforeThreshold, "teacher").second is CareerResult.Hired) hiresBefore++
            if (careerEngine.applyForJob(afterThreshold, "teacher").second is CareerResult.Hired) hiresAfter++
        }
        assertTrue("Clean streak should improve hiring after 5+ years", hiresAfter > hiresBefore)
    }

    @Test
    fun cleanStreak_doesNotReducePenaltyBeforeFiveYears() {
        val recent = TestFixtures.character(
            age = 22,
            stats = Stats(smarts = 65),
            education = EducationState(stage = SchoolStage.GRADUATED, gpa = 3.0f),
            criminalRecord = CriminalRecord(hasRecord = true, timesArrested = 1, lastArrestAge = 20)
        )
        val slightlyLater = recent.copy(age = 23)
        var recentHires = 0
        var laterHires = 0
        repeat(400) {
            if (careerEngine.applyForJob(recent, "teacher").second is CareerResult.Hired) recentHires++
            if (careerEngine.applyForJob(slightlyLater, "teacher").second is CareerResult.Hired) laterHires++
        }
        assertTrue(
            "Within 5-year window penalty should remain similarly harsh",
            kotlin.math.abs(recentHires - laterHires) < 80
        )
    }
}
