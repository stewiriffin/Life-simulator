package com.maisha.game.domain

/**
 * Classifies event-log lines so the Life tab can tint milestone entries.
 * Heuristic on English log text (domain engines still write English narrative).
 */
enum class EventLogTone {
    MILESTONE,
    POSITIVE,
    NEGATIVE,
    NEUTRAL
}

object EventLogClassifier {
    private val milestoneKeywords = listOf(
        "married", "marriage", "born", "graduated", "graduation", "promoted",
        "promotion", "achievement", "centenarian", "legacy", "heir", "quest complete",
        "citizenship", "elected", "won"
    )
    private val positiveKeywords = listOf(
        "hired", "friend", "gift", "donated", "volunteer", "recovered", "bought",
        "profit", "followers", "salary increases", "passed"
    )
    private val negativeKeywords = listOf(
        "arrest", "fired", "died", "death", "ill", "sick", "divorce", "broke up",
        "downsiz", "failed", "jailed", "incarcerat", "stolen", "crash", "debt"
    )

    fun classify(entry: String): EventLogTone {
        val lower = entry.lowercase()
        if (milestoneKeywords.any { it in lower }) return EventLogTone.MILESTONE
        if (negativeKeywords.any { it in lower }) return EventLogTone.NEGATIVE
        if (positiveKeywords.any { it in lower }) return EventLogTone.POSITIVE
        return EventLogTone.NEUTRAL
    }
}
