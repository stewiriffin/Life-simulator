package com.maisha.game.domain

/**
 * Bounds serialized [Character.eventLog] growth for long-lived saves.
 * Newest entries are kept at the front; death markers are always preserved.
 */
object EventLogCap {
    const val MAX_ENTRIES = 150
    const val DEATH_MARKER_PREFIX = "::DEATH:"

    fun prepend(existing: List<String>, entry: String): List<String> =
        trim(listOf(entry) + existing)

    fun append(existing: List<String>, entry: String): List<String> =
        trim(existing + entry)

    fun trim(log: List<String>): List<String> {
        if (log.size <= MAX_ENTRIES) return log
        val deathMarkers = log.filter { it.startsWith(DEATH_MARKER_PREFIX) }
        val regular = log.filterNot { it.startsWith(DEATH_MARKER_PREFIX) }
        val allowedRegular = (MAX_ENTRIES - deathMarkers.size).coerceAtLeast(0)
        return deathMarkers + regular.take(allowedRegular)
    }
}
