// app/src/main/java/com/maisha/game/domain/PersonGenerator.kt
package com.maisha.game.domain

import com.maisha.game.data.CountryCatalog
import com.maisha.game.data.NamePool
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.Stats
import java.util.UUID
import kotlin.random.Random

/**
 * Shared NPC construction for dating prospects, friends, and generated family members.
 *
 * Call-site-specific tuning is preserved via parameters — not averaged across use cases.
 */
object PersonGenerator {

    const val FOREIGN_PROSPECT_CHANCE = 0.15f
    const val DATING_AGE_OFFSET_MIN = -5
    const val DATING_AGE_OFFSET_MAX = 6
    const val DATING_RELATIONSHIP_MIN = 30
    const val DATING_RELATIONSHIP_MAX = 61
    const val FRIEND_AGE_OFFSET_MIN = -3
    const val FRIEND_AGE_OFFSET_MAX = 4
    const val FRIEND_RELATIONSHIP_MIN = 40
    const val FRIEND_RELATIONSHIP_MAX = 61
    const val PARENT_AGE_MIN = 20
    const val PARENT_AGE_MAX = 41
    const val SIBLING_AGE_OFFSET_MIN = -5
    const val SIBLING_AGE_OFFSET_MAX = 6

    fun pickCountryWithForeignChance(playerCountry: String, foreignChance: Float): String {
        if (Random.nextFloat() >= foreignChance) return playerCountry
        val others = CountryCatalog.all().map { it.code }.filter { it != playerCountry }
        return others.randomOrNull() ?: playerCountry
    }

    fun parentStats(): Stats = Stats(
        health = Random.nextInt(55, 86),
        happiness = Random.nextInt(45, 76),
        smarts = Random.nextInt(40, 71),
        looks = Random.nextInt(40, 71),
        money = 0
    )

    fun siblingStats(): Stats = Stats(
        health = Random.nextInt(50, 81),
        happiness = Random.nextInt(45, 76),
        smarts = Random.nextInt(40, 71),
        looks = Random.nextInt(40, 71),
        money = 0
    )

    fun datingProspectStats(): Stats = Stats(
        health = Random.nextInt(50, 81),
        happiness = Random.nextInt(45, 76),
        looks = Random.nextInt(40, 81)
    )

    fun friendStats(): Stats = Stats(
        health = Random.nextInt(50, 81),
        happiness = Random.nextInt(45, 76),
        looks = Random.nextInt(40, 81)
    )

    fun buildPerson(
        name: String,
        relation: RelationType,
        countryCode: String,
        age: Int,
        relationshipLevel: Int,
        stats: Stats,
        gender: Gender = if (Random.nextBoolean()) Gender.MALE else Gender.FEMALE,
        isMarried: Boolean = false,
        secondaryCountryCode: String? = null,
        avatarConfig: AvatarConfig = AvatarConfig.random()
    ): Person = Person(
        id = UUID.randomUUID().toString(),
        name = name,
        relation = relation,
        gender = gender,
        age = age,
        relationshipLevel = relationshipLevel,
        stats = stats,
        isMarried = isMarried,
        avatarConfig = avatarConfig,
        countryCode = countryCode,
        secondaryCountryCode = secondaryCountryCode
    )

    fun buildDatingProspect(character: Character, minDatingAge: Int): Person {
        val prospectCountry = pickCountryWithForeignChance(
            character.countryCode,
            FOREIGN_PROSPECT_CHANCE
        )
        val gender = if (Random.nextBoolean()) Gender.MALE else Gender.FEMALE
        val name = NamePool.randomFullName(gender, prospectCountry)
        val ageOffset = Random.nextInt(DATING_AGE_OFFSET_MIN, DATING_AGE_OFFSET_MAX)
        val age = (character.age + ageOffset).coerceAtLeast(minDatingAge)
        return buildPerson(
            name = name,
            relation = RelationType.SPOUSE,
            countryCode = prospectCountry,
            age = age,
            relationshipLevel = Random.nextInt(DATING_RELATIONSHIP_MIN, DATING_RELATIONSHIP_MAX),
            stats = datingProspectStats(),
            gender = gender,
            isMarried = false
        )
    }

    fun buildFriend(character: Character, minFriendAge: Int): Person {
        val gender = if (Random.nextBoolean()) Gender.MALE else Gender.FEMALE
        val friendAge = (character.age + Random.nextInt(FRIEND_AGE_OFFSET_MIN, FRIEND_AGE_OFFSET_MAX))
            .coerceAtLeast(minFriendAge)
        return buildPerson(
            name = NamePool.randomFullName(gender, character.countryCode),
            relation = RelationType.FRIEND,
            countryCode = character.countryCode,
            age = friendAge,
            relationshipLevel = Random.nextInt(FRIEND_RELATIONSHIP_MIN, FRIEND_RELATIONSHIP_MAX),
            stats = friendStats(),
            gender = gender
        )
    }
}
