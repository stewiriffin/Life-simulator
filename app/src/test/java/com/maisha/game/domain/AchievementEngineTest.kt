package com.maisha.game.domain

import com.maisha.game.data.model.AchievementProgress
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.SchoolStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEngineTest {

    private val engine = AchievementEngine(FinanceEngine())

    @Test
    fun graduate_triggersWhenGraduated() {
        val character = TestFixtures.character(
            education = EducationState(stage = SchoolStage.GRADUATED)
        )
        val unlocked = engine.checkAchievements(character, emptyList())
        assertTrue(unlocked.any { it.id == "graduate" })
    }

    @Test
    fun graduate_doesNotTriggerWhenStillInSchool() {
        val character = TestFixtures.character(
            education = EducationState(stage = SchoolStage.SECONDARY)
        )
        val unlocked = engine.checkAchievements(character, emptyList())
        assertFalse(unlocked.any { it.id == "graduate" })
    }

    @Test
    fun halfCentury_triggersAtAgeFifty() {
        val character = TestFixtures.character(age = 50)
        val unlocked = engine.checkAchievements(character, emptyList())
        assertTrue(unlocked.any { it.id == "half_century" })
    }

    @Test
    fun propertyOwner_triggersWithHouseAsset() {
        val character = TestFixtures.character(
            assets = listOf(
                TestFixtures.asset(type = AssetType.HOUSE)
            )
        )
        val unlocked = engine.checkAchievements(character, emptyList())
        assertTrue(unlocked.any { it.id == "property_owner" })
    }

    @Test
    fun secondGeneration_triggersAtGenerationTwo() {
        val character = TestFixtures.character(generationNumber = 2)
        val unlocked = engine.checkAchievements(character, emptyList())
        assertTrue(unlocked.any { it.id == "second_generation" })
    }

    @Test
    fun trueFriend_requiresHighRelationshipFriend() {
        val friend = TestFixtures.person(
            id = "f1",
            relation = RelationType.FRIEND,
            relationshipLevel = 75
        )
        val character = TestFixtures.character(family = listOf(friend))
        val unlocked = engine.checkAchievements(character, emptyList())
        assertTrue(unlocked.any { it.id == "true_friend" })
    }

    @Test
    fun checkAchievements_doesNotReunlockAlreadyUnlocked() {
        val character = TestFixtures.character(age = 50)
        val progress = listOf(
            AchievementProgress(achievementId = "half_century", unlocked = true, unlockedAt = 1L)
        )
        val unlocked = engine.checkAchievements(character, progress)
        assertFalse(unlocked.any { it.id == "half_century" })
        assertEquals(0, unlocked.count { it.id == "half_century" })
    }
}
