// app/src/main/java/com/maisha/game/domain/GiftTier.kt (new)
package com.maisha.game.domain

enum class GiftTier(val baseCostKenya: Int, val relationshipBoost: Int) {
    SMALL(baseCostKenya = 400, relationshipBoost = 5),
    MEDIUM(baseCostKenya = 1_500, relationshipBoost = 12),
    LARGE(baseCostKenya = 6_000, relationshipBoost = 25)
}
