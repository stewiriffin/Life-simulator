// app/src/main/java/com/maisha/game/domain/FamilyGenerator.kt (modified — country-aware names + avatars)
package com.maisha.game.domain

import com.maisha.game.data.NamePool
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.Stats
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FamilyGenerator @Inject constructor() {

    fun generateFamily(characterAge: Int = 0, countryCode: String = "KE"): List<Person> {
        val family = mutableListOf<Person>()
        val pool = NamePool.getNamePool(countryCode)
        val surname = pool.randomSurname()

        family += Person(
            id = UUID.randomUUID().toString(),
            name = "${pool.randomFemaleFirstName()} $surname",
            relation = RelationType.MOTHER,
            age = Random.nextInt(20, 41),
            relationshipLevel = 50,
            stats = parentStats(),
            avatarConfig = AvatarConfig.random(),
            countryCode = countryCode
        )

        family += Person(
            id = UUID.randomUUID().toString(),
            name = "${pool.randomMaleFirstName()} $surname",
            relation = RelationType.FATHER,
            age = Random.nextInt(20, 41),
            relationshipLevel = 50,
            stats = parentStats(),
            avatarConfig = AvatarConfig.random(),
            countryCode = countryCode
        )

        if (Random.nextFloat() < SIBLING_CHANCE) {
            val siblingCount = Random.nextInt(1, 3)
            repeat(siblingCount) {
                val ageOffset = Random.nextInt(-5, 6)
                family += Person(
                    id = UUID.randomUUID().toString(),
                    name = "${pool.randomSiblingName().substringBeforeLast(" ")} $surname",
                    relation = RelationType.SIBLING,
                    age = (characterAge + ageOffset).coerceAtLeast(0),
                    relationshipLevel = 50,
                    stats = siblingStats(),
                    avatarConfig = AvatarConfig.random(),
                    countryCode = countryCode
                )
            }
        }

        return family
    }

    private fun parentStats(): Stats = Stats(
        health = Random.nextInt(55, 86),
        happiness = Random.nextInt(45, 76),
        smarts = Random.nextInt(40, 71),
        looks = Random.nextInt(40, 71),
        money = 0
    )

    private fun siblingStats(): Stats = Stats(
        health = Random.nextInt(50, 81),
        happiness = Random.nextInt(45, 76),
        smarts = Random.nextInt(40, 71),
        looks = Random.nextInt(40, 71),
        money = 0
    )

    companion object {
        private const val SIBLING_CHANCE = 0.4f
    }
}
