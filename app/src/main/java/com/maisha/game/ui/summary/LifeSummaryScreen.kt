// app/src/main/java/com/maisha/game/ui/summary/LifeSummaryScreen.kt (modified — share life card)
package com.maisha.game.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maisha.game.R
import com.maisha.game.ads.AdUnitConfig
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Stats
import com.maisha.game.ui.components.AdaptiveBannerAd
import com.maisha.game.data.IllustrationCatalog
import com.maisha.game.data.model.IllustrationRef
import com.maisha.game.ui.components.AppLoadingIndicator
import com.maisha.game.ui.components.IllustrationImage
import com.maisha.game.ui.components.DismissibleTipCard
import com.maisha.game.ui.components.MoneyStatRow
import com.maisha.game.ui.components.CountryFlag
import com.maisha.game.ui.components.PersonAvatar
import com.maisha.game.ui.components.StatBar
import com.maisha.game.ui.components.StatType
import com.maisha.game.ui.share.ShareCardComposable
import com.maisha.game.ui.share.ShareCardData
import com.maisha.game.ui.share.ShareCardDimensions
import com.maisha.game.ui.theme.MaishaTheme
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.ComposableToImage
import com.maisha.game.util.ShareIntentHelper
import com.maisha.game.util.formatMoney
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SummaryHeader(
                    character = character,
                    deathCauseLabel = uiState.deathCauseLabel,
                    deathFlavorText = uiState.deathFlavorText
                )
            }
            if (uiState.showAchievementsCarryOverTip) {
                item {
                    DismissibleTipCard(
                        text = stringResource(R.string.tip_first_death_achievements),
                        onDismiss = onDismissAchievementsTip
                    )
                }
            }

            item {
                SummarySectionCard(title = stringResource(R.string.section_final_stats)) {
                    FinalStatsSection(
                        stats = character.stats,
                        netWorth = uiState.netWorth,
                        countryCode = character.countryCode
                    )
                }
            }

            item {
                SummarySectionCard(title = stringResource(R.string.section_life_recap)) {
                    RecapRow(
                        label = stringResource(R.string.label_education),
                        value = uiState.educationRecap
                    )
                    RecapRow(
                        label = stringResource(R.string.label_career),
                        value = uiState.careerRecap,
                        illustration = uiState.shareCardData?.careerJobId?.let {
                            IllustrationCatalog.getIllustrationForJob(it)
                        }
                    )
                    RecapRow(
                        label = stringResource(R.string.label_family),
                        value = uiState.spouseRecap
                    )
                    RecapRow(
                        label = stringResource(R.string.label_closest_bond),
                        value = uiState.closestBondRecap
                    )
                    RecapRow(
                        label = stringResource(R.string.label_children),
                        value = pluralStringResource(
                            R.plurals.child_count,
                            uiState.childrenCount,
                            uiState.childrenCount
                        )
                    )
                    RecapRow(
                        label = stringResource(R.string.label_net_worth),
                        value = formatMoney(uiState.netWorth, uiState.character?.countryCode ?: "KE")
                    )
                }
            }

            if (uiState.eventHighlights.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.section_life_highlights),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                itemsIndexed(uiState.eventHighlights) { index, entry ->
                    SummaryEventLogCard(
                        entry = entry,
                        ageTag = character.age - index
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
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
            Button(
                onClick = onContinueLegacy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(top = 10.dp),
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
                        .fillMaxWidth(0.92f)
                        .aspectRatio(4f / 5f)
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
private fun SummaryHeader(
    character: Character,
    deathCauseLabel: String,
    deathFlavorText: String
) {
    val deathYear = character.birthYear + character.age

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PersonAvatar(
            avatarConfig = character.avatarConfig,
            size = 64,
            age = character.age
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CountryFlag(countryCode = character.countryCode, size = 20.dp)
            Text(
                text = character.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = CoralNegative.copy(alpha = 0.15f)
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = deathCauseLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CoralNegative,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = deathFlavorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun FinalStatsSection(stats: Stats, netWorth: Int, countryCode: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StatBar(type = StatType.HEALTH, value = stats.health)
        StatBar(type = StatType.HAPPINESS, value = stats.happiness)
        StatBar(type = StatType.SMARTS, value = stats.smarts)
        StatBar(type = StatType.LOOKS, value = stats.looks)
        MoneyStatRow(amount = stats.money, countryCode = countryCode)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_net_worth),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatMoney(netWorth, countryCode),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = GoldAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
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
