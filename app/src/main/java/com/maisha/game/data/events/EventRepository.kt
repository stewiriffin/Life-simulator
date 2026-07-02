// app/src/main/java/com/maisha/game/data/events/EventRepository.kt
package com.maisha.game.data.events

import android.content.Context
import com.maisha.game.data.FlavorInterpolator
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.LifeEventList
import com.maisha.game.data.model.RelationType
import com.maisha.game.domain.CareerEngine
import com.maisha.game.domain.EducationEngine
import com.maisha.game.domain.FinanceEngine
import com.maisha.game.domain.RelationshipEngine
import com.maisha.game.domain.RelocationEngine
import com.maisha.game.domain.hasChild
import com.maisha.game.domain.hasSpouse
import com.maisha.game.domain.isMarried
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class EventRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val financeEngine: FinanceEngine
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val allEvents: List<LifeEvent> by lazy {
        val starter = loadEventsFromAsset("data/events/starter_events.json")
        val education = loadEventsFromAsset("data/events/education_events.json")
        val career = loadEventsFromAsset("data/events/career_events.json")
        val finance = loadEventsFromAsset("data/events/finance_events.json")
        val relationship = loadEventsFromAsset("data/events/relationship_events.json")
        val general = loadEventsFromAsset("data/events/general_events.json")
        starter + education + career + finance + relationship + general
    }

    private fun loadEventsFromAsset(path: String): List<LifeEvent> {
        val inputStream = context.assets.open(path)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        return json.decodeFromString<LifeEventList>(jsonString).events
    }

    fun getEligibleEvents(
        age: Int,
        usedIds: Set<String>,
        character: Character? = null
    ): List<LifeEvent> {
        return allEvents.filter { event ->
            age in event.minAge..event.maxAge &&
                (ONE_TIME_TAG !in event.tags || event.id !in usedIds) &&
                EducationEngine.EXAM_SYSTEM_TAG !in event.tags &&
                CareerEngine.CAREER_SYSTEM_TAG !in event.tags &&
                RelocationEngine.RELOCATION_SYSTEM_TAG !in event.tags &&
                passesFinanceGate(event, character) &&
                passesRelationshipGate(event, character) &&
                passesCountryGate(event, character) &&
                passesRelocationGate(event, character)
        }.map { event -> resolveFlavorForCharacter(event, character) }
    }

    private fun resolveFlavorForCharacter(event: LifeEvent, character: Character?): LifeEvent {
        if (character == null) return event
        return FlavorInterpolator.resolveEvent(event, character.countryCode)
    }

    private fun passesRelocationGate(event: LifeEvent, character: Character?): Boolean {
        if (RelocationEngine.REQUIRES_RELOCATION_TAG !in event.tags) return true
        if (character == null) return false
        return character.birthCountryCode != character.countryCode
    }

    private fun passesCountryGate(event: LifeEvent, character: Character?): Boolean {
        val restriction = event.restrictedToCountry ?: return true
        if (character == null) return false
        return character.countryCode == restriction
    }

    private fun passesRelationshipGate(event: LifeEvent, character: Character?): Boolean {
        val requirementTags = setOf(
            RelationshipEngine.REQUIRES_SPOUSE_TAG,
            RelationshipEngine.REQUIRES_MARRIED_TAG,
            RelationshipEngine.REQUIRES_CHILD_TAG,
            RelationshipEngine.REQUIRES_PARENT_TAG,
            RelationshipEngine.REQUIRES_CHILD_SCHOOL_AGE_TAG,
            RelationshipEngine.REQUIRES_SINGLE_TAG
        )
        val hasRequirements = event.tags.any { it in requirementTags }
        if (RelationshipEngine.RELATIONSHIP_TAG !in event.tags && !hasRequirements) return true
        if (character == null) return false

        if (RelationshipEngine.REQUIRES_SPOUSE_TAG in event.tags && !character.hasSpouse()) {
            return false
        }
        if (RelationshipEngine.REQUIRES_MARRIED_TAG in event.tags && !character.isMarried()) {
            return false
        }
        if (RelationshipEngine.REQUIRES_CHILD_TAG in event.tags && !character.hasChild()) {
            return false
        }
        if (RelationshipEngine.REQUIRES_PARENT_TAG in event.tags) {
            val hasParent = character.family.any {
                it.relation == RelationType.MOTHER || it.relation == RelationType.FATHER
            }
            if (!hasParent) return false
        }
        if (RelationshipEngine.REQUIRES_CHILD_SCHOOL_AGE_TAG in event.tags) {
            val schoolAgeChild = character.family.any {
                it.relation == RelationType.CHILD && it.age in CHILD_SCHOOL_MIN_AGE..CHILD_SCHOOL_MAX_AGE
            }
            if (!schoolAgeChild) return false
        }
        if (RelationshipEngine.REQUIRES_SINGLE_TAG in event.tags && character.hasSpouse()) {
            return false
        }
        return true
    }

    private fun passesFinanceGate(event: LifeEvent, character: Character?): Boolean {
        if (FinanceEngine.FINANCE_TAG !in event.tags) return true
        if (character == null) return false
        return financeEngine.meetsFinanceEventThreshold(character)
    }

    fun pickRandomEvent(eligible: List<LifeEvent>): LifeEvent? {
        if (eligible.isEmpty()) return null
        val totalWeight = eligible.sumOf { it.weight }
        if (totalWeight <= 0) return eligible.random()
        var roll = Random.nextInt(totalWeight)
        for (event in eligible) {
            roll -= event.weight
            if (roll < 0) return event
        }
        return eligible.last()
    }

    companion object {
        const val ONE_TIME_TAG = "one_time"
        const val STUDY_EFFORT_TAG = "study_effort"
        const val WORK_EFFORT_TAG = "work_effort"
        private const val CHILD_SCHOOL_MIN_AGE = 5
        private const val CHILD_SCHOOL_MAX_AGE = 7
    }
}
