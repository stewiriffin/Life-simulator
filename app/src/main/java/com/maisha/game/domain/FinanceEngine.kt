// app/src/main/java/com/maisha/game/domain/FinanceEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.AssetCatalog
import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.Asset
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EconomicClimate
import com.maisha.game.data.model.EconomicState
import java.util.UUID
import com.maisha.game.util.clampCondition
import com.maisha.game.util.clampStat
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

sealed class PurchaseResult {
    data class Success(val character: Character) : PurchaseResult()
    data object InsufficientFunds : PurchaseResult()
}

sealed class RepairResult {
    data class Success(val character: Character) : RepairResult()
    data object InsufficientFunds : RepairResult()
    data object AssetNotFound : RepairResult()
}

data class EconomicShiftResult(
    val character: Character,
    val climate: EconomicClimate
)

@Singleton
class FinanceEngine @Inject constructor() {

    /**
     * Buys a catalog asset scaled to [Character.countryCode]; deducts scaled price from money.
     *
     * @return [PurchaseResult.InsufficientFunds] if unknown id or not enough cash.
     */
    fun purchaseAsset(character: Character, assetCatalogId: String): PurchaseResult {
        val catalogItem = AssetCatalog.findById(assetCatalogId)
            ?: return PurchaseResult.InsufficientFunds
        if (!catalogItem.isPurchasable || catalogItem.isHeirloom) {
            return PurchaseResult.InsufficientFunds
        }

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

    /** Sells [assetId] at [Asset.currentValue] and removes it from [Character.assets]. Heirlooms cannot be sold. */
    fun sellAsset(character: Character, assetId: String): Character {
        val asset = character.assets.find { it.id == assetId } ?: return character
        if (asset.isHeirloom) return character
        return character.copy(
            stats = character.stats.copy(
                money = character.stats.money + asset.currentValue
            ),
            assets = character.assets.filterNot { it.id == assetId }
        )
    }

    /**
     * Deducts annual upkeep (12× monthly, with multi-asset discount and economic climate).
     * If money goes negative, floors at 0 and applies a happiness penalty.
     */
    fun applyUpkeep(character: Character): Character {
        if (character.assets.isEmpty()) return character

        val rawAnnual = character.assets.sumOf { it.monthlyUpkeep * 12 }
        val stackDiscount = when {
            character.assets.size <= 1 -> 1.0
            character.assets.size == 2 -> 0.95
            character.assets.size == 3 -> 0.90
            else -> 0.85
        }
        val climateMultiplier = upkeepMultiplier(character.economicState.climate)
        val annualUpkeep = (rawAnnual * stackDiscount * climateMultiplier).toInt()
        val newMoney = character.stats.money - annualUpkeep

        return if (newMoney < 0) {
            appreciateHeirlooms(
                character.copy(
                    stats = character.stats.copy(
                        money = 0,
                        happiness = clampStat(character.stats.happiness - DEBT_HAPPINESS_PENALTY)
                    )
                )
            )
        } else {
            appreciateHeirlooms(
                character.copy(
                    stats = character.stats.copy(money = newMoney)
                )
            )
        }
    }

    /**
     * Grants a rare heirloom from [AssetCatalog] heirdom entries.
     * Skips if the character already owns that catalog item by name.
     */
    fun grantHeirloom(character: Character, catalogId: String): Character {
        val catalogItem = AssetCatalog.findHeirloomById(catalogId) ?: return character
        if (character.assets.any { it.isHeirloom && it.name == catalogItem.name }) return character

        val baseValue = EconomyScaler.scaleAmount(catalogItem.purchasePrice, character.countryCode)
        val heirloom = Asset(
            id = UUID.randomUUID().toString(),
            type = AssetType.HEIRLOOM,
            name = catalogItem.name,
            purchasePrice = baseValue,
            currentValue = baseValue,
            condition = 100,
            monthlyUpkeep = 0,
            isHeirloom = true,
            generationAcquired = character.generationNumber
        )

        return character.copy(
            assets = character.assets + heirloom,
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "A family heirloom joined your estate: ${catalogItem.name}."
            )
        )
    }

    private fun appreciateHeirlooms(character: Character): Character {
        if (character.assets.none { it.isHeirloom }) return character
        val appreciated = character.assets.map { asset ->
            if (!asset.isHeirloom) asset else {
                val boosted = (asset.currentValue * (1f + HEIRLOOM_ANNUAL_APPRECIATION_RATE)).toInt()
                    .coerceAtLeast(asset.currentValue + 1)
                asset.copy(currentValue = boosted, condition = 100)
            }
        }
        return character.copy(assets = appreciated)
    }

    /** Random yearly wear; may trigger critical failure when condition is below 40%. */
    fun degradeAssets(character: Character): Character {
        if (character.assets.isEmpty()) return character

        val modifier = character.economicState.marketModifier
        var updatedLog = character.eventLog
        val degraded = character.assets.map { asset ->
            if (asset.isHeirloom) asset else {
                val worn = applyConditionDegradation(asset, modifier)
                val afterFailure = rollCriticalFailure(worn, modifier)
                if (afterFailure.condition == 0 && worn.condition > 0) {
                    updatedLog = EventLogCap.prepend(updatedLog, criticalFailureLog(afterFailure))
                }
                afterFailure
            }
        }
        return character.copy(assets = degraded, eventLog = updatedLog)
    }

    /**
     * Pays to restore [assetId] to 100% condition. Cost scales with [Asset.purchasePrice] and wear.
     */
    fun repairAsset(character: Character, assetId: String): RepairResult {
        val index = character.assets.indexOfFirst { it.id == assetId }
        if (index == -1) return RepairResult.AssetNotFound

        val asset = character.assets[index]
        if (asset.isHeirloom || asset.condition >= 100) {
            return RepairResult.Success(character)
        }

        val cost = calculateRepairCost(asset, character.countryCode)
        if (character.stats.money < cost) {
            return RepairResult.InsufficientFunds
        }

        val repaired = recalculateValue(
            asset.copy(condition = 100),
            character.economicState.marketModifier
        )
        val updatedAssets = character.assets.toMutableList().apply { this[index] = repaired }
        return RepairResult.Success(
            character.copy(
                stats = character.stats.copy(money = character.stats.money - cost),
                assets = updatedAssets,
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    "Repaired ${asset.name} for ${formatRepairCost(cost, character.countryCode)}."
                )
            )
        )
    }

    /** Repair quote for UI confirmation dialogs. */
    fun calculateRepairCost(asset: Asset, countryCode: String): Int {
        val wearFraction = (100 - asset.condition.coerceIn(0, 100)) / 100f
        val rawCost = (asset.purchasePrice * REPAIR_BASE_RATE * wearFraction).toInt()
            .coerceAtLeast(MIN_REPAIR_COST)
        return EconomyScaler.scaleAmount(rawCost, countryCode)
    }

    /** Cash plus sum of [Asset.currentValue]. Used by achievements and UI net-worth displays. */
    fun calculateNetWorth(character: Character): Int {
        val assetValue = character.assets.sumOf { it.currentValue }
        return character.stats.money + assetValue
    }

    /** Pays annual pension to retired characters during the yearly finance tick. */
    fun applyPension(character: Character): Character {
        if (!character.career.isRetired || character.career.pensionAmount <= 0) return character
        return character.copy(
            stats = character.stats.copy(
                money = character.stats.money + character.career.pensionAmount
            )
        )
    }

    /**
     * Rolls or forces a yearly macro-economic shift (boom / bust / neutral) and refreshes asset values.
     *
     * @param forced `boom`, `bust`, or `random` from event choices; null rolls naturally.
     */
    fun applyEconomicShift(character: Character, forced: String? = null): EconomicShiftResult {
        val climate = resolveClimate(forced)
        val modifier = marketModifierFor(climate)
        val logEntry = economicLogFor(climate)
        val updatedAssets = character.assets.map { recalculateValue(it, modifier) }
        val updated = character.copy(
            economicState = EconomicState(climate = climate, marketModifier = modifier),
            assets = updatedAssets,
            eventLog = if (climate == EconomicClimate.NEUTRAL) {
                character.eventLog
            } else {
                EventLogCap.prepend(character.eventLog, logEntry)
            }
        )
        return EconomicShiftResult(updated, climate)
    }

    /** Adjusts condition on a specific owned asset by id. */
    fun applyConditionToAsset(
        character: Character,
        assetId: String,
        conditionDelta: Int
    ): Character {
        val index = character.assets.indexOfFirst { it.id == assetId }
        if (index == -1) return character

        val asset = character.assets[index]
        val newCondition = clampCondition(asset.condition + conditionDelta)
        val updatedAsset = recalculateValue(
            asset.copy(condition = newCondition),
            character.economicState.marketModifier
        )
        return character.copy(
            assets = character.assets.toMutableList().apply { this[index] = updatedAsset }
        )
    }

    /** Sets condition on the first asset of [type] to an absolute value (0–100). */
    fun setAssetConditionByType(
        character: Character,
        type: AssetType,
        condition: Int
    ): Character {
        val index = character.assets.indexOfFirst { it.type == type }
        if (index == -1) return character

        val asset = character.assets[index]
        val updatedAsset = recalculateValue(
            asset.copy(condition = clampCondition(condition)),
            character.economicState.marketModifier
        )
        return character.copy(
            assets = character.assets.toMutableList().apply { this[index] = updatedAsset }
        )
    }

    /** Adjusts condition on the first asset of [type]; used by finance event choices. */
    fun applyConditionToAssetType(
        character: Character,
        type: AssetType,
        conditionDelta: Int
    ): Character {
        val index = character.assets.indexOfFirst { it.type == type }
        if (index == -1) return character
        return applyConditionToAsset(character, character.assets[index].id, conditionDelta)
    }

    /** Adjusts condition on the first owned asset when event has no [EventChoice.targetAssetType]. */
    fun applyConditionToFirstAsset(
        character: Character,
        conditionDelta: Int
    ): Character {
        if (character.assets.isEmpty()) return character
        return applyConditionToAsset(character, character.assets.first().id, conditionDelta)
    }

    /** Gate for finance-tagged random events: owns assets or has at least FINANCE_EVENT_MONEY_THRESHOLD cash. */
    fun meetsFinanceEventThreshold(character: Character): Boolean {
        return character.assets.isNotEmpty() || character.stats.money >= FINANCE_EVENT_MONEY_THRESHOLD
    }

    /**
     * Sets [Asset.currentValue] from purchase price, condition, and [marketModifier].
     * Real estate may appreciate; vehicles only depreciate via condition.
     */
    fun recalculateValue(asset: Asset, marketModifier: Float = 1.0f): Asset {
        val conditionFactor = asset.condition / 100f
        val value = when (asset.type) {
            AssetType.HOUSE -> {
                val marketAdjusted = asset.purchasePrice * conditionFactor * marketModifier
                val appreciation = if (Random.nextFloat() < REAL_ESTATE_APPRECIATION_CHANCE) {
                    asset.purchasePrice * Random.nextFloat() * MAX_ANNUAL_APPRECIATION_RATE
                } else {
                    0f
                }
                (marketAdjusted + appreciation).toInt()
            }
            AssetType.CAR, AssetType.MOTORBIKE -> {
                (asset.purchasePrice * conditionFactor).toInt()
            }
            AssetType.HEIRLOOM -> {
                val appreciation = asset.currentValue * HEIRLOOM_ANNUAL_APPRECIATION_RATE
                (asset.currentValue + appreciation).toInt()
            }
        }
        return asset.copy(currentValue = value.coerceAtLeast(0))
    }

    private fun resolveClimate(forced: String?): EconomicClimate {
        return when (forced?.lowercase()) {
            "boom" -> EconomicClimate.BOOM
            "bust" -> EconomicClimate.BUST
            "random" -> rollClimate()
            null -> rollClimate()
            else -> rollClimate()
        }
    }

    private fun rollClimate(): EconomicClimate {
        val roll = Random.nextFloat()
        return when {
            roll < BOOM_CHANCE -> EconomicClimate.BOOM
            roll < BOOM_CHANCE + BUST_CHANCE -> EconomicClimate.BUST
            else -> EconomicClimate.NEUTRAL
        }
    }

    private fun marketModifierFor(climate: EconomicClimate): Float = when (climate) {
        EconomicClimate.BOOM -> BOOM_MARKET_MODIFIER
        EconomicClimate.BUST -> BUST_MARKET_MODIFIER
        EconomicClimate.NEUTRAL -> NEUTRAL_MARKET_MODIFIER
    }

    private fun upkeepMultiplier(climate: EconomicClimate): Double = when (climate) {
        EconomicClimate.BOOM -> BOOM_UPKEEP_MULTIPLIER
        EconomicClimate.BUST -> BUST_UPKEEP_MULTIPLIER
        EconomicClimate.NEUTRAL -> 1.0
    }

    private fun applyConditionDegradation(asset: Asset, marketModifier: Float): Asset {
        val degradation = Random.nextInt(MIN_DEGRADATION, MAX_DEGRADATION + 1)
        val newCondition = clampCondition(asset.condition - degradation)
        return recalculateValue(asset.copy(condition = newCondition), marketModifier)
    }

    private fun rollCriticalFailure(asset: Asset, marketModifier: Float): Asset {
        if (asset.isHeirloom) return asset
        if (asset.condition >= CRITICAL_CONDITION_THRESHOLD) return asset
        if (Random.nextFloat() >= CRITICAL_FAILURE_CHANCE) return asset
        return recalculateValue(asset.copy(condition = 0), marketModifier)
    }

    private fun criticalFailureLog(asset: Asset): String = when (asset.type) {
        AssetType.HOUSE -> "Critical failure: ${asset.name} suffered major damage — the roof collapsed."
        AssetType.CAR -> "Critical failure: ${asset.name}'s engine blew out completely."
        AssetType.MOTORBIKE -> "Critical failure: ${asset.name}'s engine seized on the road."
        AssetType.HEIRLOOM -> ""
    }

    private fun formatRepairCost(amount: Int, countryCode: String): String =
        com.maisha.game.util.formatMoney(amount, countryCode)

    private fun economicLogFor(climate: EconomicClimate): String = when (climate) {
        EconomicClimate.BOOM -> "A strong economy lifted property values this year."
        EconomicClimate.BUST -> "A tough economy pulled asset prices down this year."
        EconomicClimate.NEUTRAL -> ""
    }

    companion object {
        const val FINANCE_TAG = "finance"
        private const val FINANCE_EVENT_MONEY_THRESHOLD = 50_000
        private const val DEBT_HAPPINESS_PENALTY = 5
        private const val MIN_DEGRADATION = 2
        private const val MAX_DEGRADATION = 8
        private const val CRITICAL_CONDITION_THRESHOLD = 40
        private const val CRITICAL_FAILURE_CHANCE = 0.25f
        const val REPAIR_UI_THRESHOLD = 70
        private const val REPAIR_BASE_RATE = 0.18f
        private const val MIN_REPAIR_COST = 3_000
        private const val BOOM_CHANCE = 0.12f
        private const val BUST_CHANCE = 0.10f
        const val BOOM_MARKET_MODIFIER = 1.12f
        const val BUST_MARKET_MODIFIER = 0.88f
        const val NEUTRAL_MARKET_MODIFIER = 1.0f
        private const val BOOM_UPKEEP_MULTIPLIER = 1.06
        private const val BUST_UPKEEP_MULTIPLIER = 0.96
        private const val REAL_ESTATE_APPRECIATION_CHANCE = 0.35f
        private const val MAX_ANNUAL_APPRECIATION_RATE = 0.06f
        private const val HEIRLOOM_ANNUAL_APPRECIATION_RATE = 0.03f
    }
}
