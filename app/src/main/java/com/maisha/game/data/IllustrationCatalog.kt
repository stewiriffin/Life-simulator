// app/src/main/java/com/maisha/game/data/IllustrationCatalog.kt (new)
package com.maisha.game.data

import com.maisha.game.data.model.AchievementCategory
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.IllustrationRef
import com.maisha.game.data.model.ResourceType

/**
 * Maps game entities to illustration resources. Placeholder drawables in res/drawable/ill_* —
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
        "shop_attendant" to vector("job_shop_attendant", "ill_job_retail"),
        "security_guard" to vector("job_security_guard", "ill_job_security"),
        "waiter" to vector("job_waiter", "ill_job_service"),
        "teacher" to vector("job_teacher", "ill_job_teacher"),
        "software_developer" to vector("job_software_developer", "ill_job_tech"),
        "nurse" to vector("job_nurse", "ill_job_medical"),
        "accountant" to vector("job_accountant", "ill_job_finance"),
        "bank_clerk" to vector("job_bank_clerk", "ill_job_finance"),
        "journalist" to vector("job_journalist", "ill_job_media"),
        "engineer" to vector("job_engineer", "ill_job_engineering"),
        "civil_servant" to vector("job_civil_servant", "ill_job_government")
    )

    private val assetTypeIllustrations: Map<AssetType, IllustrationRef> = mapOf(
        AssetType.HOUSE to vector("asset_house", "ill_asset_house"),
        AssetType.CAR to vector("asset_car", "ill_asset_car"),
        AssetType.MOTORBIKE to vector("asset_motorbike", "ill_asset_motorbike")
    )

    private val achievementIllustrations: Map<AchievementCategory, IllustrationRef> = mapOf(
        AchievementCategory.CAREER to vector("achievement_career", "ill_achievement_career"),
        AchievementCategory.EDUCATION to vector("achievement_education", "ill_achievement_education"),
        AchievementCategory.FAMILY to vector("achievement_family", "ill_achievement_family"),
        AchievementCategory.WEALTH to vector("achievement_wealth", "ill_achievement_wealth"),
        AchievementCategory.LONGEVITY to vector("achievement_longevity", "ill_achievement_longevity"),
        AchievementCategory.MISCHIEF to vector("achievement_mischief", "ill_achievement_mischief")
    )

    private val defaultJob = vector("job_default", "ill_job_service")
    private val defaultAsset = vector("asset_default", "ill_asset_car")
    private val defaultAchievement = vector("achievement_default", "ill_achievement_career")

    fun getIllustrationForJob(jobId: String): IllustrationRef =
        jobIllustrations[jobId] ?: defaultJob

    fun getIllustrationForAsset(assetType: AssetType): IllustrationRef =
        assetTypeIllustrations[assetType] ?: defaultAsset

    fun getIllustrationForCatalogAsset(catalogId: String): IllustrationRef {
        val catalog = AssetCatalog.findById(catalogId)
        return if (catalog != null) getIllustrationForAsset(catalog.type) else defaultAsset
    }

    fun getIllustrationForAchievementCategory(category: AchievementCategory): IllustrationRef =
        achievementIllustrations[category] ?: defaultAchievement

    /** Ensures every JobPool entry has a mapping (for tests / validation). */
    fun allJobIllustrations(): List<Pair<String, IllustrationRef>> =
        JobPool.jobs.map { job -> job.id to getIllustrationForJob(job.id) }

    fun allCatalogAssetIllustrations(): List<Pair<String, IllustrationRef>> =
        AssetCatalog.items.map { item -> item.id to getIllustrationForAsset(item.type) }
}
