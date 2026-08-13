package com.maisha.game.domain

import com.maisha.game.data.model.Asset
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.BucketGoal
import com.maisha.game.data.model.BucketGoalKind
import com.maisha.game.data.model.FameTier
import com.maisha.game.data.model.SkillProgress
import com.maisha.game.data.model.SkillType
import com.maisha.game.data.model.SocialMediaState
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YearQuestStreakTest {
    private val engine = YearQuestEngine()

    @Test
    fun applyStreak_incrementsOnCleanYearAndResetsOnMiss() {
        val character = TestFixtures.character(age = 20, questYearStreak = 2)
        val quests = listOf(
            YearQuest(YearQuestKind.RAISE_HAPPINESS, 5, "x", rewardKarma = 2),
            YearQuest(YearQuestKind.RAISE_HEALTH, 5, "y", rewardKarma = 2)
        )
        val clean = listOf(
            YearQuestProgress(quests[0], 5, true),
            YearQuestProgress(quests[1], 5, true)
        )
        val afterClean = engine.applyStreak(character, quests, clean)
        assertEquals(3, afterClean.questYearStreak)
        assertTrue(afterClean.stats.karma > character.stats.karma)

        val miss = listOf(
            YearQuestProgress(quests[0], 5, true),
            YearQuestProgress(quests[1], 1, false)
        )
        val afterMiss = engine.applyStreak(afterClean, quests, miss)
        assertEquals(0, afterMiss.questYearStreak)
    }

    @Test
    fun raiseSkill_progressCountsLevelGains() {
        val before = TestFixtures.character(
            age = 16,
            skills = listOf(SkillProgress(SkillType.COOKING, 10))
        )
        val after = before.copy(
            skills = listOf(SkillProgress(SkillType.COOKING, 22))
        )
        val quest = YearQuest(YearQuestKind.RAISE_SKILL, 10, "year_quest_raise_skill")
        val progress = engine.evaluate(listOf(quest), before, after).single()
        assertTrue(progress.completed)
        assertEquals(12, progress.current)
    }
}

class MilestoneEngineTest {
    private val engine = MilestoneEngine()

    @Test
    fun checkNewUnlocks_detectsAgeAndDriving() {
        val before = TestFixtures.character(age = 17, hasDrivingLicense = false)
        val after = before.copy(age = 18, hasDrivingLicense = true)
        val unlocks = engine.checkNewUnlocks(before, after, netWorthAfter = 0)
        assertTrue(unlocks.any { it.id == MilestoneEngine.ID_AGE_18 })
        assertTrue(unlocks.any { it.id == MilestoneEngine.ID_DRIVING })
        val applied = engine.applyUnlocks(after, unlocks)
        assertTrue(applied.unlockedMilestoneIds.contains(MilestoneEngine.ID_AGE_18))
        assertTrue(applied.eventLog.any { it.contains("Life milestone") })
    }
}

class FameTierTest {
    private val engine = SocialMediaEngine(FinanceEngine())

    @Test
    fun fameTierFor_usesFollowerThresholds() {
        assertEquals(FameTier.UNKNOWN, engine.fameTierFor(0))
        assertEquals(FameTier.LOCAL, engine.fameTierFor(1_000))
        assertEquals(FameTier.REGIONAL, engine.fameTierFor(10_000))
        assertEquals(FameTier.NATIONAL, engine.fameTierFor(100_000))
        assertEquals(FameTier.GLOBAL, engine.fameTierFor(1_000_000))
    }

    @Test
    fun syncFameTier_ranksUpAndLogs() {
        val character = TestFixtures.character(
            age = 20,
            socialMedia = SocialMediaState(
                hasAccount = true,
                followers = 12_000,
                fameTier = FameTier.LOCAL
            ),
            stats = Stats(happiness = 50)
        )
        val updated = engine.syncFameTier(character)
        assertEquals(FameTier.REGIONAL, updated.socialMedia.fameTier)
        assertTrue(updated.stats.happiness > 50)
        assertTrue(updated.eventLog.first().contains("Fame rose"))
    }
}

class SkillMasteryTierTest {
    private val engine = SkillEngine()

    @Test
    fun masteryTier_breakpoints() {
        assertEquals(SkillMasteryTier.NOVICE, engine.masteryTier(0))
        assertEquals(SkillMasteryTier.ADEPT, engine.masteryTier(25))
        assertEquals(SkillMasteryTier.EXPERT, engine.masteryTier(50))
        assertEquals(SkillMasteryTier.MASTER, engine.masteryTier(75))
    }

    @Test
    fun showcaseSkill_requiresMasterAndOncePerYear() {
        val character = TestFixtures.character(
            age = 22,
            skills = listOf(SkillProgress(SkillType.GUITAR, 80)),
            stats = Stats(money = 1_000, happiness = 50)
        )
        when (val first = engine.showcaseSkill(character, SkillType.GUITAR)) {
            is SkillResult.Success -> {
                assertTrue(first.character.skillShowcaseDoneThisYear)
                assertTrue(first.character.stats.money > character.stats.money)
            }
            is SkillResult.Failed -> error(first.reason.name)
        }
        val again = engine.showcaseSkill(
            character.copy(skillShowcaseDoneThisYear = true),
            SkillType.GUITAR
        )
        assertTrue(again is SkillResult.Failed)
    }
}

class BucketListEngineTest {
    private val engine = BucketListEngine()

    @Test
    fun adopt_and_complete_home_goal() {
        val character = TestFixtures.character(
            age = 25,
            stats = Stats(money = 100_000, karma = 40, happiness = 40)
        )
        when (val adopted = engine.adopt(character, "own_home")) {
            is BucketAdoptResult.Success -> {
                assertEquals(1, adopted.character.bucketList.size)
                assertTrue(adopted.character.stats.money < character.stats.money)
                val withHome = adopted.character.copy(
                    assets = listOf(
                        Asset(
                            id = "h1",
                            type = AssetType.HOUSE,
                            name = "Home",
                            purchasePrice = 50_000,
                            currentValue = 50_000,
                            monthlyUpkeep = 500
                        )
                    )
                )
                val done = engine.evaluate(withHome, netWorth = 150_000)
                assertTrue(done.bucketList.single().completed)
                assertTrue(done.stats.karma > adopted.character.stats.karma)
            }
            else -> error("Expected adopt success: $adopted")
        }
    }

    @Test
    fun adopt_rejectsWhenFull() {
        val goals = (1..3).map {
            BucketGoal(
                id = "g$it",
                kind = BucketGoalKind.OWN_HOME,
                templateId = "own_home"
            )
        }
        val character = TestFixtures.character(
            age = 30,
            stats = Stats(money = 1_000_000),
            bucketList = goals
        )
        assertEquals(BucketAdoptResult.Full, engine.adopt(character, "raise_child"))
    }
}
