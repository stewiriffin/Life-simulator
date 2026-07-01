// app/src/main/java/com/maisha/game/data/local/CharacterEntity.kt (modified — country + avatar)
package com.maisha.game.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "character_save")
data class CharacterEntity(
    @PrimaryKey val slotId: Int,
    val name: String,
    val age: Int,
    val gender: String,
    val health: Int,
    val happiness: Int,
    val smarts: Int,
    val looks: Int,
    val money: Int,
    val birthYear: Int,
    val alive: Boolean,
    val countryCode: String = "KE",
    val avatarConfigJson: String = """{"skinTone":2,"hairStyle":1,"hairColor":2,"outfitColor":3}""",
    val eventLogJson: String,
    val triggeredEventIdsJson: String,
    val familyJson: String = "[]",
    val educationJson: String = "{}",
    val careerJson: String = "{}",
    val assetsJson: String = "[]",
    val criminalRecordJson: String = "{}",
    val healthConditionsJson: String = "[]",
    val generationNumber: Int = 1
)
