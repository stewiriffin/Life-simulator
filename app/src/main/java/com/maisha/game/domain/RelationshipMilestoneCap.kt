package com.maisha.game.domain

import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationshipMilestone

/**
 * Bounds serialized [Person.milestones] growth for long-lived relationships and legacy carry-over.
 * Milestones are appended oldest-first; keeps the most recent entries (matches [MilestoneTimeline] display).
 */
object RelationshipMilestoneCap {
    const val MAX_ENTRIES = 25

    fun trim(milestones: List<RelationshipMilestone>): List<RelationshipMilestone> {
        if (milestones.size <= MAX_ENTRIES) return milestones
        return milestones.takeLast(MAX_ENTRIES)
    }

    fun trimFamily(family: List<Person>): List<Person> =
        family.map { person -> person.copy(milestones = trim(person.milestones)) }
}
