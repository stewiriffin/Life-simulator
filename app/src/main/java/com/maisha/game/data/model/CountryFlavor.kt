// app/src/main/java/com/maisha/game/data/model/CountryFlavor.kt (new)
package com.maisha.game.data.model

/**
 * Light cultural texture per country — exam names, everyday transport, and familiar
 * payment/greeting references. Researched terms; see CountryCatalog for sources.
 */
data class CountryFlavor(
    val countryCode: String,
    val primaryExamName: String,
    val secondaryExamName: String,
    val commonTransportMode: String,
    val popularMoneyAppOrBank: String? = null,
    val greetingPhrase: String? = null
)
