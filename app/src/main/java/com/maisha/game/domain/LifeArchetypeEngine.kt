package com.maisha.game.domain

import com.maisha.game.data.model.CareerTrack
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.SchoolStage

/**
 * Ribbon-lite life label shown on the memorial screen — derived from how the character lived.
 */
object LifeArchetypeEngine {

    fun resolveTitleKey(character: Character, netWorth: Int): String {
        val scores = mutableMapOf<String, Int>()

        fun bump(key: String, amount: Int) {
            scores[key] = (scores[key] ?: 0) + amount
        }

        when (character.education.stage) {
            SchoolStage.GRADUATED -> bump("archetype_scholar", 40)
            SchoolStage.UNIVERSITY -> bump("archetype_scholar", 25)
            else -> if (character.education.gpa >= 3.5f) bump("archetype_scholar", 15)
        }

        if (netWorth >= 2_000_000) bump("archetype_builder", 35)
        else if (netWorth >= 500_000) bump("archetype_builder", 20)

        if (character.politics.currentOffice != null) bump("archetype_public_servant", 45)
        if (character.businesses.isNotEmpty()) bump("archetype_builder", 15)

        if (character.isLivingAbroad() || character.ancestryHistory.size >= 2) {
            bump("archetype_globetrotter", 20)
        }

        val livingChildren = character.family.count { it.relation == RelationType.CHILD && it.alive }
        if (livingChildren >= 2 || character.generationNumber >= 2) {
            bump("archetype_patriarch", 25)
        }

        when (character.career.careerTrack) {
            CareerTrack.ENTERTAINMENT -> bump("archetype_artist", 30 + character.career.trackLevel * 10)
            CareerTrack.PRO_SPORTS -> bump("archetype_athlete", 30 + character.career.trackLevel * 10)
            CareerTrack.NONE -> Unit
        }

        if (character.socialMedia.followers >= 50_000) bump("archetype_artist", 20)
        if (character.criminalRecord.timesArrested >= 2) bump("archetype_reformed", 15)
        if (character.stats.karma >= 75) bump("archetype_model_citizen", 25)

        val top = scores.maxByOrNull { it.value }
        return top?.key ?: when {
            character.age >= 70 -> "archetype_elder"
            character.stats.karma >= 60 -> "archetype_model_citizen"
            else -> "archetype_everyday"
        }
    }
}
