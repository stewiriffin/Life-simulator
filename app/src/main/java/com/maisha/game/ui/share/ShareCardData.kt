// app/src/main/java/com/maisha/game/ui/share/ShareCardData.kt (modified — illustration refs for share card)
package com.maisha.game.ui.share

import androidx.annotation.StringRes
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.AchievementCategory
import com.maisha.game.data.model.AvatarConfig

data class ShareAchievementBadge(
    @StringRes val titleRes: Int,
    val emoji: String,
    val category: AchievementCategory
)

data class ShareCardData(
    val characterName: String,
    val avatarConfig: AvatarConfig,
    val birthYear: Int,
    val deathYear: Int,
    val ageAtDeath: Int,
    val countryCode: String = "KE",
    val deathCauseLabel: String,
    val topStatLabel: String,
    val topStatValue: Int,
    val netWorthFormatted: String,
    val careerHeadline: String,
    val careerJobId: String? = null,
    val topAssetType: AssetType? = null,
    val topAssetSummary: String? = null,
    val familySummary: String,
    val closestBondSummary: String = "",
    val achievementBadges: List<ShareAchievementBadge>
)

object ShareCardDimensions {
    /** 4:5 portrait — fits Instagram feed & WhatsApp status without heavy cropping. */
    const val WIDTH_PX = 1080
    const val HEIGHT_PX = 1350
}
