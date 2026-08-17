// app/src/main/java/com/maisha/game/data/IllustrationCatalog.kt
package com.maisha.game.data

import com.maisha.game.data.model.AchievementCategory
import com.maisha.game.data.model.Asset
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.IllustrationRef
import com.maisha.game.data.model.ResourceType

/**
 * Central map of bundled BitLife-style raster art (drawable-nodpi/img_*) and legacy vectors (ill_*).
 * Swap [resourceName] files to retheme without touching UI code.
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

    fun rasterNamed(resourceName: String): IllustrationRef =
        raster(resourceName, resourceName)

    // —— Jobs (category art shared by job ids) ——
    private val jobCategoryRaster: Map<String, IllustrationRef> = mapOf(
        "transport" to raster("job_transport", "img_job_transport"),
        "retail" to raster("job_retail", "img_job_retail"),
        "security" to raster("job_security", "img_job_security"),
        "teacher" to raster("job_teacher", "img_job_teacher"),
        "tech" to raster("job_tech", "img_job_tech"),
        "medical" to raster("job_medical", "img_job_medical"),
        "finance" to raster("job_finance", "img_job_finance"),
        "media" to raster("job_media", "img_job_media"),
        "engineering" to raster("job_engineering", "img_job_engineering"),
        "government" to raster("job_government", "img_job_government"),
        "service" to raster("job_service", "img_job_service")
    )

    private val jobIllustrations: Map<String, IllustrationRef> = mapOf(
        "matatu_conductor" to jobCategoryRaster.getValue("transport"),
        "danfo_conductor" to jobCategoryRaster.getValue("transport"),
        "jeepney_driver" to jobCategoryRaster.getValue("transport"),
        "auto_rickshaw_driver" to jobCategoryRaster.getValue("transport"),
        "angkot_driver" to jobCategoryRaster.getValue("transport"),
        "mototaxi_rider" to jobCategoryRaster.getValue("transport"),
        "driver" to jobCategoryRaster.getValue("transport"),
        "shop_attendant" to jobCategoryRaster.getValue("retail"),
        "security_guard" to jobCategoryRaster.getValue("security"),
        "teacher" to jobCategoryRaster.getValue("teacher"),
        "software_developer" to jobCategoryRaster.getValue("tech"),
        "nurse" to jobCategoryRaster.getValue("medical"),
        "accountant" to jobCategoryRaster.getValue("finance"),
        "journalist" to jobCategoryRaster.getValue("media"),
        "engineer" to jobCategoryRaster.getValue("engineering"),
        "civil_servant" to jobCategoryRaster.getValue("government")
    )

    private val assetTypeIllustrations: Map<AssetType, IllustrationRef> = mapOf(
        AssetType.HOUSE to raster("asset_house", "img_asset_house_suburban"),
        AssetType.CAR to raster("asset_car", "img_asset_car_sedan"),
        AssetType.MOTORBIKE to raster("asset_motorbike", "img_asset_motorbike_used"),
        AssetType.HEIRLOOM to raster("asset_heirloom", "img_asset_heirloom_ring")
    )

    private val catalogAssetIllustrations: Map<String, IllustrationRef> = mapOf(
        "motorbike_used" to raster("asset_motorbike_used", "img_asset_motorbike_used"),
        "boda_basic" to raster("asset_boda_basic", "img_asset_motorbike_used"),
        "motorbike_new" to raster("asset_motorbike_new", "img_asset_motorbike_new"),
        "boda_new" to raster("asset_boda_new", "img_asset_motorbike_new"),
        "car_used_compact" to raster("asset_car_compact", "img_asset_car_compact"),
        "car_vitz" to raster("asset_car_vitz", "img_asset_car_compact"),
        "car_used_mid" to raster("asset_car_mid", "img_asset_car_mid"),
        "car_probox" to raster("asset_car_probox", "img_asset_car_mid"),
        "car_sedan_used" to raster("asset_car_sedan", "img_asset_car_sedan"),
        "car_new" to raster("asset_car_new", "img_asset_car_new"),
        "apartment_studio" to raster("asset_house_studio", "img_asset_house_studio"),
        "bedsitter_rongai" to raster("asset_bedsitter", "img_asset_house_studio"),
        "apartment_1br" to raster("asset_house_apartment", "img_asset_house_apartment"),
        "apartment_kasarani" to raster("asset_apartment_kasarani", "img_asset_house_apartment"),
        "house_suburban" to raster("asset_house_suburban", "img_asset_house_suburban"),
        "house_thika" to raster("asset_house_thika", "img_asset_house_suburban"),
        "house_family" to raster("asset_house_family", "img_asset_house_luxury"),
        "house_karen" to raster("asset_house_karen", "img_asset_house_luxury"),
        "jp_tokyo_micro" to raster("asset_jp_micro", "img_asset_house_studio"),
        "gb_london_flat" to raster("asset_gb_flat", "img_asset_house_apartment"),
        "us_suburban_home" to raster("asset_us_suburban", "img_asset_house_suburban"),
        "fr_haussmann_flat" to raster("asset_fr_haussmann", "img_asset_house_apartment"),
        "de_altbau_wohnung" to raster("asset_de_altbau", "img_asset_house_apartment"),
        "br_cobertura" to raster("asset_br_cobertura", "img_asset_house_condo"),
        "mx_casa_colonia" to raster("asset_mx_casa", "img_asset_house_suburban"),
        "ca_condo_tower" to raster("asset_ca_condo", "img_asset_house_condo"),
        "heirloom_pocket_watch" to raster("asset_heirloom_watch", "img_asset_heirloom_watch"),
        "heirloom_rare_gemstone" to raster("asset_heirloom_gem", "img_asset_heirloom_gem"),
        "heirloom_ancient_manuscript" to raster("asset_heirloom_scroll", "img_asset_heirloom_scroll"),
        "heirloom_ivory_comb" to raster("asset_heirloom_comb", "img_asset_heirloom_comb"),
        "heirloom_gold_signet" to raster("asset_heirloom_ring", "img_asset_heirloom_ring")
    )

    private val achievementIllustrations: Map<AchievementCategory, IllustrationRef> = mapOf(
        AchievementCategory.CAREER to raster("achievement_career", "img_achievement_career"),
        AchievementCategory.EDUCATION to raster("achievement_education", "img_achievement_education"),
        AchievementCategory.FAMILY to raster("achievement_family", "img_achievement_family"),
        AchievementCategory.WEALTH to raster("achievement_wealth", "img_achievement_wealth"),
        AchievementCategory.LONGEVITY to raster("achievement_longevity", "img_achievement_longevity"),
        AchievementCategory.MISCHIEF to raster("achievement_mischief", "img_achievement_mischief"),
        AchievementCategory.WORLDLY to raster("achievement_worldly", "img_achievement_worldly")
    )

    private val achievementIconRaster: Map<String, IllustrationRef> = mapOf(
        "briefcase" to achievementIllustrations.getValue(AchievementCategory.CAREER),
        "office" to achievementIllustrations.getValue(AchievementCategory.CAREER),
        "shuffle" to achievementIllustrations.getValue(AchievementCategory.CAREER),
        "graduation_cap" to achievementIllustrations.getValue(AchievementCategory.EDUCATION),
        "star" to achievementIllustrations.getValue(AchievementCategory.EDUCATION),
        "door" to achievementIllustrations.getValue(AchievementCategory.EDUCATION),
        "rings" to achievementIllustrations.getValue(AchievementCategory.FAMILY),
        "baby" to achievementIllustrations.getValue(AchievementCategory.FAMILY),
        "family" to achievementIllustrations.getValue(AchievementCategory.FAMILY),
        "heart" to achievementIllustrations.getValue(AchievementCategory.FAMILY),
        "inseparable" to achievementIllustrations.getValue(AchievementCategory.FAMILY),
        "tree" to achievementIllustrations.getValue(AchievementCategory.FAMILY),
        "dynasty" to achievementIllustrations.getValue(AchievementCategory.FAMILY),
        "handshake" to achievementIllustrations.getValue(AchievementCategory.FAMILY),
        "friends" to achievementIllustrations.getValue(AchievementCategory.FAMILY),
        "coins" to achievementIllustrations.getValue(AchievementCategory.WEALTH),
        "million" to achievementIllustrations.getValue(AchievementCategory.WEALTH),
        "house" to achievementIllustrations.getValue(AchievementCategory.WEALTH),
        "portfolio" to achievementIllustrations.getValue(AchievementCategory.WEALTH),
        "calendar" to achievementIllustrations.getValue(AchievementCategory.LONGEVITY),
        "sunset" to achievementIllustrations.getValue(AchievementCategory.LONGEVITY),
        "crown" to achievementIllustrations.getValue(AchievementCategory.LONGEVITY),
        "handcuffs" to achievementIllustrations.getValue(AchievementCategory.MISCHIEF),
        "repeat" to achievementIllustrations.getValue(AchievementCategory.MISCHIEF),
        "shield" to achievementIllustrations.getValue(AchievementCategory.MISCHIEF),
        "globe" to raster("achievement_globe", "img_achievement_worldly"),
        "world" to raster("achievement_globe", "img_achievement_worldly")
    )

    private val defaultJob = jobCategoryRaster.getValue("service")
    private val defaultAsset = raster("asset_default", "img_asset_car_sedan")
    private val defaultAchievement = achievementIllustrations.getValue(AchievementCategory.CAREER)

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

    fun getIllustrationForAchievementIcon(iconName: String): IllustrationRef =
        achievementIconRaster[iconName] ?: defaultAchievement

    fun allJobIllustrations(): List<Pair<String, IllustrationRef>> =
        JobPool.jobs.map { job -> job.id to getIllustrationForJob(job.id) }

    fun allCatalogAssetIllustrations(): List<Pair<String, IllustrationRef>> =
        AssetCatalog.items.map { item -> item.id to getIllustrationForCatalogAsset(item.id) }

    /** All unique raster bundles the app expects (for validation tests). */
    fun allRasterResourceNames(): Set<String> =
        buildSet {
            jobCategoryRaster.values.forEach { add(it.resourceName) }
            catalogAssetIllustrations.values.forEach { add(it.resourceName) }
            achievementIllustrations.values.forEach { add(it.resourceName) }
            addAll(
                listOf(
                    "img_empty_family", "img_empty_assets", "img_empty_actions",
                    "img_empty_achievements", "img_empty_retired",
                    "img_onboarding_welcome", "img_onboarding_age_up", "img_onboarding_choices",
                    "img_onboarding_world", "img_onboarding_ready",
                    "img_stat_health", "img_stat_happiness", "img_stat_smarts", "img_stat_looks",
                    "img_stat_money", "img_stat_relationship", "img_stat_karma", "img_stat_condition",
                    "img_nav_life", "img_nav_family", "img_nav_career", "img_nav_assets", "img_nav_actions",
                    "img_icon_globe", "img_icon_clinic", "img_icon_hospital",
                    "img_icon_crime_pickpocket", "img_icon_crime_shoplift", "img_icon_crime_fraud"
                )
            )
        }
}
