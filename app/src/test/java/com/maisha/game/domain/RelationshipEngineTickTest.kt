// app/src/test/java/com/maisha/game/domain/RelationshipEngineTickTest.kt (new)
package com.maisha.game.domain

import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.RelationshipTier
import com.maisha.game.data.model.Stats
import com.maisha.game.data.model.relationshipTierFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationshipEngineTickTest {

    private val engine = RelationshipEngine(FinanceEngine())

    @Test
    fun neglectedSibling_decaysTowardNeutral() {
        val sibling = Person(
            id = "s1",
            name = "Sam",
            relation = RelationType.SIBLING,
            age = 20,
            relationshipLevel = 70,
            interactedThisYear = false
        )
        val character = Character(
            name = "Player",
            gender = Gender.MALE,
            birthYear = 2000,
            age = 25,
            stats = Stats(),
            family = listOf(sibling)
        )
        val result = engine.tickFamilyYear(character)
        assertEquals(69, result.family.first().relationshipLevel)
    }

    @Test
    fun neglectedSpouse_decaysSlower() {
        val spouse = Person(
            id = "p1",
            name = "Pat",
            relation = RelationType.SPOUSE,
            age = 25,
            relationshipLevel = 70,
            interactedThisYear = false
        )
        val character = Character(
            name = "Player",
            gender = Gender.MALE,
            birthYear = 2000,
            age = 25,
            stats = Stats(),
            family = listOf(spouse)
        )
        val result = engine.tickFamilyYear(character)
        assertEquals(69, result.family.first().relationshipLevel)
    }

    @Test
    fun tierDrop_emitsDecayNotice() {
        val sibling = Person(
            id = "s1",
            name = "Sam",
            relation = RelationType.SIBLING,
            age = 20,
            relationshipLevel = 68,
            interactedThisYear = false
        )
        val character = Character(
            name = "Player",
            gender = Gender.MALE,
            birthYear = 2000,
            age = 20,
            stats = Stats(),
            family = listOf(sibling)
        )
        val result = engine.tickFamilyYear(character)
        assertTrue(result.decayNotices.isNotEmpty())
        assertEquals(RelationshipTier.FRIENDLY, relationshipTierFor(result.family.first().relationshipLevel))
    }
}
