// app/src/main/java/com/maisha/game/domain/FamilyGenerator.kt
package com.maisha.game.domain

import com.maisha.game.data.NamePool
import com.maisha.game.data.model.AvatarConfig
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

        val mother = PersonGenerator.buildPerson(
            name = "${pool.randomFemaleFirstName()} $surname",
            relation = RelationType.MOTHER,
            countryCode = countryCode,
            age = Random.nextInt(PersonGenerator.PARENT_AGE_MIN, PersonGenerator.PARENT_AGE_MAX),
            relationshipLevel = 50,
            stats = PersonGenerator.parentStats(),
            gender = com.maisha.game.data.model.Gender.FEMALE
        )
        family += mother

        val father = PersonGenerator.buildPerson(
            name = "${pool.randomMaleFirstName()} $surname",
            relation = RelationType.FATHER,
            countryCode = countryCode,
            age = Random.nextInt(PersonGenerator.PARENT_AGE_MIN, PersonGenerator.PARENT_AGE_MAX),
            relationshipLevel = 50,
            stats = PersonGenerator.parentStats(),
            gender = com.maisha.game.data.model.Gender.MALE
        )
        family += father

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
                    stats = PersonGenerator.siblingStats(),
                    avatarConfig = inheritAvatarConfig(mother.avatarConfig, father.avatarConfig)
                )
            }
        }

        return family
    }

    companion object {
        private const val SIBLING_CHANCE = 0.4f

        /**
         * Blends parental traits so children resemble their parents.
         * Skin tone and hair color are inherited from either parent or an intermediate value;
         * facial features are taken from one parent; hair style and outfit stay partly random.
         */
        fun inheritAvatarConfig(
            parentA: AvatarConfig,
            parentB: AvatarConfig
        ): AvatarConfig {
            val skinTone = blendIndex(parentA.skinTone, parentB.skinTone, AvatarConfig.SKIN_TONE_COUNT)
            val hairColor = blendIndex(parentA.hairColor, parentB.hairColor, AvatarConfig.HAIR_COLOR_COUNT)
            val facialFeature = when (Random.nextInt(3)) {
                0 -> parentA.facialFeature
                1 -> parentB.facialFeature
                else -> listOfNotNull(parentA.facialFeature, parentB.facialFeature).randomOrNull()
            }
            val hairStyle = when (Random.nextInt(3)) {
                0 -> parentA.hairStyle
                1 -> parentB.hairStyle
                else -> Random.nextInt(AvatarConfig.HAIR_STYLE_COUNT)
            }
            val outfitColor = Random.nextInt(AvatarConfig.OUTFIT_COLOR_COUNT)
            return AvatarConfig(
                skinTone = skinTone,
                hairStyle = hairStyle.floorMod(AvatarConfig.HAIR_STYLE_COUNT),
                hairColor = hairColor,
                outfitColor = outfitColor,
                accessoryId = null,
                facialFeature = facialFeature,
                facialHair = null,
                eyewear = null,
                agingDetails = null
            )
        }

        /**
         * Picks parent A's trait, parent B's trait, or the midpoint between them.
         */
        fun blendIndex(parentA: Int, parentB: Int, count: Int): Int {
            val a = parentA.floorMod(count)
            val b = parentB.floorMod(count)
            return when (Random.nextInt(3)) {
                0 -> a
                1 -> b
                else -> ((a + b) / 2).coerceIn(0, count - 1)
            }
        }

        private fun Int.floorMod(size: Int): Int {
            if (size <= 0) return 0
            val r = this % size
            return if (r < 0) r + size else r
        }
    }
}
