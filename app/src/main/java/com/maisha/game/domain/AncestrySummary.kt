// app/src/main/java/com/maisha/game/domain/AncestrySummary.kt (new)
package com.maisha.game.domain

import com.maisha.game.data.model.Character

object AncestrySummary {

    fun distinctCountriesLived(character: Character): Int {
        val codes = linkedSetOf<String>()
        character.ancestryHistory.forEach { entry ->
            codes.add(entry.countryCode)
            codes.addAll(entry.relocatedTo)
        }
        codes.add(character.birthCountryCode)
        codes.add(character.countryCode)
        codes.addAll(character.relocationHistory)
        character.secondaryCountryCode?.let { codes.add(it) }
        return codes.size
    }

    fun hasLineageHistory(character: Character): Boolean =
        character.ancestryHistory.isNotEmpty() || character.generationNumber > 1
}
