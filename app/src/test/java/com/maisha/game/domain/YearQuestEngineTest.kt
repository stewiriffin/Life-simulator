package com.maisha.game.domain

import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class YearQuestEngineTest {
    private val engine = YearQuestEngine()

    @Test
    fun generate_softQuestsFromAge3() {
        val preschooler = TestFixtures.character(
            age = 3,
            stats = Stats(happiness = 50, health = 70, smarts = 40, looks = 50, money = 0)
        )
        val quests = engine.generate(preschooler, Random(2))
        assertTrue(quests.isNotEmpty())
        assertTrue(
            quests.all {
                it.kind == YearQuestKind.RAISE_HAPPINESS || it.kind == YearQuestKind.RAISE_HEALTH
            }
        )
    }

    @Test
    fun generate_returnsQuestsForTeenWithFamily() {
        val character = TestFixtures.character(
            age = 16,
            stats = Stats(health = 60, happiness = 55, smarts = 50, looks = 50, money = 5_000),
            education = EducationState(stage = SchoolStage.SECONDARY)
        )
        val quests = engine.generate(character, Random(0))
        assertTrue(quests.isNotEmpty())
        assertTrue(quests.size <= YearQuestEngine.QUESTS_PER_YEAR)
    }

    @Test
    fun evaluate_marksHappinessQuestComplete() {
        val before = TestFixtures.character(
            age = 20,
            stats = Stats(happiness = 40, health = 70, smarts = 50, looks = 50, money = 10_000)
        )
        val quest = YearQuest(
            kind = YearQuestKind.RAISE_HAPPINESS,
            target = 8,
            titleResHint = "year_quest_raise_happiness"
        )
        val after = before.copy(stats = before.stats.copy(happiness = 50))
        val progress = engine.evaluate(listOf(quest), before, after).single()
        assertTrue(progress.completed)
        assertEquals(10, progress.current)
    }

    @Test
    fun applyRewards_increasesKarmaAndLogs() {
        val character = TestFixtures.character(age = 22, stats = Stats(karma = 50))
        val completed = listOf(
            YearQuestProgress(
                quest = YearQuest(YearQuestKind.STAY_OUT_OF_TROUBLE, 1, "x", rewardKarma = 4),
                current = 1,
                completed = true
            )
        )
        val updated = engine.applyRewards(character, completed)
        assertEquals(54, updated.stats.karma)
        assertTrue(updated.eventLog.first().contains("Year quest complete"))
    }
}

class DynastyScoreTest {
    @Test
    fun calculate_rewardsGenerationAndLongevity() {
        val young = TestFixtures.character(age = 20, generationNumber = 1)
        val elderHeir = TestFixtures.character(age = 70, generationNumber = 3)
        val youngScore = DynastyScore.calculate(young, netWorth = 50_000).total
        val elderScore = DynastyScore.calculate(elderHeir, netWorth = 50_000).total
        assertTrue(elderScore > youngScore)
    }
}

class EventLogClassifierTest {
    @Test
    fun classify_detectsMilestoneAndNegative() {
        assertEquals(EventLogTone.MILESTONE, EventLogClassifier.classify("You got married today."))
        assertEquals(EventLogTone.NEGATIVE, EventLogClassifier.classify("You were arrested for fraud."))
        assertEquals(EventLogTone.POSITIVE, EventLogClassifier.classify("You were hired as a nurse."))
    }
}
