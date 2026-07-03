// app/src/main/java/com/maisha/game/domain/RelocationEngine.kt (modified — repeatable relocation for World Traveler)
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

    /** True if [Character.relocationCount] > 0 or birth country differs from current residence. */
    fun hasRelocated(character: Character): Boolean =
        character.relocationCount > 0 || character.birthCountryCode != character.countryCode

    /** Unique one-time event id per relocation index (supports multiple moves / World Traveler). */
    fun relocationOpportunityEventId(relocationCount: Int): String =
        "${RELOCATION_OPPORTUNITY_EVENT_ID}_$relocationCount"

    /** Random subset of countries excluding the character's current [Character.countryCode]. */
    fun getRelocationOpportunities(character: Character): List<Country> {
        val current = character.countryCode
        return CountryCatalog.all()
            .filter { it.code != current }
            .shuffled()
            .take(RELOCATION_DESTINATION_COUNT)
    }

    /**
     * Whether to roll a relocation offer this year: age window, not incarcerated, event not yet triggered,
     * and minimum gap since last move when [Character.relocationCount] > 0.
     */
    fun shouldOfferRelocation(character: Character, triggeredEventIds: Set<String>): Boolean {
        if (character.age < MIN_RELOCATION_AGE || character.age > MAX_RELOCATION_AGE) return false
        if (character.criminalRecord.currentlyIncarcerated) return false
        val eventId = relocationOpportunityEventId(character.relocationCount)
        if (eventId in triggeredEventIds) return false
        if (character.relocationCount > 0) {
            val lastAge = character.lastRelocationAge ?: return false
            if (character.age - lastAge < MIN_YEARS_BETWEEN_RELOCATIONS) return false
        }
        return Random.nextFloat() < RELOCATION_OFFER_CHANCE
    }

    /**
     * System [LifeEvent] with up to three destination choices plus stay-put; choices use [EventChoice.relocateToCountry].
     */
    fun buildRelocationOpportunityEvent(
        character: Character,
        destinations: List<Country>
    ): LifeEvent {
        val originName = CountryCatalog.getCountry(character.countryCode).displayName
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
            id = relocationOpportunityEventId(character.relocationCount),
            minAge = character.age,
            maxAge = character.age,
            text = "An opportunity opens abroad — work contacts, a study pathway, or family already settled " +
                "in another country. You wonder if it is time to leave $originName for a new chapter.",
            choices = destinationChoices + declineChoice,
            tags = listOf(RELOCATION_SYSTEM_TAG, ONE_TIME_TAG),
            weight = 1
        )
    }

    /**
     * Applies a move: updates [Character.countryCode], increments relocation counters/history, clears job
     * (credentials don't transfer), and applies a random happiness dip.
     */
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
            relocationCount = character.relocationCount + 1,
            lastRelocationAge = character.age,
            relocationHistory = character.relocationHistory + newCountry.code,
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
        private const val MAX_RELOCATION_AGE = 55
        private const val RELOCATION_DESTINATION_COUNT = 3
        private const val RELOCATION_OFFER_CHANCE = 0.035f
        private const val RELOCATION_HAPPINESS_DIP_MIN = 6
        private const val RELOCATION_HAPPINESS_DIP_MAX = 10
        private const val MIN_YEARS_BETWEEN_RELOCATIONS = 4
    }
}
