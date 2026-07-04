// app/src/main/java/com/maisha/game/data/JobPool.kt (modified — country-aware universal + flavor tiers)
package com.maisha.game.data

import com.maisha.game.data.model.Job
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.HustleType
import com.maisha.game.data.model.SkillType

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
 * - ZA: minibus taxi conductor (DBE/News24 — dominant informal PSV)
 * - EG: microbus driver (AUC/Cairo transport research — informal paratransit)
 *
 * US, CA, GB, FR, DE, MX: no single dominant informal transport-job title;
 * universal tier only (verified P35).
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
            baseSalary = 240_000,
            requiresDrivingLicense = true
        ),
        Job(
            id = "delivery_driver",
            title = "Delivery Driver",
            minEducation = SchoolStage.SECONDARY,
            baseSalary = 260_000,
            requiresDrivingLicense = true
        ),
        Job(
            id = "trucker",
            title = "Trucker",
            minEducation = SchoolStage.SECONDARY,
            baseSalary = 380_000,
            requiresDrivingLicense = true
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
            baseSalary = 900_000,
            skillBypass = SkillType.PROGRAMMING,
            minSkillLevel = 70
        ),
        Job(
            id = "journalist",
            title = "Journalist",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 500_000,
            skillBypass = SkillType.WRITING,
            minSkillLevel = 70
        ),
        Job(
            id = "chef",
            title = "Chef",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 520_000,
            skillBypass = SkillType.COOKING,
            minSkillLevel = 65
        ),
        Job(
            id = "session_musician",
            title = "Session Musician",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 480_000,
            skillBypass = SkillType.GUITAR,
            minSkillLevel = 65
        ),
        Job(
            id = "martial_arts_instructor",
            title = "Martial Arts Instructor",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 450_000,
            skillBypass = SkillType.MARTIAL_ARTS,
            minSkillLevel = 65
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
        ),
        Job(
            id = "brand_ambassador",
            title = "Brand Ambassador",
            minEducation = SchoolStage.SECONDARY,
            baseSalary = 700_000,
            minFollowers = 50_000
        ),
        Job(
            id = "professional_streamer",
            title = "Professional Streamer",
            minEducation = SchoolStage.SECONDARY,
            baseSalary = 850_000,
            minFollowers = 100_000
        ),
        Job(
            id = "content_creator",
            title = "Content Creator",
            minEducation = SchoolStage.SECONDARY,
            baseSalary = 600_000,
            minFollowers = 25_000
        ),
        Job(
            id = "military_private",
            title = "Private",
            minEducation = SchoolStage.NONE,
            baseSalary = 280_000,
            isMilitary = true
        ),
        Job(
            id = "military_sergeant",
            title = "Sergeant",
            minEducation = SchoolStage.NONE,
            baseSalary = 420_000,
            isMilitary = true
        ),
        Job(
            id = "military_general",
            title = "General",
            minEducation = SchoolStage.NONE,
            baseSalary = 900_000,
            isMilitary = true
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
                baseSalary = 175_000,
                requiresDrivingLicense = true
            )
        ),
        "IN" to listOf(
            Job(
                id = "auto_rickshaw_driver",
                title = "Auto-Rickshaw Driver",
                minEducation = SchoolStage.SECONDARY,
                baseSalary = 170_000,
                requiresDrivingLicense = true
            )
        ),
        "ID" to listOf(
            Job(
                id = "angkot_driver",
                title = "Angkot Driver",
                minEducation = SchoolStage.SECONDARY,
                baseSalary = 175_000,
                requiresDrivingLicense = true
            )
        ),
        "BR" to listOf(
            Job(
                id = "mototaxi_rider",
                title = "Mototaxi Rider",
                minEducation = SchoolStage.SECONDARY,
                baseSalary = 180_000
            )
        ),
        "ZA" to listOf(
            Job(
                id = "minibus_taxi_conductor",
                title = "Minibus Taxi Conductor",
                minEducation = SchoolStage.SECONDARY,
                baseSalary = 185_000
            )
        ),
        "EG" to listOf(
            Job(
                id = "microbus_driver",
                title = "Microbus Driver",
                minEducation = SchoolStage.SECONDARY,
                baseSalary = 175_000,
                requiresDrivingLicense = true
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

    data class SideHustleSpec(
        val type: HustleType,
        val basePayoutMin: Int,
        val basePayoutMax: Int,
        val minAge: Int = 18,
        val minSmarts: Int = 0,
        val requiresVehicle: Boolean = false,
        val requiresIctDegree: Boolean = false,
        val requiresGraduated: Boolean = false
    )

    private val sideHustleSpecs: List<SideHustleSpec> = listOf(
        SideHustleSpec(
            type = HustleType.RIDE_SHARE,
            basePayoutMin = 8_000,
            basePayoutMax = 28_000,
            requiresVehicle = true
        ),
        SideHustleSpec(
            type = HustleType.FREELANCE_CODING,
            basePayoutMin = 15_000,
            basePayoutMax = 45_000,
            minSmarts = 75,
            requiresIctDegree = true
        ),
        SideHustleSpec(
            type = HustleType.TUTORING,
            basePayoutMin = 6_000,
            basePayoutMax = 18_000,
            requiresGraduated = true
        ),
        SideHustleSpec(
            type = HustleType.FOOD_DELIVERY,
            basePayoutMin = 4_000,
            basePayoutMax = 14_000,
            minAge = 16
        ),
        SideHustleSpec(
            type = HustleType.RESELLING,
            basePayoutMin = 3_000,
            basePayoutMax = 12_000,
            minSmarts = 40
        )
    )

    fun getSideHustleSpec(type: HustleType): SideHustleSpec? =
        sideHustleSpecs.find { it.type == type }

    fun getAllSideHustleTypes(): List<HustleType> = sideHustleSpecs.map { it.type }

    fun meetsSideHustlePrerequisites(character: Character, type: HustleType): Boolean {
        val spec = getSideHustleSpec(type) ?: return false
        if (character.age < spec.minAge) return false
        if (spec.requiresVehicle && !ownsVehicle(character)) return false
        if (spec.requiresGraduated && character.education.stage != SchoolStage.GRADUATED) return false
        if (spec.requiresIctDegree) {
            val hasIct = character.education.courseOfStudy
                ?.equals(ICT_COURSE_NAME, ignoreCase = true) == true
            if (!hasIct && character.stats.smarts < spec.minSmarts) return false
        } else if (character.stats.smarts < spec.minSmarts) {
            return false
        }
        return true
    }

    private fun ownsVehicle(character: Character): Boolean =
        character.assets.any { it.type == AssetType.CAR || it.type == AssetType.MOTORBIKE }

    private const val ICT_COURSE_NAME = "Computer Science"
}
