// app/src/main/java/com/maisha/game/data/DatingPool.kt (modified — cross-country prospects)
package com.maisha.game.data

import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.Stats
import java.util.UUID
import kotlin.random.Random

object DatingPool {

    private const val POOL_SIZE = 20
    private const val PROSPECT_COUNT = 3
    private const val FOREIGN_PROSPECT_CHANCE = 0.15f

    fun generateProspects(character: Character): List<Person> {
        if (character.age < MIN_DATING_AGE) return emptyList()
        return (0 until PROSPECT_COUNT).map { buildProspect(character) }
    }

    private fun buildProspect(character: Character): Person {
        val prospectCountry = pickProspectCountry(character.countryCode)
        val gender = if (Random.nextBoolean()) Gender.MALE else Gender.FEMALE
        val name = NamePool.randomFullName(gender, prospectCountry)
        val ageOffset = Random.nextInt(-5, 6)
        val age = (character.age + ageOffset).coerceAtLeast(MIN_DATING_AGE)
        return Person(
            id = UUID.randomUUID().toString(),
            name = name,
            relation = RelationType.SPOUSE,
            age = age,
            relationshipLevel = Random.nextInt(30, 61),
            stats = Stats(
                health = Random.nextInt(50, 81),
                happiness = Random.nextInt(45, 76),
                looks = Random.nextInt(40, 81)
            ),
            isMarried = false,
            avatarConfig = AvatarConfig.random(),
            countryCode = prospectCountry
        )
    }

    private fun pickProspectCountry(playerCountry: String): String {
        if (Random.nextFloat() >= FOREIGN_PROSPECT_CHANCE) return playerCountry
        val others = CountryCatalog.all().map { it.code }.filter { it != playerCountry }
        return others.randomOrNull() ?: playerCountry
    }

    private const val MIN_DATING_AGE = 18
}
