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
 * @property citizenships Passport countries held; defaults to [birthCountryCode] when empty (legacy saves).
 * @property currentVisa Active visa when living without citizenship in [countryCode]; null if citizen.
 * @property visaYearsRemaining Years left on [currentVisa] before deportation risk.
 * @property hasDrivingLicense Whether the character has passed a driving test.
 * @property will Custom estate shares: person id → percentage (1–100). Null uses even split among children.
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
    val citizenships: List<String> = emptyList(),
    val currentVisa: VisaType? = null,
    val visaYearsRemaining: Int = 0,
    val hasDrivingLicense: Boolean = false,
    /** Person id → estate percentage. Null triggers even split among living children. */
    val will: Map<String, Int>? = null,
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
    /** Fictional stock/investment portfolio (cash at risk, not gambling). */
    val investmentPortfolioValue: Int = 0,
    /** Last yearly portfolio return percentage (for UI). */
    val lastPortfolioReturnPercent: Int = 0,
    val lifestyle: LifestyleState = LifestyleState(),
    val socialMedia: SocialMediaState = SocialMediaState(),
    val skills: List<SkillProgress> = emptyList(),
    val businesses: List<Business> = emptyList(),
    val politics: PoliticalState = PoliticalState()
) {
    /** Passports held; falls back to birth country for pre-immigration saves. */
    fun passportsHeld(): List<String> =
        citizenships.ifEmpty { listOf(birthCountryCode) }.distinct()

    fun holdsCitizenship(countryCode: String = this.countryCode): Boolean =
        countryCode in passportsHeld()

    fun isLivingAbroad(): Boolean = !holdsCitizenship(countryCode)
}
