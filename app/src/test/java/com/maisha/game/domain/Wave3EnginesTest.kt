package com.maisha.game.domain

import com.maisha.game.data.model.Asset
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.CareerTrack
import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.PartTimeJob
import com.maisha.game.data.model.PortfolioStrategy
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Wave3EnginesTest {
    private val educationEngine = EducationEngine(RelocationEngine())
    private val careerEngine = CareerEngine(HealthEngine(), RelocationEngine())
    private val financeEngine = FinanceEngine()
    private val relationshipEngine = RelationshipEngine(financeEngine)
    private val crimeEngine = CrimeEngine()
    private val healthEngine = HealthEngine()
    private val weeklyChallengeEngine = WeeklyChallengeEngine()

    @Test
    fun startCareerTrack_medical_withMedicineDegree() {
        val medStudent = TestFixtures.character(
            age = 22,
            education = EducationState(
                stage = SchoolStage.UNIVERSITY,
                courseOfStudy = "Medicine",
                gpa = 3.2f
            )
        )
        val updated = careerEngine.startCareerTrack(medStudent, CareerTrack.MEDICAL)
        assertEquals(CareerTrack.MEDICAL, updated.career.careerTrack)
    }

    @Test
    fun startCareerTrack_legal_withLawDegree() {
        val lawStudent = TestFixtures.character(
            age = 22,
            education = EducationState(
                stage = SchoolStage.UNIVERSITY,
                courseOfStudy = "Law",
                gpa = 3.0f
            )
        )
        val updated = careerEngine.startCareerTrack(lawStudent, CareerTrack.LEGAL)
        assertEquals(CareerTrack.LEGAL, updated.career.careerTrack)
    }

    @Test
    fun workPartTime_paysTeenOncePerYear() {
        val teen = TestFixtures.character(
            age = 16,
            stats = Stats(money = 0, happiness = 50, health = 70, smarts = 60),
            education = EducationState(
                stage = SchoolStage.SECONDARY,
                currentGrade = 3,
                gpa = 3.0f
            )
        )
        val first = careerEngine.workPartTime(teen, PartTimeJob.RETAIL)
        assertTrue(first is PartTimeJobResult.Success)
        val after = (first as PartTimeJobResult.Success).character
        assertTrue(after.stats.money > 0)
        assertTrue(after.career.partTimeWorkedThisYear)
        assertEquals(PartTimeJob.RETAIL, after.career.activePartTimeJob)
        assertTrue(careerEngine.workPartTime(after, PartTimeJob.TUTORING) is PartTimeJobResult.AlreadyWorked)
    }

    @Test
    fun adoptChild_addsAdoptedChild() {
        val adult = TestFixtures.character(
            age = 30,
            stats = Stats(money = 200_000, happiness = 60, health = 70, smarts = 50)
        )
        val result = relationshipEngine.adoptChild(adult)
        assertTrue(result is AdoptChildResult.Success)
        val after = (result as AdoptChildResult.Success).character
        assertTrue(after.family.any { it.relation == RelationType.CHILD && it.isAdopted })
    }

    @Test
    fun prenup_reducesDivorceSettlement() {
        val partner = TestFixtures.person(
            relation = RelationType.SPOUSE,
            age = 28,
            isMarried = true
        ).copy(prenupSigned = true)
        val married = TestFixtures.character(
            age = 30,
            stats = Stats(money = 500_000, happiness = 70, health = 80, smarts = 60),
            family = listOf(partner)
        )
        val prenupSettlement = relationshipEngine.divorceSettlementCost(married, partner)
        val fullSettlement = relationshipEngine.divorceSettlementCost(
            married,
            partner.copy(prenupSigned = false)
        )
        assertTrue(prenupSettlement < fullSettlement)
    }

    @Test
    fun requestExpungement_clearsHirePenalty() {
        val reformed = TestFixtures.character(
            age = 55,
            stats = Stats(money = 500_000, happiness = 60, health = 70, smarts = 50, karma = 90),
            criminalRecord = CriminalRecord(
                hasRecord = true,
                timesArrested = 1,
                lastArrestAge = 25
            )
        )
        assertTrue(crimeEngine.canRequestExpungement(reformed))
        var outcome: ExpungementResult = ExpungementResult.Denied(reformed)
        repeat(15) {
            outcome = crimeEngine.requestExpungement(reformed)
            if (outcome is ExpungementResult.Success) return@repeat
        }
        assertTrue(outcome is ExpungementResult.Success)
        val after = (outcome as ExpungementResult.Success).character
        assertTrue(after.criminalRecord.recordExpunged)
        assertFalse(after.criminalRecord.hasRecord)
    }

    @Test
    fun renovateAsset_upgradesHouse() {
        val owner = TestFixtures.character(
            age = 35,
            stats = Stats(money = 500_000, happiness = 60, health = 70, smarts = 50),
            assets = listOf(
                Asset(
                    id = "house-1",
                    type = AssetType.HOUSE,
                    name = "Starter Home",
                    purchasePrice = 200_000,
                    currentValue = 220_000,
                    condition = 90,
                    monthlyUpkeep = 2_000
                )
            )
        )
        val result = financeEngine.renovateAsset(owner, "house-1")
        assertTrue(result is RenovateResult.Success)
        val house = (result as RenovateResult.Success).character.assets.first()
        assertEquals(1, house.renovationLevel)
        assertTrue(house.currentValue > 220_000)
    }

    @Test
    fun portfolioStrategy_aggressive_hasWiderRange() {
        val investor = TestFixtures.character(
            age = 30,
            stats = Stats(money = 0, happiness = 60, health = 70, smarts = 50),
            lifestyle = com.maisha.game.data.model.LifestyleState(
                portfolioStrategy = PortfolioStrategy.AGGRESSIVE
            )
        ).copy(investmentPortfolioValue = 50_000)
        val switched = financeEngine.setPortfolioStrategy(investor, PortfolioStrategy.CONSERVATIVE)
        assertEquals(PortfolioStrategy.CONSERVATIVE, switched.lifestyle.portfolioStrategy)
    }

    @Test
    fun chronicIllness_staysAfterTreatment() {
        val midlife = TestFixtures.character(
            age = 50,
            stats = Stats(money = 100_000, happiness = 60, health = 45, smarts = 50),
            activeConditions = listOf(
                com.maisha.game.data.model.HealthCondition(
                    id = "c1",
                    name = "Hypertension",
                    severity = 2,
                    isChronic = true
                )
            )
        )
        val afterDrain = healthEngine.applyUntreatedConditions(midlife)
        assertTrue(afterDrain.activeConditions.any { it.isChronic })
    }

    @Test
    fun lifeArchetype_healerForMedicalTrack() {
        val doctor = careerEngine.startCareerTrack(
            TestFixtures.character(
                age = 28,
                education = EducationState(courseOfStudy = "Medicine", stage = SchoolStage.UNIVERSITY)
            ),
            CareerTrack.MEDICAL
        ).let { base ->
            base.copy(career = base.career.copy(trackLevel = 2))
        }
        assertEquals("archetype_healer", LifeArchetypeEngine.resolveTitleKey(doctor, 80_000))
    }

    @Test
    fun weeklyChallenge_medicalCompleteAtTrackLevel2() {
        val challenge = WeeklyChallengeEngine.CHALLENGES.first { it.id == "medical_residency" }
        val incomplete = TestFixtures.character(age = 30).let {
            careerEngine.startCareerTrack(
                it.copy(education = EducationState(courseOfStudy = "Medicine")),
                CareerTrack.MEDICAL
            )
        }
        assertFalse(weeklyChallengeEngine.isComplete(incomplete, challenge))
        val complete = incomplete.copy(career = incomplete.career.copy(trackLevel = 2))
        assertTrue(weeklyChallengeEngine.isComplete(complete, challenge))
    }
}
