// app/src/main/java/com/maisha/game/data/model/Stats.kt
package com.maisha.game.data.model

import com.maisha.game.util.clampStat
import kotlinx.serialization.Serializable

/**
 * Core numeric attributes for the player or an NPC.
 *
 * Health/happiness/smarts/looks/[karma] are clamped 0–100 via [coerceCapped] and [applyEffects].
 * Money is floored at 0 (not capped at 100).
 * [karma] is a hidden luck/morality stat (not shown on standard StatBars).
 */
@Serializable
data class Stats(
    val health: Int = 50,
    val happiness: Int = 50,
    val smarts: Int = 50,
    val looks: Int = 50,
    val money: Int = 0,
    /** Hidden moral luck (0–100). Default neutral. */
    val karma: Int = 50
) {
    fun coerceCapped(): Stats = copy(
        health = clampStat(health),
        happiness = clampStat(happiness),
        smarts = clampStat(smarts),
        looks = clampStat(looks),
        karma = clampStat(karma),
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
                "karma" -> updated.copy(karma = updated.karma + delta)
                else -> updated
            }
        }
        return updated.coerceCapped()
    }
}
