package com.maisha.game.data

import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.LifeEventList
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CountryCatalogContentTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val eventAssetPaths = listOf(
        "data/events/starter_events.json",
        "data/events/education_events.json",
        "data/events/career_events.json",
        "data/events/finance_events.json",
        "data/events/relationship_events.json",
        "data/events/general_events.json",
        "data/events/holiday_events.json",
        "data/events/crime_events.json"
    )

    @Test
    fun `Kenya job pool includes matatu conductor`() {
        val jobs = JobPool.getJobsForCountry("KE")
        assertTrue(jobs.any { it.id == "matatu_conductor" })
        assertTrue(jobs.any { it.id == "teacher" })
    }

    @Test
    fun `Nigeria job pool includes danfo conductor`() {
        val jobs = JobPool.getJobsForCountry("NG")
        assertTrue(jobs.any { it.id == "danfo_conductor" })
    }

    @Test
    fun `US job pool is universal only`() {
        val jobs = JobPool.getJobsForCountry("US")
        assertFalse(JobPool.hasCountryFlavorJobs("US"))
        assertTrue(jobs.any { it.id == "driver" })
        assertFalse(jobs.any { it.id == "matatu_conductor" })
    }

    @Test
    fun `Kenya asset catalog includes bedsitter`() {
        val assets = AssetCatalog.getAssetsForCountry("KE")
        assertTrue(assets.any { it.name.contains("Bedsitter", ignoreCase = true) })
        assertTrue(assets.any { it.id == "boda_basic" })
    }

    @Test
    fun `India asset catalog uses PG room naming`() {
        val assets = AssetCatalog.getAssetsForCountry("IN")
        assertTrue(assets.any { it.name == "PG Room" })
    }

    @Test
    fun `Canada asset catalog includes downtown condo`() {
        val assets = AssetCatalog.getAssetsForCountry("CA")
        assertTrue(AssetCatalog.hasCountryFlavorAssets("CA"))
        assertTrue(assets.any { it.name == "Downtown Condo" })
        assertTrue(assets.any { it.name == "Suburban Bungalow" })
    }

    @Test
    fun `Japan US and UK exclusive housing listings are country-gated`() {
        assertTrue(AssetCatalog.getAssetsForCountry("JP").any { it.name == "Tokyo Micro-Apartment" })
        assertTrue(AssetCatalog.getAssetsForCountry("US").any { it.name == "Suburban American Home" })
        assertTrue(AssetCatalog.getAssetsForCountry("GB").any { it.name == "London Flat" })
        assertFalse(AssetCatalog.getAssetsForCountry("KE").any { it.name == "Tokyo Micro-Apartment" })
        assertFalse(AssetCatalog.getAssetsForCountry("FR").any { it.name == "London Flat" })
    }

    @Test
    fun `every roster country has at least 3 restrictedToCountry events`() {
        val events = loadAllEvents()
        CountryCatalog.all().forEach { country ->
            val count = events.count { it.restrictedToCountry == country.code }
            assertTrue(
                "${country.code} has only $count restricted events (need >= 3)",
                count >= 3
            )
        }
    }

    private fun loadAllEvents(): List<LifeEvent> =
        eventAssetPaths.flatMap { path -> loadEventsFromAsset(path) }

    private fun loadEventsFromAsset(relativePath: String): List<LifeEvent> {
        val file = File("src/main/assets/$relativePath")
        assertTrue("Missing asset $relativePath at ${file.absolutePath}", file.exists())
        return json.decodeFromString<LifeEventList>(file.readText()).events
    }

    @Test
    fun `Nigeria asset catalog uses self-contain naming`() {
        val assets = AssetCatalog.getAssetsForCountry("NG")
        assertTrue(assets.any { it.name == "Self-Contain" })
    }

    @Test
    fun `South Africa job pool includes minibus taxi conductor`() {
        val jobs = JobPool.getJobsForCountry("ZA")
        assertTrue(jobs.any { it.id == "minibus_taxi_conductor" })
    }

    @Test
    fun `findById resolves country-specific and universal entries`() {
        assertEquals("Matatu Conductor", JobPool.findById("matatu_conductor")?.title)
        assertEquals("Used Boda Boda", AssetCatalog.findById("boda_basic")?.name)
        assertEquals("Used Motorbike", AssetCatalog.findById("motorbike_used")?.name)
    }
}
