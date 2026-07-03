// app/src/main/java/com/maisha/game/domain/EffortResolver.kt
package com.maisha.game.domain

import com.maisha.game.data.model.StudyEffort
import com.maisha.game.data.model.WorkEffort
import kotlin.random.Random

/**
 * Single source of truth for study/work effort numeric deltas (Prompt 28 tuned values).
 *
 * [workYear*] deltas apply during annual [CareerEngine.workYear]; [workEvent*] apply from
 * work-tagged event choices via [CareerEngine.applyWorkEffort] (different ranges by design).
 */
object EffortResolver {

    fun studyGpaDelta(effort: StudyEffort): Float = when (effort) {
        StudyEffort.SLACK -> -Random.nextDouble(0.1, 0.31).toFloat()
        StudyEffort.NORMAL -> Random.nextDouble(0.05, 0.16).toFloat()
        StudyEffort.HARD -> Random.nextDouble(0.1, 0.31).toFloat()
    }

    fun studySmartsDelta(effort: StudyEffort): Int = when (effort) {
        StudyEffort.SLACK -> -1
        StudyEffort.NORMAL -> 1
        StudyEffort.HARD -> 2
    }

    fun studyHappinessDelta(effort: StudyEffort): Int =
        if (effort == StudyEffort.HARD) -1 else 0

    fun workYearPerformanceDelta(effort: WorkEffort): Int = when (effort) {
        WorkEffort.COAST -> -Random.nextInt(5, 16)
        WorkEffort.NORMAL -> Random.nextInt(-2, 6)
        WorkEffort.GRIND -> Random.nextInt(5, 16)
    }

    fun workYearHappinessDelta(effort: WorkEffort): Int = when (effort) {
        WorkEffort.COAST -> Random.nextInt(1, 4)
        WorkEffort.NORMAL -> Random.nextInt(0, 2)
        WorkEffort.GRIND -> -Random.nextInt(2, 6)
    }

    fun workYearHealthDelta(effort: WorkEffort): Int = when (effort) {
        WorkEffort.GRIND -> -Random.nextInt(1, 4)
        else -> 0
    }

    fun workEventPerformanceDelta(effort: WorkEffort): Int = when (effort) {
        WorkEffort.COAST -> -Random.nextInt(8, 18)
        WorkEffort.NORMAL -> Random.nextInt(0, 6)
        WorkEffort.GRIND -> Random.nextInt(8, 18)
    }

    fun workEventHappinessDelta(effort: WorkEffort): Int = when (effort) {
        WorkEffort.COAST -> Random.nextInt(2, 6)
        WorkEffort.NORMAL -> 0
        WorkEffort.GRIND -> -Random.nextInt(4, 10)
    }

    fun workEventHealthDelta(effort: WorkEffort): Int =
        if (effort == WorkEffort.GRIND) -Random.nextInt(2, 6) else 0
}
