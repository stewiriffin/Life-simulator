// app/src/main/java/com/maisha/game/util/FormatUtils.kt (modified — country-aware money)
package com.maisha.game.util

import com.maisha.game.data.CountryCatalog
import java.text.NumberFormat
import java.util.Locale

private val numberFormats = mutableMapOf<String, NumberFormat>()

private fun numberFormatFor(countryCode: String): NumberFormat {
    return numberFormats.getOrPut(countryCode) {
        val country = CountryCatalog.getCountry(countryCode)
        NumberFormat.getNumberInstance(Locale("", country.code))
    }
}

fun formatMoney(amount: Int, countryCode: String = "KE"): String {
    val country = CountryCatalog.getCountry(countryCode)
    val formatted = numberFormatFor(countryCode).format(amount)
    return "${country.currencySymbol} $formatted"
}

fun formatAge(age: Int): String = "Age $age"
