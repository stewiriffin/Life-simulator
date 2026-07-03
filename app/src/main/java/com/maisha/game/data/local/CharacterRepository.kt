// app/src/main/java/com/maisha/game/data/local/CharacterRepository.kt (modified — multi-slot)
package com.maisha.game.data.local

import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Character
import com.maisha.game.domain.AncestryHistoryCap
import com.maisha.game.domain.EventLogCap
import com.maisha.game.domain.RelationshipMilestoneCap
import com.maisha.game.util.SerializationUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

const val MAX_SLOTS = 3

data class SlotSummary(
    val slotId: Int,
    val name: String?,
    val age: Int?,
    val alive: Boolean?,
    val isEmpty: Boolean,
    val isCorrupted: Boolean = false,
    val countryCode: String? = null,
    val avatarConfig: AvatarConfig? = null,
    val generationNumber: Int? = null,
    val netWorth: Int? = null,
    val jobTitle: String? = null,
    val isRetired: Boolean = false
)

data class SavedGame(
    val character: Character,
    val triggeredEventIds: Set<String>
)

/**
 * Per-slot game persistence (slots 0..[MAX_SLOTS]-1).
 *
 * Each slot stores one [Character] plus [SavedGame.triggeredEventIds] for one-time JSON events.
 * Not global — achievements and settings live elsewhere.
 */
@Singleton
class CharacterRepository @Inject constructor(
    private val databaseHealth: GameDatabaseAccess
) {
    private val json = SerializationUtils.json

    val isDatabaseAvailable: Boolean
        get() = databaseHealth.isAvailable

    private val characterDao: CharacterDao?
        get() = databaseHealth.characterDao

    fun getCharacter(slotId: Int): Flow<Character?> =
        savedGameFlow(slotId).map { it?.character }

    fun savedGameFlow(slotId: Int): Flow<SavedGame?> =
        when (val dao = characterDao) {
            null -> flowOf(null)
            else -> dao.getCharacterFlow(requireValidSlot(slotId)).map { entity ->
                when (val result = entity?.let(CharacterSaveMapper::toSavedGame)) {
                    is SavedGameLoadResult.Success -> result.game
                    else -> null
                }
            }
        }

    suspend fun loadGame(slotId: Int): SavedGameLoadResult {
        val dao = characterDao ?: return SavedGameLoadResult.Corrupted
        val entity = dao.getCharacter(requireValidSlot(slotId)) ?: return SavedGameLoadResult.NotFound
        return CharacterSaveMapper.toSavedGame(entity)
    }

    suspend fun saveGame(slotId: Int, character: Character, triggeredEventIds: Set<String>) {
        val dao = characterDao ?: return
        val validSlot = requireValidSlot(slotId)
        dao.saveCharacter(character.toEntity(validSlot, triggeredEventIds))
    }

    suspend fun saveCharacter(slotId: Int, character: Character, triggeredEventIds: Set<String> = emptySet()) {
        saveGame(slotId, character, triggeredEventIds)
    }

    fun getAllSlots(): Flow<List<SlotSummary>> {
        val dao = characterDao
        if (dao == null) {
            return flowOf(emptySlotSummaries())
        }
        return dao.getAllCharactersFlow().map { entities ->
            val bySlot = entities.associateBy { it.slotId }
            (0 until MAX_SLOTS).map { slotId ->
                val entity = bySlot[slotId]
                if (entity == null) {
                    SlotSummary(
                        slotId = slotId,
                        name = null,
                        age = null,
                        alive = null,
                        isEmpty = true
                    )
                } else {
                    CharacterSaveMapper.toSlotSummary(entity)
                }
            }
        }
    }

    suspend fun clearSlot(slotId: Int) {
        characterDao?.clearSlot(requireValidSlot(slotId))
    }

    suspend fun clearAllSlots() {
        characterDao?.clearAll()
    }

    private fun emptySlotSummaries(): List<SlotSummary> =
        (0 until MAX_SLOTS).map { slotId ->
            SlotSummary(
                slotId = slotId,
                name = null,
                age = null,
                alive = null,
                isEmpty = true
            )
        }

    private fun requireValidSlot(slotId: Int): Int {
        require(slotId in 0 until MAX_SLOTS) { "Invalid slotId: $slotId" }
        return slotId
    }

    private fun Character.toEntity(slotId: Int, triggeredEventIds: Set<String>): CharacterEntity {
        return CharacterEntity(
            slotId = slotId,
            name = name,
            age = age,
            gender = gender.name,
            health = stats.health,
            happiness = stats.happiness,
            smarts = stats.smarts,
            looks = stats.looks,
            money = stats.money,
            birthYear = birthYear,
            alive = alive,
            countryCode = countryCode,
            birthCountryCode = birthCountryCode,
            secondaryCountryCode = secondaryCountryCode,
            relocationCount = relocationCount,
            yearsInCurrentCountry = yearsInCurrentCountry,
            lastRelocationAge = lastRelocationAge,
            lastHolidayAge = lastHolidayAge,
            relocationHistoryJson = json.encodeToString(relocationHistory),
            ancestryHistoryJson = json.encodeToString(AncestryHistoryCap.trim(ancestryHistory)),
            avatarConfigJson = json.encodeToString(avatarConfig),
            eventLogJson = json.encodeToString(EventLogCap.trim(eventLog)),
            triggeredEventIdsJson = json.encodeToString(triggeredEventIds.toList()),
            familyJson = json.encodeToString(RelationshipMilestoneCap.trimFamily(family)),
            educationJson = json.encodeToString(education),
            careerJson = json.encodeToString(career),
            assetsJson = json.encodeToString(assets),
            petsJson = json.encodeToString(pets),
            criminalRecordJson = json.encodeToString(criminalRecord),
            healthConditionsJson = json.encodeToString(activeConditions),
            generationNumber = generationNumber,
            lifestyleJson = json.encodeToString(lifestyle),
            socialMediaJson = json.encodeToString(socialMedia),
            skillsJson = json.encodeToString(skills),
            businessesJson = json.encodeToString(businesses)
        )
    }
}
