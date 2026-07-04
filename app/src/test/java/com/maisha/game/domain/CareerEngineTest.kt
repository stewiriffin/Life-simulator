package com.maisha.game.domain

import com.maisha.game.data.JobPool
import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.HustleType
import com.maisha.game.data.model.Job
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import com.maisha.game.data.model.WorkEffort
import com.maisha.game.domain.SideHustleFailure
import com.maisha.game.domain.SideHustleResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CareerEngineTest {

    private val engine = CareerEngine(HealthEngine(), RelocationEngine())

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

    @Test
    fun executeSideHustle_grantsCashAndReducesHappiness() {
        val character = TestFixtures.character(
            age = 20,
            stats = Stats(money = 0, happiness = 80, health = 80, smarts = 50)
        )
        when (val result = engine.executeSideHustle(character, HustleType.FOOD_DELIVERY)) {
            is SideHustleResult.Success -> {
                assertTrue(result.payout > 0)
                assertTrue(result.character.stats.money > 0)
                assertTrue(result.character.stats.happiness < 80)
                assertTrue(result.character.stats.health < 80)
                assertTrue(result.character.career.sideHustleDoneThisYear)
            }
            is SideHustleResult.Failed -> error("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun isJobEligible_acceptsApplicantWithHighSkillDespiteMissingDegree() {
        val developerJob = com.maisha.game.data.JobPool.findById("software_developer")!!
        val noDegreeHighSkill = TestFixtures.character(
            age = 22,
            education = EducationState(stage = SchoolStage.SECONDARY),
            skills = listOf(
                com.maisha.game.data.model.SkillProgress(
                    type = com.maisha.game.data.model.SkillType.PROGRAMMING,
                    level = 80
                )
            )
        )
        val noDegreeLowSkill = TestFixtures.character(
            age = 22,
            education = EducationState(stage = SchoolStage.SECONDARY),
            skills = listOf(
                com.maisha.game.data.model.SkillProgress(
                    type = com.maisha.game.data.model.SkillType.PROGRAMMING,
                    level = 40
                )
            )
        )
        assertTrue(engine.isJobEligible(noDegreeHighSkill, developerJob))
        assertFalse(engine.isJobEligible(noDegreeLowSkill, developerJob))
        assertTrue(
            engine.getEligibleJobs(noDegreeHighSkill).any { it.id == "software_developer" }
        )
        assertTrue(
            engine.getEligibleJobs(noDegreeLowSkill).none { it.id == "software_developer" }
        )
    }

    @Test
    fun isJobEligible_rejectsInfluencerJobIfFollowersAreTooLow() {
        val influencerJob = com.maisha.game.data.JobPool.findById("brand_ambassador")!!
        val noAccount = TestFixtures.character(
            age = 22,
            education = EducationState(stage = SchoolStage.SECONDARY),
            socialMedia = com.maisha.game.data.model.SocialMediaState()
        )
        val lowFollowers = noAccount.copy(
            socialMedia = com.maisha.game.data.model.SocialMediaState(
                hasAccount = true,
                followers = 1_000
            )
        )
        val enoughFollowers = noAccount.copy(
            socialMedia = com.maisha.game.data.model.SocialMediaState(
                hasAccount = true,
                followers = influencerJob.minFollowers
            )
        )
        assertFalse(engine.isJobEligible(noAccount, influencerJob))
        assertFalse(engine.isJobEligible(lowFollowers, influencerJob))
        assertTrue(engine.isJobEligible(enoughFollowers, influencerJob))
        assertTrue(
            engine.getEligibleJobs(enoughFollowers).any { it.id == "brand_ambassador" }
        )
        assertTrue(
            engine.getEligibleJobs(lowFollowers).none { it.id == "brand_ambassador" }
        )
    }

    @Test
    fun executeSideHustle_failsIfPrerequisitesNotMet() {
        val noVehicle = TestFixtures.character(
            age = 25,
            stats = Stats(smarts = 60),
            assets = emptyList()
        )
        val result = engine.executeSideHustle(noVehicle, HustleType.RIDE_SHARE)
        assertTrue(result is SideHustleResult.Failed)
        assertEquals(
            SideHustleFailure.PREREQUISITES_NOT_MET,
            (result as SideHustleResult.Failed).reason
        )
    }

    @Test
    fun isJobEligible_rejectsDrivingJobIfNoLicense() {
        val noLicense = TestFixtures.character(
            age = 22,
            education = EducationState(stage = SchoolStage.SECONDARY),
            stats = Stats(smarts = 60, money = 50_000)
        ).copy(hasDrivingLicense = false)
        val licensed = noLicense.copy(hasDrivingLicense = true)
        assertFalse(engine.isJobEligible(noLicense, JobPool.findById("delivery_driver")!!))
        assertFalse(engine.isJobEligible(noLicense, JobPool.findById("trucker")!!))
        assertFalse(engine.isJobEligible(noLicense, JobPool.findById("driver")!!))
        assertTrue(engine.isJobEligible(licensed, JobPool.findById("delivery_driver")!!))
        assertTrue(engine.isJobEligible(licensed, JobPool.findById("trucker")!!))
    }

    @Test
    fun workYear_appliesHazardPayMultiplierDuringDeployment() {
        val militaryJob = Job(
            id = "military_private",
            title = "Private",
            minEducation = SchoolStage.NONE,
            baseSalary = 200_000,
            isMilitary = true
        )
        val baseMoney = 10_000
        val deployed = TestFixtures.character(
            age = 22,
            stats = Stats(health = 80, happiness = 70, smarts = 50, looks = 50, money = baseMoney),
            career = CareerState(
                currentJob = militaryJob,
                pendingDeployment = true
            )
        )
        val peacetime = deployed.copy(
            career = deployed.career.copy(pendingDeployment = false)
        )
        val afterDeploy = engine.workYear(deployed, WorkEffort.NORMAL)
        val afterPeace = engine.workYear(peacetime, WorkEffort.NORMAL)
        val deployPay = afterDeploy.stats.money - baseMoney
        val peacePay = afterPeace.stats.money - baseMoney
        assertEquals(militaryJob.baseSalary * CareerEngine.HAZARD_PAY_MULTIPLIER, deployPay)
        assertEquals(militaryJob.baseSalary, peacePay)
        assertTrue(afterDeploy.career.isDeployed)
        assertFalse(afterPeace.career.isDeployed)
    }
}
