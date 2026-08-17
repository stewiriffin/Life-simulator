// app/src/main/java/com/maisha/game/data/IllustrationCatalog.kt (modified — per-catalog asset icons)
package com.maisha.game.data

import com.maisha.game.data.model.AchievementCategory
import com.maisha.game.data.model.Asset
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.IllustrationRef
import com.maisha.game.data.model.ResourceType

/**
 * Maps game entities to illustration resources. Vector drawables in res/drawable/ill_* —
 * replace resourceName only when importing final art.
 */
object IllustrationCatalog {

    private fun vector(id: String, resourceName: String) = IllustrationRef(
        id = id,
        resourceType = ResourceType.VECTOR_DRAWABLE,
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
        AssetType.HOUSE to vector("asset_house", "ill_asset_house"),
        AssetType.CAR to vector("asset_car", "ill_asset_car"),
        AssetType.MOTORBIKE to vector("asset_motorbike", "ill_asset_motorbike"),
        AssetType.HEIRLOOM to vector("asset_heirloom", "ill_asset_heirloom_ring")
    )

    /** Per [AssetCatalog] id — swap drawable files without touching call sites. */
    private val catalogAssetIllustrations: Map<String, IllustrationRef> = mapOf(
        // Motorbikes
        "motorbike_used" to vector("asset_motorbike_used", "ill_asset_motorbike_used"),
        "boda_basic" to vector("asset_boda_basic", "ill_asset_motorbike_used"),
        "motorbike_new" to vector("asset_motorbike_new", "ill_asset_motorbike_new"),
        "boda_new" to vector("asset_boda_new", "ill_asset_motorbike_new"),
        // Cars
        "car_used_compact" to vector("asset_car_compact", "ill_asset_car_compact"),
        "car_vitz" to vector("asset_car_vitz", "ill_asset_car_compact"),
        "car_used_mid" to vector("asset_car_mid", "ill_asset_car_mid"),
        "car_probox" to vector("asset_car_probox", "ill_asset_car_mid"),
        "car_sedan_used" to vector("asset_car_sedan", "ill_asset_car_sedan"),
        "car_new" to vector("asset_car_new", "ill_asset_car_new"),
        // Houses — universal + Kenya
        "apartment_studio" to vector("asset_house_studio", "ill_asset_house_studio"),
        "bedsitter_rongai" to vector("asset_bedsitter", "ill_asset_house_studio"),
        "apartment_1br" to vector("asset_house_apartment", "ill_asset_house_apartment"),
        "apartment_kasarani" to vector("asset_apartment_kasarani", "ill_asset_house_apartment"),
        "house_suburban" to vector("asset_house_suburban", "ill_asset_house_suburban"),
        "house_thika" to vector("asset_house_thika", "ill_asset_house_suburban"),
        "house_family" to vector("asset_house_family", "ill_asset_house_luxury"),
        "house_karen" to vector("asset_house_karen", "ill_asset_house_luxury"),
        // Country exclusives
        "jp_tokyo_micro" to vector("asset_jp_micro", "ill_asset_house_studio"),
        "gb_london_flat" to vector("asset_gb_flat", "ill_asset_house_apartment"),
        "us_suburban_home" to vector("asset_us_suburban", "ill_asset_house_suburban"),
        "fr_haussmann_flat" to vector("asset_fr_haussmann", "ill_asset_house_apartment"),
        "de_altbau_wohnung" to vector("asset_de_altbau", "ill_asset_house_apartment"),
        "br_cobertura" to vector("asset_br_cobertura", "ill_asset_house_condo"),
        "mx_casa_colonia" to vector("asset_mx_casa", "ill_asset_house_suburban"),
        "ca_condo_tower" to vector("asset_ca_condo", "ill_asset_house_condo"),
        // Heirlooms
        "heirloom_pocket_watch" to vector("asset_heirloom_watch", "ill_asset_heirloom_watch"),
        "heirloom_rare_gemstone" to vector("asset_heirloom_gem", "ill_asset_heirloom_gem"),
        "heirloom_ancient_manuscript" to vector("asset_heirloom_scroll", "ill_asset_heirloom_scroll"),
        "heirloom_ivory_comb" to vector("asset_heirloom_comb", "ill_asset_heirloom_comb"),
        "heirloom_gold_signet" to vector("asset_heirloom_ring", "ill_asset_heirloom_ring")
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
    private val defaultAsset = vector("asset_default", "ill_asset_car")
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
