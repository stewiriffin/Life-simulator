// app/src/main/java/com/maisha/game/domain/FamilyGenerator.kt (modified — country-aware names + avatars)
package com.maisha.game.domain

import com.maisha.game.data.NamePool
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FamilyGenerator @Inject constructor() {

    fun generateFamily(characterAge: Int = 0, countryCode: String = "KE"): List<Person> {
        val family = mutableListOf<Person>()
        val pool = NamePool.getNamePool(countryCode)
        val surname = pool.randomSurname()

        family += PersonGenerator.buildPerson(
            name = "${pool.randomFemaleFirstName()} $surname",
            relation = RelationType.MOTHER,
            countryCode = countryCode,
            age = Random.nextInt(PersonGenerator.PARENT_AGE_MIN, PersonGenerator.PARENT_AGE_MAX),
            relationshipLevel = 50,
            stats = PersonGenerator.parentStats(),
            gender = com.maisha.game.data.model.Gender.FEMALE
        )

        family += PersonGenerator.buildPerson(
            name = "${pool.randomMaleFirstName()} $surname",
            relation = RelationType.FATHER,
            countryCode = countryCode,
            age = Random.nextInt(PersonGenerator.PARENT_AGE_MIN, PersonGenerator.PARENT_AGE_MAX),
            relationshipLevel = 50,
            stats = PersonGenerator.parentStats(),
            gender = com.maisha.game.data.model.Gender.MALE
        )

        if (Random.nextFloat() < SIBLING_CHANCE) {
            val siblingCount = Random.nextInt(1, 3)
            repeat(siblingCount) {
                val ageOffset = Random.nextInt(
                    PersonGenerator.SIBLING_AGE_OFFSET_MIN,
                    PersonGenerator.SIBLING_AGE_OFFSET_MAX
                )
                family += PersonGenerator.buildPerson(
                    name = "${pool.randomSiblingName().substringBeforeLast(" ")} $surname",
                    relation = RelationType.SIBLING,
                    countryCode = countryCode,
                    age = (characterAge + ageOffset).coerceAtLeast(0),
                    relationshipLevel = 50,
                    stats = PersonGenerator.siblingStats()
                )
            }
        }

        return family
    }

    companion object {
        private const val SIBLING_CHANCE = 0.4f
    }
}
