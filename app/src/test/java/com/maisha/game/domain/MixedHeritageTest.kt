package com.maisha.game.domain

import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MixedHeritageTest {

    private val relationshipEngine = RelationshipEngine()

    @Test
    fun `cross-country spouse sets child secondary country and mixed name`() {
        val spouse = Person(
            id = "spouse",
            name = "Ada Okafor",
            relation = RelationType.SPOUSE,
            gender = Gender.FEMALE,
            age = 28,
            isMarried = true,
            countryCode = "NG"
        )
        val character = Character(
            name = "Test",
            age = 30,
            gender = Gender.MALE,
            stats = Stats(health = 80, happiness = 70, smarts = 60, looks = 50, money = 0),
            birthYear = 1995,
            countryCode = "KE",
            birthCountryCode = "KE",
            family = listOf(spouse)
        )

        val updated = relationshipEngine.haveChild(character)
        val child = updated.family.first { it.relation == RelationType.CHILD }
        assertEquals("KE", child.countryCode)
        assertEquals("NG", child.secondaryCountryCode)
        assertTrue(child.name.isNotBlank())
    }

    @Test
    fun `same-country spouses do not set secondary country`() {
        val spouse = Person(
            id = "spouse",
            name = "Jane",
            relation = RelationType.SPOUSE,
            gender = Gender.FEMALE,
            age = 28,
            isMarried = true,
            countryCode = "KE"
        )
        val character = Character(
            name = "Test",
            age = 30,
            gender = Gender.MALE,
            stats = Stats(health = 80, happiness = 70, smarts = 60, looks = 50, money = 0),
            birthYear = 1995,
            countryCode = "KE",
            family = listOf(spouse)
        )

        val child = relationshipEngine.haveChild(character).family.first { it.relation == RelationType.CHILD }
        assertNull(child.secondaryCountryCode)
    }
}
