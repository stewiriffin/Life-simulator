// app/src/main/java/com/maisha/game/ui/share/ShareCardComposable.kt (modified — object illustrations)
package com.maisha.game.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maisha.game.R
import com.maisha.game.data.IllustrationCatalog
import com.maisha.game.data.model.IllustrationRef
import com.maisha.game.ui.components.CountryFlag
import com.maisha.game.ui.components.IllustrationImage
import com.maisha.game.ui.avatar.AvatarImage
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.NavyElevated
import com.maisha.game.ui.theme.NavySurface
import com.maisha.game.ui.theme.TealLight
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.ui.theme.TextMuted
import com.maisha.game.ui.theme.TextPrimary
import com.maisha.game.ui.theme.TextSecondary

@Composable
fun ShareCardComposable(
    data: ShareCardData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        NavyDeep,
                        NavySurface,
                        NavyElevated.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        TealPrimary.copy(alpha = 0.6f),
                        GoldAccent.copy(alpha = 0.35f),
                        TealLight.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .size(120.dp)
                .clip(CircleShape)
                .background(TealPrimary.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(GoldAccent.copy(alpha = 0.06f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                ShareWordmark()
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AvatarImage(
                        config = data.avatarConfig,
                        size = 72.dp,
                        age = data.ageAtDeath
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CountryFlag(countryCode = data.countryCode, size = 24.dp)
                            Text(
                                text = data.characterName,
                                color = TextPrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 36.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        R.string.share_card_life_span,
                        data.birthYear,
                        data.deathYear
                    ),
                    color = TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.format_age, data.ageAtDeath),
                    color = TealLight,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = data.deathCauseLabel,
                        color = TextPrimary.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start,
                        lineHeight = 18.sp
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShareStatChip(
                        label = data.topStatLabel,
                        value = "${data.topStatValue}%",
                        modifier = Modifier.weight(1f)
                    )
                    ShareStatChip(
                        label = stringResource(R.string.label_net_worth),
                        value = data.netWorthFormatted,
                        modifier = Modifier.weight(1f),
                        accent = GoldAccent
                    )
                }
                ShareHighlightLine(
                    illustration = data.careerJobId?.let {
                        IllustrationCatalog.getIllustrationForJob(it)
                    },
                    text = data.careerHeadline
                )
                if (data.topAssetType != null && data.topAssetSummary != null) {
                    ShareHighlightLine(
                        illustration = IllustrationCatalog.getIllustrationForAsset(data.topAssetType),
                        text = data.topAssetSummary
                    )
                }
                ShareHighlightLine(
                    illustration = null,
                    text = data.familySummary
                )
                if (data.achievementBadges.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        data.achievementBadges.take(2).forEach { badge ->
                            ShareAchievementChip(badge = badge, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.share_card_footer),
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ShareWordmark() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "🌍", fontSize = 22.sp)
        Text(
            text = stringResource(R.string.app_name).uppercase(),
            color = GoldAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun ShareStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = TealLight
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NavyDeep.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = accent,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ShareHighlightLine(
    illustration: IllustrationRef?,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (illustration != null) {
            IllustrationImage(
                ref = illustration,
                size = 28.dp
            )
        }
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun ShareAchievementChip(
    badge: ShareAchievementBadge,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(GoldAccent.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IllustrationImage(
            ref = IllustrationCatalog.getIllustrationForAchievementCategory(badge.category),
            size = 22.dp
        )
        Text(
            text = stringResource(badge.titleRes),
            color = GoldAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
    }
}

fun achievementEmoji(iconName: String): String = when (iconName) {
    "briefcase" -> "💼"
    "office" -> "🏢"
    "shuffle" -> "🔀"
    "graduation_cap" -> "🎓"
    "star" -> "⭐"
    "door" -> "🚪"
    "rings" -> "💍"
    "baby" -> "👶"
    "family" -> "👨‍👩‍👧"
    "heart" -> "❤️"
    "coins" -> "🪙"
    "million" -> "💰"
    "house" -> "🏠"
    "portfolio" -> "📈"
    "calendar" -> "📅"
    "sunset" -> "🌅"
    "crown" -> "👑"
    "handcuffs" -> "🚔"
    "repeat" -> "🔁"
    "shield" -> "🛡️"
    "globe" -> "🌍"
    "inseparable" -> "💞"
    "tree" -> "🌳"
    "dynasty" -> "👑"
    "handshake" -> "🤝"
    "friends" -> "👥"
    else -> "🏆"
}
