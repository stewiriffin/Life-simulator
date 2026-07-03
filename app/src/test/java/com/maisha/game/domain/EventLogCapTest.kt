package com.maisha.game.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventLogCapTest {

    @Test
    fun prepend_keepsNewestEntriesWithinCap() {
        val existing = List(EventLogCap.MAX_ENTRIES) { "entry-$it" }
        val result = EventLogCap.prepend(existing, "newest")
        assertEquals(EventLogCap.MAX_ENTRIES, result.size)
        assertEquals("newest", result.first())
    }

    @Test
    fun trim_preservesDeathMarkers() {
        val death = "${EventLogCap.DEATH_MARKER_PREFIX}OLD_AGE::Passed away"
        val regular = List(EventLogCap.MAX_ENTRIES) { "event-$it" }
        val log = listOf(death) + regular
        val result = EventLogCap.trim(log)
        assertTrue(result.any { it.startsWith(EventLogCap.DEATH_MARKER_PREFIX) })
        assertEquals(EventLogCap.MAX_ENTRIES, result.size)
    }

    @Test
    fun trim_doesNotTreatDeathMarkerInMiddleOfLineAsProtected() {
        val regular = List(EventLogCap.MAX_ENTRIES + 5) { "event-$it" }
        val embedded = regular + "Flavor text with ${EventLogCap.DEATH_MARKER_PREFIX} in the middle"
        val result = EventLogCap.trim(embedded)
        assertEquals(EventLogCap.MAX_ENTRIES, result.size)
        assertTrue(result.none { it.contains(EventLogCap.DEATH_MARKER_PREFIX) })
    }
}
