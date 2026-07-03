package com.maisha.game.domain

import com.maisha.game.data.AchievementCatalog
import com.maisha.game.data.model.AchievementProgress
import com.maisha.game.data.model.AncestryEntry
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM proxy for the 50+ year memory soak — verifies capped lists stay bounded without UI.
 * Device heap profiling still recommended; see docs/PERFORMANCE_AUDIT.md (Prompt 49).
 */
class MemorySoakTest {

    @Test
    fun fiftyAgeUps_eventLogStaysWithinCap() {
        val engine = TestFixtures.gameEngine()
        var character = TestFixtures.character(
            age = 20,
            stats = Stats(money = 50_000, smarts = 60, health = 70, happiness = 60),
            education = com.maisha.game.data.model.EducationState(stage = SchoolStage.GRADUATED),
            career = com.maisha.game.data.model.CareerState(
                currentJob = TestFixtures.job(),
                yearsAtCurrentJob = 3
            )
        )
        val progress = AchievementCatalog.all.map {
            AchievementProgress(achievementId = it.id, unlocked = false, unlockedAt = null)
        }

        repeat(55) {
            val outcome = engine.ageUp(character, emptySet(), progress, slotId = 0)
            character = outcome.character.copy(
                eventLog = EventLogCap.trim(outcome.character.eventLog)
            )
        }

        assertTrue(
            "eventLog should stay <= ${EventLogCap.MAX_ENTRIES}, was ${character.eventLog.size}",
            character.eventLog.size <= EventLogCap.MAX_ENTRIES
        )
    }

    @Test
    fun legacyChain_ancestryHistoryStaysWithinCap() {
        var history = emptyList<AncestryEntry>()
        repeat(30) { index ->
            val gen = index + 1
            history = AncestryHistoryCap.trim(
                history + AncestryEntry(
                    generationNumber = gen,
                    characterName = "Gen $gen",
                    countryCode = "KE",
                    ageAtDeath = 80,
                    cause = "OLD_AGE"
                )
            )
        }
        assertTrue(history.size <= AncestryHistoryCap.MAX_ENTRIES)
    }

    @Test
    fun repeatedMilestones_perPersonStaysWithinCap() {
        var milestones = emptyList<com.maisha.game.data.model.RelationshipMilestone>()
        repeat(40) { year ->
            milestones = RelationshipMilestoneCap.trim(
                milestones + com.maisha.game.data.model.RelationshipMilestone(
                    ageAtEvent = 20 + year,
                    kind = com.maisha.game.data.model.MilestoneKind.BIG_ARGUMENT.name,
                    subjectName = "Pat"
                )
            )
        }
        assertTrue(
            "milestones should stay <= ${RelationshipMilestoneCap.MAX_ENTRIES}, was ${milestones.size}",
            milestones.size <= RelationshipMilestoneCap.MAX_ENTRIES
        )
    }
}
