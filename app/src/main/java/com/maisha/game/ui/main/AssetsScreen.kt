// app/src/main/java/com/maisha/game/ui/main/AssetsScreen.kt
package com.maisha.game.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.AssetCatalog
import com.maisha.game.data.CatalogAsset
import com.maisha.game.data.IllustrationCatalog
import com.maisha.game.data.model.Asset
import com.maisha.game.data.model.Character
import com.maisha.game.ui.components.EmptyStateCard
import com.maisha.game.ui.components.IllustrationImage
import com.maisha.game.ui.components.StatBar
import com.maisha.game.ui.components.StatType
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.formatMoney

@Composable
fun AssetsScreen(
    character: Character,
    netWorth: Int,
    uiState: LifeUiState,
    snackbarHostState: SnackbarHostState,
    onPurchaseAsset: (String) -> Unit,
    onSellAsset: (String) -> Unit,
    onAssetsMessageDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingPurchase by remember { mutableStateOf<CatalogAsset?>(null) }

    LaunchedEffect(uiState.assetsMessage) {
        uiState.assetsMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onAssetsMessageDismissed()
        }
    }

    pendingPurchase?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingPurchase = null },
            title = { Text(stringResource(R.string.dialog_confirm_purchase_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dialog_confirm_purchase_body,
                        item.name,
                        formatMoney(item.purchasePrice, character.countryCode),
                        formatMoney(item.monthlyUpkeep, character.countryCode)
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPurchaseAsset(item.id)
                        pendingPurchase = null
                    }
                ) {
                    Text(stringResource(R.string.btn_buy))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPurchase = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaishaSpacing.md, vertical = MaishaSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MaishaSpacing.sm + 2.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.screen_assets),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    R.string.format_cash_net_worth,
                    formatMoney(character.stats.money, character.countryCode),
                    formatMoney(netWorth, character.countryCode)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        item {
            Text(
                text = stringResource(R.string.section_owned_count, character.assets.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (character.assets.isEmpty()) {
            item {
                EmptyStateCard(
                    illustration = EmptyStateIllustration.ASSETS,
                    title = stringResource(R.string.section_owned_count, 0),
                    message = stringResource(R.string.empty_assets)
                )
            }
        } else {
            items(character.assets, key = { it.id }) { asset ->
                OwnedAssetCard(
                    asset = asset,
                    countryCode = character.countryCode,
                    onSell = { onSellAsset(asset.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.section_shop),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(AssetCatalog.getAssetsForCountry(character.countryCode), key = { it.id }) { catalogItem ->
            ShopAssetCard(
                item = catalogItem,
                countryCode = character.countryCode,
                canAfford = character.stats.money >= catalogItem.purchasePrice,
                onBuy = { pendingPurchase = catalogItem }
            )
        }
    }
}

@Composable
private fun OwnedAssetCard(
    asset: Asset,
    countryCode: String,
    onSell: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IllustrationImage(
                    ref = IllustrationCatalog.getIllustrationForAsset(asset.type),
                    size = 52.dp,
                    contentDescription = asset.name
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = asset.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = asset.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatMoney(asset.currentValue, countryCode),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            StatBar(
                type = StatType.CONDITION,
                value = asset.condition,
                label = stringResource(R.string.stat_condition)
            )

            Text(
                text = stringResource(
                    R.string.format_upkeep_monthly,
                    formatMoney(asset.monthlyUpkeep, countryCode)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onSell,
                modifier = Modifier.fillMaxWidth(),
                shape = MaishaRadius.buttonShape
            ) {
                Text(stringResource(R.string.btn_sell))
            }
        }
    }
}

@Composable
private fun ShopAssetCard(
    item: CatalogAsset,
    countryCode: String,
    canAfford: Boolean,
    onBuy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (canAfford) 1f else 0.65f),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IllustrationImage(
                ref = IllustrationCatalog.getIllustrationForAsset(item.type),
                size = 52.dp,
                contentDescription = item.name
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.format_asset_price_upkeep,
                        formatMoney(item.purchasePrice),
                        formatMoney(item.monthlyUpkeep)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!canAfford) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = stringResource(R.string.content_desc_locked),
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.label_insufficient_funds),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Button(
                onClick = onBuy,
                enabled = canAfford,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = MaishaRadius.buttonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    if (canAfford) {
                        stringResource(R.string.btn_buy)
                    } else {
                        stringResource(R.string.btn_locked)
                    }
                )
            }
        }
    }
}
