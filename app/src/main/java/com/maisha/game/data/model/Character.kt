// app/src/main/java/com/maisha/game/data/model/Character.kt (modified — birthCountryCode for relocation)
package com.maisha.game.data.model

/**
 * Complete playable state for one life in a save slot.
 *
 * Immutable between engine calls; engines return updated copies via [copy].
 * Persisted by [com.maisha.game.data.local.CharacterRepository] as a Room row with JSON blobs.
 *
 * @property countryCode Current country of residence (jobs, events, flavor).
 * @property birthCountryCode Country where this life began; used for heritage and relocation achievements.
 * @property secondaryCountryCode Optional second nationality (mixed-heritage flows); usually null on Character.
 * @property generationNumber Starts at 1 for a new slot life; increments on legacy continuation.
 * @property ancestryHistory Deceased prior generations in this slot (append-only on legacy).
 * @property eventLog Newest-first text lines; capped on save via [com.maisha.game.domain.EventLogCap].
 */
data class Character(
    val name: String,
    val age: Int,
    val gender: Gender,
    val stats: Stats,
    val birthYear: Int,
    val alive: Boolean = true,
    val countryCode: String = "KE",
    val birthCountryCode: String = "KE",
    val secondaryCountryCode: String? = null,
    val relocationCount: Int = 0,
    val yearsInCurrentCountry: Int = 0,
    val lastRelocationAge: Int? = null,
    val lastHolidayAge: Int? = null,
    val relocationHistory: List<String> = emptyList(),
    val ancestryHistory: List<AncestryEntry> = emptyList(),
    val avatarConfig: AvatarConfig = AvatarConfig.DEFAULT,
    val eventLog: List<String> = emptyList(),
    val family: List<Person> = emptyList(),
    val education: EducationState = EducationState(),
    val career: CareerState = CareerState(),
    val assets: List<Asset> = emptyList(),
    val pets: List<Pet> = emptyList(),
    val criminalRecord: CriminalRecord = CriminalRecord(),
    val activeConditions: List<HealthCondition> = emptyList(),
    val generationNumber: Int = 1,
    val economicState: EconomicState = EconomicState(),
    val lifestyle: LifestyleState = LifestyleState(),
    val socialMedia: SocialMediaState = SocialMediaState(),
    val skills: List<SkillProgress> = emptyList(),
    val businesses: List<Business> = emptyList()
)
