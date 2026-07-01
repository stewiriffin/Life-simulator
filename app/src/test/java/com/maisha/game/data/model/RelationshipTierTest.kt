// app/src/test/java/com/maisha/game/data/model/RelationshipTierTest.kt (new)
package com.maisha.game.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RelationshipTierTest {

    @Test
    fun tierBoundaries_mapCorrectly() {
        assertEquals(RelationshipTier.ESTRANGED, relationshipTierFor(0))
        assertEquals(RelationshipTier.DISTANT, relationshipTierFor(20))
        assertEquals(RelationshipTier.COOL, relationshipTierFor(40))
        assertEquals(RelationshipTier.COOL, relationshipTierFor(50))
        assertEquals(RelationshipTier.FRIENDLY, relationshipTierFor(55))
        assertEquals(RelationshipTier.CLOSE, relationshipTierFor(70))
        assertEquals(RelationshipTier.INSEPARABLE, relationshipTierFor(90))
    }
}
