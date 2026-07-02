// app/src/main/java/com/maisha/game/data/model/Character.kt (modified — birthCountryCode for relocation)
package com.maisha.game.data.model

data class Character(
    val name: String,
    val age: Int,
    val gender: Gender,
    val stats: Stats,
    val birthYear: Int,
    val alive: Boolean = true,
    val countryCode: String = "KE",
    val birthCountryCode: String = "KE",
    val avatarConfig: AvatarConfig = AvatarConfig.DEFAULT,
    val eventLog: List<String> = emptyList(),
    val family: List<Person> = emptyList(),
    val education: EducationState = EducationState(),
    val career: CareerState = CareerState(),
    val assets: List<Asset> = emptyList(),
    val criminalRecord: CriminalRecord = CriminalRecord(),
    val activeConditions: List<HealthCondition> = emptyList(),
    val generationNumber: Int = 1
)
