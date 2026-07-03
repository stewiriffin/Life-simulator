package com.maisha.game.domain

import com.maisha.game.data.model.CrimeType
import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.LawyerTier
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrimeEngineTest {

    private val engine = CrimeEngine()
    private val careerEngine = CareerEngine(HealthEngine())

    @Test
    fun crimeTypes_produceDifferentSentenceLengthsWhenCaught() {
        val pickpocketSentences = mutableListOf<Int>()
        val fraudSentences = mutableListOf<Int>()
        repeat(200) {
            val base = TestFixtures.character(age = 20, stats = Stats(money = 500_000))
            val pickArrested = engine.processArrest(base, CrimeType.PICKPOCKET)
            val fraudArrested = engine.processArrest(base, CrimeType.FRAUD)
            val pickTrial = engine.goToTrial(
                pickArrested,
                LawyerTier.PUBLIC_DEFENDER,
                netWorth = 500_000
            )
            val fraudTrial = engine.goToTrial(
                fraudArrested,
                LawyerTier.PUBLIC_DEFENDER,
                netWorth = 500_000
            )
            if (pickTrial is TrialResult.Sentenced) pickpocketSentences += pickTrial.sentenceYears
            if (fraudTrial is TrialResult.Sentenced) fraudSentences += fraudTrial.sentenceYears
        }
        assertTrue(pickpocketSentences.isNotEmpty())
        assertTrue(fraudSentences.isNotEmpty())
        val pickAvg = pickpocketSentences.average()
        val fraudAvg = fraudSentences.average()
        assertTrue("Fraud sentences should average longer than pickpocket", fraudAvg > pickAvg)
    }

    @Test
    fun processArrest_entersAwaitingTrialInsteadOfSentencing() {
        val character = TestFixtures.character(age = 20)
        val arrested = engine.processArrest(character, CrimeType.SHOPLIFT)
        assertTrue(arrested.criminalRecord.awaitingTrial)
        assertEquals(CrimeType.SHOPLIFT.name, arrested.criminalRecord.pendingCrimeType)
        assertFalse(arrested.criminalRecord.currentlyIncarcerated)
        assertEquals(0, arrested.criminalRecord.yearsRemaining)
    }

    @Test
    fun goToTrial_expensiveLawyerReducesSentenceProbability() {
        val base = TestFixtures.character(
            age = 25,
            stats = Stats(money = 2_000_000)
        )
        var publicSentences = 0
        var publicTotalYears = 0
        var expensiveLenientOutcomes = 0
        repeat(300) {
            val arrested = engine.processArrest(base, CrimeType.FRAUD)
            when (val publicResult = engine.goToTrial(
                arrested,
                LawyerTier.PUBLIC_DEFENDER,
                netWorth = 2_000_000
            )) {
                is TrialResult.Sentenced -> {
                    publicSentences++
                    publicTotalYears += publicResult.sentenceYears
                }
                is TrialResult.Acquitted -> Unit
                TrialResult.Ineligible -> Unit
            }
            val arrestedAgain = engine.processArrest(base, CrimeType.FRAUD)
            when (val expensiveResult = engine.goToTrial(
                arrestedAgain,
                LawyerTier.EXPENSIVE,
                netWorth = 2_000_000
            )) {
                is TrialResult.Acquitted -> expensiveLenientOutcomes++
                is TrialResult.Sentenced -> {
                    if (expensiveResult.sentenceYears <= 3) expensiveLenientOutcomes++
                }
                TrialResult.Ineligible -> Unit
            }
        }
        val publicAvg = publicTotalYears.toFloat() / publicSentences.coerceAtLeast(1)
        assertTrue(publicSentences > 100)
        assertTrue(expensiveLenientOutcomes > 150)
        assertTrue(expensiveLenientOutcomes > publicAvg)
    }

    @Test
    fun serveYear_triggersParoleReleaseOnGoodBehavior() {
        val imprisoned = TestFixtures.character(
            stats = Stats(happiness = 40),
            criminalRecord = CriminalRecord(
                currentlyIncarcerated = true,
                yearsRemaining = 4,
                totalSentenceYears = 6,
                yearsServed = 2,
                negativePrisonEvents = 0,
                paroleBonus = 6
            )
        )
        var paroles = 0
        repeat(400) {
            val after = engine.serveYear(imprisoned)
            if (!after.criminalRecord.currentlyIncarcerated &&
                after.eventLog.first().contains("parole", ignoreCase = true)
            ) {
                paroles++
            }
        }
        assertTrue("Good behavior should sometimes trigger parole", paroles > 0)
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

    @Test
    fun attemptCrime_successRateScalesWithSmartsStat() {
        val lowSmarts = TestFixtures.character(age = 20, stats = Stats(smarts = 15))
        val highSmarts = TestFixtures.character(age = 20, stats = Stats(smarts = 95))
        var lowSuccesses = 0
        var highSuccesses = 0
        repeat(500) {
            if (engine.attemptCrime(lowSmarts, CrimeType.PICKPOCKET) is CrimeResult.Success) lowSuccesses++
            if (engine.attemptCrime(highSmarts, CrimeType.PICKPOCKET) is CrimeResult.Success) highSuccesses++
        }
        assertTrue(
            "Higher smarts should improve crime success rate",
            highSuccesses > lowSuccesses
        )
    }
}
