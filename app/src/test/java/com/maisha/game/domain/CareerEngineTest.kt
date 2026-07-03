package com.maisha.game.domain

import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import com.maisha.game.data.model.WorkEffort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CareerEngineTest {

    private val engine = CareerEngine(HealthEngine())

    @Test
    fun getEligibleJobs_filtersByEducationStage() {
        val secondary = TestFixtures.character(
            age = 20,
            education = EducationState(stage = SchoolStage.SECONDARY)
        )
        val graduated = TestFixtures.character(
            age = 22,
            education = EducationState(stage = SchoolStage.GRADUATED, gpa = 3.0f)
        )
        val secondaryJobs = engine.getEligibleJobs(secondary)
        val gradJobs = engine.getEligibleJobs(graduated)
        assertTrue(gradJobs.size >= secondaryJobs.size)
        assertTrue(gradJobs.any { it.minEducation == SchoolStage.GRADUATED })
    }

    @Test
    fun criminalRecord_reducesHireSuccessRate() {
        val clean = TestFixtures.character(
            age = 22,
            stats = Stats(smarts = 70),
            education = EducationState(stage = SchoolStage.GRADUATED, gpa = 3.2f)
        )
        val record = clean.copy(
            criminalRecord = CriminalRecord(hasRecord = true, timesArrested = 1, lastArrestAge = 20)
        )
        var cleanHires = 0
        var recordHires = 0
        repeat(400) {
            if (engine.applyForJob(clean, "teacher").second is CareerResult.Hired) cleanHires++
            if (engine.applyForJob(record, "teacher").second is CareerResult.Hired) recordHires++
        }
        assertTrue("Criminal record should reduce hires", cleanHires > recordHires)
    }

    @Test
    fun workYear_grindImprovesPerformanceMoreThanCoast() {
        val baseJob = TestFixtures.job(performanceScore = 50)
        val character = TestFixtures.character(
            career = CareerState(currentJob = baseJob, yearsAtCurrentJob = 1),
            stats = Stats(money = 0, happiness = 80, health = 80)
        )
        var grindGains = 0
        var coastLosses = 0
        repeat(200) {
            val afterGrind = engine.workYear(character, WorkEffort.GRIND)
            val afterCoast = engine.workYear(character, WorkEffort.COAST)
            val grindPerf = afterGrind.career.currentJob!!.performanceScore
            val coastPerf = afterCoast.career.currentJob!!.performanceScore
            if (grindPerf > 50) grindGains++
            if (coastPerf < 50) coastLosses++
        }
        assertTrue(grindGains > 100)
        assertTrue(coastLosses > 100)
    }

    @Test
    fun evaluatePromotion_atThresholdPromotes() {
        val atThreshold = TestFixtures.character(
            career = CareerState(
                currentJob = TestFixtures.job(performanceScore = 65),
                yearsAtCurrentJob = 3
            )
        )
        val (promoted, wasPromoted) = engine.evaluatePromotion(atThreshold)
        assertTrue(wasPromoted)
        assertEquals(2, promoted.career.currentJob!!.level)
    }

    @Test
    fun evaluatePromotion_oneBelowThresholdDoesNotPromote() {
        val below = TestFixtures.character(
            career = CareerState(
                currentJob = TestFixtures.job(performanceScore = 64),
                yearsAtCurrentJob = 3
            )
        )
        val (_, wasPromoted) = engine.evaluatePromotion(below)
        assertFalse(wasPromoted)
    }

    @Test
    fun evaluateFiring_atThresholdDoesNotFire() {
        val atThreshold = TestFixtures.character(
            career = CareerState(
                currentJob = TestFixtures.job(performanceScore = 20),
                yearsAtCurrentJob = 2
            )
        )
        val (_, fired) = engine.evaluateFiring(atThreshold)
        assertFalse(fired)
    }

    @Test
    fun evaluateFiring_oneBelowThresholdFires() {
        val below = TestFixtures.character(
            career = CareerState(
                currentJob = TestFixtures.job(performanceScore = 19),
                yearsAtCurrentJob = 2
            )
        )
        val (after, fired) = engine.evaluateFiring(below)
        assertTrue(fired)
        assertEquals(null, after.career.currentJob)
    }

    @Test
    fun shouldTriggerDownsizing_falseWhenUnemployed() {
        val unemployed = TestFixtures.character(age = 30)
        assertFalse(engine.shouldTriggerDownsizing(unemployed))
    }

    @Test
    fun applyDownsizing_removesJobAndReducesHappiness() {
        val job = TestFixtures.job()
        val character = TestFixtures.character(
            stats = Stats(happiness = 60),
            career = CareerState(currentJob = job, yearsAtCurrentJob = 2)
        )
        val (after, title) = engine.applyDownsizing(character)
        assertEquals(job.title, title)
        assertEquals(null, after.career.currentJob)
        assertEquals(0, after.career.yearsAtCurrentJob)
        assertEquals(45, after.stats.happiness)
        assertTrue(after.eventLog.first().contains("downsizing", ignoreCase = true))

        val (unchanged, emptyTitle) = engine.applyDownsizing(TestFixtures.character())
        assertEquals("", emptyTitle)
        assertEquals(null, unchanged.career.currentJob)
        assertEquals(50, unchanged.stats.happiness)
    }

    @Test
    fun retire_setsRetirementStateAndCalculatesPension() {
        val salary = 500_000
        val character = TestFixtures.character(
            age = 62,
            career = CareerState(currentJob = TestFixtures.job(baseSalary = salary))
        )
        val result = engine.retire(character)
        assertTrue(result is RetirementResult.Success)
        val updated = (result as RetirementResult.Success).character
        assertTrue(updated.career.isRetired)
        assertEquals(null, updated.career.currentJob)
        assertTrue(updated.career.pensionAmount in (salary * 0.40).toInt()..(salary * 0.60).toInt())
        assertTrue(updated.career.jobHistory.isNotEmpty())
    }

    @Test
    fun retire_ineligibleWhenTooYoung() {
        val character = TestFixtures.character(
            age = 55,
            career = CareerState(currentJob = TestFixtures.job())
        )
        assertTrue(engine.retire(character) is RetirementResult.Ineligible)
    }

    @Test
    fun financeEngine_addsPensionToNetWorthDuringRetirement() {
        val financeEngine = FinanceEngine()
        val character = TestFixtures.character(
            stats = Stats(money = 100_000),
            career = CareerState(isRetired = true, pensionAmount = 25_000)
        )
        val after = financeEngine.applyPension(character)
        assertEquals(125_000, after.stats.money)
        assertEquals(125_000, financeEngine.calculateNetWorth(after))
    }
}
