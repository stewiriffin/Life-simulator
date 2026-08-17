package com.maisha.game.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.maisha.game.data.model.AssetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IllustrationCatalogTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun everyCatalogAsset_hasDrawableResource() {
        IllustrationCatalog.allCatalogAssetIllustrations().forEach { (catalogId, ref) ->
            val resId = context.resources.getIdentifier(ref.resourceName, "drawable", context.packageName)
            assertTrue("Missing drawable for $catalogId (${ref.resourceName})", resId != 0)
        }
    }

    @Test
    fun catalogAssets_sameType_canHaveDistinctArt() {
        val compact = IllustrationCatalog.getIllustrationForCatalogAsset("car_vitz")
        val luxury = IllustrationCatalog.getIllustrationForCatalogAsset("car_new")
        assertNotEquals(compact.resourceName, luxury.resourceName)

        val studio = IllustrationCatalog.getIllustrationForCatalogAsset("apartment_studio")
        val mansion = IllustrationCatalog.getIllustrationForCatalogAsset("house_karen")
        assertNotEquals(studio.resourceName, mansion.resourceName)
    }

    @Test
    fun purchaseSetsCatalogId_forIllustrationLookup() {
        val engine = com.maisha.game.domain.FinanceEngine()
        val character = com.maisha.game.domain.TestFixtures.character(
            age = 25,
            stats = com.maisha.game.data.model.Stats(money = 5_000_000)
        )
        val result = engine.purchaseAsset(character, "car_new")
        assertTrue(result is com.maisha.game.domain.PurchaseResult.Success)
        val asset = (result as com.maisha.game.domain.PurchaseResult.Success).character.assets.single()
        assertEquals("car_new", asset.catalogId)
        assertEquals(
            "ill_asset_car_new",
            IllustrationCatalog.getIllustrationForOwnedAsset(asset).resourceName
        )
    }

    @Test
    fun heirloomCatalogIds_mapToUniqueIcons() {
        val ids = AssetCatalog.getHeirloomAssets().map { it.id }
        val names = ids.map {
            IllustrationCatalog.getIllustrationForCatalogAsset(it).resourceName
        }.toSet()
        assertEquals(ids.size, names.size)
        assertTrue(names.all { it.startsWith("ill_asset_heirloom_") })
    }

    @Test
    fun ownedAssetWithoutCatalogId_fallsBackToType() {
        val asset = com.maisha.game.domain.TestFixtures.asset(type = AssetType.CAR)
        assertEquals(
            IllustrationCatalog.getIllustrationForAsset(AssetType.CAR).resourceName,
            IllustrationCatalog.getIllustrationForOwnedAsset(asset).resourceName
        )
    }
}
