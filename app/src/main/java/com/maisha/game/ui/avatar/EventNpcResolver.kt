// app/src/main/java/com/maisha/game/ui/avatar/EventNpcResolver.kt (new)
package com.maisha.game.ui.avatar

import com.maisha.game.data.model.Character
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import com.maisha.game.domain.spouse

object EventNpcResolver {

    fun resolveNpc(character: Character, event: LifeEvent): Person? {
        val tags = event.tags
        if ("requires_spouse" in tags || "relationship" in tags) {
            return character.spouse()
        }
        if ("requires_child" in tags || "requires_child_school_age" in tags) {
            return character.family.firstOrNull { it.relation == RelationType.CHILD }
        }
        if ("requires_parent" in tags || "family" in tags) {
            return character.family.firstOrNull {
                it.relation == RelationType.MOTHER || it.relation == RelationType.FATHER
            }
        }
        if (event.choices.any { it.siblingRelationshipEffect != 0 }) {
            return character.family.firstOrNull { it.relation == RelationType.SIBLING }
        }
        if (event.choices.any { it.familyRelationshipEffect != 0 }) {
            return character.family.firstOrNull {
                it.relation == RelationType.MOTHER || it.relation == RelationType.FATHER
            }
        }
        if (event.choices.any { it.spouseRelationshipEffect != 0 }) {
            return character.spouse()
        }
        return null
    }
}
