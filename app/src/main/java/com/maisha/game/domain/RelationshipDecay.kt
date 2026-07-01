// app/src/main/java/com/maisha/game/domain/RelationshipDecay.kt (new)
package com.maisha.game.domain

import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationshipDecayNotice
import com.maisha.game.data.model.RelationshipTier

data class FamilyYearTickResult(
    val family: List<Person>,
    val decayNotices: List<RelationshipDecayNotice> = emptyList()
)
