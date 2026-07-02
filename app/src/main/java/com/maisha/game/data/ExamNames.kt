// app/src/main/java/com/maisha/game/data/ExamNames.kt (modified — delegates to CountryFlavor)
package com.maisha.game.data

object ExamNames {

    fun primaryExamName(countryCode: String): String =
        CountryCatalog.flavorFor(countryCode).primaryExamName

    fun secondaryExamName(countryCode: String): String =
        CountryCatalog.flavorFor(countryCode).secondaryExamName
}
