// app/src/main/java/com/maisha/game/data/CountryCatalog.kt (new)
package com.maisha.game.data

import com.maisha.game.data.model.Country

object CountryCatalog {

    private val countries: List<Country> = listOf(
        Country("KE", "Kenya", "KES", "KSh"),
        Country("NG", "Nigeria", "NGN", "₦"),
        Country("ZA", "South Africa", "ZAR", "R"),
        Country("EG", "Egypt", "EGP", "E£"),
        Country("US", "United States", "USD", "$"),
        Country("CA", "Canada", "CAD", "CA$"),
        Country("GB", "United Kingdom", "GBP", "£"),
        Country("FR", "France", "EUR", "€"),
        Country("DE", "Germany", "EUR", "€"),
        Country("IN", "India", "INR", "₹"),
        Country("JP", "Japan", "JPY", "¥"),
        Country("PH", "Philippines", "PHP", "₱"),
        Country("ID", "Indonesia", "IDR", "Rp"),
        Country("BR", "Brazil", "BRL", "R$"),
        Country("MX", "Mexico", "MXN", "MX$")
    )

    private val byCode: Map<String, Country> = countries.associateBy { it.code }

    fun all(): List<Country> = countries

    fun getCountry(code: String): Country = byCode[code] ?: byCode.getValue("KE")

    fun search(query: String): List<Country> {
        if (query.isBlank()) return countries
        val q = query.trim().lowercase()
        return countries.filter {
            it.displayName.lowercase().contains(q) || it.code.lowercase().contains(q)
        }
    }
}
