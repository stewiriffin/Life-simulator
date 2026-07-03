// app/src/main/java/com/maisha/game/data/model/Country.kt (new)
package com.maisha.game.data.model

/**
 * Roster entry for country selection and economy scaling (ISO 3166-1 alpha-2 [code]).
 */
data class Country(
    val code: String,
    val displayName: String,
    val currencyCode: String,
    val currencySymbol: String
)
