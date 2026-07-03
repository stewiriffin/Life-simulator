package com.maisha.game.domain

import com.maisha.game.data.model.RelationshipMilestone
import org.junit.Assert.assertEquals
import org.junit.Test

class RelationshipMilestoneCapTest {

    @Test
    fun trim_keepsMostRecentEntries() {
        val milestones = (1..30).map { age ->
            RelationshipMilestone(ageAtEvent = age, description = "event-$age")
        }
        val trimmed = RelationshipMilestoneCap.trim(milestones)
        assertEquals(RelationshipMilestoneCap.MAX_ENTRIES, trimmed.size)
        assertEquals(6, trimmed.first().ageAtEvent)
        assertEquals(30, trimmed.last().ageAtEvent)
    }
}
