// app/src/main/java/com/maisha/game/ui/share/ShareCardComposable.kt
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maisha.game.R
import com.maisha.game.data.IllustrationCatalog
import com.maisha.game.data.model.AchievementCategory
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.IllustrationRef
import com.maisha.game.ui.avatar.AvatarImage
import com.maisha.game.ui.components.CountryFlag
import com.maisha.game.ui.components.IllustrationImage
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.MaishaTheme
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
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NavyDeep,
                        NavySurface,
                        NavyElevated,
                        NavyDeep
                    )
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        TealPrimary.copy(alpha = 0.7f),
                        GoldAccent.copy(alpha = 0.45f),
                        TealLight.copy(alpha = 0.25f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .size(220.dp)
                .clip(CircleShape)
                .background(TealPrimary.copy(alpha = 0.1f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(140.dp)
                .clip(CircleShape)
                .background(GoldAccent.copy(alpha = 0.08f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ShareWordmark()
                Spacer(modifier = Modifier.height(28.dp))
                AvatarImage(
                    config = data.avatarConfig,
                    size = 120.dp,
                    age = data.ageAtDeath
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CountryFlag(countryCode = data.countryCode, size = 28.dp)
                    Text(
                        text = data.characterName,
                        color = TextPrimary,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        lineHeight = 38.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(
                        R.string.share_card_life_span,
                        data.birthYear,
                        data.deathYear
                    ),
                    color = TextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.format_age, data.ageAtDeath),
                    color = TealLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = data.deathCauseLabel,
                        color = TextPrimary.copy(alpha = 0.92f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                ShareHighlightLine(
                    illustration = null,
                    text = data.familySummary
                )
                if (data.legacySentence.isNotBlank()) {
                    Text(
                        text = data.legacySentence,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldAccent.copy(alpha = 0.1f))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
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

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.share_card_watermark),
                    color = GoldAccent.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.share_card_footer),
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
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
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = accent,
            fontSize = 20.sp,
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
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp,
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

@Preview(showBackground = true, widthDp = 270, heightDp = 480)
@Composable
private fun ShareCardPreview() {
    MaishaTheme {
        ShareCardComposable(
            data = ShareCardData(
                characterName = "Amina Otieno",
                avatarConfig = AvatarConfig.DEFAULT,
                birthYear = 1965,
                deathYear = 2040,
                ageAtDeath = 75,
                countryCode = "KE",
                deathCauseLabel = "Passed away peacefully in old age",
                topStatLabel = "Happiness",
                topStatValue = 88,
                netWorthFormatted = "KES 2.4M",
                careerHeadline = "Teacher · Level 4",
                familySummary = "Married with 2 children",
                legacySentence = "Amina Otieno lived 75 years as a Teacher, built KES 2.4M, and raised 2 children.",
                achievementBadges = listOf(
                    ShareAchievementBadge(
                        titleRes = R.string.app_name,
                        emoji = "🏆",
                        category = AchievementCategory.FAMILY
                    )
                )
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}
