package com.maisha.game.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsEdgeCaseTest {

    @Test
    fun applyEffects_clampsMoneyAtZero() {
        val stats = Stats(money = 100)
        val after = stats.applyEffects(mapOf("money" to -500))
        assertEquals(0, after.money)
    }

    @Test
    fun applyEffects_clampsStatsAtBounds() {
        val stats = Stats(health = 95, happiness = 5)
        val after = stats.applyEffects(
            mapOf(
                "health" to 20,
                "happiness" to -20
            )
        )
        assertEquals(100, after.health)
        assertEquals(0, after.happiness)
    }
}
