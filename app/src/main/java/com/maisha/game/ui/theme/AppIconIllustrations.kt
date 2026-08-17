package com.maisha.game.ui.theme

import androidx.compose.ui.graphics.vector.ImageVector
import com.maisha.game.data.IllustrationCatalog
import com.maisha.game.data.model.IllustrationRef
import com.maisha.game.ui.components.MainTab
import com.maisha.game.ui.components.StatType

/**
 * Maps [AppIcons] vectors to BitLife-style raster art in [IllustrationCatalog].
 */
object AppIconIllustrations {

    fun refFor(icon: ImageVector): IllustrationRef? = iconToResource[icon]?.let {
        IllustrationCatalog.rasterNamed(it)
    }

    fun refForStat(type: StatType): IllustrationRef = IllustrationCatalog.rasterNamed(
        when (type) {
            StatType.HEALTH -> "img_stat_health"
            StatType.HAPPINESS -> "img_stat_happiness"
            StatType.SMARTS -> "img_stat_smarts"
            StatType.LOOKS -> "img_stat_looks"
            StatType.MONEY, StatType.NET_WORTH -> "img_stat_money"
            StatType.RELATIONSHIP -> "img_stat_relationship"
            StatType.CONDITION -> "img_stat_condition"
            StatType.PERFORMANCE -> "img_achievement_career"
            StatType.FOLLOWERS -> "img_stat_looks"
            StatType.SKILL -> "img_stat_smarts"
            StatType.KARMA -> "img_stat_karma"
        }
    )

    fun refForMainTab(tab: MainTab): IllustrationRef = IllustrationCatalog.rasterNamed(
        when (tab) {
            MainTab.LIFE -> "img_nav_life"
            MainTab.FAMILY -> "img_nav_family"
            MainTab.CAREER -> "img_nav_career"
            MainTab.ASSETS -> "img_nav_assets"
            MainTab.ACTIONS -> "img_nav_actions"
        }
    )

    private val iconToResource: Map<ImageVector, String> = mapOf(
        AppIcons.Health to "img_stat_health",
        AppIcons.Happiness to "img_stat_happiness",
        AppIcons.Smarts to "img_stat_smarts",
        AppIcons.Looks to "img_stat_looks",
        AppIcons.Money to "img_stat_money",
        AppIcons.Relationship to "img_stat_relationship",
        AppIcons.Condition to "img_stat_condition",
        AppIcons.Performance to "img_achievement_career",
        AppIcons.Career to "img_achievement_career",
        AppIcons.Education to "img_achievement_education",
        AppIcons.Family to "img_achievement_family",
        AppIcons.Wealth to "img_achievement_wealth",
        AppIcons.Longevity to "img_achievement_longevity",
        AppIcons.Mischief to "img_achievement_mischief",
        AppIcons.Achievements to "img_empty_achievements",
        AppIcons.NavLife to "img_nav_life",
        AppIcons.NavFamily to "img_nav_family",
        AppIcons.NavCareer to "img_nav_career",
        AppIcons.NavAssets to "img_nav_assets",
        AppIcons.NavActions to "img_nav_actions",
        AppIcons.HealthClinic to "img_icon_clinic",
        AppIcons.HealthHospital to "img_icon_hospital",
        AppIcons.CrimePickpocket to "img_icon_crime_pickpocket",
        AppIcons.CrimeShoplift to "img_icon_crime_shoplift",
        AppIcons.CrimeFraud to "img_icon_crime_fraud",
        AppIcons.AssetsEmpty to "img_empty_assets",
        AppIcons.SpendTime to "img_stat_relationship",
        AppIcons.Gift to "img_achievement_family",
        AppIcons.Travel to "img_achievement_worldly",
        AppIcons.Compliment to "img_stat_happiness",
        AppIcons.Argue to "img_achievement_mischief",
        AppIcons.AskMoney to "img_stat_money",
        AppIcons.Advice to "img_stat_smarts",
        AppIcons.Prank to "img_achievement_mischief",
        AppIcons.SetUpDate to "img_stat_relationship",
        AppIcons.Insult to "img_achievement_mischief",
        AppIcons.Peaceful to "img_stat_happiness"
    )
}
