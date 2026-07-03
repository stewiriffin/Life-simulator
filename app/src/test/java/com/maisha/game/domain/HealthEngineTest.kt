package com.maisha.game.domain

import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.LifestyleState
import com.maisha.game.data.model.Stats
import com.maisha.game.data.model.WorkEffort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthEngineTest {

    private val engine = HealthEngine()

    @Test
    fun illnessRoll_lowHealthProducesMoreIllnessesThanHighHealth() {
        val healthy = TestFixtures.character(stats = Stats(health = 95))
        val fragile = TestFixtures.character(stats = Stats(health = 15))
        var healthyIllnesses = 0
        var fragileIllnesses = 0
        repeat(500) {
            if (engine.rollForIllness(healthy) != null) healthyIllnesses++
            if (engine.rollForIllness(fragile) != null) fragileIllnesses++
        }
        assertTrue("Low health should roll illness more often", fragileIllnesses > healthyIllnesses)
    }

    @Test
    fun addCondition_prependsEventLogEntry() {
        val character = TestFixtures.character()
        val condition = com.maisha.game.data.model.HealthCondition(
            id = "c1",
            name = "Malaria",
            severity = 2
        )
        val updated = engine.addCondition(character, condition)
        assertTrue(updated.eventLog.first().contains("Malaria"))
        assertEquals(1, updated.activeConditions.size)
    }

    @Test
    fun processHealthProgression_appliesGymBonusAndStressPenalty() {
        val baseStats = Stats(
            health = 80,
            happiness = 80,
            looks = 70,
            money = 500_000
        )
        val baseline = TestFixtures.character(stats = baseStats)
        val withGym = baseline.copy(
            lifestyle = LifestyleState(hasGymMembership = true)
        )

        val baselineAfter = engine.processHealthProgression(baseline)
        val gymAfter = engine.processHealthProgression(withGym)

        assertTrue(gymAfter.stats.health >= baselineAfter.stats.health)
        assertTrue(gymAfter.stats.looks >= baselineAfter.stats.looks)

        val employed = TestFixtures.character(
            stats = baseStats,
            career = CareerState(
                currentJob = TestFixtures.job(id = "engineer", title = "Engineer", level = 3)
            )
        )
        val afterNormal = engine.applyWorkEffortStress(employed, WorkEffort.NORMAL)
        val afterGrind = engine.applyWorkEffortStress(employed, WorkEffort.GRIND)

        assertTrue(afterGrind.stats.health < afterNormal.stats.health)
        assertTrue(afterGrind.stats.happiness < afterNormal.stats.happiness)
    }
}
