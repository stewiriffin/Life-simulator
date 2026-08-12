// app/src/main/java/com/maisha/game/data/model/CountryFlavor.kt
package com.maisha.game.data.model

/**
 * Light cultural texture per country — exams, transport, money apps, greetings,
 * holidays, and player-facing facility / school-year labels.
 */
data class CountryFlavor(
    val countryCode: String,
    val primaryExamName: String,
    val secondaryExamName: String,
    val commonTransportMode: String,
    val popularMoneyAppOrBank: String? = null,
    val greetingPhrase: String? = null,
    val notableHolidays: List<HolidayFlavor> = emptyList(),
    /** Private-care facility label shown in Actions / health logs. */
    val privateHospitalName: String = "Private Hospital",
    /** Secondary-school year word: Form / Year / Grade / Class. */
    val secondaryGradeLabel: String = "Year",
    /** Soft guardian word for universal childhood events when localized. */
    val guardianHonorific: String = "Mom"
)
