package com.maisha.game.domain

import com.maisha.game.data.JobPool
import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.CareerTrack
import com.maisha.game.data.model.ClubRank
import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.HustleType
import com.maisha.game.data.model.Job
import com.maisha.game.data.model.PartTimeJob
import com.maisha.game.data.model.SchoolClub
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import com.maisha.game.data.model.TalentTraining
import com.maisha.game.data.model.WorkEffort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
                assertTrue(result.character.career.energyLevel < 100)
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
            countryCode = "KE",
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
        val peaceGross = militaryJob.baseSalary
        val deployGross = militaryJob.baseSalary * CareerEngine.HAZARD_PAY_MULTIPLIER
        assertEquals(
            peaceGross - FinanceEngine.calculateIncomeTax(peaceGross, "KE"),
            peacePay
        )
        assertEquals(
            deployGross - FinanceEngine.calculateIncomeTax(deployGross, "KE"),
            deployPay
        )
        assertTrue(deployPay > peacePay)
        assertTrue(afterDeploy.career.isDeployed)
        assertFalse(afterPeace.career.isDeployed)
    }

    @Test
    fun workHarder_raisesPerformanceAndStress() {
        val employed = TestFixtures.character(
            career = CareerState(
                currentJob = TestFixtures.job(id = "software_developer", title = "Software Developer"),
                performance = 60f,
                stress = 30f,
                bossRelationship = 10
            )
        )
        val result = engine.workHarder(employed)
        assertTrue(result is OfficeActionResult.Success)
        val after = (result as OfficeActionResult.Success).character
        assertTrue(after.career.performance > 60f)
        assertTrue(after.career.stress > 30f)
        assertTrue(after.career.officeWorkActionDoneThisYear)
        assertEquals(OfficeActionResult.AlreadyDone, engine.workHarder(after))
    }

    @Test
    fun askForPromotion_succeedsAtHighPerformanceAndBossTrust() {
        val employed = TestFixtures.character(
            career = CareerState(
                currentJob = TestFixtures.job(
                    id = "software_developer",
                    title = "Junior Developer",
                    level = 1,
                    performanceScore = 92
                ),
                performance = 92f,
                bossRelationship = 20,
                yearsAtCurrentJob = 2
            )
        )
        val result = engine.askForPromotion(employed)
        assertTrue(result is OfficeActionResult.Success)
        val after = (result as OfficeActionResult.Success).character
        assertEquals(2, after.career.currentJob!!.level)
        assertTrue(after.career.currentJob!!.title.contains("Mid") || after.career.currentJob!!.level == 2)
    }

    @Test
    fun askForPromotion_deniedWithoutBossTrust() {
        val employed = TestFixtures.character(
            career = CareerState(
                currentJob = TestFixtures.job(performanceScore = 95),
                performance = 95f,
                bossRelationship = -5
            )
        )
        val result = engine.askForPromotion(employed)
        assertTrue(result is OfficeActionResult.Denied)
    }

    @Test
    fun evaluatePromotion_strongPerformanceWithBossTriggers() {
        val employed = TestFixtures.character(
            career = CareerState(
                currentJob = TestFixtures.job(
                    id = "software_developer",
                    level = 1,
                    performanceScore = 91
                ),
                performance = 91f,
                bossRelationship = 12,
                yearsAtCurrentJob = 1
            )
        )
        val (promoted, wasPromoted) = engine.evaluatePromotion(employed)
        assertTrue(wasPromoted)
        assertEquals(2, promoted.career.currentJob!!.level)
    }

    @Test
    fun applyOfficePolitics_lookForJobClearsEmployment() {
        val employed = TestFixtures.character(
            career = CareerState(
                currentJob = TestFixtures.job(title = "Teacher"),
                companyName = "Horizon Labs"
            )
        )
        val after = engine.applyOfficePoliticsAction(
            employed,
            com.maisha.game.data.model.OfficePoliticsAction.LOOK_FOR_JOB
        )
        assertEquals(null, after.career.currentJob)
        assertTrue(after.career.jobHistory.contains("Teacher"))
    }

    @Test
    fun networkColleague_improvesWeakestBond() {
        val employed = TestFixtures.character(
            career = CareerState(
                currentJob = TestFixtures.job(),
                colleagueRelationships = mapOf("Alex" to 20, "Jordan" to 50),
                companyName = "Nexus Group"
            )
        )
        val result = engine.performOfficeAction(
            employed,
            com.maisha.game.data.model.OfficeAction.NETWORK_COLLEAGUE
        )
        assertTrue(result is OfficeActionResult.Success)
        val after = (result as OfficeActionResult.Success).character
        assertTrue(after.career.colleagueRelationships.getValue("Alex") > 20)
        assertTrue(after.career.networkColleagueDoneThisYear)
    }

    @Test
    fun ensureWorkplaceInitialized_backfillsCompanyAndColleagues() {
        val employed = TestFixtures.character(
            career = CareerState(
                currentJob = TestFixtures.job(performanceScore = 70)
            )
        )
        val after = engine.ensureWorkplaceInitialized(employed)
        assertTrue(!after.career.companyName.isNullOrBlank())
        assertTrue(after.career.colleagueRelationships.isNotEmpty())
        assertEquals(70f, after.career.performance, 0.01f)
    }

    @Test
    fun workYear_burnoutWhenStressHigh() {
        val employed = TestFixtures.character(
            stats = Stats(happiness = 60, health = 70, money = 0),
            career = CareerState(
                currentJob = TestFixtures.job(baseSalary = 100_000),
                stress = 85f,
                performance = 55f,
                companyName = "Atlas Health",
                colleagueRelationships = mapOf("Sam" to 40)
            ),
            countryCode = "KE"
        )
        val after = engine.workYear(employed, WorkEffort.NORMAL)
        assertTrue(after.stats.health < 70)
        assertTrue(after.eventLog.any { it.contains("burnout", ignoreCase = true) })
    }

    @Test
    fun workPartTime_universityStudentSetsActiveJobAndDrainsEnergy() {
        val student = TestFixtures.character(
            age = 19,
            stats = Stats(money = 0, happiness = 60, health = 70, smarts = 65),
            education = EducationState(stage = SchoolStage.UNIVERSITY, currentGrade = 2, gpa = 3.1f),
            career = CareerState(energyLevel = 100)
        )
        val result = engine.workPartTime(student, PartTimeJob.BARISTA)
        assertTrue(result is PartTimeJobResult.Success)
        val after = (result as PartTimeJobResult.Success).character
        assertEquals(PartTimeJob.BARISTA, after.career.activePartTimeJob)
        assertTrue(after.career.partTimeWorkedThisYear)
        assertTrue(after.career.energyLevel < 100)
        assertTrue(after.stats.money > 0)
    }

    @Test
    fun workPartTime_rejectsWithoutSchoolEnrollment() {
        val adult = TestFixtures.character(
            age = 20,
            education = EducationState(stage = SchoolStage.GRADUATED),
            career = CareerState(energyLevel = 100)
        )
        assertTrue(engine.workPartTime(adult, PartTimeJob.RETAIL) is PartTimeJobResult.Ineligible)
    }

    @Test
    fun executeSideHustle_youthCraftsAvailableAt14() {
        val teen = TestFixtures.character(
            age = 14,
            stats = Stats(money = 0, happiness = 70, health = 80, smarts = 50)
        )
        when (val result = engine.executeSideHustle(teen, HustleType.HANDMADE_CRAFTS)) {
            is SideHustleResult.Success -> {
                assertTrue(result.payout > 0)
                assertTrue(result.character.career.energyLevel < 100)
            }
            is SideHustleResult.Failed -> error("Expected crafts success but got ${result.reason}")
        }
    }

    @Test
    fun quitPartTimeJob_clearsActiveRole() {
        val student = TestFixtures.character(
            age = 17,
            education = EducationState(stage = SchoolStage.SECONDARY, currentGrade = 3, gpa = 3.0f),
            career = CareerState(activePartTimeJob = PartTimeJob.BARISTA, partTimeWorkedThisYear = true)
        )
        val result = engine.quitPartTimeJob(student)
        assertTrue(result is QuitPartTimeResult.Success)
        assertNull((result as QuitPartTimeResult.Success).character.career.activePartTimeJob)
    }

    @Test
    fun restToRecoverEnergy_raisesEnergyOnce() {
        val student = TestFixtures.character(
            age = 18,
            education = EducationState(stage = SchoolStage.UNIVERSITY, currentGrade = 1, gpa = 3.0f),
            career = CareerState(energyLevel = 40)
        )
        val first = engine.restToRecoverEnergy(student)
        assertTrue(first is StudentEnergyRestResult.Success)
        val after = (first as StudentEnergyRestResult.Success).character
        assertTrue(after.career.energyLevel > 40)
        assertTrue(engine.restToRecoverEnergy(after) is StudentEnergyRestResult.AlreadyRested)
    }

    @Test
    fun finishStudentWorkYear_paysResidualThenClearsJob() {
        val student = TestFixtures.character(
            age = 17,
            stats = Stats(money = 1_000, happiness = 50, health = 70, smarts = 55),
            education = EducationState(stage = SchoolStage.SECONDARY, currentGrade = 3, gpa = 3.0f),
            career = CareerState(activePartTimeJob = PartTimeJob.TUTORING, energyLevel = 50)
        )
        val after = engine.finishStudentWorkYear(student)
        assertNull(after.career.activePartTimeJob)
        assertTrue(after.stats.money > 1_000)
        assertTrue(after.career.energyLevel > 50)
    }

    @Test
    fun trainTalent_gymRaisesAthleticism() {
        val teen = TestFixtures.character(
            age = 15,
            career = CareerState(athleticism = 30)
        )
        val result = engine.trainTalent(teen, TalentTraining.GYM_SESSION)
        assertTrue(result is TalentTrainingResult.Success)
        val after = (result as TalentTrainingResult.Success).character
        assertTrue(after.career.athleticism > 30)
        assertTrue(after.career.gymTrainedThisYear)
        assertTrue(engine.trainTalent(after, TalentTraining.SPORTS_DRILL) is TalentTrainingResult.AlreadyDone)
    }

    @Test
    fun proSportsTrack_levelsRaiseFameAndStageLabels() {
        val athlete = TestFixtures.character(
            age = 18,
            stats = Stats(health = 70, happiness = 60, smarts = 50, looks = 55),
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                schoolClub = SchoolClub.FOOTBALL,
                clubRank = ClubRank.CAPTAIN
            ),
            career = CareerState(athleticism = 55)
        )
        val started = engine.startCareerTrack(athlete, CareerTrack.PRO_SPORTS)
        assertEquals(CareerTrack.PRO_SPORTS, started.career.careerTrack)
        assertTrue(started.career.fame > 0)
        assertEquals("High school / prospect", engine.trackStageLabel(CareerTrack.PRO_SPORTS, 0))
        assertEquals("Drafted pro", engine.trackStageLabel(CareerTrack.PRO_SPORTS, 1))
        assertEquals("Street busking", engine.trackStageLabel(CareerTrack.ENTERTAINMENT, 0))
    }

    @Test
    fun signEndorsement_requiresFameAndTrackLevel() {
        val star = TestFixtures.character(
            age = 22,
            career = CareerState(
                careerTrack = CareerTrack.ENTERTAINMENT,
                trackLevel = 2,
                fame = 50,
                musicalTalent = 60
            )
        )
        val result = engine.signEndorsementDeal(star)
        assertTrue(result is EndorsementResult.Success)
        val after = (result as EndorsementResult.Success).character
        assertTrue(after.career.endorsementActive)
        assertTrue(after.career.endorsementPayoutPerYear > 0)
    }

    @Test
    fun streetBusk_earnsCashAndFame() {
        val artist = TestFixtures.character(
            age = 18,
            stats = Stats(money = 0, happiness = 50, health = 70, smarts = 50, looks = 55),
            career = CareerState(musicalTalent = 40, fame = 10)
        )
        val result = engine.trainTalent(artist, TalentTraining.STREET_BUSK)
        assertTrue(result is TalentTrainingResult.Success)
        val after = (result as TalentTrainingResult.Success).character
        assertTrue(after.stats.money > 0)
        assertTrue(after.career.fame > 10)
        assertTrue(after.career.talentGigDoneThisYear)
    }

    @Test
    fun leaveCareerTrack_clearsPathway() {
        val star = TestFixtures.character(
            career = CareerState(careerTrack = CareerTrack.PRO_SPORTS, trackLevel = 2, fame = 40)
        )
        val after = engine.leaveCareerTrack(star)
        assertEquals(CareerTrack.NONE, after.career.careerTrack)
        assertEquals(0, after.career.trackLevel)
    }

    @Test
    fun tickFameAndTalentYear_decaysFameWhenIdle() {
        val faded = TestFixtures.character(
            age = 30,
            career = CareerState(fame = 50, fameIdleYears = 2)
        )
        val after = engine.tickFameAndTalentYear(faded)
        assertTrue(after.career.fame < 50)
        assertTrue(after.career.fameIdleYears >= 3)
    }
}
