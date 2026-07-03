package com.maisha.game.data.events

import com.maisha.game.domain.FinanceEngine
import org.junit.Assert.assertNull
import org.junit.Test

class EventRepositoryEdgeCaseTest {

    private val repository = EventRepository.forTesting(FinanceEngine())

    @Test
    fun pickRandomEvent_returnsNullForEmptyList() {
        assertNull(repository.pickRandomEvent(emptyList()))
    }
}
