package com.maisha.game.domain

import com.maisha.game.data.model.AncestryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class AncestryHistoryCapTest {

    @Test
    fun trim_keepsEarliestGenerationsUpToMax() {
        val history = (1..30).map { gen ->
            AncestryEntry(
                generationNumber = gen,
                characterName = "Gen $gen",
                countryCode = "KE",
                ageAtDeath = 80,
                cause = "OLD_AGE"
            )
        }
        val trimmed = AncestryHistoryCap.trim(history)
        assertEquals(AncestryHistoryCap.MAX_ENTRIES, trimmed.size)
        assertEquals(1, trimmed.first().generationNumber)
        assertEquals(AncestryHistoryCap.MAX_ENTRIES, trimmed.last().generationNumber)
    }
}
