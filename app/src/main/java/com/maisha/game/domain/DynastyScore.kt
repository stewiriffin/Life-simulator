package com.maisha.game.domain

import com.maisha.game.data.model.Character
import com.maisha.game.data.model.SchoolStage
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Soft prestige score for dynasty / life-summary bragging — not a spendable currency.
 * Pure derivation from existing Character fields.
 */
object DynastyScore {

    data class Breakdown(
        val total: Int,
        val generationPoints: Int,
        val longevityPoints: Int,
        val wealthPoints: Int,
        val educationPoints: Int,
        val familyPoints: Int,
        val virtuePoints: Int,
        val titleKey: String
    )

    fun calculate(character: Character, netWorth: Int): Breakdown {
        val generationPoints = ((character.generationNumber - 1).coerceAtLeast(0) * 120)
            .coerceAtMost(600)
        val longevityPoints = (character.age * 4).coerceAtMost(400)
        val wealthPoints = wealthScore(netWorth).coerceAtMost(350)
        val educationPoints = when (character.education.stage) {
            SchoolStage.GRADUATED -> 100
            SchoolStage.UNIVERSITY -> 60
            SchoolStage.SECONDARY -> 30
            SchoolStage.PRIMARY -> 10
            SchoolStage.NONE -> if (character.education.kcseGrade != null) 20 else 0
        }
        val livingFamily = character.family.count { it.alive }
        val familyPoints = (livingFamily * 12 + character.ancestryHistory.size * 25)
            .coerceAtMost(250)
        val virtuePoints = ((character.stats.karma - 50) * 2).coerceIn(-80, 100)
        val total = (generationPoints + longevityPoints + wealthPoints +
            educationPoints + familyPoints + virtuePoints).coerceAtLeast(0)

        val titleKey = when {
            total >= 1200 -> "dynasty_title_legend"
            total >= 800 -> "dynasty_title_powerhouse"
            total >= 450 -> "dynasty_title_rising"
            total >= 200 -> "dynasty_title_rooted"
            else -> "dynasty_title_seedling"
        }

        return Breakdown(
            total = total,
            generationPoints = generationPoints,
            longevityPoints = longevityPoints,
            wealthPoints = wealthPoints,
            educationPoints = educationPoints,
            familyPoints = familyPoints,
            virtuePoints = virtuePoints,
            titleKey = titleKey
        )
    }

    private fun wealthScore(netWorth: Int): Int {
        if (netWorth <= 0) return 0
        // Log scale so early millions don't dominate forever.
        return (ln(netWorth.toDouble() / 10_000.0 + 1.0) * 45.0).roundToInt()
    }
}
