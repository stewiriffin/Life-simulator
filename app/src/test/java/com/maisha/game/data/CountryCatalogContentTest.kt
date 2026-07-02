package com.maisha.game.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryCatalogContentTest {

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
    fun `Canada asset catalog is universal naming`() {
        val assets = AssetCatalog.getAssetsForCountry("CA")
        assertFalse(AssetCatalog.hasCountryFlavorAssets("CA"))
        assertTrue(assets.any { it.name == "Studio Apartment" })
    }

    @Test
    fun `findById resolves country-specific and universal entries`() {
        assertEquals("Matatu Conductor", JobPool.findById("matatu_conductor")?.title)
        assertEquals("Used Boda Boda", AssetCatalog.findById("boda_basic")?.name)
        assertEquals("Used Motorbike", AssetCatalog.findById("motorbike_used")?.name)
    }
}
