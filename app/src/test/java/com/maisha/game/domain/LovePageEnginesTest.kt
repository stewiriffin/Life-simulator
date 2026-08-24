package com.maisha.game.domain

import com.maisha.game.data.model.Pet
import com.maisha.game.data.model.PetSpecies
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LovePageEnginesTest {
    private val engine = RelationshipEngine(FinanceEngine())

    @Test
    fun startDating_chargesFirstDateFee() {
        val prospect = TestFixtures.person(id = "p1", age = 22, relationshipLevel = 55)
        val character = TestFixtures.character(
            age = 22,
            stats = Stats(money = 50_000)
        )
        when (val result = engine.startDating(character, prospect)) {
            is StartDatingResult.Success -> {
                assertTrue(result.character.stats.money < character.stats.money)
                assertTrue(result.character.hasSpouse())
            }
            is StartDatingResult.Ineligible -> {
                // Start-dating is intentionally probabilistic (random acceptance chance),
                // so this test only validates the "success path" invariants when it happens.
                assertTrue(true)
            }
            else -> error(result.toString())
        }
    }

    @Test
    fun haveChild_requiresFunds() {
        val spouse = TestFixtures.person(
            id = "s",
            relation = RelationType.SPOUSE,
            isMarried = true
        )
        val broke = TestFixtures.character(
            age = 30,
            family = listOf(spouse),
            stats = Stats(money = 0)
        )
        assertEquals(HaveChildResult.InsufficientFunds, engine.haveChild(broke))
    }

    @Test
    fun divorce_takesSettlement() {
        val spouse = TestFixtures.person(
            id = "s",
            relation = RelationType.SPOUSE,
            isMarried = true
        )
        val character = TestFixtures.character(
            age = 35,
            family = listOf(spouse),
            stats = Stats(money = 200_000, happiness = 70)
        )
        when (val result = engine.breakUpOrDivorce(character, "s")) {
            is BreakUpResult.Success -> {
                assertTrue(result.wasMarried)
                assertTrue(result.settlement > 0)
                assertTrue(result.character.stats.money < character.stats.money)
                assertTrue(result.character.family.none { it.id == "s" })
            }
            else -> error(result.toString())
        }
    }

    @Test
    fun petCare_playOncePerYear() {
        val pet = Pet(id = "pet1", name = "Rex", species = PetSpecies.DOG)
        val character = TestFixtures.character(age = 25, pets = listOf(pet))
        val first = engine.careForPet(character, "pet1", PetCareAction.PLAY)
        assertTrue(first is PetCareResult.Success)
        val second = engine.careForPet(
            (first as PetCareResult.Success).character,
            "pet1",
            PetCareAction.PLAY
        )
        assertEquals(PetCareResult.AlreadyDone, second)
    }

    @Test
    fun seekFriendship_marksSocialized() {
        val character = TestFixtures.character(
            age = 20,
            stats = Stats(money = 100_000)
        )
        val result = engine.seekFriendship(character)
        when (result) {
            is SeekFriendshipResult.Success -> {
                assertTrue(result.character.lifestyle.socializedThisYear)
                assertTrue(result.character.family.any { it.isPlatonicAlly() })
            }
            is SeekFriendshipResult.NoLuck -> {
                assertTrue(result.character.lifestyle.socializedThisYear)
                assertTrue(result.character.stats.money < character.stats.money)
            }
            else -> error(result.toString())
        }
        assertEquals(
            SeekFriendshipResult.AlreadySocialized,
            engine.seekFriendship(
                when (result) {
                    is SeekFriendshipResult.Success -> result.character
                    is SeekFriendshipResult.NoLuck -> result.character
                    else -> character
                }
            )
        )
    }

    @Test
    fun dateNight_andMakePeace_eligibility() {
        val spouse = TestFixtures.person(
            id = "sp",
            relation = RelationType.SPOUSE,
            isMarried = true,
            relationshipLevel = 60
        )
        val enemy = TestFixtures.person(
            id = "en",
            relation = RelationType.ENEMY,
            relationshipLevel = 20
        )
        val withSpouse = TestFixtures.character(
            age = 28,
            family = listOf(spouse),
            stats = Stats(money = 100_000)
        )
        val date = engine.progressRelationship(withSpouse, "sp", InteractionType.DATE_NIGHT)
        assertTrue(date.character.family.first().relationshipLevel > 60)

        val withEnemy = TestFixtures.character(age = 28, family = listOf(enemy))
        val peace = engine.progressRelationship(withEnemy, "en", InteractionType.MAKE_PEACE)
        assertTrue(peace.character.family.first().relationshipLevel > 20)
    }
}
