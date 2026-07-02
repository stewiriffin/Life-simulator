// app/src/main/java/com/maisha/game/data/AssetCatalog.kt (modified — country-aware universal + flavor naming)
package com.maisha.game.data

import com.maisha.game.data.model.AssetType

typealias AssetCatalogEntry = CatalogAsset

data class CatalogAsset(
    val id: String,
    val type: AssetType,
    val name: String,
    val purchasePrice: Int,
    val monthlyUpkeep: Int
)

/**
 * Asset shop catalog: universal tier with country-scaled pricing at purchase time,
 * plus verified local naming where it adds real texture.
 *
 * Flavor naming (verified terms):
 * - KE: bedsitter (East African studio rental), boda boda (motorcycle taxi)
 * - NG: self-contain (Lagos/Nigeria studio rental — Mixta Africa, Respicio legal guides)
 * - PH: bedspace (boarding-house rental — Respicio & Co. Philippines)
 * - IN: PG room (paying-guest accommodation)
 * - GB: bedsit (British studio rental term)
 * - BR: kitnet (Brazilian studio apartment)
 * - JP: 1K apartment (one room + kitchen — standard Japanese listing term)
 *
 * US, CA, FR, DE, ZA, EG, MX, ID: universal housing/vehicle names only (verified P35).
 */
object AssetCatalog {

    private val universalAssets: List<CatalogAsset> = listOf(
        CatalogAsset(
            id = "motorbike_used",
            type = AssetType.MOTORBIKE,
            name = "Used Motorbike",
            purchasePrice = 80_000,
            monthlyUpkeep = 2_000
        ),
        CatalogAsset(
            id = "motorbike_new",
            type = AssetType.MOTORBIKE,
            name = "New Motorbike",
            purchasePrice = 150_000,
            monthlyUpkeep = 3_500
        ),
        CatalogAsset(
            id = "car_used_compact",
            type = AssetType.CAR,
            name = "Used Compact Car",
            purchasePrice = 600_000,
            monthlyUpkeep = 5_000
        ),
        CatalogAsset(
            id = "car_used_mid",
            type = AssetType.CAR,
            name = "Used Mid-Size Car",
            purchasePrice = 900_000,
            monthlyUpkeep = 6_000
        ),
        CatalogAsset(
            id = "car_sedan_used",
            type = AssetType.CAR,
            name = "Used Sedan",
            purchasePrice = 1_200_000,
            monthlyUpkeep = 7_000
        ),
        CatalogAsset(
            id = "car_new",
            type = AssetType.CAR,
            name = "New Car",
            purchasePrice = 3_500_000,
            monthlyUpkeep = 12_000
        ),
        CatalogAsset(
            id = "apartment_studio",
            type = AssetType.HOUSE,
            name = "Studio Apartment",
            purchasePrice = 1_500_000,
            monthlyUpkeep = 4_000
        ),
        CatalogAsset(
            id = "apartment_1br",
            type = AssetType.HOUSE,
            name = "1BR Apartment",
            purchasePrice = 2_800_000,
            monthlyUpkeep = 8_000
        ),
        CatalogAsset(
            id = "house_suburban",
            type = AssetType.HOUSE,
            name = "Suburban House",
            purchasePrice = 6_500_000,
            monthlyUpkeep = 10_000
        ),
        CatalogAsset(
            id = "house_family",
            type = AssetType.HOUSE,
            name = "Family Home",
            purchasePrice = 15_000_000,
            monthlyUpkeep = 25_000
        )
    )

    private val kenyaAssets: List<CatalogAsset> = listOf(
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

    private val housingNameOverrides: Map<String, Map<String, String>> = mapOf(
        "NG" to mapOf("apartment_studio" to "Self-Contain"),
        "PH" to mapOf("apartment_studio" to "Bedspace"),
        "IN" to mapOf("apartment_studio" to "PG Room"),
        "GB" to mapOf("apartment_studio" to "Bedsit"),
        "BR" to mapOf("apartment_studio" to "Kitnet"),
        "JP" to mapOf("apartment_studio" to "1K Apartment")
    )

    /** Legacy flat list — all catalog entries for global lookup. */
    val items: List<CatalogAsset> by lazy {
        (kenyaAssets + universalAssets).distinctBy { it.id }
    }

    fun getAssetsForCountry(countryCode: String): List<CatalogAsset> {
        if (countryCode == "KE") return kenyaAssets
        val overrides = housingNameOverrides[countryCode] ?: emptyMap()
        return universalAssets.map { asset ->
            overrides[asset.id]?.let { localizedName -> asset.copy(name = localizedName) } ?: asset
        }
    }

    fun hasCountryFlavorAssets(countryCode: String): Boolean =
        countryCode == "KE" || countryCode in housingNameOverrides

    fun findById(catalogId: String): CatalogAsset? = items.find { it.id == catalogId }
}
