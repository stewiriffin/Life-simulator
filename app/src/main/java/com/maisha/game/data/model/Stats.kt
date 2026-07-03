// app/src/main/java/com/maisha/game/data/model/Stats.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Stats(
    val health: Int = 50,
    val happiness: Int = 50,
    val smarts: Int = 50,
    val looks: Int = 50,
    val money: Int = 0
) {
    fun coerceCapped(): Stats = copy(
        health = health.coerceIn(0, 100),
        happiness = happiness.coerceIn(0, 100),
        smarts = smarts.coerceIn(0, 100),
        looks = looks.coerceIn(0, 100),
        money = money.coerceAtLeast(0)
    )

    fun applyEffects(effects: Map<String, Int>): Stats {
        var updated = this
        effects.forEach { (stat, delta) ->
            updated = when (stat) {
                "health" -> updated.copy(health = updated.health + delta)
                "happiness" -> updated.copy(happiness = updated.happiness + delta)
                "smarts" -> updated.copy(smarts = updated.smarts + delta)
                "looks" -> updated.copy(looks = updated.looks + delta)
                "money" -> updated.copy(money = updated.money + delta)
                else -> updated
            }
        }
        return updated.coerceCapped()
    }
}
