package com.maisha.game.data.events

import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.domain.FinanceEngine
import com.maisha.game.domain.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventRepositoryFilterTest {

    private val repository = EventRepository.forTesting(FinanceEngine())

    @Test
    fun getEligibleEvents_excludesCountryRestrictedEventForWrongCountry() {
        val events = listOf(
            event("ke_only", restrictedToCountry = "KE"),
            event("global_event")
        )
        val repo = EventRepository.forTesting(FinanceEngine(), events)
        val ngCharacter = TestFixtures.character(age = 25, countryCode = "NG")

        val eligible = repo.getEligibleEvents(25, emptySet(), ngCharacter).map { it.id }

        assertFalse(eligible.contains("ke_only"))
        assertTrue(eligible.contains("global_event"))
    }

    @Test
    fun getEligibleEvents_includesCountryRestrictedEventForMatchingCountry() {
        val events = listOf(event("ke_only", restrictedToCountry = "KE"))
        val repo = EventRepository.forTesting(FinanceEngine(), events)
        val keCharacter = TestFixtures.character(age = 25, countryCode = "KE")

        val eligible = repo.getEligibleEvents(25, emptySet(), keCharacter).map { it.id }

        assertEquals(listOf("ke_only"), eligible)
    }

    @Test
    fun getEligibleEvents_excludesOneTimeEventsAlreadyTriggered() {
        val events = listOf(
            event("once", tags = listOf(EventRepository.ONE_TIME_TAG))
        )
        val repo = EventRepository.forTesting(FinanceEngine(), events)
        val character = TestFixtures.character(age = 30)

        val first = repo.getEligibleEvents(30, emptySet(), character)
        val second = repo.getEligibleEvents(30, setOf("once"), character)

        assertEquals(1, first.size)
        assertTrue(second.isEmpty())
    }

    @Test
    fun getEligibleEvents_excludesCareerSystemTaggedEvents() {
        val events = listOf(
            event("career_system_evt", tags = listOf(com.maisha.game.domain.CareerEngine.CAREER_SYSTEM_TAG)),
            event("player_career_evt", tags = listOf("career"))
        )
        val repo = EventRepository.forTesting(FinanceEngine(), events)
        val character = TestFixtures.character(age = 30)

        val eligible = repo.getEligibleEvents(30, emptySet(), character).map { it.id }

        assertFalse(eligible.contains("career_system_evt"))
        assertTrue(eligible.contains("player_career_evt"))
    }

    private fun event(
        id: String,
        restrictedToCountry: String? = null,
        tags: List<String> = emptyList()
    ): LifeEvent = LifeEvent(
        id = id,
        minAge = 18,
        maxAge = 99,
        text = id,
        choices = listOf(EventChoice(label = "OK", resultText = "Done")),
        tags = tags,
        weight = 1,
        restrictedToCountry = restrictedToCountry
    )
}
