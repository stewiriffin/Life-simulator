// app/src/main/java/com/maisha/game/domain/RelocationEngine.kt (new)
package com.maisha.game.domain

import com.maisha.game.data.CountryCatalog
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Country
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.LifeEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class RelocationEngine @Inject constructor() {

    fun hasRelocated(character: Character): Boolean =
        character.birthCountryCode != character.countryCode

    fun getRelocationOpportunities(character: Character): List<Country> {
        val current = character.countryCode
        val candidates = CountryCatalog.all()
            .filter { it.code != current }
            .shuffled()
        return candidates.take(RELOCATION_DESTINATION_COUNT)
    }

    fun shouldOfferRelocation(character: Character, triggeredEventIds: Set<String>): Boolean {
        if (character.age < MIN_RELOCATION_AGE || character.age > MAX_RELOCATION_AGE) return false
        if (hasRelocated(character)) return false
        if (RELOCATION_OPPORTUNITY_EVENT_ID in triggeredEventIds) return false
        if (character.criminalRecord.currentlyIncarcerated) return false
        return Random.nextFloat() < RELOCATION_OFFER_CHANCE
    }

    fun buildRelocationOpportunityEvent(
        character: Character,
        destinations: List<Country>
    ): LifeEvent {
        val originName = CountryCatalog.getCountry(character.birthCountryCode).displayName
        val destinationChoices = destinations.map { country ->
            EventChoice(
                label = "Move to ${country.displayName}",
                statEffects = mapOf("happiness" to -8),
                resultText = "You packed your bags and left $originName behind. " +
                    "Starting over in ${country.displayName} means requalifying for local work — " +
                    "your old job credentials don't transfer automatically.",
                relocateToCountry = country.code
            )
        }
        val declineChoice = EventChoice(
            label = "Stay in $originName",
            statEffects = mapOf("happiness" to 2),
            resultText = "Home still feels right. You tucked the idea away for another year."
        )
        return LifeEvent(
            id = RELOCATION_OPPORTUNITY_EVENT_ID,
            minAge = character.age,
            maxAge = character.age,
            text = "An opportunity opens abroad — work contacts, a study pathway, or family already settled " +
                "in another country. You wonder if it is time to leave $originName for a new chapter.",
            choices = destinationChoices + declineChoice,
            tags = listOf(RELOCATION_SYSTEM_TAG, ONE_TIME_TAG),
            weight = 1
        )
    }

    fun relocate(character: Character, newCountry: Country): Character {
        val formerJob = character.career.currentJob
        val updatedCareer = if (formerJob != null) {
            character.career.copy(
                currentJob = null,
                yearsAtCurrentJob = 0,
                jobHistory = character.career.jobHistory + formerJob.title
            )
        } else {
            character.career
        }

        val happinessDip = Random.nextInt(RELOCATION_HAPPINESS_DIP_MIN, RELOCATION_HAPPINESS_DIP_MAX + 1)
        return character.copy(
            countryCode = newCountry.code,
            career = updatedCareer,
            stats = character.stats.copy(
                happiness = (character.stats.happiness - happinessDip).coerceIn(0, 100)
            )
        )
    }

    companion object {
        const val RELOCATION_SYSTEM_TAG = "relocation_system"
        const val REQUIRES_RELOCATION_TAG = "requires_relocation"
        const val RELOCATION_OPPORTUNITY_EVENT_ID = "relocation_opportunity_system"
        private const val ONE_TIME_TAG = "one_time"

        private const val MIN_RELOCATION_AGE = 18
        private const val MAX_RELOCATION_AGE = 50
        private const val RELOCATION_DESTINATION_COUNT = 3
        private const val RELOCATION_OFFER_CHANCE = 0.035f
        private const val RELOCATION_HAPPINESS_DIP_MIN = 6
        private const val RELOCATION_HAPPINESS_DIP_MAX = 10
    }
}
