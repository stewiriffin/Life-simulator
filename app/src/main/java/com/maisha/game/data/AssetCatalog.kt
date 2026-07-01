// app/src/main/java/com/maisha/game/data/AssetCatalog.kt
package com.maisha.game.data

import com.maisha.game.data.model.AssetType

data class CatalogAsset(
    val id: String,
    val type: AssetType,
    val name: String,
    val purchasePrice: Int,
    val monthlyUpkeep: Int
)

object AssetCatalog {

    val items: List<CatalogAsset> = listOf(
        CatalogAsset(
            id = "boda_basic",
            type = AssetType.MOTORBIKE,
            name = "Used Boda Boda",
            purchasePrice = 80_000,
            monthlyUpkeep = 2_000
        ),
        CatalogAsset(
            id = "boda_new",
            type = AssetType.MOTORBIKE,
            name = "Honda Boxer 150",
            purchasePrice = 150_000,
            monthlyUpkeep = 3_500
        ),
        CatalogAsset(
            id = "car_vitz",
            type = AssetType.CAR,
            name = "Used Toyota Vitz",
            purchasePrice = 600_000,
            monthlyUpkeep = 5_000
        ),
        CatalogAsset(
            id = "car_probox",
            type = AssetType.CAR,
            name = "Used Toyota Probox",
            purchasePrice = 900_000,
            monthlyUpkeep = 6_000
        ),
        CatalogAsset(
            id = "car_sedan_used",
            type = AssetType.CAR,
            name = "Used Toyota Axio",
            purchasePrice = 1_200_000,
            monthlyUpkeep = 7_000
        ),
        CatalogAsset(
            id = "car_new",
            type = AssetType.CAR,
            name = "New Toyota Corolla",
            purchasePrice = 3_500_000,
            monthlyUpkeep = 12_000
        ),
        CatalogAsset(
            id = "bedsitter_rongai",
            type = AssetType.HOUSE,
            name = "Bedsitter — Rongai",
            purchasePrice = 1_500_000,
            monthlyUpkeep = 4_000
        ),
        CatalogAsset(
            id = "apartment_kasarani",
            type = AssetType.HOUSE,
            name = "1BR Apartment — Kasarani",
            purchasePrice = 2_800_000,
            monthlyUpkeep = 8_000
        ),
        CatalogAsset(
            id = "house_thika",
            type = AssetType.HOUSE,
            name = "Maisonette — Thika",
            purchasePrice = 6_500_000,
            monthlyUpkeep = 10_000
        ),
        CatalogAsset(
            id = "house_karen",
            type = AssetType.HOUSE,
            name = "Family Home — Karen",
            purchasePrice = 15_000_000,
            monthlyUpkeep = 25_000
        )
    )

    fun findById(catalogId: String): CatalogAsset? = items.find { it.id == catalogId }
}
