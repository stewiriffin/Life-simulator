package com.maisha.game.domain

import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.PetSpecies
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.Stats
import com.maisha.game.domain.AdoptPetResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationshipEngineTest {

    private val engine = RelationshipEngine(FinanceEngine())

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

    @Test
    fun applySpouseRelationshipEffect_modifiesLevelBasedOnCharacterState() {
        val spouse = TestFixtures.person(
            id = "sp",
            relation = RelationType.SPOUSE,
            relationshipLevel = 60,
            isMarried = true
        )
        val base = TestFixtures.character(
            family = listOf(spouse),
            stats = com.maisha.game.data.model.Stats(happiness = 75, money = 250_000)
        )

        val prosperous = engine.applySpouseRelationshipEffect(base, netWorth = 250_000)
        assertEquals(65, prosperous.family.first().relationshipLevel)

        val struggling = engine.applySpouseRelationshipEffect(
            base.copy(stats = base.stats.copy(happiness = 50, money = 5_000)),
            netWorth = 5_000
        )
        assertEquals(52, struggling.family.first().relationshipLevel)

        val jailed = engine.applySpouseRelationshipEffect(
            base.copy(
                criminalRecord = CriminalRecord(currentlyIncarcerated = true, yearsRemaining = 2)
            ),
            netWorth = 250_000
        )
        assertEquals(45, jailed.family.first().relationshipLevel)

        val eventBoost = engine.applySpouseRelationshipEffect(base, delta = 10)
        assertEquals(70, eventBoost.family.first().relationshipLevel)
    }

    @Test
    fun adoptPet_addsPetToCharacterAndDeductsCash() {
        val character = TestFixtures.character(
            age = 25,
            stats = Stats(money = 100_000)
        )
        when (val result = engine.adoptPet(character, PetSpecies.CAT, "Whiskers")) {
            is AdoptPetResult.Success -> {
                assertEquals(1, result.character.pets.size)
                assertEquals("Whiskers", result.character.pets.first().name)
                assertTrue(result.character.stats.money < 100_000)
            }
            else -> error("Expected adoption success")
        }
    }

    @Test
    fun interactWithFamilyMember_payAllowanceIncreasesRelationshipAndDrainsCash() {
        val child = TestFixtures.person(
            id = "child1",
            relation = RelationType.CHILD,
            age = 10,
            relationshipLevel = 50
        )
        val character = TestFixtures.character(
            age = 35,
            stats = Stats(money = 50_000, happiness = 60),
            family = listOf(child)
        )
        val cost = engine.allowanceCost(character)
        val result = engine.progressRelationship(
            character,
            "child1",
            InteractionType.PAY_ALLOWANCE
        )
        assertEquals(50_000 - cost, result.character.stats.money)
        assertTrue(result.character.family.first().relationshipLevel > 50)
        assertTrue(result.character.family.first().interactedThisYear)
    }

    @Test
    fun tickFamilyYear_deductsChildSupportForChildrenUnderEighteen() {
        val minor = TestFixtures.person(
            id = "kid",
            relation = RelationType.CHILD,
            age = 8,
            relationshipLevel = 60
        )
        val adultChild = TestFixtures.person(
            id = "adult_kid",
            relation = RelationType.CHILD,
            age = 20,
            relationshipLevel = 60
        )
        val withMinor = TestFixtures.character(
            age = 40,
            stats = Stats(money = 100_000, happiness = 70),
            family = listOf(minor)
        )
        val withAdultOnly = TestFixtures.character(
            age = 40,
            stats = Stats(money = 100_000, happiness = 70),
            family = listOf(adultChild)
        )
        val minorTick = engine.tickFamilyYear(withMinor)
        val adultTick = engine.tickFamilyYear(withAdultOnly)
        assertTrue(minorTick.stats.money < 100_000)
        assertEquals(100_000, adultTick.stats.money)
        assertEquals(9, minorTick.family.first().age)
    }

    @Test
    fun interactWithFamilyMember_failsIfParentingActionUsedOnAdultOrNonChild() {
        val adultChild = TestFixtures.person(
            id = "adult",
            relation = RelationType.CHILD,
            age = 19,
            relationshipLevel = 50
        )
        val sibling = TestFixtures.person(
            id = "sib",
            relation = RelationType.SIBLING,
            age = 12,
            relationshipLevel = 50
        )
        val character = TestFixtures.character(
            age = 40,
            stats = Stats(money = 50_000),
            family = listOf(adultChild, sibling)
        )
        val onAdult = engine.progressRelationship(
            character,
            "adult",
            InteractionType.PAY_ALLOWANCE
        )
        val onSibling = engine.progressRelationship(
            character,
            "sib",
            InteractionType.HELP_WITH_HOMEWORK
        )
        assertEquals(50_000, onAdult.character.stats.money)
        assertEquals(50, onAdult.character.family.first { it.id == "adult" }.relationshipLevel)
        assertEquals(50, onSibling.character.family.first { it.id == "sib" }.relationshipLevel)
        assertTrue(onAdult.message.contains("under 18"))
        assertTrue(onSibling.message.contains("under 18"))
    }

    @Test
    fun statusShift_upgradesFriendToBestFriendAtHighRelationship() {
        val friend = TestFixtures.person(
            id = "f1",
            relation = RelationType.FRIEND,
            relationshipLevel = 91
        )
        val upgraded = engine.applySocialStatusShift(friend)
        assertEquals(RelationType.BEST_FRIEND, upgraded.relation)

        val coldFriend = TestFixtures.person(
            id = "f2",
            relation = RelationType.FRIEND,
            relationshipLevel = 10
        )
        val enemy = engine.applySocialStatusShift(coldFriend)
        assertEquals(RelationType.ENEMY, enemy.relation)
    }

    @Test
    fun throwParty_boostsRelationshipForAllFriendsBasedOnBudget() {
        val friend = TestFixtures.person(
            id = "f1",
            relation = RelationType.FRIEND,
            relationshipLevel = 50
        )
        val bestFriend = TestFixtures.person(
            id = "f2",
            relation = RelationType.BEST_FRIEND,
            relationshipLevel = 92
        )
        val sibling = TestFixtures.person(
            id = "s1",
            relation = RelationType.SIBLING,
            relationshipLevel = 40
        )
        val parent = TestFixtures.person(
            id = "m1",
            relation = RelationType.MOTHER,
            relationshipLevel = 60
        )
        val character = TestFixtures.character(
            stats = Stats(money = 200_000, happiness = 50),
            family = listOf(friend, bestFriend, sibling, parent)
        )
        val smallBudget = engine.minPartyBudget(character.countryCode)
        val largeBudget = smallBudget * 5
        val smallBoost = engine.partyBoostForBudget(smallBudget, character.countryCode)
        val largeBoost = engine.partyBoostForBudget(largeBudget, character.countryCode)
        assertTrue(largeBoost > smallBoost)
        assertTrue(smallBoost >= RelationshipEngine.PARTY_BOOST_MIN)
        assertTrue(largeBoost <= RelationshipEngine.PARTY_BOOST_MAX)

        val result = engine.throwParty(character, largeBudget)
        assertTrue(result is PartyResult.Success)
        val after = (result as PartyResult.Success).character
        assertEquals(200_000 - largeBudget, after.stats.money)
        assertEquals(
            (50 + result.boost).coerceAtMost(100),
            after.family.first { it.id == "f1" }.relationshipLevel
        )
        assertEquals(
            (92 + result.boost).coerceAtMost(100),
            after.family.first { it.id == "f2" }.relationshipLevel
        )
        assertEquals(
            (40 + result.boost).coerceAtMost(100),
            after.family.first { it.id == "s1" }.relationshipLevel
        )
        assertEquals(60, after.family.first { it.id == "m1" }.relationshipLevel)
    }
}
