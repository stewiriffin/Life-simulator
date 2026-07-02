// app/src/main/java/com/maisha/game/data/model/CountryFlavor.kt (modified — notableHolidays)
package com.maisha.game.data.model

/**
 * Light cultural texture per country — exam names, everyday transport, familiar
 * payment/greeting references, and verified national/cultural holidays.
 * Researched terms; see CountryCatalog for sources.
 */
data class CountryFlavor(
    val countryCode: String,
    val primaryExamName: String,
    val secondaryExamName: String,
    val commonTransportMode: String,
    val popularMoneyAppOrBank: String? = null,
    val greetingPhrase: String? = null,
    val notableHolidays: List<HolidayFlavor> = emptyList()
)
