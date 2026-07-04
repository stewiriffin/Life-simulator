// app/src/main/java/com/maisha/game/data/AssetCatalog.kt (modified — country-aware universal + flavor naming)
package com.maisha.game.data

import com.maisha.game.data.model.AssetType

typealias AssetCatalogEntry = CatalogAsset

data class CatalogAsset(
    val id: String,
    val type: AssetType,
    val name: String,
    val purchasePrice: Int,
    val monthlyUpkeep: Int,
    val isHeirloom: Boolean = false,
    val isPurchasable: Boolean = true
)

/**
 * Asset shop catalog: universal tier with country-scaled pricing at purchase time
 * ([EconomyScaler] applied at buy), plus local naming and exclusive listings.
 *
 * Flavor naming (verified terms):
 * - KE: bedsitter (East African studio rental), boda boda (motorcycle taxi)
 * - NG: self-contain (Lagos/Nigeria studio rental)
 * - PH: bedspace (boarding-house rental)
 * - IN: PG room (paying-guest accommodation)
 * - GB: bedsit / London Flat
 * - BR: kitnet
 * - JP: 1K / Tokyo Micro-Apartment
 * - US: Suburban American Home
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

    /** Rare generational items — acquired only via events, never sold in the shop. */
    private val heirloomAssets: List<CatalogAsset> = listOf(
        CatalogAsset(
            id = "heirloom_pocket_watch",
            type = AssetType.HEIRLOOM,
            name = "18th Century Pocket Watch",
            purchasePrice = 250_000,
            monthlyUpkeep = 0,
            isHeirloom = true,
            isPurchasable = false
        ),
        CatalogAsset(
            id = "heirloom_rare_gemstone",
            type = AssetType.HEIRLOOM,
            name = "Rare Gemstone",
            purchasePrice = 400_000,
            monthlyUpkeep = 0,
            isHeirloom = true,
            isPurchasable = false
        ),
        CatalogAsset(
            id = "heirloom_ancient_manuscript",
            type = AssetType.HEIRLOOM,
            name = "Ancient Manuscript",
            purchasePrice = 180_000,
            monthlyUpkeep = 0,
            isHeirloom = true,
            isPurchasable = false
        ),
        CatalogAsset(
            id = "heirloom_ivory_comb",
            type = AssetType.HEIRLOOM,
            name = "Carved Ivory Comb",
            purchasePrice = 120_000,
            monthlyUpkeep = 0,
            isHeirloom = true,
            isPurchasable = false
        ),
        CatalogAsset(
            id = "heirloom_gold_signet",
            type = AssetType.HEIRLOOM,
            name = "Gold Signet Ring",
            purchasePrice = 320_000,
            monthlyUpkeep = 0,
            isHeirloom = true,
            isPurchasable = false
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
        "JP" to mapOf("apartment_studio" to "1K Apartment"),
        "MX" to mapOf("apartment_studio" to "Estudio"),
        "FR" to mapOf("apartment_studio" to "Studio Parisien"),
        "DE" to mapOf("apartment_studio" to "Einzimmerwohnung"),
        "CA" to mapOf("house_suburban" to "Suburban Bungalow"),
        "EG" to mapOf("apartment_studio" to "Studio Flat — Cairo"),
        "ID" to mapOf("apartment_studio" to "Kost Room"),
        "ZA" to mapOf("apartment_studio" to "Bachelor Flat")
    )

    /** Exclusive listings only offered in that country (prices still EconomyScaler-scaled at purchase). */
    private val countryExclusiveAssets: Map<String, List<CatalogAsset>> = mapOf(
        "JP" to listOf(
            CatalogAsset(
                id = "jp_tokyo_micro",
                type = AssetType.HOUSE,
                name = "Tokyo Micro-Apartment",
                purchasePrice = 2_200_000,
                monthlyUpkeep = 6_000
            )
        ),
        "GB" to listOf(
            CatalogAsset(
                id = "gb_london_flat",
                type = AssetType.HOUSE,
                name = "London Flat",
                purchasePrice = 4_500_000,
                monthlyUpkeep = 12_000
            )
        ),
        "US" to listOf(
            CatalogAsset(
                id = "us_suburban_home",
                type = AssetType.HOUSE,
                name = "Suburban American Home",
                purchasePrice = 7_500_000,
                monthlyUpkeep = 14_000
            )
        ),
        "FR" to listOf(
            CatalogAsset(
                id = "fr_haussmann_flat",
                type = AssetType.HOUSE,
                name = "Haussmann Flat",
                purchasePrice = 5_000_000,
                monthlyUpkeep = 11_000
            )
        ),
        "DE" to listOf(
            CatalogAsset(
                id = "de_altbau_wohnung",
                type = AssetType.HOUSE,
                name = "Altbau Wohnung",
                purchasePrice = 4_200_000,
                monthlyUpkeep = 9_000
            )
        ),
        "BR" to listOf(
            CatalogAsset(
                id = "br_cobertura",
                type = AssetType.HOUSE,
                name = "Cobertura Apartment",
                purchasePrice = 8_000_000,
                monthlyUpkeep = 15_000
            )
        ),
        "MX" to listOf(
            CatalogAsset(
                id = "mx_casa_colonia",
                type = AssetType.HOUSE,
                name = "Casa de Colonia",
                purchasePrice = 5_500_000,
                monthlyUpkeep = 10_000
            )
        ),
        "CA" to listOf(
            CatalogAsset(
                id = "ca_condo_tower",
                type = AssetType.HOUSE,
                name = "Downtown Condo",
                purchasePrice = 6_000_000,
                monthlyUpkeep = 11_000
            )
        )
    )

    /** Legacy flat list — all catalog entries for global lookup. */
    val items: List<CatalogAsset> by lazy {
        (
            kenyaAssets +
                universalAssets +
                heirloomAssets +
                countryExclusiveAssets.values.flatten()
            ).distinctBy { it.id }
    }

    fun getAssetsForCountry(countryCode: String): List<CatalogAsset> =
        getPurchasableAssetsForCountry(countryCode)

    fun getPurchasableAssetsForCountry(countryCode: String): List<CatalogAsset> {
        val base = if (countryCode == "KE") kenyaAssets else {
            val overrides = housingNameOverrides[countryCode] ?: emptyMap()
            universalAssets.map { asset ->
                overrides[asset.id]?.let { localizedName -> asset.copy(name = localizedName) } ?: asset
            }
        }
        val exclusive = countryExclusiveAssets[countryCode].orEmpty()
        return (base + exclusive).filter { it.isPurchasable }
    }

    fun findHeirloomById(catalogId: String): CatalogAsset? =
        heirloomAssets.find { it.id == catalogId }

    fun getHeirloomAssets(): List<CatalogAsset> = heirloomAssets

    fun hasCountryFlavorAssets(countryCode: String): Boolean =
        countryCode == "KE" ||
            countryCode in housingNameOverrides ||
            countryCode in countryExclusiveAssets

    fun findById(catalogId: String): CatalogAsset? = items.find { it.id == catalogId }
}
