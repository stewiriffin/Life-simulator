package com.maisha.game.domain

import com.maisha.game.data.model.AvatarConfig
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyGeneratorTest {

    @Test
    fun generateChild_inheritsSkinToneAndHairColorFromParents() {
        val mother = AvatarConfig(
            skinTone = 1,
            hairStyle = 2,
            hairColor = 0,
            outfitColor = 3,
            facialFeature = 2
        )
        val father = AvatarConfig(
            skinTone = 6,
            hairStyle = 4,
            hairColor = 3,
            outfitColor = 1,
            facialFeature = 4
        )
        val allowedSkin = setOf(1, 6, (1 + 6) / 2)
        val allowedHair = setOf(0, 3, (0 + 3) / 2)
            val allowedFeatures = setOf(2, 4)

        repeat(60) {
            val child = FamilyGenerator.inheritAvatarConfig(mother, father)
            assertTrue(
                "Unexpected skinTone ${child.skinTone}",
                child.skinTone in allowedSkin
            )
            assertTrue(
                "Unexpected hairColor ${child.hairColor}",
                child.hairColor in allowedHair
            )
            assertTrue(
                "Unexpected facialFeature ${child.facialFeature}",
                child.facialFeature in allowedFeatures
            )
        }
    }

    @Test
    fun generateFamily_siblingsInheritFromParents() {
        val family = FamilyGenerator().generateFamily(characterAge = 5, countryCode = "KE")
        val mother = family.first { it.relation == com.maisha.game.data.model.RelationType.MOTHER }
        val father = family.first { it.relation == com.maisha.game.data.model.RelationType.FATHER }
        val siblings = family.filter {
            it.relation == com.maisha.game.data.model.RelationType.SIBLING
        }
        siblings.forEach { sibling ->
            val skin = sibling.avatarConfig.skinTone
            val hair = sibling.avatarConfig.hairColor
            val midSkin = (mother.avatarConfig.skinTone + father.avatarConfig.skinTone) / 2
            val midHair = (mother.avatarConfig.hairColor + father.avatarConfig.hairColor) / 2
            assertTrue(
                skin == mother.avatarConfig.skinTone ||
                    skin == father.avatarConfig.skinTone ||
                    skin == midSkin
            )
            assertTrue(
                hair == mother.avatarConfig.hairColor ||
                    hair == father.avatarConfig.hairColor ||
                    hair == midHair
            )
        }
    }
}
