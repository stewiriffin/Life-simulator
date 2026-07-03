package com.maisha.game.domain

import com.maisha.game.data.CountryCatalog
import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.Job
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RelocationEngineTest {

    private val engine = RelocationEngine()

    @Test
    fun `relocate switches country and clears current job`() {
        val character = Character(
            name = "Test",
            age = 25,
            gender = Gender.MALE,
            stats = Stats(health = 80, happiness = 70, smarts = 60, looks = 50, money = 10_000),
            birthYear = 2000,
            countryCode = "KE",
            birthCountryCode = "KE",
            career = CareerState(
                currentJob = Job(
                    id = "matatu_conductor",
                    title = "Matatu Conductor",
                    minEducation = SchoolStage.SECONDARY,
                    baseSalary = 180_000
                ),
                yearsAtCurrentJob = 2
            )
        )

        val canada = CountryCatalog.getCountry("CA")
        val relocated = engine.relocate(character, canada)

        assertEquals("CA", relocated.countryCode)
        assertEquals("KE", relocated.birthCountryCode)
        assertEquals(1, relocated.relocationCount)
        assertEquals(listOf("CA"), relocated.relocationHistory)
        assertEquals(25, relocated.lastRelocationAge)
        assertNull(relocated.career.currentJob)
        assertTrue(relocated.career.jobHistory.contains("Matatu Conductor"))
        assertTrue(relocated.stats.happiness < character.stats.happiness)
    }

    @Test
    fun `hasRelocated is false when birth and current country match`() {
        val character = baseCharacter()
        assertFalse(engine.hasRelocated(character))
    }

    @Test
    fun `getRelocationOpportunities excludes current country`() {
        val character = baseCharacter().copy(countryCode = "KE", birthCountryCode = "KE")
        val opportunities = engine.getRelocationOpportunities(character)
        assertTrue(opportunities.isNotEmpty())
        assertTrue(opportunities.none { it.code == "KE" })
    }

    @Test
    fun `shouldOfferRelocation false when under minimum age`() {
        val character = baseCharacter().copy(age = 17)
        assertFalse(engine.shouldOfferRelocation(character, emptySet()))
    }

    @Test
    fun `shouldOfferRelocation false when incarcerated`() {
        val character = baseCharacter().copy(
            age = 30,
            criminalRecord = com.maisha.game.data.model.CriminalRecord(
                currentlyIncarcerated = true,
                yearsRemaining = 2
            )
        )
        assertFalse(engine.shouldOfferRelocation(character, emptySet()))
    }

    @Test
    fun `shouldOfferRelocation false when opportunity event already triggered`() {
        val character = baseCharacter().copy(age = 30)
        val eventId = engine.relocationOpportunityEventId(character.relocationCount)
        assertFalse(engine.shouldOfferRelocation(character, setOf(eventId)))
    }

    @Test
    fun `shouldOfferRelocation false when moved too recently`() {
        val character = baseCharacter().copy(
            age = 30,
            relocationCount = 1,
            lastRelocationAge = 28
        )
        assertFalse(engine.shouldOfferRelocation(character, emptySet()))
    }

    private fun baseCharacter() = TestFixtures.character(age = 22, countryCode = "KE")
}
