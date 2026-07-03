package com.maisha.game.domain

import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyAncestryTest {

    private val mortalityEngine = MortalityEngine()
    private val engine = LegacyEngine(mortalityEngine, FinanceEngine())

    @Test
    fun `createLegacyCharacter appends deceased to ancestry history`() {
        val heir = Person(
            id = "heir",
            name = "Child Heir",
            relation = RelationType.CHILD,
            gender = Gender.MALE,
            age = 20,
            countryCode = "KE"
        )
        val deceased = Character(
            name = "Grandparent",
            age = 72,
            gender = Gender.MALE,
            stats = Stats(money = 40_000),
            birthYear = 1950,
            alive = false,
            countryCode = "CA",
            birthCountryCode = "KE",
            relocationHistory = listOf("CA"),
            generationNumber = 2,
            ancestryHistory = listOf(
                com.maisha.game.data.model.AncestryEntry(
                    generationNumber = 1,
                    characterName = "Founder",
                    countryCode = "KE",
                    relocatedTo = emptyList(),
                    ageAtDeath = 80,
                    cause = "Passed away peacefully in old age"
                )
            ),
            eventLog = listOf("::DEATH:OLD_AGE::Passed away peacefully at home in your sleep."),
            family = listOf(heir)
        )

        val legacy = engine.createLegacyCharacter(deceased, heir)

        assertEquals(3, legacy.generationNumber)
        assertEquals(2, legacy.ancestryHistory.size)
        assertEquals("Founder", legacy.ancestryHistory[0].characterName)
        assertEquals("Grandparent", legacy.ancestryHistory[1].characterName)
        assertEquals(listOf("CA"), legacy.ancestryHistory[1].relocatedTo)
        assertEquals(72, legacy.ancestryHistory[1].ageAtDeath)
        assertTrue(legacy.ancestryHistory[1].cause!!.contains("old age", ignoreCase = true))
    }
}
