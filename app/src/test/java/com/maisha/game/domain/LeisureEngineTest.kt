package com.maisha.game.domain

import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeisureEngineTest {
    private val engine = LeisureEngine()

    @Test
    fun perform_successSpendsCashAndBoostsHappiness() {
        val before = TestFixtures.character(
            age = 20,
            stats = Stats(money = 50_000, happiness = 40, health = 70, smarts = 50, looks = 50)
        )
        val cost = engine.cost(LeisureActivity.NATURE_DAY, before.countryCode)
        val result = engine.perform(before, LeisureActivity.NATURE_DAY)
        assertTrue(result is LeisureResult.Success)
        val after = (result as LeisureResult.Success).character
        assertEquals(before.stats.money - cost, after.stats.money)
        assertTrue(after.stats.happiness > before.stats.happiness)
        assertTrue(after.eventLog.first().contains("nature"))
    }

    @Test
    fun perform_insufficientFunds() {
        val broke = TestFixtures.character(
            age = 20,
            stats = Stats(money = 10, happiness = 40, health = 70)
        )
        val result = engine.perform(broke, LeisureActivity.SPA_DAY)
        assertEquals(LeisureResult.InsufficientFunds, result)
    }

    @Test
    fun perform_ineligibleWhenIncarcerated() {
        val locked = TestFixtures.character(
            age = 22,
            stats = Stats(money = 100_000),
            criminalRecord = CriminalRecord(currentlyIncarcerated = true, yearsRemaining = 2)
        )
        assertEquals(
            LeisureResult.Ineligible,
            engine.perform(locked, LeisureActivity.NIGHT_OUT)
        )
    }

    @Test
    fun activitiesFor_childGetsPlaygroundNotSpa() {
        val child = TestFixtures.character(
            age = 10,
            stats = Stats(money = 20_000, happiness = 50, health = 70)
        )
        val activities = engine.activitiesFor(child)
        assertTrue(activities.contains(LeisureActivity.PLAYGROUND))
        assertTrue(activities.contains(LeisureActivity.STUDY_BUDDY))
        assertTrue(activities.contains(LeisureActivity.CHORES))
        assertFalse(activities.contains(LeisureActivity.SPA_DAY))
    }

    @Test
    fun chores_paysCashAndIsEligibleForTween() {
        val tween = TestFixtures.character(
            age = 12,
            stats = Stats(money = 100, happiness = 50, health = 70)
        )
        val result = engine.perform(tween, LeisureActivity.CHORES)
        assertTrue(result is LeisureResult.Success)
        val after = (result as LeisureResult.Success).character
        assertTrue(after.stats.money > tween.stats.money)
    }

    @Test
    fun adultLeisure_ineligibleForYoungChild() {
        val toddler = TestFixtures.character(
            age = 4,
            stats = Stats(money = 50_000)
        )
        assertEquals(
            LeisureResult.Ineligible,
            engine.perform(toddler, LeisureActivity.PLAYGROUND)
        )
    }

    @Test
    fun toddlerGetsStorytimeAndParkActivities() {
        val toddler = TestFixtures.character(
            age = 3,
            stats = Stats(money = 50_000, happiness = 50, health = 70, smarts = 40)
        )
        val activities = engine.activitiesFor(toddler)
        assertTrue(activities.contains(LeisureActivity.STORYTIME))
        assertTrue(activities.contains(LeisureActivity.PARK_CAREGIVER))
        assertFalse(activities.contains(LeisureActivity.PLAYGROUND))
        val result = engine.perform(toddler, LeisureActivity.STORYTIME)
        assertTrue(result is LeisureResult.Success)
    }

    @Test
    fun midLifeGetsRetreatAndReunion() {
        val adult = TestFixtures.character(
            age = 35,
            stats = Stats(money = 200_000, happiness = 50, health = 70)
        )
        val activities = engine.activitiesFor(adult)
        assertTrue(activities.contains(LeisureActivity.WEEKEND_RETREAT))
        assertTrue(activities.contains(LeisureActivity.REUNION_DINNER))
    }

    @Test
    fun seniorGetsGrandkidsAndMemoir() {
        val senior = TestFixtures.character(
            age = 62,
            stats = Stats(money = 100_000, happiness = 50, health = 70, smarts = 60)
        )
        val activities = engine.activitiesFor(senior)
        assertTrue(activities.contains(LeisureActivity.GRANDKIDS_DAY))
        assertTrue(activities.contains(LeisureActivity.MEMOIR_WRITING))
    }
}

class CrimeStatusMapperTest {
    @Test
    fun map_awaitingTrialAndPrison() {
        assertEquals(
            CrimeStatusKind.AWAITING_TRIAL,
            CrimeStatusMapper.map(CriminalRecord(awaitingTrial = true)).kind
        )
        val prison = CrimeStatusMapper.map(
            CriminalRecord(
                currentlyIncarcerated = true,
                yearsRemaining = 3,
                yearsServed = 1,
                totalSentenceYears = 4
            )
        )
        assertEquals(CrimeStatusKind.INCARCERATED, prison.kind)
        assertEquals(3, prison.yearsRemaining)
        assertTrue(prison.showParoleHint)
    }
}

class ActionQuestHintsTest {
    @Test
    fun matches_skillAndSocialFamilies() {
        assertTrue(
            ActionQuestHints.helpsQuest(YearQuestKind.RAISE_SKILL, ActionFamily.SKILL_PRACTICE)
        )
        assertTrue(
            ActionQuestHints.helpsQuest(YearQuestKind.GROW_FOLLOWERS, ActionFamily.SOCIAL_POST)
        )
        assertTrue(
            ActionQuestHints.anyMatch(
                listOf(YearQuest(YearQuestKind.EARN_MONEY, 1_000, "x")),
                ActionFamily.SIDE_HUSTLE
            )
        )
    }
}
