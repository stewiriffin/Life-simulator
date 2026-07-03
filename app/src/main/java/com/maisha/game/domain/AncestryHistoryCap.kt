// app/src/main/java/com/maisha/game/domain/AncestryHistoryCap.kt
package com.maisha.game.domain

import com.maisha.game.data.model.AncestryEntry

/**
 * Bounds serialized [com.maisha.game.data.model.Character.ancestryHistory] across long legacy chains.
 * Keeps the earliest generations (lowest generation numbers) for timeline display.
 */
object AncestryHistoryCap {
    const val MAX_ENTRIES = 25

    fun trim(history: List<AncestryEntry>): List<AncestryEntry> {
        if (history.size <= MAX_ENTRIES) return history
        return history.sortedBy { it.generationNumber }.take(MAX_ENTRIES)
    }
}
