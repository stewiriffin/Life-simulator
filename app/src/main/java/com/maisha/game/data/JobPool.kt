// app/src/main/java/com/maisha/game/data/JobPool.kt (modified — country-aware universal + flavor tiers)
package com.maisha.game.data

import com.maisha.game.data.model.Job
import com.maisha.game.data.model.SchoolStage

/**
 * Job catalog: universal tier (works in any country) plus optional verified local titles.
 *
 * Flavor research notes (verified / widely used terms):
 * - KE: matatu conductor (Nairobi informal PSV sector)
 * - NG: danfo conductor (Lagos informal bus network)
 * - PH: jeepney driver (iconic Filipino public transport)
 * - IN: auto-rickshaw driver (urban informal transport)
 * - ID: angkot driver (shared minivan transport)
 * - BR: mototaxi rider (mototaxista — informal motorcycle taxi)
 *
 * Countries without a verified local transport-job title use universal tier only.
 */
object JobPool {

    private val universalJobs: List<Job> = listOf(
        Job(
            id = "shop_attendant",
            title = "Shop Attendant",
            minEducation = SchoolStage.SECONDARY,
            baseSalary = 200_000
        ),
        Job(
            id = "security_guard",
            title = "Security Guard",
            minEducation = SchoolStage.SECONDARY,
            baseSalary = 220_000
        ),
        Job(
            id = "driver",
            title = "Driver",
            minEducation = SchoolStage.SECONDARY,
            baseSalary = 240_000
        ),
        Job(
            id = "teacher",
            title = "Teacher",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 480_000
        ),
        Job(
            id = "nurse",
            title = "Nurse",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 550_000
        ),
        Job(
            id = "accountant",
            title = "Accountant",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 650_000
        ),
        Job(
            id = "software_developer",
            title = "Software Developer",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 900_000
        ),
        Job(
            id = "journalist",
            title = "Journalist",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 500_000
        ),
        Job(
            id = "engineer",
            title = "Engineer",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 1_000_000
        ),
        Job(
            id = "civil_servant",
            title = "Civil Servant",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 450_000
        )
    )

    private val countryFlavorJobs: Map<String, List<Job>> = mapOf(
        "KE" to listOf(
            Job(
                id = "matatu_conductor",
                title = "Matatu Conductor",
                minEducation = SchoolStage.SECONDARY,
                baseSalary = 180_000
            )
        ),
        "NG" to listOf(
            Job(
                id = "danfo_conductor",
                title = "Danfo Conductor",
                minEducation = SchoolStage.SECONDARY,
                baseSalary = 185_000
            )
        ),
        "PH" to listOf(
            Job(
                id = "jeepney_driver",
                title = "Jeepney Driver",
                minEducation = SchoolStage.SECONDARY,
                baseSalary = 175_000
            )
        ),
        "IN" to listOf(
            Job(
                id = "auto_rickshaw_driver",
                title = "Auto-Rickshaw Driver",
                minEducation = SchoolStage.SECONDARY,
                baseSalary = 170_000
            )
        ),
        "ID" to listOf(
            Job(
                id = "angkot_driver",
                title = "Angkot Driver",
                minEducation = SchoolStage.SECONDARY,
                baseSalary = 175_000
            )
        ),
        "BR" to listOf(
            Job(
                id = "mototaxi_rider",
                title = "Mototaxi Rider",
                minEducation = SchoolStage.SECONDARY,
                baseSalary = 180_000
            )
        )
    )

    /** All jobs across universal + flavor tiers (for illustration validation / lookup). */
    val jobs: List<Job> by lazy {
        (universalJobs + countryFlavorJobs.values.flatten()).distinctBy { it.id }
    }

    fun getJobsForCountry(countryCode: String): List<Job> =
        universalJobs + (countryFlavorJobs[countryCode] ?: emptyList())

    fun hasCountryFlavorJobs(countryCode: String): Boolean =
        countryCode in countryFlavorJobs

    fun findById(jobId: String): Job? = jobs.find { it.id == jobId }
}
