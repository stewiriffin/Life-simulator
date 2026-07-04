package com.maisha.game.domain

import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.Job
import com.maisha.game.data.model.PetSpecies
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MortalityEngineTest {

    private val engine = MortalityEngine()
    private val relationshipEngine = RelationshipEngine(FinanceEngine())

    @Test
    fun deathProbability_risesWithAge() {
        var deathsYoung = 0
        var deathsOld = 0
        var deathsVeryOld = 0
        repeat(500) {
            val young = TestFixtures.character(age = 30, stats = Stats(health = 80))
            val old = TestFixtures.character(age = 75, stats = Stats(health = 80))
            val veryOld = TestFixtures.character(age = 98, stats = Stats(health = 80))
            if (engine.checkDeath(young) is DeathResult.Died) deathsYoung++
            if (engine.checkDeath(old) is DeathResult.Died) deathsOld++
            if (engine.checkDeath(veryOld) is DeathResult.Died) deathsVeryOld++
        }
        assertTrue(deathsOld > deathsYoung)
        assertTrue(deathsVeryOld > deathsOld)
        assertTrue(deathsVeryOld > 200)
    }

    @Test
    fun lowHealth_increasesDeathChanceAtSameAge() {
        var healthyDeaths = 0
        var criticalDeaths = 0
        repeat(500) {
            val healthy = TestFixtures.character(age = 45, stats = Stats(health = 90))
            val critical = TestFixtures.character(age = 45, stats = Stats(health = 8))
            if (engine.checkDeath(healthy) is DeathResult.Died) healthyDeaths++
            if (engine.checkDeath(critical) is DeathResult.Died) criticalDeaths++
        }
        assertTrue(criticalDeaths > healthyDeaths)
    }

    @Test
    fun petMortality_triggersDeathAtAppropriateSpeciesAge() {
        val youngFish = TestFixtures.pet(species = PetSpecies.FISH, age = 0)
        val oldFish = TestFixtures.pet(species = PetSpecies.FISH, age = 3)
        val oldDog = TestFixtures.pet(species = PetSpecies.DOG, age = 15)

        var youngFishDeaths = 0
        var oldFishDeaths = 0
        var oldDogDeaths = 0
        repeat(200) {
            if (relationshipEngine.rollsPetDeathThisYear(youngFish)) youngFishDeaths++
            if (relationshipEngine.rollsPetDeathThisYear(oldFish)) oldFishDeaths++
            if (relationshipEngine.rollsPetDeathThisYear(oldDog)) oldDogDeaths++
        }

        assertTrue(oldFishDeaths > youngFishDeaths)
        assertTrue(oldDogDeaths > youngFishDeaths)
    }

    @Test
    fun checkDeath_highKarmaReducesFatalRollProbability() {
        val lowKarma = TestFixtures.character(
            age = 45,
            stats = Stats(health = 8, happiness = 40, smarts = 50, looks = 50, money = 10_000, karma = 10)
        )
        val highKarma = lowKarma.copy(
            stats = lowKarma.stats.copy(karma = 95)
        )
        assertTrue(engine.karmaMiracleChance(highKarma) > 0f)
        assertEquals(0f, engine.karmaMiracleChance(lowKarma), 0.0001f)
        assertTrue(
            engine.estimatedDeathProbability(highKarma) <
                engine.estimatedDeathProbability(lowKarma)
        )

        var lowDeaths = 0
        var highDeaths = 0
        repeat(600) {
            if (engine.checkDeath(lowKarma) is DeathResult.Died) lowDeaths++
            if (engine.checkDeath(highKarma) is DeathResult.Died) highDeaths++
        }
        assertTrue(
            "High karma deaths ($highDeaths) should be fewer than low karma ($lowDeaths)",
            highDeaths < lowDeaths
        )
    }

    @Test
    fun donateToCharity_deductsCashAndIncreasesKarma() {
        val gameEngine = TestFixtures.gameEngine()
        val character = TestFixtures.character(
            age = 30,
            stats = Stats(health = 70, happiness = 50, smarts = 50, looks = 50, money = 100_000, karma = 50)
        )
        val amount = 10_000
        val result = gameEngine.donateToCharity(character, amount)
        assertTrue(result is GameEngine.DonationResult.Success)
        val success = result as GameEngine.DonationResult.Success
        assertEquals(90_000, success.character.stats.money)
        assertTrue(success.character.stats.karma > character.stats.karma)
        assertTrue(success.karmaGained >= 1)
    }

    @Test
    fun checkDeath_increasesProbabilityDuringActiveDeployment() {
        val civilian = TestFixtures.character(
            age = 28,
            stats = Stats(health = 70, happiness = 60, smarts = 50, looks = 50, money = 10_000)
        )
        val deployed = civilian.copy(
            career = CareerState(
                currentJob = Job(
                    id = "military_private",
                    title = "Private",
                    minEducation = SchoolStage.NONE,
                    baseSalary = 280_000,
                    isMilitary = true
                ),
                isDeployed = true
            )
        )
        assertTrue(
            engine.estimatedDeathProbability(deployed) >
                engine.estimatedDeathProbability(civilian) * 2f
        )

        var civilianDeaths = 0
        var deployedDeaths = 0
        repeat(800) {
            if (engine.checkDeath(civilian) is DeathResult.Died) civilianDeaths++
            if (engine.checkDeath(deployed) is DeathResult.Died) deployedDeaths++
        }
        assertTrue(
            "Deployed deaths ($deployedDeaths) should exceed civilian ($civilianDeaths)",
            deployedDeaths > civilianDeaths
        )
    }
}
