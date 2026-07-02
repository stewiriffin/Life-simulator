// app/src/main/java/com/maisha/game/data/CountryCatalog.kt (modified — P35 content completion pass)
package com.maisha.game.data

import com.maisha.game.data.model.Country
import com.maisha.game.data.model.CountryFlavor
import com.maisha.game.data.model.HolidayFlavor

/**
 * Country roster + per-country flavor.
 *
 * Content research notes (P35 audit — verified sources cited in coverage doc):
 * - Exams: JP Common Test (DNC/NIAD-Japan); ZA Systemic Evaluation (DBE, post-ANA);
 *   MX secondary updated from abolished COMIPEMS (La Jornada 2025) to UNAM entrance exam.
 * - Money apps: GB Monzo (UK FCA-licensed neobank, Finextra/SplitMetrics 2024).
 * - Holidays: second entries added per national calendars (gov.za, fmino.gov.ng, etc.).
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
        CountryFlavor(
            countryCode = "KE",
            primaryExamName = "KCPE",
            secondaryExamName = "KCSE",
            commonTransportMode = "matatu",
            popularMoneyAppOrBank = "M-Pesa",
            greetingPhrase = "Habari",
            notableHolidays = listOf(
                HolidayFlavor("Jamhuri Day", "December republic celebrations fill streets with music and flags."),
                HolidayFlavor("Madaraka Day", "June self-rule commemorations bring communities together.")
            )
        ),
        CountryFlavor(
            countryCode = "NG",
            primaryExamName = "BECE",
            secondaryExamName = "WAEC",
            commonTransportMode = "danfo",
            popularMoneyAppOrBank = "OPay",
            greetingPhrase = "Sannu",
            notableHolidays = listOf(
                HolidayFlavor("Independence Day", "October first parades and green-white celebrations nationwide."),
                HolidayFlavor("Democracy Day", "June twelfth commemorations honour Nigeria's return to democratic rule.")
            )
        ),
        CountryFlavor(
            countryCode = "ZA",
            primaryExamName = "Systemic Evaluation",
            secondaryExamName = "NSC Matric",
            commonTransportMode = "minibus taxi",
            popularMoneyAppOrBank = "SnapScan",
            greetingPhrase = "Sawubona",
            notableHolidays = listOf(
                HolidayFlavor("Heritage Day", "September braais and cultural dress honour the nation's diversity."),
                HolidayFlavor("Freedom Day", "April twenty-seventh marks South Africa's first democratic elections in 1994.")
            )
        ),
        CountryFlavor(
            countryCode = "EG",
            primaryExamName = "Thanaweya Amma Prep",
            secondaryExamName = "Thanaweya Amma",
            commonTransportMode = "metro",
            popularMoneyAppOrBank = "Fawry",
            greetingPhrase = "Ahlan",
            notableHolidays = listOf(
                HolidayFlavor("Revolution Day", "July commemorations mark the 1952 republic with public ceremonies."),
                HolidayFlavor("Sinai Liberation Day", "April twenty-fifth parades recall the 1982 return of the Sinai Peninsula.")
            )
        ),
        CountryFlavor(
            countryCode = "US",
            primaryExamName = "State Assessment",
            secondaryExamName = "SAT",
            commonTransportMode = "school bus",
            popularMoneyAppOrBank = "Venmo",
            greetingPhrase = "Hey",
            notableHolidays = listOf(
                HolidayFlavor("Thanksgiving", "November gatherings centre on gratitude, turkey, and family tables."),
                HolidayFlavor("Independence Day", "July fourth fireworks and barbecues mark the nation's founding.")
            )
        ),
        CountryFlavor(
            countryCode = "CA",
            primaryExamName = "Provincial Assessment",
            secondaryExamName = "Diploma Exam",
            commonTransportMode = "school bus",
            popularMoneyAppOrBank = "Interac",
            greetingPhrase = "Hello",
            notableHolidays = listOf(
                HolidayFlavor("Canada Day", "July first fireworks and maple flags mark confederation."),
                HolidayFlavor("Thanksgiving", "October long-weekend feasts gather families before winter sets in.")
            )
        ),
        CountryFlavor(
            countryCode = "GB",
            primaryExamName = "GCSE",
            secondaryExamName = "A-Levels",
            commonTransportMode = "bus",
            popularMoneyAppOrBank = "Monzo",
            greetingPhrase = "Hello",
            notableHolidays = listOf(
                HolidayFlavor("Guy Fawkes Night", "November bonfires and fireworks light up neighbourhoods."),
                HolidayFlavor("Remembrance Day", "November eleventh silences honour those who served in wartime.")
            )
        ),
        CountryFlavor(
            countryCode = "FR",
            primaryExamName = "Brevet",
            secondaryExamName = "Baccalauréat",
            commonTransportMode = "métro",
            popularMoneyAppOrBank = "Lydia",
            greetingPhrase = "Bonjour",
            notableHolidays = listOf(
                HolidayFlavor("Bastille Day", "July fourteenth parades and fireworks celebrate the republic."),
                HolidayFlavor("Labour Day", "May first marches and spring outings mark workers' solidarity.")
            )
        ),
        CountryFlavor(
            countryCode = "DE",
            primaryExamName = "Hauptschulabschluss",
            secondaryExamName = "Abitur",
            commonTransportMode = "U-Bahn",
            popularMoneyAppOrBank = "Giropay",
            greetingPhrase = "Hallo",
            notableHolidays = listOf(
                HolidayFlavor("Unity Day", "October third marks German reunification with civic events."),
                HolidayFlavor("Labour Day", "May first rallies and street festivals celebrate workers across Germany.")
            )
        ),
        CountryFlavor(
            countryCode = "IN",
            primaryExamName = "CBSE Class 10 Board",
            secondaryExamName = "CBSE Class 12 Board",
            commonTransportMode = "auto-rickshaw",
            popularMoneyAppOrBank = "UPI",
            greetingPhrase = "Namaste",
            notableHolidays = listOf(
                HolidayFlavor("Diwali", "Festival of lights — diyas, sweets, and family visits fill the week."),
                HolidayFlavor("Holi", "Spring colour festivals splash streets with powder and laughter.")
            )
        ),
        CountryFlavor(
            countryCode = "JP",
            primaryExamName = "Junior High Entrance Exam",
            secondaryExamName = "Common Test for University Admissions",
            commonTransportMode = "train",
            popularMoneyAppOrBank = "PayPay",
            greetingPhrase = "Konnichiwa",
            notableHolidays = listOf(
                HolidayFlavor("Obon", "Mid-August dances and lanterns honour ancestors and homecomings."),
                HolidayFlavor("New Year's Day", "January shrine visits and family meals open the year with hope.")
            )
        ),
        CountryFlavor(
            countryCode = "PH",
            primaryExamName = "NAT",
            secondaryExamName = "NAT Grade 12",
            commonTransportMode = "jeepney",
            popularMoneyAppOrBank = "GCash",
            greetingPhrase = "Kumusta",
            notableHolidays = listOf(
                HolidayFlavor("Independence Day", "June twelfth flag ceremonies and street parades nationwide."),
                HolidayFlavor("Rizal Day", "December thirtieth honours national hero José Rizal with wreath-laying.")
            )
        ),
        CountryFlavor(
            countryCode = "ID",
            primaryExamName = "UN SMP",
            secondaryExamName = "UN SMA",
            commonTransportMode = "angkot",
            popularMoneyAppOrBank = "GoPay",
            greetingPhrase = "Halo",
            notableHolidays = listOf(
                HolidayFlavor("Independence Day", "August seventeenth competitions and flag-raising at schools."),
                HolidayFlavor("Kartini Day", "April twenty-first celebrates women's emancipation with school ceremonies.")
            )
        ),
        CountryFlavor(
            countryCode = "BR",
            primaryExamName = "SAEB",
            secondaryExamName = "ENEM",
            commonTransportMode = "ônibus",
            popularMoneyAppOrBank = "Pix",
            greetingPhrase = "Olá",
            notableHolidays = listOf(
                HolidayFlavor("Carnival", "Pre-Lent samba, costumes, and street parties sweep the cities."),
                HolidayFlavor("Independence Day", "September seventh parades and green-yellow flags mark nationhood.")
            )
        ),
        CountryFlavor(
            countryCode = "MX",
            primaryExamName = "PLANEA",
            secondaryExamName = "UNAM Entrance Exam",
            commonTransportMode = "metro",
            popularMoneyAppOrBank = "SPEI",
            greetingPhrase = "Hola",
            notableHolidays = listOf(
                HolidayFlavor("Independence Day", "September sixteenth el grito re-enactments and plaza festivities."),
                HolidayFlavor("Día de los Muertos", "November altars of marigolds and pan de muerto welcome returning souls.")
            )
        )
    ).associateBy { it.countryCode }

    private val genericFlavor = CountryFlavor(
        countryCode = "XX",
        primaryExamName = "Primary Leaving Exam",
        secondaryExamName = "Secondary Certificate Exam",
        commonTransportMode = "public transport",
        popularMoneyAppOrBank = "mobile banking",
        greetingPhrase = "Hello",
        notableHolidays = emptyList()
    )

    private val byCode: Map<String, Country> = countries.associateBy { it.code }

    fun all(): List<Country> = countries

    fun getCountry(code: String): Country = byCode[code] ?: byCode.getValue("KE")

    fun flavorFor(countryCode: String): CountryFlavor =
        flavors[countryCode] ?: genericFlavor

    fun hasResearchedFlavor(countryCode: String): Boolean = countryCode in flavors

    fun hasHolidayEvents(countryCode: String): Boolean =
        flavorFor(countryCode).notableHolidays.isNotEmpty()

    fun search(query: String): List<Country> {
        if (query.isBlank()) return countries
        val q = query.trim().lowercase()
        return countries.filter {
            it.displayName.lowercase().contains(q) || it.code.lowercase().contains(q)
        }
    }
}
