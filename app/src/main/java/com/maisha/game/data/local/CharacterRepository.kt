// app/src/main/java/com/maisha/game/data/local/CharacterRepository.kt (modified — multi-slot)
package com.maisha.game.data.local

import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.AncestryEntry
import com.maisha.game.data.model.Asset
import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.HealthCondition
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.Stats
import com.maisha.game.domain.AncestryHistoryCap
import com.maisha.game.domain.EventLogCap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

const val MAX_SLOTS = 3

data class SlotSummary(
    val slotId: Int,
    val name: String?,
    val age: Int?,
    val alive: Boolean?,
    val isEmpty: Boolean,
    val countryCode: String? = null,
    val avatarConfig: com.maisha.game.data.model.AvatarConfig? = null,
    val generationNumber: Int? = null
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
    private val characterDao: CharacterDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getCharacter(slotId: Int): Flow<Character?> =
        savedGameFlow(slotId).map { it?.character }

    fun savedGameFlow(slotId: Int): Flow<SavedGame?> =
        characterDao.getCharacterFlow(requireValidSlot(slotId)).map { entity ->
            entity?.toSavedGame()
        }

    suspend fun loadGame(slotId: Int): SavedGame? {
        return characterDao.getCharacter(requireValidSlot(slotId))?.toSavedGame()
    }

    suspend fun saveGame(slotId: Int, character: Character, triggeredEventIds: Set<String>) {
        val validSlot = requireValidSlot(slotId)
        characterDao.saveCharacter(character.toEntity(validSlot, triggeredEventIds))
    }

    suspend fun saveCharacter(slotId: Int, character: Character, triggeredEventIds: Set<String> = emptySet()) {
        saveGame(slotId, character, triggeredEventIds)
    }

    fun getAllSlots(): Flow<List<SlotSummary>> =
        characterDao.getAllCharactersFlow().map { entities ->
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
                    SlotSummary(
                        slotId = slotId,
                        name = entity.name,
                        age = entity.age,
                        alive = entity.alive,
                        isEmpty = false,
                        countryCode = entity.countryCode,
                        avatarConfig = runCatching {
                            json.decodeFromString<AvatarConfig>(entity.avatarConfigJson)
                        }.getOrDefault(AvatarConfig.DEFAULT),
                        generationNumber = entity.generationNumber
                    )
                }
            }
        }

    suspend fun clearSlot(slotId: Int) {
        // Clears only character data for this slot — achievement_progress is untouched.
        characterDao.clearSlot(requireValidSlot(slotId))
    }

    suspend fun clearAllSlots() {
        characterDao.clearAll()
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
            lastRelocationAge = lastRelocationAge,
            lastHolidayAge = lastHolidayAge,
            relocationHistoryJson = json.encodeToString(relocationHistory),
            ancestryHistoryJson = json.encodeToString(AncestryHistoryCap.trim(ancestryHistory)),
            avatarConfigJson = json.encodeToString(avatarConfig),
            eventLogJson = json.encodeToString(EventLogCap.trim(eventLog)),
            triggeredEventIdsJson = json.encodeToString(triggeredEventIds.toList()),
            familyJson = json.encodeToString(family),
            educationJson = json.encodeToString(education),
            careerJson = json.encodeToString(career),
            assetsJson = json.encodeToString(assets),
            criminalRecordJson = json.encodeToString(criminalRecord),
            healthConditionsJson = json.encodeToString(activeConditions),
            generationNumber = generationNumber
        )
    }

    private fun CharacterEntity.toSavedGame(): SavedGame {
        val log: List<String> = json.decodeFromString(eventLogJson)
        val triggeredIds: List<String> = json.decodeFromString(triggeredEventIdsJson)
        val family: List<Person> = runCatching {
            json.decodeFromString<List<Person>>(familyJson)
        }.getOrDefault(emptyList())
        val education: EducationState = runCatching {
            json.decodeFromString<EducationState>(educationJson)
        }.getOrDefault(EducationState())
        val career: CareerState = runCatching {
            json.decodeFromString<CareerState>(careerJson)
        }.getOrDefault(CareerState())
        val assets: List<Asset> = runCatching {
            json.decodeFromString<List<Asset>>(assetsJson)
        }.getOrDefault(emptyList())
        val criminalRecord: CriminalRecord = runCatching {
            json.decodeFromString<CriminalRecord>(criminalRecordJson)
        }.getOrDefault(CriminalRecord())
        val healthConditions: List<HealthCondition> = runCatching {
            json.decodeFromString<List<HealthCondition>>(healthConditionsJson)
        }.getOrDefault(emptyList())
        val avatar: AvatarConfig = runCatching {
            json.decodeFromString<AvatarConfig>(avatarConfigJson)
        }.getOrDefault(AvatarConfig.DEFAULT)
        val relocationHistory: List<String> = runCatching {
            json.decodeFromString<List<String>>(relocationHistoryJson)
        }.getOrDefault(emptyList())
        val ancestryHistory: List<AncestryEntry> = runCatching {
            json.decodeFromString<List<AncestryEntry>>(ancestryHistoryJson)
        }.getOrDefault(emptyList())
        return SavedGame(
            character = Character(
                name = name,
                age = age,
                gender = Gender.valueOf(gender),
                stats = Stats(
                    health = health,
                    happiness = happiness,
                    smarts = smarts,
                    looks = looks,
                    money = money
                ),
                birthYear = birthYear,
                alive = alive,
                countryCode = countryCode,
                birthCountryCode = birthCountryCode,
                secondaryCountryCode = secondaryCountryCode,
                relocationCount = relocationCount,
                lastRelocationAge = lastRelocationAge,
                lastHolidayAge = lastHolidayAge,
                relocationHistory = relocationHistory,
                ancestryHistory = ancestryHistory,
                avatarConfig = avatar,
                eventLog = log,
                family = family,
                education = education,
                career = career,
                assets = assets,
                criminalRecord = criminalRecord,
                activeConditions = healthConditions,
                generationNumber = generationNumber
            ),
            triggeredEventIds = triggeredIds.toSet()
        )
    }
}
