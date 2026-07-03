// app/src/test/java/com/maisha/game/domain/FriendshipGenerationTest.kt (new — Prompt 26)
package com.maisha.game.domain

import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FriendshipGenerationTest {

    private val engine = RelationshipEngine(FinanceEngine())

    @Test
    fun tooYoung_returnsNull() {
        val character = baseCharacter(age = 5)
        assertNull(engine.generateFriendshipOpportunity(character))
    }

    @Test
    fun atMaxFriends_returnsNull() {
        val friends = (1..4).map { index ->
            com.maisha.game.data.model.Person(
                id = "f$index",
                name = "Friend $index",
                relation = RelationType.FRIEND,
                age = 20
            )
        }
        val character = baseCharacter(age = 20).copy(family = friends)
        assertNull(engine.generateFriendshipOpportunity(character))
    }

    @Test
    fun schoolAge_canProduceFriend() {
        val character = baseCharacter(age = 12)
        var attempts = 0
        var found: com.maisha.game.data.model.Person? = null
        while (attempts < 5_000 && found == null) {
            found = engine.generateFriendshipOpportunity(character)
            attempts++
        }
        assertNotNull("Expected friend within 5000 rolls at 10% chance", found)
        assertEquals(RelationType.FRIEND, found!!.relation)
    }

    private fun baseCharacter(age: Int) = Character(
        name = "Player",
        gender = Gender.MALE,
        birthYear = 2000,
        age = age,
        stats = Stats()
    )
}
