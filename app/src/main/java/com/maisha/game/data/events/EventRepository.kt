// app/src/main/java/com/maisha/game/data/events/EventRepository.kt
package com.maisha.game.data.events

import android.content.Context
import com.maisha.game.data.CountryCatalog
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
import com.maisha.game.domain.hasMixedHeritageChild
import com.maisha.game.domain.hasMixedHeritageContext
import com.maisha.game.domain.hasSpouse
import com.maisha.game.domain.isMarried
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class EventRepository private constructor(
    private val financeEngine: FinanceEngine,
    private val assetContext: Context?,
    private val testEvents: List<LifeEvent>?
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        financeEngine: FinanceEngine
    ) : this(financeEngine, context, null)

    private val json = Json { ignoreUnknownKeys = true }

    private val allEvents: List<LifeEvent> by lazy {
        when {
            testEvents != null -> testEvents
            assetContext != null -> loadAllFromAssets(assetContext)
            else -> emptyList()
        }
    }

    /** Pre-indexed by age so [getEligibleEvents] skips scanning the full pool each age-up. */
    private val eventsByAge: Array<List<LifeEvent>> by lazy {
        buildAgeIndex(allEvents)
    }

    private fun buildAgeIndex(events: List<LifeEvent>): Array<List<LifeEvent>> {
        if (events.isEmpty()) return arrayOf(emptyList())
        val maxAge = events.maxOf { it.maxAge }.coerceAtLeast(DEFAULT_MAX_AGE)
        return Array(maxAge + 1) { age ->
            events.filter { age in it.minAge..it.maxAge }
        }
    }

    private fun loadAllFromAssets(context: Context): List<LifeEvent> {
        val starter = loadEventsFromAsset(context, "data/events/starter_events.json")
        val education = loadEventsFromAsset(context, "data/events/education_events.json")
        val career = loadEventsFromAsset(context, "data/events/career_events.json")
        val finance = loadEventsFromAsset(context, "data/events/finance_events.json")
        val relationship = loadEventsFromAsset(context, "data/events/relationship_events.json")
        val general = loadEventsFromAsset(context, "data/events/general_events.json")
        val holidays = loadEventsFromAsset(context, "data/events/holiday_events.json")
        return starter + education + career + finance + relationship + general + holidays
    }

    private fun loadEventsFromAsset(context: Context, path: String): List<LifeEvent> {
        val inputStream = context.assets.open(path)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        return json.decodeFromString<LifeEventList>(jsonString).events
    }

    fun getEligibleEvents(
        age: Int,
        usedIds: Set<String>,
        character: Character? = null
    ): List<LifeEvent> {
        val ageCandidates = eventsByAge.getOrElse(age) {
            allEvents.filter { age in it.minAge..it.maxAge }
        }
        return ageCandidates.filter { event ->
            (ONE_TIME_TAG !in event.tags || event.id !in usedIds) &&
                EducationEngine.EXAM_SYSTEM_TAG !in event.tags &&
                CareerEngine.CAREER_SYSTEM_TAG !in event.tags &&
                RelocationEngine.RELOCATION_SYSTEM_TAG !in event.tags &&
                passesFinanceGate(event, character) &&
                passesRelationshipGate(event, character) &&
                passesCountryGate(event, character) &&
                passesRelocationGate(event, character) &&
                passesHolidayGate(event, character) &&
                passesMixedHeritageGate(event, character)
        }.mapNotNull { event -> resolveFlavorForCharacter(event, character) }
    }

    private fun resolveFlavorForCharacter(event: LifeEvent, character: Character?): LifeEvent? {
        if (character == null) return event
        if (FlavorInterpolator.HOLIDAY_TAG in event.tags) {
            return FlavorInterpolator.resolveHolidayEvent(event, character.countryCode)
        }
        return FlavorInterpolator.resolveEvent(event, character.countryCode)
    }

    private fun passesHolidayGate(event: LifeEvent, character: Character?): Boolean {
        if (FlavorInterpolator.HOLIDAY_TAG !in event.tags) return true
        if (character == null) return false
        if (!CountryCatalog.hasHolidayEvents(character.countryCode)) return false
        val lastAge = character.lastHolidayAge ?: return true
        return character.age - lastAge >= HOLIDAY_COOLDOWN_YEARS
    }

    private fun passesMixedHeritageGate(event: LifeEvent, character: Character?): Boolean {
        val needsMixed = RelationshipEngine.REQUIRES_MIXED_HERITAGE_TAG in event.tags
        val needsMixedChild = RelationshipEngine.REQUIRES_MIXED_HERITAGE_CHILD_TAG in event.tags
        if (!needsMixed && !needsMixedChild) return true
        if (character == null) return false
        if (needsMixed && !character.hasMixedHeritageContext()) return false
        if (needsMixedChild && !character.hasMixedHeritageChild()) return false
        return true
    }

    private fun passesRelocationGate(event: LifeEvent, character: Character?): Boolean {
        if (RelocationEngine.REQUIRES_RELOCATION_TAG !in event.tags) return true
        if (character == null) return false
        return character.relocationCount > 0 ||
            character.birthCountryCode != character.countryCode
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
        fun forTesting(
            financeEngine: FinanceEngine = FinanceEngine(),
            events: List<LifeEvent> = emptyList()
        ): EventRepository = EventRepository(financeEngine, null, events)

        const val ONE_TIME_TAG = "one_time"
        const val STUDY_EFFORT_TAG = "study_effort"
        const val WORK_EFFORT_TAG = "work_effort"
        private const val CHILD_SCHOOL_MIN_AGE = 5
        private const val CHILD_SCHOOL_MAX_AGE = 7
        private const val HOLIDAY_COOLDOWN_YEARS = 3
        private const val DEFAULT_MAX_AGE = 120
    }
}
