package com.maisha.game.data

import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.EventChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlavorInterpolatorTest {

    @Test
    fun `interpolates Nigeria transport and exams`() {
        val text = "After {secondaryExam}, you missed the {transportMode}."
        val resolved = FlavorInterpolator.interpolate(text, CountryCatalog.flavorFor("NG"))
        assertEquals("After WAEC, you missed the danfo.", resolved)
    }

    @Test
    fun `interpolates Philippines GCash and jeepney`() {
        val text = "Pay with {moneyApp} after the {transportMode} ride."
        val resolved = FlavorInterpolator.interpolate(text, CountryCatalog.flavorFor("PH"))
        assertEquals("Pay with GCash after the jeepney ride.", resolved)
    }

    @Test
    fun `resolveEvent updates choices`() {
        val event = LifeEvent(
            id = "test",
            minAge = 10,
            maxAge = 15,
            text = "Ride the {transportMode}.",
            choices = listOf(
                EventChoice(
                    label = "Use {moneyApp}",
                    resultText = "Paid with {moneyApp}.",
                    statEffects = emptyMap()
                )
            )
        )
        val resolved = FlavorInterpolator.resolveEvent(event, "IN")
        assertEquals("Ride the auto-rickshaw.", resolved.text)
        assertEquals("Use UPI", resolved.choices.first().label)
        assertEquals("Paid with UPI.", resolved.choices.first().resultText)
    }

    @Test
    fun `unknown country uses generic fallback`() {
        val flavor = CountryCatalog.flavorFor("XX")
        assertFalse(CountryCatalog.hasResearchedFlavor("XX"))
        assertEquals("public transport", flavor.commonTransportMode)
    }

    @Test
    fun `all roster countries have researched flavor`() {
        CountryCatalog.all().forEach { country ->
            assertTrue(
                "Missing flavor for ${country.code}",
                CountryCatalog.hasResearchedFlavor(country.code)
            )
        }
    }

    @Test
    fun `interpolates United Kingdom Monzo`() {
        val text = "Pay with {moneyApp}."
        val resolved = FlavorInterpolator.interpolate(text, CountryCatalog.flavorFor("GB"))
        assertEquals("Pay with Monzo.", resolved)
    }

    @Test
    fun `interpolates Japan verified exam names`() {
        val text = "After {secondaryExam}, you prepare for {primaryExam}."
        val resolved = FlavorInterpolator.interpolate(text, CountryCatalog.flavorFor("JP"))
        assertEquals(
            "After Common Test for University Admissions, you prepare for Junior High Entrance Exam.",
            resolved
        )
    }

    @Test
    fun `Kenya flavor includes Jamhuri Day holiday`() {
        val holidays = CountryCatalog.flavorFor("KE").notableHolidays
        assertTrue(holidays.any { it.name == "Jamhuri Day" })
    }

    @Test
    fun `holiday event resolves placeholders for Kenya`() {
        val event = com.maisha.game.data.model.LifeEvent(
            id = "test_holiday",
            minAge = 10,
            maxAge = 20,
            text = "{holidayDescription} Celebrate {holidayName}.",
            choices = listOf(
                com.maisha.game.data.model.EventChoice(
                    label = "Join in",
                    resultText = "You enjoyed {holidayName}.",
                    statEffects = emptyMap()
                )
            ),
            tags = listOf(FlavorInterpolator.HOLIDAY_TAG)
        )
        val resolved = FlavorInterpolator.resolveHolidayEvent(event, "KE")
        assertNotNull(resolved)
        assertTrue(resolved!!.text.contains("Jamhuri Day") || resolved.text.contains("Madaraka Day"))
    }

    @Test
    fun `all roster countries produce valid flag emoji`() {
        CountryCatalog.all().forEach { country ->
            val emoji = com.maisha.game.ui.components.countryCodeToFlagEmoji(country.code)
            assertEquals(4, emoji.length)
            assertFalse(emoji.contains(country.code))
        }
    }
}
