// app/src/main/java/com/maisha/game/ui/summary/LifeSummaryScreen.kt
package com.maisha.game.ui.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maisha.game.R
import com.maisha.game.ads.AdUnitConfig
import com.maisha.game.data.IllustrationCatalog
import com.maisha.game.data.model.AchievementCategory
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.IllustrationRef
import com.maisha.game.data.model.Stats
import com.maisha.game.data.model.ageStageFor
import com.maisha.game.ui.components.AdaptiveBannerAd
import com.maisha.game.ui.components.AppLoadingIndicator
import com.maisha.game.ui.components.CountryFlag
import com.maisha.game.ui.components.DismissibleTipCard
import com.maisha.game.ui.components.IllustrationImage
import com.maisha.game.ui.components.PersonAvatar
import com.maisha.game.ui.components.StatBar
import com.maisha.game.ui.components.StatType
import com.maisha.game.ui.legacy.AncestryTimelinePreview
import com.maisha.game.ui.share.ShareAchievementBadge
import com.maisha.game.ui.share.ShareCardComposable
import com.maisha.game.ui.share.ShareCardData
import com.maisha.game.ui.share.ShareCardDimensions
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.MaishaTheme
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.ComposableToImage
import com.maisha.game.util.ShareIntentHelper
import com.maisha.game.util.formatMoney
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CASCADE_STAGGER_MS = 90
private const val CASCADE_DURATION_MS = 420

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LifeSummaryScreen(
    uiState: LifeSummaryUiState,
    onStartNewLife: () -> Unit,
    onContinueLegacy: () -> Unit,
    onHeirSelected: (com.maisha.game.data.model.Person) -> Unit,
    onConfirmLegacyContinuation: () -> Unit,
    onDismissHeirSelection: () -> Unit,
    onDismissLegacyConfirmation: () -> Unit,
    onWatchSecondWind: () -> Unit,
    onDismissAchievementsTip: () -> Unit,
    onShareMyLife: () -> Unit,
    onDismissSharePreview: () -> Unit,
    onShareCapturingStarted: () -> Unit,
    onShareCompleted: () -> Unit,
    onShareFailed: (String) -> Unit,
    onDismissShareError: () -> Unit
) {
    if (uiState.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLoadingIndicator()
        }
        return
    }

    val character = uiState.character ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shareCardData = uiState.shareCardData
    var cascadeVisible by remember { mutableStateOf(false) }
    LaunchedEffect(character.name) {
        cascadeVisible = false
        delay(40)
        cascadeVisible = true
    }

    if (uiState.showSharePreview && shareCardData != null) {
        SharePreviewDialog(
            cardData = shareCardData,
            isCapturing = uiState.isShareCapturing,
            onDismiss = onDismissSharePreview,
            onShare = {
                onShareCapturingStarted()
                scope.launch {
                    try {
                        val bitmap = withContext(Dispatchers.Main) {
                            ComposableToImage.captureComposableAsBitmap(
                                context = context,
                                widthPx = ShareCardDimensions.WIDTH_PX,
                                heightPx = ShareCardDimensions.HEIGHT_PX
                            ) {
                                MaishaTheme {
                                    ShareCardComposable(
                                        data = shareCardData,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        val uri = ComposableToImage.saveBitmapToCache(context, bitmap)
                        ShareIntentHelper.shareImage(
                            context = context,
                            imageUri = uri,
                            caption = context.getString(R.string.share_default_caption)
                        )
                        onShareCompleted()
                    } catch (_: Exception) {
                        onShareFailed(context.getString(R.string.share_error))
                    }
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            CascadeCard(visible = cascadeVisible, index = 0) {
                MemorialHeader(
                    character = character,
                    deathCauseLabel = uiState.deathCauseLabel,
                    deathFlavorText = uiState.deathFlavorText
                )
            }

            if (uiState.showAchievementsCarryOverTip) {
                CascadeCard(visible = cascadeVisible, index = 1) {
                    DismissibleTipCard(
                        text = stringResource(R.string.tip_first_death_achievements),
                        onDismiss = onDismissAchievementsTip
                    )
                }
            }

            CascadeCard(visible = cascadeVisible, index = 2) {
                SummarySectionCard(title = stringResource(R.string.section_final_stats)) {
                    FinalStatsGrid(
                        stats = character.stats,
                        netWorth = uiState.netWorth,
                        countryCode = character.countryCode
                    )
                }
            }

            CascadeCard(visible = cascadeVisible, index = 3) {
                SummarySectionCard(title = stringResource(R.string.section_wealth_career)) {
                    WealthCareerSection(
                        educationRecap = uiState.educationRecap,
                        careerRecap = uiState.careerRecap,
                        careerIllustration = uiState.shareCardData?.careerJobId?.let {
                            IllustrationCatalog.getIllustrationForJob(it)
                        },
                        spouseRecap = uiState.spouseRecap,
                        closestBondRecap = uiState.closestBondRecap,
                        childrenCount = uiState.childrenCount,
                        netWorth = uiState.netWorth,
                        money = character.stats.money,
                        countryCode = character.countryCode
                    )
                }
            }

            val badges = shareCardData?.achievementBadges.orEmpty()
            if (badges.isNotEmpty()) {
                CascadeCard(visible = cascadeVisible, index = 4) {
                    SummarySectionCard(title = stringResource(R.string.section_achievements_unlocked)) {
                        AchievementsShowcase(badges = badges)
                    }
                }
            }

            if (uiState.eventHighlights.isNotEmpty()) {
                CascadeCard(visible = cascadeVisible, index = 5) {
                    SummarySectionCard(title = stringResource(R.string.section_life_highlights)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.eventHighlights.forEachIndexed { index, entry ->
                                SummaryEventLogCard(
                                    entry = entry,
                                    ageTag = character.age - index
                                )
                            }
                        }
                    }
                }
            }

            if (character.ancestryHistory.isNotEmpty() || character.generationNumber > 1) {
                CascadeCard(visible = cascadeVisible, index = 6) {
                    AncestryTimelinePreview(character = character)
                }
            }

            if (uiState.showHeirSelection && uiState.eligibleHeirs.isNotEmpty()) {
                HeirSelectionDialog(
                    heirs = uiState.eligibleHeirs,
                    slotNumber = uiState.slotId + 1,
                    selectedHeir = uiState.selectedHeir,
                    showConfirmation = uiState.showLegacyConfirmation,
                    onHeirSelected = onHeirSelected,
                    onConfirmContinue = onConfirmLegacyContinuation,
                    onDismissConfirmation = onDismissLegacyConfirmation,
                    onDismiss = onDismissHeirSelection
                )
            }

            if (uiState.eligibleHeirs.isNotEmpty()) {
                CascadeCard(visible = cascadeVisible, index = 7) {
                    Button(
                        onClick = onContinueLegacy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_continue_legacy),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (shareCardData != null) {
                OutlinedButton(
                    onClick = onShareMyLife,
                    enabled = !uiState.isShareCapturing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (uiState.isShareCapturing) {
                        AppLoadingIndicator(size = 22.dp)
                    } else {
                        Text(
                            text = stringResource(R.string.btn_share_my_life),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            uiState.shareErrorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = CoralNegative,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
                TextButton(
                    onClick = onDismissShareError,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }

            Button(
                onClick = onStartNewLife,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = NavyDeep
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_start_new_life),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.showSecondWindButton) {
                OutlinedButton(
                    onClick = onWatchSecondWind,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_watch_second_wind),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 2,
                        textAlign = TextAlign.Center
                    )
                }
            }

            uiState.secondWindMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TealPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = stringResource(R.string.label_sponsored),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            textAlign = TextAlign.Center
        )
        AdaptiveBannerAd(
            adUnitId = AdUnitConfig.BANNER,
            modifier = Modifier.padding(bottom = 10.dp)
        )
    }
}

@Composable
private fun CascadeCard(
    visible: Boolean,
    index: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(CASCADE_DURATION_MS, delayMillis = index * CASCADE_STAGGER_MS)) +
            slideInVertically(
                animationSpec = tween(CASCADE_DURATION_MS, delayMillis = index * CASCADE_STAGGER_MS),
                initialOffsetY = { it / 5 }
            )
    ) {
        Box(modifier = Modifier.padding(bottom = 10.dp)) {
            content()
        }
    }
}

@Composable
private fun SharePreviewDialog(
    cardData: ShareCardData,
    isCapturing: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!isCapturing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyDeep.copy(alpha = 0.96f))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.share_preview_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )
                Text(
                    text = stringResource(R.string.share_preview_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                ShareCardComposable(
                    data = cardData,
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .aspectRatio(9f / 16f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isCapturing,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                    Button(
                        onClick = onShare,
                        enabled = !isCapturing,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = NavyDeep
                        )
                    ) {
                        if (isCapturing) {
                            AppLoadingIndicator(size = 20.dp)
                        } else {
                            Text(
                                text = stringResource(R.string.btn_share),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemorialHeader(
    character: Character,
    deathCauseLabel: String,
    deathFlavorText: String
) {
    val deathYear = character.birthYear + character.age
    val stage = ageStageFor(character.age)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = NavyDeep.copy(alpha = 0.92f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PersonAvatar(
                avatarConfig = character.avatarConfig,
                size = 88,
                age = character.age
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.section_memorial),
                style = MaterialTheme.typography.labelLarge,
                color = GoldAccent,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CountryFlag(countryCode = character.countryCode, size = 22.dp)
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = stringResource(
                    R.string.format_life_span,
                    character.birthYear,
                    deathYear,
                    stringResource(R.string.format_age, character.age)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = stage.name.lowercase().replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.labelMedium,
                color = TealPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CoralNegative.copy(alpha = 0.18f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = deathCauseLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center
                    )
                    if (deathFlavorText.isNotBlank()) {
                        Text(
                            text = deathFlavorText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummarySectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TealPrimary
            )
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FinalStatsGrid(stats: Stats, netWorth: Int, countryCode: String) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 2
    ) {
        val chipModifier = Modifier.fillMaxWidth(0.47f)
        StatChip(label = stringResource(R.string.stat_health), value = "${stats.health}", modifier = chipModifier)
        StatChip(label = stringResource(R.string.stat_happiness), value = "${stats.happiness}", modifier = chipModifier)
        StatChip(label = stringResource(R.string.stat_smarts), value = "${stats.smarts}", modifier = chipModifier)
        StatChip(label = stringResource(R.string.stat_looks), value = "${stats.looks}", modifier = chipModifier)
        StatChip(
            label = stringResource(R.string.stat_money),
            value = formatMoney(stats.money, countryCode),
            accent = true,
            modifier = chipModifier
        )
        StatChip(
            label = stringResource(R.string.label_net_worth),
            value = formatMoney(netWorth, countryCode),
            accent = true,
            modifier = chipModifier
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatBar(type = StatType.HEALTH, value = stats.health)
        StatBar(type = StatType.HAPPINESS, value = stats.happiness)
        StatBar(type = StatType.SMARTS, value = stats.smarts)
        StatBar(type = StatType.LOOKS, value = stats.looks)
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    accent: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = if (accent) GoldAccent.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (accent) GoldAccent else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WealthCareerSection(
    educationRecap: String,
    careerRecap: String,
    careerIllustration: IllustrationRef?,
    spouseRecap: String,
    closestBondRecap: String,
    childrenCount: Int,
    netWorth: Int,
    money: Int,
    countryCode: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RecapRow(label = stringResource(R.string.label_education), value = educationRecap)
        RecapRow(
            label = stringResource(R.string.label_career),
            value = careerRecap,
            illustration = careerIllustration
        )
        RecapRow(label = stringResource(R.string.label_family), value = spouseRecap)
        RecapRow(label = stringResource(R.string.label_closest_bond), value = closestBondRecap)
        RecapRow(
            label = stringResource(R.string.label_children),
            value = pluralStringResource(R.plurals.child_count, childrenCount, childrenCount)
        )
        RecapRow(
            label = stringResource(R.string.stat_money),
            value = formatMoney(money, countryCode)
        )
        RecapRow(
            label = stringResource(R.string.label_net_worth),
            value = formatMoney(netWorth, countryCode)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AchievementsShowcase(badges: List<ShareAchievementBadge>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        badges.forEach { badge ->
            Row(
                modifier = Modifier
                    .background(
                        GoldAccent.copy(alpha = 0.12f),
                        RoundedCornerShape(10.dp)
                    )
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
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldAccent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecapRow(
    label: String,
    value: String,
    illustration: IllustrationRef? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (illustration != null) {
            IllustrationImage(ref = illustration, size = 32.dp)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.65f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SummaryEventLogCard(entry: String, ageTag: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.format_age, ageTag.coerceAtLeast(0)),
                style = MaterialTheme.typography.labelSmall,
                color = TealPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = entry,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Life summary 360dp")
@Composable
private fun LifeSummaryScreenPreview() {
    val character = Character(
        name = "Amina Otieno",
        age = 75,
        gender = Gender.FEMALE,
        stats = Stats(health = 20, happiness = 70, smarts = 65, looks = 55, money = 400_000),
        birthYear = 1965,
        alive = false,
        countryCode = "KE",
        avatarConfig = AvatarConfig.DEFAULT,
        generationNumber = 2,
        ancestryHistory = listOf(
            com.maisha.game.data.model.AncestryEntry(
                generationNumber = 1,
                characterName = "Juma Otieno",
                countryCode = "KE",
                ageAtDeath = 68,
                cause = "Passed away peacefully in old age"
            )
        )
    )
    MaishaTheme {
        LifeSummaryScreen(
            uiState = LifeSummaryUiState(
                isLoading = false,
                character = character,
                deathCauseLabel = "Passed away peacefully in old age",
                deathFlavorText = "Passed away peacefully at home in your sleep.",
                netWorth = 2_400_000,
                careerRecap = "Teacher · Level 4",
                educationRecap = "University graduate",
                spouseRecap = "Married to David",
                closestBondRecap = "Closest bond: David",
                childrenCount = 2,
                eventHighlights = listOf("Promoted at work.", "Welcomed a child."),
                shareCardData = ShareCardData(
                    characterName = character.name,
                    avatarConfig = character.avatarConfig,
                    birthYear = 1965,
                    deathYear = 2040,
                    ageAtDeath = 75,
                    countryCode = "KE",
                    deathCauseLabel = "Passed away peacefully in old age",
                    topStatLabel = "Happiness",
                    topStatValue = 70,
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
                )
            ),
            onStartNewLife = {},
            onContinueLegacy = {},
            onHeirSelected = {},
            onConfirmLegacyContinuation = {},
            onDismissHeirSelection = {},
            onDismissLegacyConfirmation = {},
            onWatchSecondWind = {},
            onDismissAchievementsTip = {},
            onShareMyLife = {},
            onDismissSharePreview = {},
            onShareCapturingStarted = {},
            onShareCompleted = {},
            onShareFailed = {},
            onDismissShareError = {}
        )
    }
}
