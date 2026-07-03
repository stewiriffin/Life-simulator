// app/src/main/java/com/maisha/game/ui/theme/AppIcons.kt (new)
package com.maisha.game.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.maisha.game.data.model.AchievementCategory
import com.maisha.game.data.model.CrimeType
import com.maisha.game.domain.InteractionType
import com.maisha.game.ui.components.MainTab
import com.maisha.game.ui.components.StatType

/**
 * Maisha custom icon set — simple geometric placeholders using theme-aligned line art.
 * Swap path data or replace with @drawable vectors later; keep these names stable.
 *
 * Icon-to-concept mapping (for commissioning final art):
 * - Stats: health=heart, happiness=sun, smarts=book, looks=sparkle, money=coin
 * - Achievement categories: career=briefcase, education=cap, family=house+heart,
 *   wealth=coin-stack, longevity=hourglass, mischief=mask
 * - Bottom nav: life=path, family=people, career=briefcase, assets=chest, actions=bolt
 * - Actions: crime=mask/wallet, health=cross, clinic=hospital cross
 * - Interactions: spend-time=clock, gift=box, travel=plane, etc.
 */
object AppIcons {

    // —— Stats ——
    val Health: ImageVector = icon("Health") {
        moveTo(12f, 20f)
        curveToRelative(-3.5f, -2.5f, -6f, -5f, -6f, -8.5f)
        curveTo(6f, 8f, 8.5f, 6f, 12f, 9f)
        curveToRelative(3.5f, -3f, 6f, -1f, 6f, 2.5f)
        curveTo(18f, 15f, 15.5f, 17.5f, 12f, 20f)
        close()
    }

    val Happiness: ImageVector = icon("Happiness") {
        moveTo(12f, 3f)
        arcTo(9f, 9f, 0f, true, true, 12f, 21f)
        arcTo(9f, 9f, 0f, true, true, 12f, 3f)
        close()
        moveTo(8.5f, 10.5f)
        lineTo(8.5f, 10.5f)
        moveTo(15.5f, 10.5f)
        lineTo(15.5f, 10.5f)
        moveTo(8.5f, 14f)
        curveTo(9.5f, 16f, 14.5f, 16f, 15.5f, 14f)
    }

    val Smarts: ImageVector = icon("Smarts") {
        moveTo(6f, 4f)
        lineTo(18f, 4f)
        lineTo(18f, 18f)
        lineTo(6f, 18f)
        close()
        moveTo(8f, 7f)
        lineTo(16f, 7f)
        moveTo(8f, 10f)
        lineTo(14f, 10f)
        moveTo(8f, 13f)
        lineTo(16f, 13f)
    }

    val Looks: ImageVector = icon("Looks") {
        moveTo(12f, 2f)
        lineTo(14.5f, 9f)
        lineTo(22f, 9f)
        lineTo(16f, 13.5f)
        lineTo(18.5f, 20.5f)
        lineTo(12f, 16f)
        lineTo(5.5f, 20.5f)
        lineTo(8f, 13.5f)
        lineTo(2f, 9f)
        lineTo(9.5f, 9f)
        close()
    }

    val Money: ImageVector = icon("Money") {
        moveTo(12f, 3f)
        arcTo(9f, 9f, 0f, true, true, 12f, 21f)
        arcTo(9f, 9f, 0f, true, true, 12f, 3f)
        close()
        moveTo(12f, 7f)
        lineTo(12f, 17f)
        moveTo(9.5f, 9.5f)
        curveTo(9.5f, 8.5f, 10.5f, 8f, 12f, 8f)
        curveTo(13.5f, 8f, 14.5f, 8.5f, 14.5f, 9.5f)
        curveTo(14.5f, 10.5f, 13.5f, 11f, 12f, 11f)
        curveTo(10.5f, 11f, 9.5f, 11.5f, 9.5f, 12.5f)
        curveTo(9.5f, 13.5f, 10.5f, 14f, 12f, 14f)
        curveTo(13.5f, 14f, 14.5f, 13.5f, 14.5f, 12.5f)
    }

    val Relationship: ImageVector = icon("Relationship") {
        moveTo(12f, 20f)
        curveToRelative(-2.5f, -1.8f, -5f, -4f, -5f, -6.5f)
        curveTo(7f, 10f, 9f, 8f, 12f, 10.5f)
        curveToRelative(3f, -2.5f, 5f, -0.5f, 5f, 3.5f)
        curveTo(17f, 16f, 14.5f, 18.2f, 12f, 20f)
        close()
        moveTo(5f, 11f)
        curveToRelative(-1.5f, -1f, -2.5f, -2.5f, -2.5f, -4f)
        curveTo(2.5f, 5f, 4f, 4f, 5.5f, 5.5f)
        curveTo(7f, 4f, 8.5f, 5f, 8.5f, 7f)
        curveTo(8.5f, 8.5f, 7.5f, 10f, 5f, 11f)
        close()
    }

    val Condition: ImageVector = icon("Condition") {
        moveTo(14.7f, 6.3f)
        lineTo(17.7f, 3.3f)
        lineTo(20.7f, 6.3f)
        lineTo(17.7f, 9.3f)
        close()
        moveTo(3f, 17f)
        lineTo(13f, 7f)
        lineTo(16f, 10f)
        lineTo(6f, 20f)
        lineTo(3f, 20f)
        close()
    }

    val Performance: ImageVector = icon("Performance") {
        moveTo(12f, 2f)
        lineTo(15f, 9f)
        lineTo(22f, 9.5f)
        lineTo(17f, 14.5f)
        lineTo(18.5f, 21.5f)
        lineTo(12f, 18f)
        lineTo(5.5f, 21.5f)
        lineTo(7f, 14.5f)
        lineTo(2f, 9.5f)
        lineTo(9f, 9f)
        close()
    }

    // —— Achievement categories ——
    val Career: ImageVector = icon("Career") {
        moveTo(4f, 8f)
        lineTo(20f, 8f)
        lineTo(20f, 19f)
        lineTo(4f, 19f)
        close()
        moveTo(8f, 8f)
        lineTo(8f, 6f)
        curveTo(8f, 5f, 9f, 4f, 10f, 4f)
        lineTo(14f, 4f)
        curveTo(15f, 4f, 16f, 5f, 16f, 6f)
        lineTo(16f, 8f)
    }

    val Education: ImageVector = icon("Education") {
        moveTo(2f, 8f)
        lineTo(12f, 3f)
        lineTo(22f, 8f)
        lineTo(12f, 13f)
        close()
        moveTo(5f, 10f)
        lineTo(5f, 16f)
        curveTo(8f, 18f, 16f, 18f, 19f, 16f)
        lineTo(19f, 10f)
    }

    val Family: ImageVector = icon("Family") {
        moveTo(4f, 11f)
        lineTo(10f, 11f)
        lineTo(10f, 20f)
        lineTo(4f, 20f)
        close()
        moveTo(14f, 8f)
        lineTo(20f, 8f)
        lineTo(20f, 20f)
        lineTo(14f, 20f)
        close()
        moveTo(11f, 20f)
        lineTo(13f, 20f)
        lineTo(12f, 16f)
        close()
    }

    val Wealth: ImageVector = icon("Wealth") {
        moveTo(6f, 14f)
        lineTo(18f, 14f)
        lineTo(18f, 18f)
        lineTo(6f, 18f)
        close()
        moveTo(7f, 11f)
        lineTo(17f, 11f)
        lineTo(17f, 14f)
        lineTo(7f, 14f)
        close()
        moveTo(8f, 8f)
        lineTo(16f, 8f)
        lineTo(16f, 11f)
        lineTo(8f, 11f)
        close()
    }

    val Longevity: ImageVector = icon("Longevity") {
        moveTo(8f, 4f)
        lineTo(16f, 4f)
        lineTo(12f, 10f)
        close()
        moveTo(8f, 20f)
        lineTo(16f, 20f)
        lineTo(12f, 14f)
        close()
        moveTo(10f, 11f)
        lineTo(14f, 11f)
        lineTo(14f, 13f)
        lineTo(10f, 13f)
        close()
    }

    val Mischief: ImageVector = icon("Mischief") {
        moveTo(12f, 3f)
        curveTo(8f, 3f, 5f, 6f, 5f, 10f)
        curveTo(5f, 14f, 8f, 17f, 12f, 21f)
        curveTo(16f, 17f, 19f, 14f, 19f, 10f)
        curveTo(19f, 6f, 16f, 3f, 12f, 3f)
        close()
        moveTo(9f, 10f)
        lineTo(9f, 10f)
        moveTo(15f, 10f)
        lineTo(15f, 10f)
    }

    val Achievements: ImageVector = icon("Achievements") {
        moveTo(7f, 4f)
        lineTo(17f, 4f)
        lineTo(19f, 10f)
        lineTo(12f, 21f)
        lineTo(5f, 10f)
        close()
        moveTo(12f, 10f)
        lineTo(12f, 14f)
    }

    // —— Bottom nav ——
    val NavLife: ImageVector = icon("NavLife") {
        moveTo(4f, 18f)
        lineTo(20f, 18f)
        moveTo(6f, 18f)
        lineTo(10f, 8f)
        lineTo(14f, 14f)
        lineTo(18f, 6f)
    }

    val NavFamily: ImageVector = icon("NavFamily") {
        moveTo(8f, 11f)
        arcTo(2f, 2f, 0f, true, true, 8f, 15f)
        arcTo(2f, 2f, 0f, true, true, 8f, 11f)
        moveTo(16f, 11f)
        arcTo(2f, 2f, 0f, true, true, 16f, 15f)
        arcTo(2f, 2f, 0f, true, true, 16f, 11f)
        moveTo(5f, 19f)
        curveTo(5f, 16f, 7f, 14f, 10f, 14f)
        moveTo(19f, 19f)
        curveTo(19f, 16f, 17f, 14f, 14f, 14f)
    }

    val NavCareer: ImageVector = Career

    val NavAssets: ImageVector = icon("NavAssets") {
        moveTo(4f, 10f)
        lineTo(20f, 10f)
        lineTo(20f, 19f)
        lineTo(4f, 19f)
        close()
        moveTo(7f, 10f)
        lineTo(7f, 7f)
        curveTo(7f, 5f, 9f, 4f, 12f, 4f)
        curveTo(15f, 4f, 17f, 5f, 17f, 7f)
        lineTo(17f, 10f)
    }

    val NavActions: ImageVector = icon("NavActions") {
        moveTo(13f, 2f)
        lineTo(11f, 12f)
        lineTo(21f, 12f)
        lineTo(6f, 22f)
        lineTo(8f, 14f)
        lineTo(2f, 14f)
        close()
    }

    // —— Actions / crime / health ——
    val CrimePickpocket: ImageVector = icon("CrimePickpocket") {
        moveTo(4f, 8f)
        lineTo(20f, 8f)
        lineTo(20f, 16f)
        lineTo(4f, 16f)
        close()
        moveTo(6f, 11f)
        lineTo(18f, 11f)
    }

    val CrimeShoplift: ImageVector = icon("CrimeShoplift") {
        moveTo(6f, 6f)
        lineTo(18f, 6f)
        lineTo(20f, 20f)
        lineTo(4f, 20f)
        close()
        moveTo(9f, 10f)
        lineTo(15f, 10f)
    }

    val CrimeFraud: ImageVector = icon("CrimeFraud") {
        moveTo(3f, 10f)
        lineTo(21f, 10f)
        lineTo(21f, 19f)
        lineTo(3f, 19f)
        close()
        moveTo(6f, 10f)
        lineTo(6f, 6f)
        lineTo(18f, 6f)
        lineTo(18f, 10f)
        moveTo(12f, 13f)
        arcTo(2f, 2f, 0f, true, true, 12f, 17f)
        arcTo(2f, 2f, 0f, true, true, 12f, 13f)
    }

    val HealthClinic: ImageVector = icon("HealthClinic") {
        moveTo(6f, 4f)
        lineTo(18f, 4f)
        lineTo(18f, 20f)
        lineTo(6f, 20f)
        close()
        moveTo(11f, 9f)
        lineTo(13f, 9f)
        lineTo(13f, 15f)
        lineTo(11f, 15f)
        close()
        moveTo(9f, 11f)
        lineTo(15f, 11f)
        lineTo(15f, 13f)
        lineTo(9f, 13f)
        close()
    }

    val HealthHospital: ImageVector = icon("HealthHospital") {
        moveTo(3f, 9f)
        lineTo(21f, 9f)
        lineTo(21f, 20f)
        lineTo(3f, 20f)
        close()
        moveTo(7f, 9f)
        lineTo(7f, 5f)
        lineTo(17f, 5f)
        lineTo(17f, 9f)
        moveTo(11f, 12f)
        lineTo(13f, 12f)
        lineTo(13f, 17f)
        lineTo(11f, 17f)
        close()
    }

    val Peaceful: ImageVector = icon("Peaceful") {
        moveTo(12f, 3f)
        arcTo(9f, 9f, 0f, true, true, 12f, 21f)
        arcTo(9f, 9f, 0f, true, true, 12f, 3f)
        close()
        moveTo(7f, 12f)
        lineTo(10.5f, 15.5f)
        lineTo(17f, 9f)
    }

    val AssetsEmpty: ImageVector = NavAssets

    // —— Interactions ——
    val SpendTime: ImageVector = icon("SpendTime") {
        moveTo(12f, 3f)
        arcTo(9f, 9f, 0f, true, true, 12f, 21f)
        arcTo(9f, 9f, 0f, true, true, 12f, 3f)
        close()
        moveTo(12f, 7f)
        lineTo(12f, 12f)
        lineTo(16f, 14f)
    }

    val Gift: ImageVector = icon("Gift") {
        moveTo(4f, 10f)
        lineTo(20f, 10f)
        lineTo(20f, 19f)
        lineTo(4f, 19f)
        close()
        moveTo(12f, 5f)
        lineTo(12f, 10f)
        moveTo(8f, 7f)
        curveTo(8f, 5f, 10f, 4f, 12f, 6f)
        curveTo(14f, 4f, 16f, 5f, 16f, 7f)
    }

    val Travel: ImageVector = icon("Travel") {
        moveTo(3f, 12f)
        lineTo(21f, 12f)
        lineTo(18f, 8f)
        lineTo(21f, 4f)
        lineTo(3f, 4f)
        lineTo(6f, 8f)
        close()
    }

    val Compliment: ImageVector = icon("Compliment") {
        moveTo(12f, 20f)
        curveToRelative(-3f, -2f, -6f, -4.5f, -6f, -7.5f)
        curveTo(6f, 9f, 9f, 7f, 12f, 10f)
        curveToRelative(3f, -3f, 6f, -1f, 6f, 2.5f)
        curveTo(18f, 15.5f, 15f, 18f, 12f, 20f)
        close()
    }

    val Argue: ImageVector = icon("Argue") {
        moveTo(4f, 4f)
        lineTo(10f, 12f)
        lineTo(4f, 20f)
        moveTo(20f, 4f)
        lineTo(14f, 12f)
        lineTo(20f, 20f)
    }

    val AskMoney: ImageVector = Money

    val Advice: ImageVector = Smarts

    val Prank: ImageVector = Mischief

    val SetUpDate: ImageVector = Relationship

    val Insult: ImageVector = icon("Insult") {
        moveTo(12f, 3f)
        arcTo(9f, 9f, 0f, true, true, 12f, 21f)
        arcTo(9f, 9f, 0f, true, true, 12f, 3f)
        close()
        moveTo(8f, 9f)
        lineTo(10f, 11f)
        moveTo(16f, 9f)
        lineTo(14f, 11f)
        moveTo(8f, 16f)
        lineTo(16f, 16f)
    }

    fun forStat(type: StatType): ImageVector = when (type) {
        StatType.HEALTH -> Health
        StatType.HAPPINESS -> Happiness
        StatType.SMARTS -> Smarts
        StatType.LOOKS -> Looks
        StatType.MONEY -> Money
        StatType.NET_WORTH -> Money
        StatType.RELATIONSHIP -> Relationship
        StatType.CONDITION -> Condition
        StatType.PERFORMANCE -> Performance
        StatType.FOLLOWERS -> Looks
        StatType.SKILL -> Smarts
    }

    fun forAchievementCategory(category: AchievementCategory): ImageVector = when (category) {
        AchievementCategory.CAREER -> Career
        AchievementCategory.EDUCATION -> Education
        AchievementCategory.FAMILY -> Family
        AchievementCategory.WEALTH -> Wealth
        AchievementCategory.LONGEVITY -> Longevity
        AchievementCategory.MISCHIEF -> Mischief
        AchievementCategory.WORLDLY -> Family
    }

    fun forMainTab(tab: MainTab): ImageVector = when (tab) {
        MainTab.LIFE -> NavLife
        MainTab.FAMILY -> NavFamily
        MainTab.CAREER -> NavCareer
        MainTab.ASSETS -> NavAssets
        MainTab.ACTIONS -> NavActions
    }

    fun forCrime(type: CrimeType): ImageVector = when (type) {
        CrimeType.PICKPOCKET -> CrimePickpocket
        CrimeType.SHOPLIFT -> CrimeShoplift
        CrimeType.FRAUD -> CrimeFraud
    }

    fun forInteraction(type: InteractionType): ImageVector = when (type) {
        InteractionType.SPEND_TIME -> SpendTime
        InteractionType.GIFT -> Gift
        InteractionType.TRAVEL_TOGETHER -> Travel
        InteractionType.COMPLIMENT -> Compliment
        InteractionType.ARGUE -> Argue
        InteractionType.ASK_FOR_MONEY -> AskMoney
        InteractionType.ASK_FOR_ADVICE -> Advice
        InteractionType.PRANK -> Prank
        InteractionType.SET_UP_ON_DATE -> SetUpDate
        InteractionType.INSULT -> Insult
        InteractionType.HELP_WITH_HOMEWORK -> Advice
        InteractionType.PAY_ALLOWANCE -> AskMoney
        InteractionType.DISCIPLINE -> Argue
    }

    private fun icon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black), fillAlpha = 1f, stroke = null, pathBuilder = block)
        }.build()
    }
}
