// app/src/main/java/com/maisha/game/data/local/CharacterEntity.kt (modified — birthCountryCode)
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
    val karma: Int = 50,
    val birthYear: Int,
    val alive: Boolean,
    val countryCode: String = "KE",
    val birthCountryCode: String = "KE",
    val secondaryCountryCode: String? = null,
    val citizenshipsJson: String = "[]",
    val currentVisa: String? = null,
    val visaYearsRemaining: Int = 0,
    val hasDrivingLicense: Boolean = false,
    val willJson: String? = null,
    val investmentPortfolioValue: Int = 0,
    val lastPortfolioReturnPercent: Int = 0,
    val relocationCount: Int = 0,
    val yearsInCurrentCountry: Int = 0,
    val lastRelocationAge: Int? = null,
    val lastHolidayAge: Int? = null,
    val relocationHistoryJson: String = "[]",
    val ancestryHistoryJson: String = "[]",
    val avatarConfigJson: String = """{"skinTone":2,"hairStyle":1,"hairColor":2,"outfitColor":3}""",
    val eventLogJson: String,
    val triggeredEventIdsJson: String,
    val familyJson: String = "[]",
    val educationJson: String = "{}",
    val careerJson: String = "{}",
    val assetsJson: String = "[]",
    val petsJson: String = "[]",
    val criminalRecordJson: String = "{}",
    val healthConditionsJson: String = "[]",
    val generationNumber: Int = 1,
    val lifestyleJson: String = "{}",
    val socialMediaJson: String = "{}",
    val skillsJson: String = "[]",
    val businessesJson: String = "[]",
    val politicsJson: String = "{}"
)
