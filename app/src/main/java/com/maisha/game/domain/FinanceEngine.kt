// app/src/main/java/com/maisha/game/domain/FinanceEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.AssetCatalog
import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.PetCatalog
import com.maisha.game.data.model.Asset
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EconomicClimate
import com.maisha.game.data.model.EconomicState
import com.maisha.game.data.model.PoliticalOffice
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.TaxPolicyType
import com.maisha.game.util.formatMoney
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
     * Ad-revenue payout for monetized social accounts, scaled by followers and country economy.
     */
    fun calculateSocialMediaPayout(character: Character): Int {
        val followers = character.socialMedia.followers.coerceAtLeast(0)
        val tiers = followers / 100_000
        val baseKenya = SOCIAL_MEDIA_BASE_PAYOUT_KENYA + tiers * SOCIAL_MEDIA_PAYOUT_PER_100K_KENYA
        return EconomyScaler.scaleAmount(baseKenya, character.countryCode)
    }

    /** Adds [payout] cash from social media monetization. */
    fun applySocialMediaRevenue(character: Character, payout: Int): Character {
        if (payout <= 0) return character
        return character.copy(
            stats = character.stats.copy(money = character.stats.money + payout)
        )
    }

    /** Deducts startup capital when founding a business. */
    fun applyBusinessInvestment(character: Character, investment: Int): Character {
        if (investment <= 0) return character
        return character.copy(
            stats = character.stats.copy(
                money = (character.stats.money - investment).coerceAtLeast(0)
            )
        )
    }

    /** Adds sale proceeds when liquidating a business. */
    fun applyBusinessSale(character: Character, valuation: Int): Character {
        if (valuation <= 0) return character
        return character.copy(
            stats = character.stats.copy(money = character.stats.money + valuation)
        )
    }

    /**
     * Passive yearly living expenses for each living [RelationType.CHILD] under 18.
     * Scaled to [Character.countryCode]; floors money at 0 with a happiness penalty if broke.
     */
    fun applyChildSupport(character: Character): Character {
        val minorChildren = character.family.count {
            it.relation == RelationType.CHILD && it.alive && it.age < MINOR_CHILD_MAX_AGE
        }
        if (minorChildren == 0) return character

        val perChild = EconomyScaler.scaleAmount(CHILD_SUPPORT_BASE_KENYA, character.countryCode)
        val total = perChild * minorChildren
        val newMoney = character.stats.money - total
        return if (newMoney < 0) {
            character.copy(
                stats = character.stats.copy(
                    money = 0,
                    happiness = clampStat(character.stats.happiness - DEBT_HAPPINESS_PENALTY)
                )
            )
        } else {
            character.copy(stats = character.stats.copy(money = newMoney))
        }
    }

    /**
     * Deducts annual pet care costs scaled to [Character.countryCode].
     * If money goes negative, floors at 0 and applies a happiness penalty.
     */
    fun applyPetUpkeep(character: Character): Character {
        if (character.pets.isEmpty()) return character

        val climateMultiplier = upkeepMultiplier(character.economicState.climate)
        val rawAnnual = character.pets.sumOf { pet ->
            val entry = PetCatalog.findBySpecies(pet.species) ?: return@sumOf 0
            EconomyScaler.scaleAmount(entry.yearlyUpkeep, character.countryCode)
        }
        val annualUpkeep = (rawAnnual * climateMultiplier).toInt()
        val newMoney = character.stats.money - annualUpkeep

        return if (newMoney < 0) {
            character.copy(
                stats = character.stats.copy(
                    money = 0,
                    happiness = clampStat(character.stats.happiness - DEBT_HAPPINESS_PENALTY)
                )
            )
        } else {
            character.copy(
                stats = character.stats.copy(money = newMoney)
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

    /** Random yearly wear; rented properties degrade faster. May trigger critical failure below 40%. */
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

    /** Houses that can be leased (non-heirloom real estate). */
    fun isRentable(asset: Asset): Boolean =
        asset.type == AssetType.HOUSE && !asset.isHeirloom

    /** Yearly rent for a leased property (5–8% of current valuation). */
    fun calculateYearlyRent(asset: Asset): Int {
        if (!asset.isRentedOut || !isRentable(asset)) return 0
        val rate = RENT_YIELD_MIN + Random.nextFloat() * (RENT_YIELD_MAX - RENT_YIELD_MIN)
        return (asset.currentValue * rate).toInt().coerceAtLeast(1)
    }

    fun estimateYearlyRent(asset: Asset): Int {
        if (!isRentable(asset)) return 0
        val midRate = (RENT_YIELD_MIN + RENT_YIELD_MAX) / 2f
        return (asset.currentValue * midRate).toInt().coerceAtLeast(1)
    }

    fun evictionFee(character: Character): Int =
        EconomyScaler.scaleAmount(EVICTION_LEGAL_FEE_KENYA, character.countryCode)

    /**
     * Collects rent from all leased properties and drifts tenant happiness.
     */
    fun collectRent(character: Character): Character {
        val rented = character.assets.filter { it.isRentedOut && isRentable(it) }
        if (rented.isEmpty()) return character

        var totalRent = 0
        val updatedAssets = character.assets.map { asset ->
            if (!asset.isRentedOut || !isRentable(asset)) {
                asset
            } else {
                val rent = calculateYearlyRent(asset)
                totalRent += rent
                val happiness = (asset.tenantHappiness ?: STARTING_TENANT_HAPPINESS) +
                    Random.nextInt(TENANT_HAPPINESS_DRIFT_MIN, TENANT_HAPPINESS_DRIFT_MAX + 1)
                asset.copy(tenantHappiness = happiness.coerceIn(0, 100))
            }
        }
        if (totalRent <= 0) return character.copy(assets = updatedAssets)

        return character.copy(
            stats = character.stats.copy(money = character.stats.money + totalRent),
            assets = updatedAssets,
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "Collected ${formatMoney(totalRent, character.countryCode)} in rental income."
            )
        )
    }

    sealed class RentalResult {
        data class Success(val character: Character) : RentalResult()
        data object NotFound : RentalResult()
        data object NotRentable : RentalResult()
        data object AlreadyRented : RentalResult()
        data object NotRented : RentalResult()
        data object InsufficientFunds : RentalResult()
    }

    /** Lists a vacant house for rent with a new tenant. */
    fun rentOutProperty(character: Character, assetId: String): RentalResult {
        val index = character.assets.indexOfFirst { it.id == assetId }
        if (index == -1) return RentalResult.NotFound
        val asset = character.assets[index]
        if (!isRentable(asset)) return RentalResult.NotRentable
        if (asset.isRentedOut) return RentalResult.AlreadyRented

        val rented = asset.copy(
            isRentedOut = true,
            tenantHappiness = STARTING_TENANT_HAPPINESS
        )
        val updatedAssets = character.assets.toMutableList().apply { this[index] = rented }
        return RentalResult.Success(
            character.copy(
                assets = updatedAssets,
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    "You rented out ${asset.name}. Tenants move in this week."
                )
            )
        )
    }

    /**
     * Ends a lease: legal fees, slight happiness hit, clears rental status.
     */
    fun evictTenant(character: Character, assetId: String): RentalResult {
        val index = character.assets.indexOfFirst { it.id == assetId }
        if (index == -1) return RentalResult.NotFound
        val asset = character.assets[index]
        if (!asset.isRentedOut) return RentalResult.NotRented

        val fee = evictionFee(character)
        if (character.stats.money < fee) return RentalResult.InsufficientFunds

        val vacant = asset.copy(isRentedOut = false, tenantHappiness = null)
        val updatedAssets = character.assets.toMutableList().apply { this[index] = vacant }
        return RentalResult.Success(
            character.copy(
                stats = character.stats.copy(
                    money = character.stats.money - fee,
                    happiness = clampStat(character.stats.happiness - EVICTION_HAPPINESS_HIT)
                ),
                assets = updatedAssets,
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    "You evicted the tenant from ${asset.name} for " +
                        "${formatMoney(fee, character.countryCode)} in legal fees."
                )
            )
        )
    }

    fun ownsRentedProperty(character: Character): Boolean =
        character.assets.any { it.isRentedOut && isRentable(it) }

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

    /** Cash, assets, businesses, and investment portfolio. */
    fun calculateNetWorth(character: Character): Int {
        val assetValue = character.assets.sumOf { it.currentValue }
        val businessValue = character.businesses.sumOf { it.valuation }
        return character.stats.money + assetValue + businessValue +
            character.investmentPortfolioValue.coerceAtLeast(0)
    }

    sealed class InvestmentResult {
        data class Success(val character: Character) : InvestmentResult()
        data object InsufficientFunds : InvestmentResult()
        data object InvalidAmount : InvestmentResult()
    }

    /** Moves liquid cash into the investment portfolio. */
    fun investFunds(character: Character, amount: Int): InvestmentResult {
        if (amount <= 0) return InvestmentResult.InvalidAmount
        if (character.stats.money < amount) return InvestmentResult.InsufficientFunds
        return InvestmentResult.Success(
            character.copy(
                stats = character.stats.copy(money = character.stats.money - amount),
                investmentPortfolioValue = character.investmentPortfolioValue + amount,
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    "Invested ${formatMoney(amount, character.countryCode)} in the market."
                )
            )
        )
    }

    /** Moves portfolio value back to liquid cash. */
    fun withdrawFunds(character: Character, amount: Int): InvestmentResult {
        if (amount <= 0) return InvestmentResult.InvalidAmount
        if (character.investmentPortfolioValue < amount) return InvestmentResult.InsufficientFunds
        return InvestmentResult.Success(
            character.copy(
                stats = character.stats.copy(money = character.stats.money + amount),
                investmentPortfolioValue = character.investmentPortfolioValue - amount,
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    "Withdrew ${formatMoney(amount, character.countryCode)} from investments."
                )
            )
        )
    }

    /**
     * Yearly portfolio tick: random return from [PORTFOLIO_RETURN_MIN_PERCENT] to
     * [PORTFOLIO_RETURN_MAX_PERCENT] when portfolio value is positive.
     */
    fun applyPortfolioMarketTick(character: Character): Character {
        if (character.investmentPortfolioValue <= 0) {
            return character.copy(lastPortfolioReturnPercent = 0)
        }
        val returnPercent = Random.nextInt(
            PORTFOLIO_RETURN_MIN_PERCENT,
            PORTFOLIO_RETURN_MAX_PERCENT + 1
        )
        return applyPortfolioReturn(character, returnPercent, logMarketMove = true)
    }

    /**
     * Applies an immediate portfolio return percentage (e.g. event-driven crash or rally).
     */
    fun applyPortfolioReturn(
        character: Character,
        returnPercent: Int,
        logMarketMove: Boolean = true
    ): Character {
        if (character.investmentPortfolioValue <= 0) {
            return character.copy(lastPortfolioReturnPercent = 0)
        }
        val before = character.investmentPortfolioValue
        val after = ((before.toLong() * (100L + returnPercent)) / 100L)
            .toInt()
            .coerceAtLeast(0)
        val sign = if (returnPercent >= 0) "+" else ""
        val logLine = if (logMarketMove) {
            "Portfolio returned $sign$returnPercent% " +
                "(${formatMoney(before, character.countryCode)} → " +
                "${formatMoney(after, character.countryCode)})."
        } else {
            null
        }
        return character.copy(
            investmentPortfolioValue = after,
            lastPortfolioReturnPercent = returnPercent,
            eventLog = if (logLine != null) {
                EventLogCap.prepend(character.eventLog, logLine)
            } else {
                character.eventLog
            }
        )
    }

    fun hasPortfolio(character: Character): Boolean =
        character.investmentPortfolioValue > 0

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
        val vacateTenant = asset.isRentedOut && conditionDelta <= TENANT_LEAVE_CONDITION_DELTA
        val updatedAsset = recalculateValue(
            asset.copy(
                condition = newCondition,
                isRentedOut = if (vacateTenant) false else asset.isRentedOut,
                tenantHappiness = if (vacateTenant) null else asset.tenantHappiness
            ),
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
        val index = indexOfPreferredAsset(character, type)
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

    /** Adjusts condition on the first asset of [type]; prefers rented houses for landlord events. */
    fun applyConditionToAssetType(
        character: Character,
        type: AssetType,
        conditionDelta: Int
    ): Character {
        val index = indexOfPreferredAsset(character, type)
        if (index == -1) return character
        return applyConditionToAsset(character, character.assets[index].id, conditionDelta)
    }

    private fun indexOfPreferredAsset(character: Character, type: AssetType): Int {
        val rented = character.assets.indexOfFirst { it.type == type && it.isRentedOut }
        if (rented >= 0) return rented
        return character.assets.indexOfFirst { it.type == type }
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
        val base = Random.nextInt(MIN_DEGRADATION, MAX_DEGRADATION + 1)
        val rentalWear = if (asset.isRentedOut) RENTED_EXTRA_DEGRADATION else 0
        val newCondition = clampCondition(asset.condition - base - rentalWear)
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

    sealed class TaxPolicyResult {
        data class Success(val character: Character) : TaxPolicyResult()
        data object Ineligible : TaxPolicyResult()
        data object AlreadyActive : TaxPolicyResult()
    }

    /**
     * Governors and presidents may enact tax policy.
     * Tax cuts boost business revenue (via [EconomyScaler]) but lower approval.
     * Wealth taxes raise approval but seize a slice of liquid cash.
     */
    fun passTaxPolicy(character: Character, type: TaxPolicyType): TaxPolicyResult {
        val office = character.politics.currentOffice
        if (office != PoliticalOffice.GOVERNOR && office != PoliticalOffice.PRESIDENT) {
            return TaxPolicyResult.Ineligible
        }
        if (character.politics.activeTaxPolicy == type) {
            return TaxPolicyResult.AlreadyActive
        }

        return when (type) {
            TaxPolicyType.TAX_CUTS -> {
                val approval = (character.politics.approvalRating - TAX_CUTS_APPROVAL_HIT)
                    .coerceIn(0, 100)
                TaxPolicyResult.Success(
                    character.copy(
                        politics = character.politics.copy(
                            activeTaxPolicy = TaxPolicyType.TAX_CUTS,
                            approvalRating = approval
                        ),
                        eventLog = EventLogCap.prepend(
                            character.eventLog,
                            "You passed tax cuts. Businesses cheer; the public is less impressed."
                        )
                    )
                )
            }
            TaxPolicyType.WEALTH_TAX -> {
                val fraction = EconomyScaler.policyWealthTaxCashFraction(TaxPolicyType.WEALTH_TAX)
                val cashHit = (character.stats.money * fraction).toInt().coerceAtLeast(0)
                val approval = (character.politics.approvalRating + WEALTH_TAX_APPROVAL_GAIN)
                    .coerceIn(0, 100)
                TaxPolicyResult.Success(
                    character.copy(
                        stats = character.stats.copy(
                            money = (character.stats.money - cashHit).coerceAtLeast(0)
                        ),
                        politics = character.politics.copy(
                            activeTaxPolicy = TaxPolicyType.WEALTH_TAX,
                            approvalRating = approval
                        ),
                        eventLog = EventLogCap.prepend(
                            character.eventLog,
                            "You passed a wealth tax. Public approval rose; your accounts lost " +
                                "${formatMoney(cashHit, character.countryCode)}."
                        )
                    )
                )
            }
        }
    }

    companion object {
        private const val TAX_CUTS_APPROVAL_HIT = 8
        private const val WEALTH_TAX_APPROVAL_GAIN = 10
        const val FINANCE_TAG = "finance"
        const val REQUIRES_RENTAL_TAG = "requires_rental"
        const val REQUIRES_PORTFOLIO_TAG = "requires_portfolio"
        /** Severe crash floor for yearly portfolio tick. */
        const val PORTFOLIO_RETURN_MIN_PERCENT = -30
        /** Bull-run ceiling for yearly portfolio tick. */
        const val PORTFOLIO_RETURN_MAX_PERCENT = 40
        private const val RENT_YIELD_MIN = 0.05f
        private const val RENT_YIELD_MAX = 0.08f
        private const val RENTED_EXTRA_DEGRADATION = 3
        private const val STARTING_TENANT_HAPPINESS = 72
        private const val TENANT_HAPPINESS_DRIFT_MIN = -5
        private const val TENANT_HAPPINESS_DRIFT_MAX = 4
        private const val EVICTION_LEGAL_FEE_KENYA = 25_000
        private const val EVICTION_HAPPINESS_HIT = 4
        /** Severe neglect on a rental causes the tenant to leave. */
        private const val TENANT_LEAVE_CONDITION_DELTA = -25
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
        private const val CHILD_SUPPORT_BASE_KENYA = 12_000
        private const val MINOR_CHILD_MAX_AGE = 18
        private const val SOCIAL_MEDIA_BASE_PAYOUT_KENYA = 80_000
        private const val SOCIAL_MEDIA_PAYOUT_PER_100K_KENYA = 40_000
    }
}
