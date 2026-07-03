package com.maisha.game.domain

import com.maisha.game.data.model.Stats
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
}
