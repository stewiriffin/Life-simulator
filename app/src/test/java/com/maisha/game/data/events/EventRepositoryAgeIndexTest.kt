package com.maisha.game.data.events

import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.domain.FinanceEngine
import com.maisha.game.domain.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class EventRepositoryAgeIndexTest {

    @Test
    fun ageIndex_returnsSameEligibleSetAsFlatFilter() {
        val events = listOf(
            event("child", minAge = 0, maxAge = 12),
            event("teen", minAge = 13, maxAge = 19),
            event("adult", minAge = 20, maxAge = 60),
            event("wide", minAge = 5, maxAge = 80),
            event("one_time", minAge = 18, maxAge = 99, tags = listOf(EventRepository.ONE_TIME_TAG))
        )
        val repository = EventRepository.forTesting(FinanceEngine(), events)
        val character = TestFixtures.character(age = 25)

        for (age in 0..90) {
            val eligible = repository.getEligibleEvents(age, emptySet(), character)
            val expected = events.filter { age in it.minAge..it.maxAge }
                .filter { event ->
                    EventRepository.ONE_TIME_TAG !in event.tags || event.id !in emptySet<String>()
                }
            assertEquals("age $age", expected.map { it.id }.toSet(), eligible.map { it.id }.toSet())
        }
    }

    private fun event(
        id: String,
        minAge: Int,
        maxAge: Int,
        tags: List<String> = emptyList()
    ): LifeEvent = LifeEvent(
        id = id,
        minAge = minAge,
        maxAge = maxAge,
        text = id,
        choices = listOf(EventChoice(label = "OK", resultText = "Done")),
        tags = tags,
        weight = 1
    )
}
