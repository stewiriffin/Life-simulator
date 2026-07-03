// app/src/test/java/com/maisha/game/domain/PerformanceBenchmarkTest.kt
package com.maisha.game.domain

import com.maisha.game.data.AchievementCatalog
import com.maisha.game.data.events.EventRepository
import com.maisha.game.data.model.AchievementProgress
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

/**
 * JVM micro-benchmarks for hot paths. Device frame timing and cold start require
 * Android Studio Profiler on itel A665L — see docs/PERFORMANCE_AUDIT.md.
 */
class PerformanceBenchmarkTest {

    @Test
    fun ageUp_medianUnder16ms_onJvm() {
        val engine = TestFixtures.gameEngine()
        var character = TestFixtures.character(
            age = 30,
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

        repeat(20) {
            val outcome = engine.ageUp(character, emptySet(), progress, slotId = 0)
            character = outcome.character
        }

        val samples = LongArray(ITERATIONS)
        repeat(ITERATIONS) { i ->
            samples[i] = measureNanoTime {
                val outcome = engine.ageUp(character, emptySet(), progress, slotId = 0)
                character = outcome.character
            }
        }
        samples.sort()
        val medianMs = samples[samples.size / 2] / 1_000_000.0
        assertTrue(
            "ageUp median ${"%.2f".format(medianMs)}ms should be < ${MAX_AGE_UP_MS}ms on JVM",
            medianMs < MAX_AGE_UP_MS
        )
    }

    @Test
    fun achievementCheck_medianUnder2ms_onJvm() {
        val financeEngine = FinanceEngine()
        val engine = AchievementEngine(financeEngine)
        val character = TestFixtures.character(
            age = 45,
            stats = Stats(money = 200_000),
            career = com.maisha.game.data.model.CareerState(
                currentJob = TestFixtures.job(level = 3),
                jobHistory = listOf("Clerk", "Analyst")
            )
        )
        val progress = AchievementCatalog.all.map {
            AchievementProgress(achievementId = it.id, unlocked = false, unlockedAt = null)
        }

        repeat(50) { engine.checkAchievements(character, progress) }

        val samples = LongArray(ITERATIONS)
        repeat(ITERATIONS) {
            samples[it] = measureNanoTime {
                engine.checkAchievements(character, progress)
            }
        }
        samples.sort()
        val medianMs = samples[samples.size / 2] / 1_000_000.0
        assertTrue(
            "achievement check median ${"%.2f".format(medianMs)}ms should be < 2ms",
            medianMs < 2.0
        )
    }

    @Test
    fun eventEligibility_age30_under5ms_withSyntheticPool() {
        val financeEngine = FinanceEngine()
        val events = syntheticEventPool(EVENT_POOL_SIZE)
        val repository = EventRepository.forTesting(financeEngine, events)
        val character = TestFixtures.character(age = 30, stats = Stats(money = 100_000))

        repeat(30) {
            repository.getEligibleEvents(30, emptySet(), character)
        }

        val samples = LongArray(ITERATIONS)
        repeat(ITERATIONS) {
            samples[it] = measureNanoTime {
                repository.getEligibleEvents(30, emptySet(), character)
            }
        }
        samples.sort()
        val medianMs = samples[samples.size / 2] / 1_000_000.0
        assertTrue(
            "getEligibleEvents median ${"%.2f".format(medianMs)}ms should be < 5ms",
            medianMs < 5.0
        )
    }

    private fun syntheticEventPool(count: Int): List<LifeEvent> =
        (0 until count).map { index ->
            val minAge = (index % 50)
            val maxAge = minAge + 30
            LifeEvent(
                id = "bench_$index",
                minAge = minAge,
                maxAge = maxAge,
                text = "Benchmark event $index",
                choices = listOf(
                    EventChoice(label = "OK", resultText = "Done")
                ),
                tags = emptyList(),
                weight = 1
            )
        }

    companion object {
        private const val ITERATIONS = 100
        private const val MAX_AGE_UP_MS = 16.0
        private const val EVENT_POOL_SIZE = 100
    }
}
