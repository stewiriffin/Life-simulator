package com.maisha.game.data.local

import android.util.Log
import com.maisha.game.data.model.Asset
import com.maisha.game.data.model.AncestryEntry
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.HealthCondition
import com.maisha.game.data.model.LifestyleState
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.Stats
import com.maisha.game.util.SerializationUtils

sealed class SavedGameLoadResult {
    data class Success(val game: SavedGame) : SavedGameLoadResult()
    data object NotFound : SavedGameLoadResult()
    data object Corrupted : SavedGameLoadResult()
}

internal object CharacterSaveMapper {

    private const val TAG = "CharacterSaveMapper"

    fun toSavedGame(entity: CharacterEntity): SavedGameLoadResult {
        return try {
            val gender = Gender.valueOf(entity.gender)
            val slotId = entity.slotId
            val log = SerializationUtils.safeDeserialize(
                entity.eventLogJson,
                fieldName = "eventLog",
                default = emptyList<String>(),
                slotId = slotId
            )
            val triggeredIds = SerializationUtils.safeDeserialize(
                entity.triggeredEventIdsJson,
                fieldName = "triggeredEventIds",
                default = emptyList<String>(),
                slotId = slotId
            )
            val family = SerializationUtils.safeDeserialize(
                entity.familyJson,
                fieldName = "family",
                default = emptyList<Person>(),
                slotId = slotId
            )
            val education = SerializationUtils.safeDeserialize(
                entity.educationJson,
                fieldName = "education",
                default = EducationState(),
                slotId = slotId
            )
            val career = SerializationUtils.safeDeserialize(
                entity.careerJson,
                fieldName = "career",
                default = CareerState(),
                slotId = slotId
            )
            val assets = SerializationUtils.safeDeserialize(
                entity.assetsJson,
                fieldName = "assets",
                default = emptyList<Asset>(),
                slotId = slotId
            )
            val criminalRecord = SerializationUtils.safeDeserialize(
                entity.criminalRecordJson,
                fieldName = "criminalRecord",
                default = CriminalRecord(),
                slotId = slotId
            )
            val healthConditions = SerializationUtils.safeDeserialize(
                entity.healthConditionsJson,
                fieldName = "activeConditions",
                default = emptyList<HealthCondition>(),
                slotId = slotId
            )
            val avatar = SerializationUtils.safeDeserialize(
                entity.avatarConfigJson,
                fieldName = "avatarConfig",
                default = AvatarConfig.DEFAULT,
                slotId = slotId
            )
            val relocationHistory = SerializationUtils.safeDeserialize(
                entity.relocationHistoryJson,
                fieldName = "relocationHistory",
                default = emptyList<String>(),
                slotId = slotId
            )
            val ancestryHistory = SerializationUtils.safeDeserialize(
                entity.ancestryHistoryJson,
                fieldName = "ancestryHistory",
                default = emptyList<AncestryEntry>(),
                slotId = slotId
            )
            val lifestyle = SerializationUtils.safeDeserialize(
                entity.lifestyleJson,
                fieldName = "lifestyle",
                default = LifestyleState(),
                slotId = slotId
            )
            SavedGameLoadResult.Success(
                SavedGame(
                    character = Character(
                        name = entity.name,
                        age = entity.age,
                        gender = gender,
                        stats = Stats(
                            health = entity.health,
                            happiness = entity.happiness,
                            smarts = entity.smarts,
                            looks = entity.looks,
                            money = entity.money
                        ),
                        birthYear = entity.birthYear,
                        alive = entity.alive,
                        countryCode = entity.countryCode,
                        birthCountryCode = entity.birthCountryCode,
                        secondaryCountryCode = entity.secondaryCountryCode,
                        relocationCount = entity.relocationCount,
                        yearsInCurrentCountry = entity.yearsInCurrentCountry,
                        lastRelocationAge = entity.lastRelocationAge,
                        lastHolidayAge = entity.lastHolidayAge,
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
                        generationNumber = entity.generationNumber,
                        lifestyle = lifestyle
                    ),
                    triggeredEventIds = triggeredIds.toSet()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load save for slot ${entity.slotId}; marking corrupted", e)
            SavedGameLoadResult.Corrupted
        }
    }

    fun toSlotSummary(entity: CharacterEntity): SlotSummary {
        return when (val result = toSavedGame(entity)) {
            is SavedGameLoadResult.Success -> {
                val character = result.game.character
                SlotSummary(
                    slotId = entity.slotId,
                    name = character.name,
                    age = character.age,
                    alive = character.alive,
                    isEmpty = false,
                    isCorrupted = false,
                    countryCode = character.countryCode,
                    avatarConfig = character.avatarConfig,
                    generationNumber = character.generationNumber
                )
            }
            is SavedGameLoadResult.Corrupted -> SlotSummary(
                slotId = entity.slotId,
                name = entity.name,
                age = entity.age,
                alive = entity.alive,
                isEmpty = false,
                isCorrupted = true,
                countryCode = entity.countryCode,
                avatarConfig = SerializationUtils.safeDeserialize(
                    entity.avatarConfigJson,
                    fieldName = "avatarConfig",
                    default = AvatarConfig.DEFAULT,
                    slotId = entity.slotId
                ),
                generationNumber = entity.generationNumber
            )
            SavedGameLoadResult.NotFound -> SlotSummary(
                slotId = entity.slotId,
                name = null,
                age = null,
                alive = null,
                isEmpty = true,
                isCorrupted = false
            )
        }
    }
}
