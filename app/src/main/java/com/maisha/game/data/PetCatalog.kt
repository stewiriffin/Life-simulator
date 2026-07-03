// app/src/main/java/com/maisha/game/data/PetCatalog.kt
package com.maisha.game.data

import com.maisha.game.data.model.PetSpecies

data class PetCatalogEntry(
    val species: PetSpecies,
    val displayName: String,
    val adoptionFee: Int,
    val yearlyUpkeep: Int,
    val defaultName: String
)

/** Shelter adoption fees and annual care costs (Kenya baseline; scaled at purchase/upkeep time). */
object PetCatalog {

    private val entries: List<PetCatalogEntry> = listOf(
        PetCatalogEntry(
            species = PetSpecies.DOG,
            displayName = "Dog",
            adoptionFee = 15_000,
            yearlyUpkeep = 24_000,
            defaultName = "Buddy"
        ),
        PetCatalogEntry(
            species = PetSpecies.CAT,
            displayName = "Cat",
            adoptionFee = 8_000,
            yearlyUpkeep = 12_000,
            defaultName = "Whiskers"
        ),
        PetCatalogEntry(
            species = PetSpecies.BIRD,
            displayName = "Bird",
            adoptionFee = 5_000,
            yearlyUpkeep = 6_000,
            defaultName = "Tweety"
        ),
        PetCatalogEntry(
            species = PetSpecies.FISH,
            displayName = "Fish",
            adoptionFee = 2_000,
            yearlyUpkeep = 3_000,
            defaultName = "Bubbles"
        ),
        PetCatalogEntry(
            species = PetSpecies.EXOTIC,
            displayName = "Exotic Pet",
            adoptionFee = 50_000,
            yearlyUpkeep = 60_000,
            defaultName = "Zazu"
        )
    )

    fun getAll(): List<PetCatalogEntry> = entries

    fun findBySpecies(species: PetSpecies): PetCatalogEntry? =
        entries.find { it.species == species }
}
