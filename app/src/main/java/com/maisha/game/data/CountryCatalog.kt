// app/src/main/java/com/maisha/game/data/CountryCatalog.kt (modified — CountryFlavor per roster country)
package com.maisha.game.data

import com.maisha.game.data.model.Country
import com.maisha.game.data.model.CountryFlavor

/**
 * Country roster + per-country flavor.
 *
 * Research notes (verified via official / reputable sources, 2025–2026):
 * - KE: KCPE/KCSE (KNEC); matatu; M-Pesa (Safaricom)
 * - NG: BECE (NECO junior cert); WAEC SSCE; danfo; OPay (CBN-licensed fintech)
 * - ZA: ANA assessments; NSC Matric; minibus taxi; SnapScan
 * - EG: Thanaweya Amma prep/final; metro/bus; Fawry
 * - US: state assessments; SAT/ACT; school bus; Venmo
 * - CA: provincial assessments; diploma exams; school bus; Interac e-Transfer
 * - GB: GCSE; A-Levels; bus/tube; mobile banking
 * - FR: Diplôme national du brevet; Baccalauréat; métro/RER; Lydia
 * - DE: Hauptschulabschluss; Abitur; U-Bahn/Bus; Giropay
 * - IN: CBSE Class 10 board; CBSE Class 12 board; auto-rickshaw; UPI (MoE push)
 * - JP: lower-secondary placement exams; university entrance exams; train; PayPay
 * - PH: NAT (DepEd K-12); NAT Grade 12; jeepney; GCash
 * - ID: UN SMP; UN SMA; angkot; GoPay
 * - BR: SAEB basic-ed assessment; ENEM (Inep/MEC); ônibus/metro; Pix
 * - MX: PLANEA basic assessment; COMIPEMS/EXANI II; metro/colectivo; SPEI
 */
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

    private val flavors: Map<String, CountryFlavor> = listOf(
        CountryFlavor("KE", "KCPE", "KCSE", "matatu", "M-Pesa", "Habari"),
        CountryFlavor("NG", "BECE", "WAEC", "danfo", "OPay", "Sannu"),
        CountryFlavor("ZA", "ANA", "NSC Matric", "minibus taxi", "SnapScan", "Sawubona"),
        CountryFlavor("EG", "Thanaweya Amma Prep", "Thanaweya Amma", "metro", "Fawry", "Ahlan"),
        CountryFlavor("US", "State Assessment", "SAT", "school bus", "Venmo", "Hey"),
        CountryFlavor("CA", "Provincial Assessment", "Diploma Exam", "school bus", "Interac", "Hello"),
        CountryFlavor("GB", "GCSE", "A-Levels", "bus", "mobile banking", "Hello"),
        CountryFlavor("FR", "Brevet", "Baccalauréat", "métro", "Lydia", "Bonjour"),
        CountryFlavor("DE", "Hauptschulabschluss", "Abitur", "U-Bahn", "Giropay", "Hallo"),
        CountryFlavor("IN", "CBSE Class 10 Board", "CBSE Class 12 Board", "auto-rickshaw", "UPI", "Namaste"),
        CountryFlavor("JP", "Junior High Exam", "University Entrance Exam", "train", "PayPay", "Konnichiwa"),
        CountryFlavor("PH", "NAT", "NAT Grade 12", "jeepney", "GCash", "Kumusta"),
        CountryFlavor("ID", "UN SMP", "UN SMA", "angkot", "GoPay", "Halo"),
        CountryFlavor("BR", "SAEB", "ENEM", "ônibus", "Pix", "Olá"),
        CountryFlavor("MX", "PLANEA", "COMIPEMS", "metro", "SPEI", "Hola")
    ).associateBy { it.countryCode }

    private val genericFlavor = CountryFlavor(
        countryCode = "XX",
        primaryExamName = "Primary Leaving Exam",
        secondaryExamName = "Secondary Certificate Exam",
        commonTransportMode = "public transport",
        popularMoneyAppOrBank = "mobile banking",
        greetingPhrase = "Hello"
    )

    private val byCode: Map<String, Country> = countries.associateBy { it.code }

    fun all(): List<Country> = countries

    fun getCountry(code: String): Country = byCode[code] ?: byCode.getValue("KE")

    fun flavorFor(countryCode: String): CountryFlavor =
        flavors[countryCode] ?: genericFlavor

    fun hasResearchedFlavor(countryCode: String): Boolean = countryCode in flavors

    fun search(query: String): List<Country> {
        if (query.isBlank()) return countries
        val q = query.trim().lowercase()
        return countries.filter {
            it.displayName.lowercase().contains(q) || it.code.lowercase().contains(q)
        }
    }
}
