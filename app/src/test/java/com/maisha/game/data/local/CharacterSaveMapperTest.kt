package com.maisha.game.data.local

import com.maisha.game.data.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterSaveMapperTest {

    @Test
    fun toSavedGame_malformedFamilyJson_loadsCoreStatsWithEmptyFamily() {
        val entity = sampleEntity(familyJson = "{broken family json")
        val result = CharacterSaveMapper.toSavedGame(entity)
        assertTrue(result is SavedGameLoadResult.Success)
        val game = (result as SavedGameLoadResult.Success).game
        assertEquals("Alex", game.character.name)
        assertEquals(25, game.character.age)
        assertTrue(game.character.family.isEmpty())
    }

    @Test
    fun toSavedGame_invalidGender_marksCorrupted() {
        val entity = sampleEntity(gender = "NOT_A_GENDER")
        val result = CharacterSaveMapper.toSavedGame(entity)
        assertTrue(result is SavedGameLoadResult.Corrupted)
    }

    @Test
    fun toSlotSummary_corruptedRow_setsIsCorrupted() {
        val entity = sampleEntity(gender = "INVALID")
        val summary = CharacterSaveMapper.toSlotSummary(entity)
        assertTrue(summary.isCorrupted)
        assertEquals("Alex", summary.name)
        assertEquals(25, summary.age)
    }

    @Test
    fun toSlotSummary_validRow_isNotCorrupted() {
        val entity = sampleEntity()
        val summary = CharacterSaveMapper.toSlotSummary(entity)
        assertEquals(false, summary.isCorrupted)
        assertEquals(false, summary.isEmpty)
    }

    private fun sampleEntity(
        familyJson: String = "[]",
        gender: String = Gender.MALE.name
    ): CharacterEntity = CharacterEntity(
        slotId = 0,
        name = "Alex",
        age = 25,
        gender = gender,
        health = 80,
        happiness = 70,
        smarts = 60,
        looks = 50,
        money = 1000,
        birthYear = 2000,
        alive = true,
        eventLogJson = "[]",
        triggeredEventIdsJson = "[]",
        familyJson = familyJson
    )
}
