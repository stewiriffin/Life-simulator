package com.maisha.game.data

import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.EventChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `all roster countries produce valid flag emoji`() {
        CountryCatalog.all().forEach { country ->
            val emoji = com.maisha.game.ui.components.countryCodeToFlagEmoji(country.code)
            assertEquals(4, emoji.length)
            assertFalse(emoji.contains(country.code))
        }
    }
}
