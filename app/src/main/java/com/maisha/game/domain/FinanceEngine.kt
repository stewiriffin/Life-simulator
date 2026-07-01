// app/src/main/java/com/maisha/game/domain/FinanceEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.AssetCatalog
import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.Asset
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.Character
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

sealed class PurchaseResult {
    data class Success(val character: Character) : PurchaseResult()
    data object InsufficientFunds : PurchaseResult()
}

@Singleton
class FinanceEngine @Inject constructor() {

    fun purchaseAsset(character: Character, assetCatalogId: String): PurchaseResult {
        val catalogItem = AssetCatalog.findById(assetCatalogId)
            ?: return PurchaseResult.InsufficientFunds

        val purchasePrice = EconomyScaler.scaleAmount(catalogItem.purchasePrice, character.countryCode)
        val monthlyUpkeep = EconomyScaler.scaleAmount(catalogItem.monthlyUpkeep, character.countryCode)

        if (character.stats.money < purchasePrice) {
            return PurchaseResult.InsufficientFunds
        }

        val asset = Asset(
            id = UUID.randomUUID().toString(),
            type = catalogItem.type,
            name = catalogItem.name,
            purchasePrice = purchasePrice,
            currentValue = purchasePrice,
            condition = 100,
            monthlyUpkeep = monthlyUpkeep
        )

        val updatedCharacter = character.copy(
            stats = character.stats.copy(
                money = character.stats.money - purchasePrice
            ),
            assets = character.assets + asset
        )
        return PurchaseResult.Success(updatedCharacter)
    }

    fun sellAsset(character: Character, assetId: String): Character {
        val asset = character.assets.find { it.id == assetId } ?: return character
        return character.copy(
            stats = character.stats.copy(
                money = character.stats.money + asset.currentValue
            ),
            assets = character.assets.filterNot { it.id == assetId }
        )
    }

    fun applyUpkeep(character: Character): Character {
        if (character.assets.isEmpty()) return character

        val rawAnnual = character.assets.sumOf { it.monthlyUpkeep * 12 }
        val stackDiscount = when {
            character.assets.size <= 1 -> 1.0
            character.assets.size == 2 -> 0.95
            character.assets.size == 3 -> 0.90
            else -> 0.85
        }
        val annualUpkeep = (rawAnnual * stackDiscount).toInt()
        val newMoney = character.stats.money - annualUpkeep

        return if (newMoney < 0) {
            character.copy(
                stats = character.stats.copy(
                    money = 0,
                    happiness = (character.stats.happiness - DEBT_HAPPINESS_PENALTY).coerceIn(0, 100)
                )
            )
        } else {
            character.copy(
                stats = character.stats.copy(money = newMoney)
            )
        }
    }

    fun degradeAssets(character: Character): Character {
        if (character.assets.isEmpty()) return character

        val degraded = character.assets.map { asset ->
            val degradation = Random.nextInt(MIN_DEGRADATION, MAX_DEGRADATION + 1)
            val newCondition = (asset.condition - degradation).coerceIn(0, 100)
            recalculateValue(asset.copy(condition = newCondition))
        }
        return character.copy(assets = degraded)
    }

    fun calculateNetWorth(character: Character): Int {
        val assetValue = character.assets.sumOf { it.currentValue }
        return character.stats.money + assetValue
    }

    fun applyConditionToAssetType(
        character: Character,
        type: AssetType,
        conditionDelta: Int
    ): Character {
        val index = character.assets.indexOfFirst { it.type == type }
        if (index == -1) return character

        val asset = character.assets[index]
        val newCondition = (asset.condition + conditionDelta).coerceIn(0, 100)
        val updatedAsset = recalculateValue(asset.copy(condition = newCondition))
        return character.copy(
            assets = character.assets.toMutableList().apply { this[index] = updatedAsset }
        )
    }

    fun applyConditionToFirstAsset(
        character: Character,
        conditionDelta: Int
    ): Character {
        if (character.assets.isEmpty()) return character
        val asset = character.assets.first()
        val newCondition = (asset.condition + conditionDelta).coerceIn(0, 100)
        val updatedAsset = recalculateValue(asset.copy(condition = newCondition))
        return character.copy(
            assets = listOf(updatedAsset) + character.assets.drop(1)
        )
    }

    fun meetsFinanceEventThreshold(character: Character): Boolean {
        return character.assets.isNotEmpty() || character.stats.money >= FINANCE_EVENT_MONEY_THRESHOLD
    }

    fun recalculateValue(asset: Asset): Asset {
        val value = (asset.purchasePrice * (asset.condition / 100f)).toInt()
        return asset.copy(currentValue = value.coerceAtLeast(0))
    }

    companion object {
        const val FINANCE_TAG = "finance"
        private const val FINANCE_EVENT_MONEY_THRESHOLD = 50_000
        private const val DEBT_HAPPINESS_PENALTY = 5
        private const val MIN_DEGRADATION = 2
        private const val MAX_DEGRADATION = 8
    }
}
