package com.maisha.game.domain

import com.maisha.game.data.model.AvatarConfig
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

class LegacyEngineTest {

    private val engine = LegacyEngine()

    @Test
    fun `deceased father maps surviving spouse to mother`() {
        val spouse = Person(
            id = "spouse",
            name = "Mary",
            relation = RelationType.SPOUSE,
            gender = Gender.FEMALE,
            age = 45,
            isMarried = true
        )
        val parent = engine.mapSurvivingParent(Gender.MALE, listOf(spouse))
        assertNotNull(parent)
        assertEquals(RelationType.MOTHER, parent!!.relation)
        assertEquals("Mary", parent.name)
    }

    @Test
    fun `deceased mother maps surviving spouse to father`() {
        val spouse = Person(
            id = "spouse",
            name = "John",
            relation = RelationType.SPOUSE,
            gender = Gender.MALE,
            age = 48,
            isMarried = true
        )
        val parent = engine.mapSurvivingParent(Gender.FEMALE, listOf(spouse))
        assertNotNull(parent)
        assertEquals(RelationType.FATHER, parent!!.relation)
        assertEquals("John", parent.name)
    }

    @Test
    fun `no surviving spouse yields no parent`() {
        assertNull(engine.mapSurvivingParent(Gender.MALE, emptyList()))
    }

    @Test
    fun `non chosen siblings keep relationship level as siblings`() {
        val heir = child("heir", age = 20, relationship = 75)
        val siblingA = child("sibling-a", age = 18, relationship = 82)
        val siblingB = child("sibling-b", age = 16, relationship = 60)
        val deceased = Character(
            name = "Parent",
            age = 55,
            gender = Gender.MALE,
            stats = Stats(money = 300_000),
            birthYear = 1970,
            alive = false,
            family = listOf(heir, siblingA, siblingB)
        )

        val legacy = engine.createLegacyCharacter(deceased, heir)
        val siblings = legacy.family.filter { it.relation == RelationType.SIBLING }
        assertEquals(2, siblings.size)
        assertEquals(82, siblings.first { it.id == "sibling-a" }.relationshipLevel)
        assertEquals(60, siblings.first { it.id == "sibling-b" }.relationshipLevel)
    }

    @Test
    fun `heir preserves avatar and country`() {
        val avatar = AvatarConfig(skinTone = 5, hairStyle = 3, hairColor = 1, outfitColor = 2)
        val heir = child("heir", age = 22, relationship = 70).copy(
            avatarConfig = avatar,
            countryCode = "NG"
        )
        val deceased = Character(
            name = "Parent",
            age = 50,
            gender = Gender.FEMALE,
            stats = Stats(money = 50_000),
            birthYear = 1975,
            alive = false,
            family = listOf(heir)
        )

        val legacy = engine.createLegacyCharacter(deceased, heir)
        assertEquals(avatar, legacy.avatarConfig)
        assertEquals("NG", legacy.countryCode)
        assertEquals(2, legacy.generationNumber)
    }

    @Test
    fun `money inheritance splits evenly among living children`() {
        val heir = child("heir", age = 20, relationship = 70)
        val sibling = child("sibling", age = 18, relationship = 65)
        val deceased = Character(
            name = "Parent",
            age = 60,
            gender = Gender.MALE,
            stats = Stats(money = 200_000),
            birthYear = 1965,
            alive = false,
            family = listOf(heir, sibling)
        )

        val legacy = engine.createLegacyCharacter(deceased, heir)
        assertEquals(100_000, legacy.stats.money)
    }

    @Test
    fun `eligible heirs require minimum age`() {
        val young = child("young", age = 10, relationship = 80)
        val oldEnough = child("adult", age = 18, relationship = 80)
        val deceased = Character(
            name = "Parent",
            age = 40,
            gender = Gender.MALE,
            stats = Stats(),
            birthYear = 1985,
            alive = false,
            family = listOf(young, oldEnough)
        )

        val heirs = engine.eligibleHeirs(deceased)
        assertEquals(1, heirs.size)
        assertEquals("adult", heirs.first().id)
    }

    private fun child(id: String, age: Int, relationship: Int): Person = Person(
        id = id,
        name = id,
        relation = RelationType.CHILD,
        gender = Gender.MALE,
        age = age,
        relationshipLevel = relationship
    )
}
