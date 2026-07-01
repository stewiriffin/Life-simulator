// app/src/main/java/com/maisha/game/data/model/RelationshipDecayNotice.kt (new)
package com.maisha.game.data.model

data class RelationshipDecayNotice(
    val personName: String,
    val previousTier: RelationshipTier,
    val newTier: RelationshipTier
)
