package com.maisha.game.domain

import com.maisha.game.data.model.SkillType
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillEngineTest {

    private val engine = SkillEngine()

    @Test
    fun practiceSkill_increasesLevelAndDrainsHappiness() {
        val character = TestFixtures.character(
            age = 20,
            stats = Stats(happiness = 80, health = 80, money = 0)
        )
        when (val result = engine.practiceSkill(character, SkillType.GUITAR)) {
            is SkillResult.Success -> {
                assertTrue(result.levelGained > 0)
                val level = result.character.skills.first { it.type == SkillType.GUITAR }.level
                assertTrue(level > 0)
                assertTrue(result.character.stats.happiness < 80)
                assertTrue(result.character.stats.health < 80)
            }
            is SkillResult.Failed -> error("Expected practice success: ${result.reason}")
        }
    }
}
