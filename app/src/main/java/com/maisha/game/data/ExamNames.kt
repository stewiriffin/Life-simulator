// app/src/main/java/com/maisha/game/data/ExamNames.kt (new)
package com.maisha.game.data

object ExamNames {

    fun primaryExamName(countryCode: String): String = when (countryCode) {
        "KE" -> "KCPE"
        "NG" -> "BECE"
        "ZA" -> "NSC"
        "EG" -> "Thanaweya Amma Prep"
        "US" -> "State Standards Exam"
        "CA" -> "Provincial Assessment"
        "GB" -> "GCSE"
        "FR" -> "Brevet"
        "DE" -> "Hauptschulabschluss"
        "IN" -> "Board Exam (Class 10)"
        "JP" -> "Junior High Exam"
        "PH" -> "NAT"
        "ID" -> "UN Exam"
        "BR" -> "ENEM Prep"
        "MX" -> "EXANI I"
        else -> "Primary Leaving Exam"
    }

    fun secondaryExamName(countryCode: String): String = when (countryCode) {
        "KE" -> "KCSE"
        "NG" -> "WAEC"
        "ZA" -> "Matric"
        "EG" -> "Thanaweya Amma"
        "US" -> "High School Diploma Exam"
        "CA" -> "Diploma Exam"
        "GB" -> "A-Levels"
        "FR" -> "Baccalauréat"
        "DE" -> "Abitur"
        "IN" -> "Board Exam (Class 12)"
        "JP" -> "University Entrance Exam"
        "PH" -> "UAN"
        "ID" -> "UN SMA"
        "BR" -> "ENEM"
        "MX" -> "EXANI II"
        else -> "Secondary Certificate Exam"
    }
}
