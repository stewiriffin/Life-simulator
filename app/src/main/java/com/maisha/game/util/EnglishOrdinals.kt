package com.maisha.game.util

/**
 * English ordinal suffixes (1st, 2nd, 3rd, 4th, 11th, 12th, 13th, 21st, …).
 */
object EnglishOrdinals {
    fun format(number: Int): String {
        require(number > 0) { "Ordinal requires a positive number, got $number" }
        val suffix = when {
            number % 100 in 11..13 -> "th"
            number % 10 == 1 -> "st"
            number % 10 == 2 -> "nd"
            number % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$number$suffix"
    }
}
