package com.maisha.game.domain

import com.maisha.game.data.model.PetSpecies
import com.maisha.game.data.model.Stats
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
        assertTrue(oldFishDeaths > 150)
    }
}
