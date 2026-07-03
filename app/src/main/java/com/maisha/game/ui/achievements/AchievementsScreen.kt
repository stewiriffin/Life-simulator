// app/src/main/java/com/maisha/game/ui/achievements/AchievementsScreen.kt
package com.maisha.game.ui.achievements

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.IllustrationCatalog
import com.maisha.game.data.model.Achievement
import com.maisha.game.data.model.AchievementCategory
import com.maisha.game.data.model.AchievementProgress
import com.maisha.game.util.achievementDescription
import com.maisha.game.util.formatMoney
import com.maisha.game.ui.components.AppLoadingIndicator
import com.maisha.game.ui.components.EmptyStateCard
import com.maisha.game.ui.components.IllustrationImage
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.MaishaTheme
import com.maisha.game.ui.theme.TealPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    uiState: AchievementsUiState,
    onBack: () -> Unit,
    formatUnlockDate: (Long?) -> String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories = AchievementCategory.entries
    val pagerState = rememberPagerState(pageCount = { categories.size })
    val scope = rememberCoroutineScope()

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TealPrimary
            ) {
                categories.forEachIndexed { index, category ->
                    val count = uiState.items.count {
                        it.achievement.category == category && it.progress.unlocked
                    }
                    val total = uiState.items.count { it.achievement.category == category }
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = {
                            Text(
                                text = "${stringResource(categoryLabelRes(category))} ($count/$total)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) { page ->
                val category = categories[page]
                val categoryItems = uiState.items.filter { it.achievement.category == category }
                if (categoryItems.isEmpty()) {
                    EmptyStateCard(
                        illustration = EmptyStateIllustration.ACHIEVEMENTS,
                        title = stringResource(categoryLabelRes(category)),
                        message = stringResource(R.string.empty_achievements_message),
                        modifier = Modifier.padding(top = 24.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(categoryItems, key = { it.achievement.id }) { item ->
                            val unlocked = item.progress.unlocked
                            val isSecret = !unlocked &&
                                item.achievement.category == AchievementCategory.MISCHIEF
                            AchievementTrophyCard(
                                item = item,
                                unlockDate = formatUnlockDate(item.progress.unlockedAt),
                                title = if (isSecret) {
                                    stringResource(R.string.achievement_secret_title)
                                } else {
                                    stringResource(item.achievement.titleRes)
                                },
                                description = if (isSecret) {
                                    stringResource(R.string.achievement_secret_description)
                                } else {
                                    achievementDescription(
                                        context,
                                        item.achievement,
                                        uiState.displayCountryCode
                                    )
                                },
                                countryCode = uiState.displayCountryCode
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementTrophyCard(
    item: AchievementListItem,
    unlockDate: String?,
    title: String,
    description: String,
    countryCode: String
) {
    val unlocked = item.progress.unlocked
    val showProgress = !unlocked &&
        item.progressFraction != null &&
        item.progressTarget != null &&
        item.progressCurrent != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (unlocked) 1f else 0.92f),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) {
                GoldAccent.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (unlocked) {
                            GoldAccent.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                IllustrationImage(
                    ref = IllustrationCatalog.getIllustrationForAchievementCategory(
                        item.achievement.category
                    ),
                    size = 28.dp,
                    tint = if (unlocked) GoldAccent else Color.Gray
                )
                if (!unlocked) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(1.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (unlocked) GoldAccent else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (showProgress) {
                val fraction = item.progressFraction!!.coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = TealPrimary,
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                )
                Text(
                    text = progressLabel(
                        achievementId = item.achievement.id,
                        current = item.progressCurrent!!,
                        target = item.progressTarget!!,
                        countryCode = countryCode
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = TealPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (unlocked && unlockDate != null) {
                Text(
                    text = unlockDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = TealPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun progressLabel(
    achievementId: String,
    current: Int,
    target: Int,
    countryCode: String
): String {
    val percent = ((current.toFloat() / target.toFloat()) * 100f).toInt().coerceIn(0, 100)
    return when (achievementId) {
        "six_figures", "first_million" -> stringResource(
            R.string.format_achievement_progress_money,
            formatMoney(current, countryCode),
            formatMoney(target, countryCode),
            percent
        )
        else -> stringResource(
            R.string.format_achievement_progress_age,
            current,
            target,
            percent
        )
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

@Preview(showBackground = true, widthDp = 360, name = "Trophy Room WEALTH")
@Composable
private fun AchievementsWealthTabPreview() {
    val wealthItems = listOf(
        AchievementListItem(
            achievement = Achievement(
                id = "six_figures",
                titleRes = R.string.achievement_six_figures_title,
                descriptionRes = R.string.achievement_six_figures_description,
                category = AchievementCategory.WEALTH,
                iconName = "coins"
            ),
            progress = AchievementProgress("six_figures", unlocked = true, unlockedAt = 1L)
        ),
        AchievementListItem(
            achievement = Achievement(
                id = "first_million",
                titleRes = R.string.achievement_first_million_title,
                descriptionRes = R.string.achievement_first_million_description,
                category = AchievementCategory.WEALTH,
                iconName = "million"
            ),
            progress = AchievementProgress("first_million"),
            progressFraction = 0.5f,
            progressCurrent = 500_000,
            progressTarget = 1_000_000
        ),
        AchievementListItem(
            achievement = Achievement(
                id = "property_owner",
                titleRes = R.string.achievement_property_owner_title,
                descriptionRes = R.string.achievement_property_owner_description,
                category = AchievementCategory.WEALTH,
                iconName = "house"
            ),
            progress = AchievementProgress("property_owner")
        )
    )
    MaishaTheme {
        AchievementsScreen(
            uiState = AchievementsUiState(
                isLoading = false,
                items = wealthItems,
                displayCountryCode = "KE",
                bestNetWorth = 500_000,
                bestAge = 40
            ),
            onBack = {},
            formatUnlockDate = { "Jan 1, 2026" }
        )
    }
}
