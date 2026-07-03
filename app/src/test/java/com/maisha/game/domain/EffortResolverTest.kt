// app/src/test/java/com/maisha/game/domain/EffortResolverTest.kt
package com.maisha.game.domain

import com.maisha.game.data.model.StudyEffort
import com.maisha.game.data.model.WorkEffort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EffortResolverTest {

    @Test
    fun studySmartsDelta_matchesTunedFixedValues() {
        assertEquals(-1, EffortResolver.studySmartsDelta(StudyEffort.SLACK))
        assertEquals(1, EffortResolver.studySmartsDelta(StudyEffort.NORMAL))
        assertEquals(2, EffortResolver.studySmartsDelta(StudyEffort.HARD))
    }

    @Test
    fun studyHappinessDelta_onlyHardReduces() {
        assertEquals(0, EffortResolver.studyHappinessDelta(StudyEffort.SLACK))
        assertEquals(0, EffortResolver.studyHappinessDelta(StudyEffort.NORMAL))
        assertEquals(-1, EffortResolver.studyHappinessDelta(StudyEffort.HARD))
    }

    @Test
    fun workYearPerformanceDelta_staysWithinTunedRanges() {
        repeat(200) {
            val coast = EffortResolver.workYearPerformanceDelta(WorkEffort.COAST)
            val normal = EffortResolver.workYearPerformanceDelta(WorkEffort.NORMAL)
            val grind = EffortResolver.workYearPerformanceDelta(WorkEffort.GRIND)
            assertTrue(coast in -15..-5)
            assertTrue(normal in -2..5)
            assertTrue(grind in 5..15)
        }
    }

    @Test
    fun workEventPerformanceDelta_staysWithinTunedRanges() {
        repeat(200) {
            val coast = EffortResolver.workEventPerformanceDelta(WorkEffort.COAST)
            val normal = EffortResolver.workEventPerformanceDelta(WorkEffort.NORMAL)
            val grind = EffortResolver.workEventPerformanceDelta(WorkEffort.GRIND)
            assertTrue(coast in -17..-8)
            assertTrue(normal in 0..5)
            assertTrue(grind in 8..17)
        }
    }
}
