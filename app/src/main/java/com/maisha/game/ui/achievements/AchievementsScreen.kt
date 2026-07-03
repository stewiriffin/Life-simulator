// app/src/main/java/com/maisha/game/ui/achievements/AchievementsScreen.kt (modified — AppIcons locked silhouettes)
package com.maisha.game.ui.achievements

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.IllustrationCatalog
import com.maisha.game.data.model.AchievementCategory
import com.maisha.game.util.achievementDescription
import com.maisha.game.ui.components.AppLoadingIndicator
import com.maisha.game.ui.components.EmptyStateCard
import com.maisha.game.ui.components.IllustrationImage
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.TealPrimary

private enum class AchievementListContentType {
    CategoryHeader,
    AchievementCard
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    uiState: AchievementsUiState,
    onBack: () -> Unit,
    formatUnlockDate: (Long?) -> String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_achievements),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AppLoadingIndicator()
            }
            return@Scaffold
        }

        if (uiState.items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                EmptyStateCard(
                    illustration = EmptyStateIllustration.ACHIEVEMENTS,
                    title = stringResource(R.string.empty_achievements_title),
                    message = stringResource(R.string.empty_achievements_message)
                )
            }
            return@Scaffold
        }

        val categoriesWithItems = remember(uiState.items) {
            AchievementCategory.entries.mapNotNull { category ->
                val categoryItems = uiState.items.filter { it.achievement.category == category }
                if (categoryItems.isEmpty()) null else category to categoryItems
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            categoriesWithItems.forEach { (category, categoryItems) ->
                item(contentType = AchievementListContentType.CategoryHeader) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IllustrationImage(
                            ref = IllustrationCatalog.getIllustrationForAchievementCategory(category),
                            size = 22.dp
                        )
                        Text(
                            text = stringResource(categoryLabelRes(category)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TealPrimary
                        )
                    }
                }

                items(
                    categoryItems,
                    key = { it.achievement.id },
                    contentType = { AchievementListContentType.AchievementCard }
                ) { item ->
                    AchievementCard(
                        item = item,
                        unlockDate = formatUnlockDate(item.progress.unlockedAt),
                        description = achievementDescription(
                            context,
                            item.achievement,
                            uiState.displayCountryCode
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    item: AchievementListItem,
    unlockDate: String?,
    description: String
) {
    val unlocked = item.progress.unlocked
    val alpha = if (unlocked) 1f else 0.55f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (unlocked) {
                            GoldAccent.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                IllustrationImage(
                    ref = IllustrationCatalog.getIllustrationForAchievementCategory(
                        item.achievement.category
                    ),
                    size = 28.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(item.achievement.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (unlocked) GoldAccent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (unlocked && unlockDate != null) {
                    Text(
                        text = stringResource(R.string.format_achievement_unlocked_date, unlockDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = TealPrimary,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@StringRes
private fun categoryLabelRes(category: AchievementCategory): Int = when (category) {
    AchievementCategory.CAREER -> R.string.achievement_category_career
    AchievementCategory.EDUCATION -> R.string.achievement_category_education
    AchievementCategory.FAMILY -> R.string.achievement_category_family
    AchievementCategory.WEALTH -> R.string.achievement_category_wealth
    AchievementCategory.LONGEVITY -> R.string.achievement_category_longevity
    AchievementCategory.MISCHIEF -> R.string.achievement_category_mischief
    AchievementCategory.WORLDLY -> R.string.achievement_category_worldly
}
