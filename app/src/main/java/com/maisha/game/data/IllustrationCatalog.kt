// app/src/main/java/com/maisha/game/data/IllustrationCatalog.kt (modified — per-catalog asset icons)
package com.maisha.game.data

import com.maisha.game.data.model.AchievementCategory
import com.maisha.game.data.model.Asset
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.IllustrationRef
import com.maisha.game.data.model.ResourceType

/**
 * Maps game entities to illustration resources. Asset shop art uses raster PNGs in
 * res/drawable-nodpi/img_asset_* (BitLife-style hero images). Jobs still use ill_* vectors.
 */
object IllustrationCatalog {

    private fun vector(id: String, resourceName: String) = IllustrationRef(
        id = id,
        resourceType = ResourceType.VECTOR_DRAWABLE,
        resourceName = resourceName
    )

    private fun raster(id: String, resourceName: String) = IllustrationRef(
        id = id,
        resourceType = ResourceType.RASTER,
        resourceName = resourceName
    )

    private val jobIllustrations: Map<String, IllustrationRef> = mapOf(
        "matatu_conductor" to vector("job_matatu_conductor", "ill_job_transport"),
        "danfo_conductor" to vector("job_danfo_conductor", "ill_job_transport"),
        "jeepney_driver" to vector("job_jeepney_driver", "ill_job_transport"),
        "auto_rickshaw_driver" to vector("job_auto_rickshaw_driver", "ill_job_transport"),
        "angkot_driver" to vector("job_angkot_driver", "ill_job_transport"),
        "mototaxi_rider" to vector("job_mototaxi_rider", "ill_job_transport"),
        "driver" to vector("job_driver", "ill_job_transport"),
        "shop_attendant" to vector("job_shop_attendant", "ill_job_retail"),
        "security_guard" to vector("job_security_guard", "ill_job_security"),
        "teacher" to vector("job_teacher", "ill_job_teacher"),
        "software_developer" to vector("job_software_developer", "ill_job_tech"),
        "nurse" to vector("job_nurse", "ill_job_medical"),
        "accountant" to vector("job_accountant", "ill_job_finance"),
        "journalist" to vector("job_journalist", "ill_job_media"),
        "engineer" to vector("job_engineer", "ill_job_engineering"),
        "civil_servant" to vector("job_civil_servant", "ill_job_government")
    )

    private val assetTypeIllustrations: Map<AssetType, IllustrationRef> = mapOf(
        AssetType.HOUSE to raster("asset_house", "img_asset_house_suburban"),
        AssetType.CAR to raster("asset_car", "img_asset_car_sedan"),
        AssetType.MOTORBIKE to raster("asset_motorbike", "img_asset_motorbike_used"),
        AssetType.HEIRLOOM to raster("asset_heirloom", "img_asset_heirloom_ring")
    )

    /** Per [AssetCatalog] id — PNG hero art in drawable-nodpi (swap files to retheme). */
    private val catalogAssetIllustrations: Map<String, IllustrationRef> = mapOf(
        // Motorbikes
        "motorbike_used" to raster("asset_motorbike_used", "img_asset_motorbike_used"),
        "boda_basic" to raster("asset_boda_basic", "img_asset_motorbike_used"),
        "motorbike_new" to raster("asset_motorbike_new", "img_asset_motorbike_new"),
        "boda_new" to raster("asset_boda_new", "img_asset_motorbike_new"),
        // Cars
        "car_used_compact" to raster("asset_car_compact", "img_asset_car_compact"),
        "car_vitz" to raster("asset_car_vitz", "img_asset_car_compact"),
        "car_used_mid" to raster("asset_car_mid", "img_asset_car_mid"),
        "car_probox" to raster("asset_car_probox", "img_asset_car_mid"),
        "car_sedan_used" to raster("asset_car_sedan", "img_asset_car_sedan"),
        "car_new" to raster("asset_car_new", "img_asset_car_new"),
        // Houses — universal + Kenya
        "apartment_studio" to raster("asset_house_studio", "img_asset_house_studio"),
        "bedsitter_rongai" to raster("asset_bedsitter", "img_asset_house_studio"),
        "apartment_1br" to raster("asset_house_apartment", "img_asset_house_apartment"),
        "apartment_kasarani" to raster("asset_apartment_kasarani", "img_asset_house_apartment"),
        "house_suburban" to raster("asset_house_suburban", "img_asset_house_suburban"),
        "house_thika" to raster("asset_house_thika", "img_asset_house_suburban"),
        "house_family" to raster("asset_house_family", "img_asset_house_luxury"),
        "house_karen" to raster("asset_house_karen", "img_asset_house_luxury"),
        // Country exclusives
        "jp_tokyo_micro" to raster("asset_jp_micro", "img_asset_house_studio"),
        "gb_london_flat" to raster("asset_gb_flat", "img_asset_house_apartment"),
        "us_suburban_home" to raster("asset_us_suburban", "img_asset_house_suburban"),
        "fr_haussmann_flat" to raster("asset_fr_haussmann", "img_asset_house_apartment"),
        "de_altbau_wohnung" to raster("asset_de_altbau", "img_asset_house_apartment"),
        "br_cobertura" to raster("asset_br_cobertura", "img_asset_house_condo"),
        "mx_casa_colonia" to raster("asset_mx_casa", "img_asset_house_suburban"),
        "ca_condo_tower" to raster("asset_ca_condo", "img_asset_house_condo"),
        // Heirlooms
        "heirloom_pocket_watch" to raster("asset_heirloom_watch", "img_asset_heirloom_watch"),
        "heirloom_rare_gemstone" to raster("asset_heirloom_gem", "img_asset_heirloom_gem"),
        "heirloom_ancient_manuscript" to raster("asset_heirloom_scroll", "img_asset_heirloom_scroll"),
        "heirloom_ivory_comb" to raster("asset_heirloom_comb", "img_asset_heirloom_comb"),
        "heirloom_gold_signet" to raster("asset_heirloom_ring", "img_asset_heirloom_ring")
    )

    private val achievementIllustrations: Map<AchievementCategory, IllustrationRef> = mapOf(
        AchievementCategory.CAREER to vector("achievement_career", "ill_achievement_career"),
        AchievementCategory.EDUCATION to vector("achievement_education", "ill_achievement_education"),
        AchievementCategory.FAMILY to vector("achievement_family", "ill_achievement_family"),
        AchievementCategory.WEALTH to vector("achievement_wealth", "ill_achievement_wealth"),
        AchievementCategory.LONGEVITY to vector("achievement_longevity", "ill_achievement_longevity"),
        AchievementCategory.MISCHIEF to vector("achievement_mischief", "ill_achievement_mischief"),
        AchievementCategory.WORLDLY to vector("achievement_worldly", "ill_achievement_family")
    )

    private val defaultJob = vector("job_default", "ill_job_service")
    private val defaultAsset = raster("asset_default", "img_asset_car_sedan")
    private val defaultAchievement = vector("achievement_default", "ill_achievement_career")

    fun getIllustrationForJob(jobId: String): IllustrationRef =
        jobIllustrations[jobId] ?: defaultJob

    fun getIllustrationForAsset(assetType: AssetType): IllustrationRef =
        assetTypeIllustrations[assetType] ?: defaultAsset

    fun getIllustrationForCatalogAsset(catalogId: String): IllustrationRef =
        catalogAssetIllustrations[catalogId]
            ?: AssetCatalog.findById(catalogId)?.type?.let(::getIllustrationForAsset)
            ?: defaultAsset

    fun getIllustrationForOwnedAsset(asset: Asset): IllustrationRef {
        asset.catalogId?.let { return getIllustrationForCatalogAsset(it) }
        AssetCatalog.items.find { catalog ->
            catalog.type == asset.type && catalog.name == asset.name
        }?.let { return getIllustrationForCatalogAsset(it.id) }
        return getIllustrationForAsset(asset.type)
    }

    fun getIllustrationForAchievementCategory(category: AchievementCategory): IllustrationRef =
        achievementIllustrations[category] ?: defaultAchievement

    /** Ensures every JobPool entry has a mapping (for tests / validation). */
    fun allJobIllustrations(): List<Pair<String, IllustrationRef>> =
        JobPool.jobs.map { job -> job.id to getIllustrationForJob(job.id) }

    fun allCatalogAssetIllustrations(): List<Pair<String, IllustrationRef>> =
        AssetCatalog.items.map { item -> item.id to getIllustrationForCatalogAsset(item.id) }
}
