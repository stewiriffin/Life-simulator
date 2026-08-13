// app/src/main/java/com/maisha/game/data/model/Pet.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class PetSpecies {
    DOG,
    CAT,
    BIRD,
    FISH,
    EXOTIC
}

/**
 * Adopted animal companion — relationship + mortality like family, upkeep like assets.
 * Capped at [com.maisha.game.domain.RelationshipEngine.MAX_PETS] per [Character].
 */
@Serializable
data class Pet(
    val id: String,
    val name: String,
    val species: PetSpecies,
    val age: Int = 0,
    val health: Int = 100,
    val relationshipLevel: Int = 60,
    /** Meaningful play done this in-game year. */
    val playedThisYear: Boolean = false,
    /** Vet visit done this in-game year. */
    val caredThisYear: Boolean = false
)
