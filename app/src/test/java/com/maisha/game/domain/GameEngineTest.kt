package com.maisha.game.domain

import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private val engine = TestFixtures.gameEngine()

    @Test
    fun ageUp_incrementsAgeByExactlyOne() {
        val character = TestFixtures.character(age = 20)
        val outcome = engine.ageUp(character, emptySet(), emptyList(), slotId = 0)
        assertEquals(21, outcome.character.age)
    }

    @Test
    fun applyChoice_clampsStatsToZeroAndOneHundred() {
        val character = TestFixtures.character(
            stats = Stats(health = 95, happiness = 5, smarts = 50, looks = 50, money = 100)
        )
        val event = LifeEvent(
            id = "test",
            minAge = 20,
            maxAge = 99,
            text = "Test",
            choices = listOf(
                EventChoice(
                    label = "Boost",
                    statEffects = mapOf(
                        "health" to 20,
                        "happiness" to -20
                    ),
                    resultText = "Done"
                )
            )
        )
        val updated = engine.applyChoice(character, event.choices.first(), event)
        assertEquals(100, updated.stats.health)
        assertEquals(0, updated.stats.happiness)
    }

    @Test
    fun ageUp_whileIncarcerated_skipsEducationAndCareerProgression() {
        val character = TestFixtures.character(
            age = 24,
            stats = Stats(money = 10_000),
            education = EducationState(
                stage = SchoolStage.PRIMARY,
                currentGrade = 3,
                gpa = 2.5f
            ),
            career = CareerState(
                currentJob = TestFixtures.job(baseSalary = 100_000),
                yearsAtCurrentJob = 2
            ),
            criminalRecord = CriminalRecord(
                currentlyIncarcerated = true,
                yearsRemaining = 2,
                timesArrested = 1
            )
        )
        val moneyBefore = character.stats.money
        val outcome = engine.ageUp(character, emptySet(), emptyList(), slotId = 0)

        assertEquals(SchoolStage.PRIMARY, outcome.character.education.stage)
        assertEquals(3, outcome.character.education.currentGrade)
        assertEquals(moneyBefore, outcome.character.stats.money)
        assertTrue(outcome.character.criminalRecord.currentlyIncarcerated)
        assertEquals(1, outcome.character.criminalRecord.yearsRemaining)
    }

    @Test
    fun ageUp_mortalityRunsAfterProgression_canEndLife() {
        var deaths = 0
        repeat(400) {
            val character = TestFixtures.character(
                age = 96,
                stats = Stats(health = 5, happiness = 5, smarts = 5, looks = 5)
            )
            val outcome = engine.ageUp(character, emptySet(), emptyList(), slotId = 0)
            if (!outcome.character.alive) deaths++
        }
        assertTrue("Very old, critically ill characters should sometimes die", deaths > 50)
    }

    @Test
    fun ageUp_doesNotUnlockAchievementsOnDeathYear() {
        var unlocksOnDeath = 0
        repeat(200) {
            val character = TestFixtures.character(
                age = 49,
                stats = com.maisha.game.data.model.Stats(health = 3, happiness = 3, smarts = 3, looks = 3)
            )
            val outcome = engine.ageUp(character, emptySet(), emptyList(), slotId = 0)
            if (!outcome.character.alive && outcome.newlyUnlockedAchievements.isNotEmpty()) {
                unlocksOnDeath++
            }
        }
        assertEquals(0, unlocksOnDeath)
    }
}
