package com.maisha.game.domain

import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.RelationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationshipEngineTest {

    private val engine = RelationshipEngine()

    @Test
    fun spendTime_increasesRelationshipAndHappiness() {
        val member = TestFixtures.person(id = "s1", relationshipLevel = 50)
        val character = TestFixtures.character(family = listOf(member))
        val result = engine.progressRelationship(character, "s1", InteractionType.SPEND_TIME)
        assertEquals(60, result.character.family.first().relationshipLevel)
        assertEquals(55, result.character.stats.happiness)
    }

    @Test
    fun argue_decreasesRelationshipAndHappiness() {
        val member = TestFixtures.person(id = "s1", relationshipLevel = 50)
        val character = TestFixtures.character(
            stats = com.maisha.game.data.model.Stats(happiness = 50),
            family = listOf(member)
        )
        val result = engine.progressRelationship(character, "s1", InteractionType.ARGUE)
        assertEquals(40, result.character.family.first().relationshipLevel)
        assertEquals(45, result.character.stats.happiness)
    }

    @Test
    fun compliment_increasesRelationship() {
        val member = TestFixtures.person(id = "s1", relationshipLevel = 55)
        val character = TestFixtures.character(family = listOf(member))
        val result = engine.progressRelationship(character, "s1", InteractionType.COMPLIMENT)
        assertTrue(result.character.family.first().relationshipLevel > 55)
    }

    @Test
    fun tickFamilyYear_decaysNeglectedRelationships() {
        val neglected = TestFixtures.person(
            id = "s1",
            relationshipLevel = 70,
            interactedThisYear = false
        )
        val recent = TestFixtures.person(
            id = "s2",
            relationshipLevel = 70,
            interactedThisYear = true
        )
        val character = TestFixtures.character(family = listOf(neglected, recent))
        val result = engine.tickFamilyYear(character)
        assertEquals(69, result.family.first { it.id == "s1" }.relationshipLevel)
        assertEquals(70, result.family.first { it.id == "s2" }.relationshipLevel)
    }

    @Test
    fun proposeMarriage_rejectsBelowThreshold() {
        val partner = TestFixtures.person(
            id = "spouse",
            relation = RelationType.SPOUSE,
            relationshipLevel = 69
        )
        val character = TestFixtures.character(family = listOf(partner))
        val (_, result) = engine.proposeMarriage(character, "spouse")
        assertTrue(result is ProposalResult.Rejected)
    }

    @Test
    fun mixedHeritageChild_setsSecondaryCountryWhenParentsDiffer() {
        val spouse = TestFixtures.person(
            id = "spouse",
            relation = RelationType.SPOUSE,
            isMarried = true,
            countryCode = "NG",
            gender = Gender.FEMALE
        )
        val character = TestFixtures.character(
            age = 30,
            countryCode = "KE",
            family = listOf(spouse)
        )
        val child = engine.haveChild(character).family.first { it.relation == RelationType.CHILD }
        assertEquals("KE", child.countryCode)
        assertEquals("NG", child.secondaryCountryCode)
    }
}
