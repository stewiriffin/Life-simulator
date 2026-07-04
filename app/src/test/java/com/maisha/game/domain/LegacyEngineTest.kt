package com.maisha.game.domain

import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.HealthCondition
import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.MilestoneKind
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyEngineTest {

    private val engine = LegacyEngine(MortalityEngine(), FinanceEngine())

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
        val heir = TestFixtures.child("heir", age = 20, relationship = 75)
        val siblingA = TestFixtures.child("sibling-a", age = 18, relationship = 82)
        val siblingB = TestFixtures.child("sibling-b", age = 16, relationship = 60)
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
        val heir = TestFixtures.child(
            id = "heir",
            age = 22,
            relationship = 70,
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
        val heir = TestFixtures.child("heir", age = 20, relationship = 70)
        val sibling = TestFixtures.child("sibling", age = 18, relationship = 65)
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
    fun createLegacyCharacter_deductsEstateTaxesBeforeSplittingInheritance() {
        val heir = TestFixtures.child("heir", age = 20, relationship = 70)
        val sibling = TestFixtures.child("sibling", age = 18, relationship = 65)
        val deceased = Character(
            name = "Parent",
            age = 60,
            gender = Gender.MALE,
            stats = Stats(money = 200_000),
            birthYear = 1965,
            alive = false,
            activeConditions = listOf(
                HealthCondition(id = "h1", name = "Chronic illness", severity = 3, treated = false)
            ),
            criminalRecord = CriminalRecord(hasRecord = true, timesArrested = 2),
            family = listOf(heir, sibling)
        )

        val legacy = engine.createLegacyCharacter(deceased, heir)
        assertTrue("Estate fees should reduce the heir's share", legacy.stats.money < 100_000)
        assertTrue("Heir should never inherit negative cash", legacy.stats.money >= 0)
        assertTrue(
            legacy.eventLog.any { it.contains("Estate settlement", ignoreCase = true) }
        )
    }

    @Test
    fun `eligible heirs require minimum age`() {
        val young = TestFixtures.child("young", age = 10, relationship = 80)
        val oldEnough = TestFixtures.child("adult", age = 18, relationship = 80)
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

    @Test
    fun createLegacyCharacter_transfersHeirloomsWithoutLiquidating() {
        val heirloom = TestFixtures.asset(
            id = "watch",
            name = "18th Century Pocket Watch",
            currentValue = 300_000,
            monthlyUpkeep = 0,
            type = AssetType.HEIRLOOM,
            isHeirloom = true,
            generationAcquired = 1
        )
        val standard = TestFixtures.asset(
            id = "car",
            currentValue = 200_000,
            type = AssetType.CAR
        )
        val heir = TestFixtures.child("heir", age = 20, relationship = 70)
        val deceased = Character(
            name = "Parent",
            age = 60,
            gender = Gender.MALE,
            stats = Stats(money = 100_000),
            birthYear = 1965,
            alive = false,
            generationNumber = 2,
            assets = listOf(heirloom, standard),
            family = listOf(heir)
        )

        val legacy = engine.createLegacyCharacter(deceased, heir)
        assertEquals(1, legacy.assets.size)
        assertTrue(legacy.assets.first().isHeirloom)
        assertEquals("watch", legacy.assets.first().id)
        assertEquals(300_000, legacy.assets.first().currentValue)
        assertTrue(
            "Standard asset value should be liquidated into inherited cash",
            legacy.stats.money > 100_000
        )
        assertTrue(
            legacy.eventLog.any { it.contains("heirloom", ignoreCase = true) }
        )
    }

    @Test
    fun createLegacyCharacter_distributesEstateAccordingToCustomWill() {
        val heir = TestFixtures.child("heir", age = 22, relationship = 70)
        val sibling = TestFixtures.child("sibling", age = 20, relationship = 65)
        val spouse = Person(
            id = "spouse",
            name = "Spouse",
            relation = RelationType.SPOUSE,
            gender = Gender.FEMALE,
            age = 50,
            isMarried = true
        )
        val deceased = Character(
            name = "Parent",
            age = 60,
            gender = Gender.MALE,
            stats = Stats(money = 200_000),
            birthYear = 1965,
            alive = false,
            family = listOf(heir, sibling, spouse),
            will = mapOf(
                "heir" to 70,
                "sibling" to 20,
                "spouse" to 10
            )
        )

        val legacy = engine.createLegacyCharacter(deceased, heir)
        assertEquals(140_000, legacy.stats.money)
        assertTrue(legacy.eventLog.any { it.contains("will", ignoreCase = true) })
    }

    @Test
    fun createLegacyCharacter_appliesGrudgeToChildExcludedFromWill() {
        val heir = TestFixtures.child("heir", age = 22, relationship = 70)
        val excluded = TestFixtures.child("excluded", age = 20, relationship = 80)
        val deceased = Character(
            name = "Parent",
            age = 60,
            gender = Gender.MALE,
            stats = Stats(money = 100_000),
            birthYear = 1965,
            alive = false,
            family = listOf(heir, excluded),
            will = mapOf(
                "heir" to 100,
                "excluded" to 0
            )
        )

        val legacy = engine.createLegacyCharacter(deceased, heir)
        assertEquals(100_000, legacy.stats.money)
        val sibling = legacy.family.first { it.id == "excluded" }
        assertEquals(RelationType.SIBLING, sibling.relation)
        assertEquals(80 - LegacyEngine.GRUDGE_RELATIONSHIP_PENALTY, sibling.relationshipLevel)
        assertTrue(sibling.milestones.any { it.kind == MilestoneKind.GRUDGE.name })
        assertTrue(legacy.eventLog.any { it.contains("grudge", ignoreCase = true) })
    }
}
