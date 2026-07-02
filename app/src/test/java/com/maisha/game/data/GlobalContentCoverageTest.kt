package com.maisha.game.data

import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.LifeEventList
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * P35: roster content coverage, placeholder wiring, and ISO country-code consistency.
 */
class GlobalContentCoverageTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val eventAssetPaths = listOf(
        "data/events/starter_events.json",
        "data/events/education_events.json",
        "data/events/career_events.json",
        "data/events/finance_events.json",
        "data/events/relationship_events.json",
        "data/events/general_events.json",
        "data/events/holiday_events.json"
    )

    @Test
    fun `every roster country has name pool with at least 10 names per gender`() {
        CountryCatalog.all().forEach { country ->
            val pool = NamePool.getNamePool(country.code)
            assertTrue(
                "${country.code} male names: ${pool.maleFirstNames.size}",
                pool.maleFirstNames.size >= 10
            )
            assertTrue(
                "${country.code} female names: ${pool.femaleFirstNames.size}",
                pool.femaleFirstNames.size >= 10
            )
            assertTrue(
                "${country.code} surnames: ${pool.surnames.size}",
                pool.surnames.size >= 10
            )
        }
    }

    @Test
    fun `every roster country has researched CountryFlavor with transport and exams`() {
        CountryCatalog.all().forEach { country ->
            assertTrue(CountryCatalog.hasResearchedFlavor(country.code))
            val flavor = CountryCatalog.flavorFor(country.code)
            assertTrue(flavor.primaryExamName.isNotBlank())
            assertTrue(flavor.secondaryExamName.isNotBlank())
            assertTrue(flavor.commonTransportMode.isNotBlank())
            assertTrue(flavor.notableHolidays.size >= 2)
        }
    }

    @Test
    fun `country catalog codes align with name pool and economy scaler keys`() {
        val rosterCodes = CountryCatalog.all().map { it.code }.toSet()
        assertTrue(rosterCodes.contains("KE"))
        assertNotEquals(
            "US and KE should have distinct name pools",
            NamePool.getNamePool("US").maleFirstNames.first(),
            NamePool.getNamePool("KE").maleFirstNames.first()
        )
        rosterCodes.forEach { code ->
            assertEquals(code, CountryCatalog.getCountry(code).code)
            assertTrue("Name pool should resolve for $code", NamePool.getNamePool(code).maleFirstNames.isNotEmpty())
        }
    }

    @Test
    fun `all placeholder-bearing events resolve for every roster country`() {
        val events = loadAllEvents()
        val placeholderEvents = events.filter { event ->
            containsKnownPlaceholder(event.text) ||
                event.choices.any { choice ->
                    containsKnownPlaceholder(choice.label) || containsKnownPlaceholder(choice.resultText)
                }
        }
        assertTrue("Expected placeholder events in assets", placeholderEvents.isNotEmpty())

        CountryCatalog.all().forEach { country ->
            placeholderEvents.forEach { event ->
                val resolved = resolveLikeEventRepository(event, country.code)
                assertNotNull("Holiday event should resolve for ${country.code}: ${event.id}", resolved)
                assertNoUnresolvedPlaceholders(resolved!!, "${country.code}/${event.id}")
            }
        }
    }

    @Test
    fun `placeholder events degrade gracefully for unknown country code`() {
        val events = loadAllEvents().filter { event ->
            FlavorInterpolator.HOLIDAY_TAG !in event.tags &&
                (containsKnownPlaceholder(event.text) ||
                    event.choices.any { containsKnownPlaceholder(it.label) || containsKnownPlaceholder(it.resultText) })
        }
        assertTrue(events.isNotEmpty())
        events.forEach { event ->
            val resolved = resolveLikeEventRepository(event, "XX")!!
            assertNoUnresolvedPlaceholders(resolved, "XX/${event.id}")
            val flavor = CountryCatalog.flavorFor("XX")
            if (event.text.contains("{transportMode}")) {
                assertTrue(resolved.text.contains(flavor.commonTransportMode))
            }
            if (event.text.contains("{moneyApp}")) {
                assertTrue(resolved.text.contains(flavor.popularMoneyAppOrBank!!))
            }
        }
    }

    @Test
    fun `ExamNames delegates to same flavor as interpolator`() {
        CountryCatalog.all().forEach { country ->
            val flavor = CountryCatalog.flavorFor(country.code)
            assertEquals(flavor.primaryExamName, ExamNames.primaryExamName(country.code))
            assertEquals(flavor.secondaryExamName, ExamNames.secondaryExamName(country.code))
        }
    }

    @Test
    fun `new job flavor entries are present for ZA and EG`() {
        assertTrue(JobPool.getJobsForCountry("ZA").any { it.id == "minibus_taxi_conductor" })
        assertTrue(JobPool.getJobsForCountry("EG").any { it.id == "microbus_driver" })
    }

    @Test
    fun `new asset flavor entries are present for NG and PH`() {
        assertTrue(AssetCatalog.getAssetsForCountry("NG").any { it.name == "Self-Contain" })
        assertTrue(AssetCatalog.getAssetsForCountry("PH").any { it.name == "Bedspace" })
    }

    private fun loadAllEvents(): List<LifeEvent> =
        eventAssetPaths.flatMap { path -> loadEventsFromAsset(path) }

    private fun loadEventsFromAsset(relativePath: String): List<LifeEvent> {
        val file = File("src/main/assets/$relativePath")
        assertTrue("Missing asset $relativePath at ${file.absolutePath}", file.exists())
        val text = file.readText()
        return json.decodeFromString<LifeEventList>(text).events
    }

    private fun resolveLikeEventRepository(event: LifeEvent, countryCode: String): LifeEvent? {
        if (FlavorInterpolator.HOLIDAY_TAG in event.tags) {
            return FlavorInterpolator.resolveHolidayEvent(event, countryCode)
        }
        return FlavorInterpolator.resolveEvent(event, countryCode)
    }

    private fun containsKnownPlaceholder(text: String): Boolean =
        FlavorInterpolator.containsPlaceholders(text)

    private fun assertNoUnresolvedPlaceholders(event: LifeEvent, label: String) {
        assertFalse("$label text unresolved: ${event.text}", hasBracePlaceholder(event.text))
        event.choices.forEach { choice ->
            assertFalse("$label choice label unresolved: ${choice.label}", hasBracePlaceholder(choice.label))
            assertFalse("$label choice result unresolved: ${choice.resultText}", hasBracePlaceholder(choice.resultText))
        }
    }

    private fun hasBracePlaceholder(text: String): Boolean =
        FlavorInterpolator.knownPlaceholders.any { placeholder -> "{$placeholder}" in text }
}
