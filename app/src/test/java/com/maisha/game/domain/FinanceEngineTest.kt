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
}
