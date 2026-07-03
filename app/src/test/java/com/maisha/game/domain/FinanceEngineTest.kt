package com.maisha.game.domain

import com.maisha.game.data.model.AssetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceEngineTest {

    private val engine = FinanceEngine()

    @Test
    fun purchaseAsset_rejectsInsufficientFunds() {
        val character = TestFixtures.character(stats = com.maisha.game.data.model.Stats(money = 1_000))
        val result = engine.purchaseAsset(character, "motorbike_used")
        assertTrue(result is PurchaseResult.InsufficientFunds)
    }

    @Test
    fun purchaseAsset_deductsMoneyOnSuccess() {
        val character = TestFixtures.character(stats = com.maisha.game.data.model.Stats(money = 500_000))
        val result = engine.purchaseAsset(character, "motorbike_used")
        assertTrue(result is PurchaseResult.Success)
        val updated = (result as PurchaseResult.Success).character
        assertTrue(updated.stats.money < 500_000)
        assertEquals(1, updated.assets.size)
    }

    @Test
    fun applyUpkeep_reducesMoneyOverMultipleYears() {
        val character = TestFixtures.character(
            stats = com.maisha.game.data.model.Stats(money = 1_000_000),
            assets = listOf(TestFixtures.asset(monthlyUpkeep = 2_000))
        )
        val afterOne = engine.applyUpkeep(character)
        val afterTwo = engine.applyUpkeep(afterOne)
        assertTrue(afterTwo.stats.money < afterOne.stats.money)
        assertTrue(afterOne.stats.money < character.stats.money)
    }

    @Test
    fun degradeAssets_reducesConditionOverYears() {
        var character = TestFixtures.character(
            assets = listOf(TestFixtures.asset(condition = 100))
        )
        repeat(5) {
            character = engine.degradeAssets(character)
        }
        assertTrue(character.assets.first().condition < 100)
    }

    @Test
    fun calculateNetWorth_sumsCashAndAssetValues() {
        val character = TestFixtures.character(
            stats = com.maisha.game.data.model.Stats(money = 50_000),
            assets = listOf(
                TestFixtures.asset(id = "a1", currentValue = 120_000, type = AssetType.HOUSE),
                TestFixtures.asset(id = "a2", currentValue = 30_000)
            )
        )
        assertEquals(200_000, engine.calculateNetWorth(character))
    }

    @Test
    fun recalculateValue_allowsRealEstateToAppreciate() {
        val house = TestFixtures.asset(
            type = AssetType.HOUSE,
            currentValue = 200_000,
            condition = 100
        ).copy(purchasePrice = 200_000)
        val appreciated = engine.recalculateValue(house, marketModifier = FinanceEngine.BOOM_MARKET_MODIFIER)
        assertTrue(
            "Real estate should appreciate under a boom modifier",
            appreciated.currentValue >= house.purchasePrice
        )

        val car = TestFixtures.asset(
            type = AssetType.CAR,
            currentValue = 100_000,
            condition = 80
        ).copy(purchasePrice = 100_000)
        val carValue = engine.recalculateValue(car, marketModifier = FinanceEngine.BOOM_MARKET_MODIFIER)
        assertTrue(
            "Vehicles should not benefit from a boom modifier",
            carValue.currentValue <= 80_000
        )
    }

    @Test
    fun applyEconomicShift_altersTotalNetWorth() {
        val character = TestFixtures.character(
            stats = com.maisha.game.data.model.Stats(money = 50_000),
            assets = listOf(
                TestFixtures.asset(
                    type = AssetType.HOUSE,
                    currentValue = 300_000,
                    condition = 100
                ).copy(purchasePrice = 300_000)
            )
        )
        val before = engine.calculateNetWorth(character)
        val boom = engine.applyEconomicShift(character, forced = "boom")
        val bust = engine.applyEconomicShift(character, forced = "bust")
        assertTrue(engine.calculateNetWorth(boom.character) != before || boom.character.economicState.marketModifier > 1f)
        assertTrue(engine.calculateNetWorth(bust.character) < engine.calculateNetWorth(boom.character))
    }

    @Test
    fun repairAsset_deductsCashAndRestoresConditionToFull() {
        val asset = TestFixtures.asset(
            id = "repair-me",
            condition = 45,
            currentValue = 50_000
        ).copy(purchasePrice = 200_000)
        val character = TestFixtures.character(
            stats = com.maisha.game.data.model.Stats(money = 500_000),
            assets = listOf(asset)
        )
        val cost = engine.calculateRepairCost(asset, character.countryCode)
        val result = engine.repairAsset(character, asset.id)
        assertTrue(result is RepairResult.Success)
        val updated = (result as RepairResult.Success).character
        assertEquals(100, updated.assets.first().condition)
        assertEquals(500_000 - cost, updated.stats.money)
    }

    @Test
    fun degradeAssets_triggersCriticalFailureOnLowCondition() {
        val worn = TestFixtures.character(
            assets = listOf(TestFixtures.asset(condition = 35))
        )
        var criticalFailures = 0
        repeat(400) {
            val after = engine.degradeAssets(worn)
            if (after.assets.first().condition == 0) {
                criticalFailures++
            }
        }
        assertTrue(
            "Assets below 40% condition should sometimes critically fail",
            criticalFailures > 40
        )
    }

    @Test
    fun applyUpkeep_heirloomsAppreciateInValue() {
        val heirloom = TestFixtures.asset(
            id = "watch",
            currentValue = 100_000,
            monthlyUpkeep = 0,
            type = AssetType.HEIRLOOM,
            isHeirloom = true
        )
        val character = TestFixtures.character(
            stats = com.maisha.game.data.model.Stats(money = 1_000_000),
            assets = listOf(heirloom)
        )
        val after = engine.applyUpkeep(character)
        assertTrue(
            "Heirlooms should appreciate during the yearly upkeep tick",
            after.assets.first().currentValue > heirloom.currentValue
        )
    }

    @Test
    fun degradeAssets_heirloomsIgnoreCriticalFailure() {
        val heirloom = TestFixtures.asset(
            condition = 10,
            isHeirloom = true,
            type = AssetType.HEIRLOOM,
            monthlyUpkeep = 0
        )
        val character = TestFixtures.character(assets = listOf(heirloom))
        repeat(200) {
            val after = engine.degradeAssets(character)
            assertEquals(10, after.assets.first().condition)
        }
    }
}
