package com.maisha.game.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterRepositoryCorruptionTest {

    @Test
    fun loadGame_whenDatabaseUnavailable_returnsCorrupted() = runBlocking {
        val repository = CharacterRepository(UnavailableDatabaseAccess())
        assertFalse(repository.isDatabaseAvailable)
        assertEquals(SavedGameLoadResult.Corrupted, repository.loadGame(0))
    }

    @Test
    fun getAllSlots_whenDatabaseUnavailable_returnsEmptySlotsWithoutCrash() = runBlocking {
        val repository = CharacterRepository(UnavailableDatabaseAccess())
        val slots = repository.getAllSlots().first()
        assertEquals(3, slots.size)
        assertTrue(slots.all { it.isEmpty && !it.isCorrupted })
    }

    private class UnavailableDatabaseAccess : GameDatabaseAccess {
        override val isAvailable: Boolean = false
        override val characterDao: CharacterDao? = null
        override val achievementDao: AchievementDao? = null
    }
}
