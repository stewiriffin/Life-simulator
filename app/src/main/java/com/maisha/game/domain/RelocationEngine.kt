// app/src/main/java/com/maisha/game/domain/RelocationEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.CountryCatalog
import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Country
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.VisaType
import com.maisha.game.util.clampStat
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

sealed class VisaRenewalResult {
    data class Success(val character: Character) : VisaRenewalResult()
    data object NotNeeded : VisaRenewalResult()
    data object CannotAfford : VisaRenewalResult()
    data object Ineligible : VisaRenewalResult()
}

sealed class CitizenshipApplicationResult {
    data class Success(val character: Character) : CitizenshipApplicationResult()
    data class FailedTest(val character: Character) : CitizenshipApplicationResult()
    data object NotEligible : CitizenshipApplicationResult()
    data object CannotAfford : CitizenshipApplicationResult()
}

data class ImmigrationTickResult(
    val character: Character,
    val deported: Boolean = false,
    val deportationMessage: String? = null
)

@Singleton
class RelocationEngine @Inject constructor() {

    /** True if [Character.relocationCount] > 0 or living without citizenship in residence country. */
    fun hasRelocated(character: Character): Boolean =
        character.relocationCount > 0 || character.isLivingAbroad()

    /** Unique one-time event id per relocation index (supports multiple moves / World Traveler). */
    fun relocationOpportunityEventId(relocationCount: Int): String =
        "${RELOCATION_OPPORTUNITY_EVENT_ID}_$relocationCount"

    /** Random subset of countries excluding the character's current [Character.countryCode]. */
    fun getRelocationOpportunities(character: Character): List<Country> {
        val current = character.countryCode
        return CountryCatalog.all()
            .filter { it.code != current }
            .shuffled()
            .take(RELOCATION_DESTINATION_COUNT)
    }

    /**
     * Whether to roll a relocation offer this year: age window, not incarcerated, event not yet triggered,
     * and minimum gap since last move when [Character.relocationCount] > 0.
     */
    fun shouldOfferRelocation(character: Character, triggeredEventIds: Set<String>): Boolean {
        if (character.age < MIN_RELOCATION_AGE || character.age > MAX_RELOCATION_AGE) return false
        if (character.criminalRecord.currentlyIncarcerated) return false
        val eventId = relocationOpportunityEventId(character.relocationCount)
        if (eventId in triggeredEventIds) return false
        if (character.relocationCount > 0) {
            val lastAge = character.lastRelocationAge ?: return false
            if (character.age - lastAge < MIN_YEARS_BETWEEN_RELOCATIONS) return false
        }
        return Random.nextFloat() < RELOCATION_OFFER_CHANCE
    }

    /**
     * System [LifeEvent] with up to three destination choices plus stay-put; choices use [EventChoice.relocateToCountry].
     */
    fun buildRelocationOpportunityEvent(
        character: Character,
        destinations: List<Country>
    ): LifeEvent {
        val originName = CountryCatalog.getCountry(character.countryCode).displayName
        val destinationChoices = destinations.map { country ->
            val visaNote = if (character.holdsCitizenship(country.code)) {
                "You already hold a passport for ${country.displayName}."
            } else {
                "You would enter on a tourist visa and need to extend or naturalize later."
            }
            EventChoice(
                label = "Move to ${country.displayName}",
                statEffects = mapOf("happiness" to -8),
                resultText = "You packed your bags and left $originName behind. " +
                    "Starting over in ${country.displayName} means requalifying for local work — " +
                    "your old job credentials don't transfer automatically. $visaNote",
                relocateToCountry = country.code
            )
        }
        val declineChoice = EventChoice(
            label = "Stay in $originName",
            statEffects = mapOf("happiness" to 2),
            resultText = "Home still feels right. You tucked the idea away for another year."
        )
        return LifeEvent(
            id = relocationOpportunityEventId(character.relocationCount),
            minAge = character.age,
            maxAge = character.age,
            text = "An opportunity opens abroad — work contacts, a study pathway, or family already settled " +
                "in another country. You wonder if it is time to leave $originName for a new chapter.",
            choices = destinationChoices + declineChoice,
            tags = listOf(RELOCATION_SYSTEM_TAG, ONE_TIME_TAG),
            weight = 1
        )
    }

    /**
     * Applies a move: updates residence, increments relocation counters/history, clears job,
     * grants a visa when the destination is not a citizenship country, and applies a happiness dip.
     */
    fun relocate(
        character: Character,
        newCountry: Country,
        visaType: VisaType? = null
    ): Character {
        val formerJob = character.career.currentJob
        val updatedCareer = if (formerJob != null) {
            character.career.copy(
                currentJob = null,
                yearsAtCurrentJob = 0,
                jobHistory = character.career.jobHistory + formerJob.title
            )
        } else {
            character.career
        }

        val isCitizen = character.holdsCitizenship(newCountry.code)
        val grantedVisa = when {
            isCitizen -> null
            visaType != null -> visaType
            else -> VisaType.TOURIST
        }
        val visaYears = if (grantedVisa == null) 0 else defaultVisaYears(grantedVisa)

        val happinessDip = Random.nextInt(RELOCATION_HAPPINESS_DIP_MIN, RELOCATION_HAPPINESS_DIP_MAX + 1)
        return character.copy(
            countryCode = newCountry.code,
            relocationCount = character.relocationCount + 1,
            yearsInCurrentCountry = 0,
            lastRelocationAge = character.age,
            relocationHistory = character.relocationHistory + newCountry.code,
            currentVisa = grantedVisa,
            visaYearsRemaining = visaYears,
            career = updatedCareer,
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness - happinessDip)
            )
        )
    }

    /** Years of continuous residence required before dual citizenship can be requested. */
    fun canApplyForCitizenship(character: Character): Boolean =
        character.isLivingAbroad() &&
            character.yearsInCurrentCountry >= NATURALIZATION_YEARS &&
            !character.holdsCitizenship(character.countryCode)

    fun canRenewVisa(character: Character): Boolean =
        character.isLivingAbroad() && character.currentVisa != null

    fun visaRenewalFee(character: Character): Int =
        EconomyScaler.scaleAmount(VISA_RENEWAL_FEE_KENYA, character.countryCode)

    fun citizenshipFee(character: Character): Int =
        EconomyScaler.scaleAmount(CITIZENSHIP_FEE_KENYA, character.countryCode)

    fun renewVisa(character: Character): VisaRenewalResult {
        if (!canRenewVisa(character)) return VisaRenewalResult.Ineligible
        val visa = character.currentVisa ?: return VisaRenewalResult.NotNeeded
        val fee = visaRenewalFee(character)
        if (character.stats.money < fee) return VisaRenewalResult.CannotAfford
        val years = defaultVisaYears(visa)
        return VisaRenewalResult.Success(
            character.copy(
                visaYearsRemaining = character.visaYearsRemaining.coerceAtLeast(0) + years,
                stats = character.stats.copy(money = character.stats.money - fee),
                eventLog = listOf(
                    "You paid legal fees and renewed your ${visa.name.lowercase()} visa for $years more years."
                ) + character.eventLog
            )
        )
    }

    /**
     * Dual citizenship application after [NATURALIZATION_YEARS] abroad.
     * Requires a smarts-based citizenship test and legal fees.
     */
    fun applyForCitizenship(character: Character): CitizenshipApplicationResult {
        if (!canApplyForCitizenship(character)) return CitizenshipApplicationResult.NotEligible
        val fee = citizenshipFee(character)
        if (character.stats.money < fee) return CitizenshipApplicationResult.CannotAfford

        val afterFee = character.copy(
            stats = character.stats.copy(money = character.stats.money - fee)
        )
        val passChance = (0.35f + afterFee.stats.smarts / 100f * 0.55f).coerceIn(0.2f, 0.95f)
        if (Random.nextFloat() > passChance) {
            return CitizenshipApplicationResult.FailedTest(
                afterFee.copy(
                    eventLog = listOf(
                        "You failed the citizenship test. The fees were not refunded."
                    ) + afterFee.eventLog
                )
            )
        }

        val country = afterFee.countryCode
        val passports = (afterFee.passportsHeld() + country).distinct()
        return CitizenshipApplicationResult.Success(
            afterFee.copy(
                citizenships = passports,
                currentVisa = null,
                visaYearsRemaining = 0,
                eventLog = listOf(
                    "You passed the citizenship test and now hold a ${CountryCatalog.getCountry(country).displayName} passport."
                ) + afterFee.eventLog
            )
        )
    }

    /**
     * Yearly immigration tick: citizens clear visa state; non-citizens burn a visa year and may be deported.
     */
    fun tickImmigrationYear(character: Character): ImmigrationTickResult {
        if (character.holdsCitizenship(character.countryCode)) {
            if (character.currentVisa == null && character.visaYearsRemaining == 0) {
                return ImmigrationTickResult(character)
            }
            return ImmigrationTickResult(
                character.copy(currentVisa = null, visaYearsRemaining = 0)
            )
        }

        val yearsLeft = character.visaYearsRemaining - 1
        if (yearsLeft > 0) {
            return ImmigrationTickResult(
                character.copy(visaYearsRemaining = yearsLeft)
            )
        }

        val home = CountryCatalog.getCountry(character.birthCountryCode)
        val deported = deport(character, home)
        val message =
            "Your visa expired. Immigration deported you back to ${home.displayName}."
        return ImmigrationTickResult(
            character = deported.copy(
                eventLog = listOf(message) + deported.eventLog
            ),
            deported = true,
            deportationMessage = message
        )
    }

    private fun deport(character: Character, home: Country): Character {
        val formerJob = character.career.currentJob
        val updatedCareer = if (formerJob != null) {
            character.career.copy(
                currentJob = null,
                yearsAtCurrentJob = 0,
                jobHistory = character.career.jobHistory + formerJob.title
            )
        } else {
            character.career
        }
        return character.copy(
            countryCode = home.code,
            yearsInCurrentCountry = 0,
            lastRelocationAge = character.age,
            relocationCount = character.relocationCount + 1,
            relocationHistory = character.relocationHistory + home.code,
            currentVisa = null,
            visaYearsRemaining = 0,
            career = updatedCareer,
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness - DEPORTATION_HAPPINESS_DIP)
            )
        )
    }

    fun defaultVisaYears(visaType: VisaType): Int = when (visaType) {
        VisaType.TOURIST -> TOURIST_VISA_YEARS
        VisaType.STUDENT -> STUDENT_VISA_YEARS
        VisaType.WORK -> WORK_VISA_YEARS
    }

    companion object {
        const val RELOCATION_SYSTEM_TAG = "relocation_system"
        const val REQUIRES_RELOCATION_TAG = "requires_relocation"
        const val REQUIRES_EXPAT_TAG = "requires_expat"
        const val REQUIRES_NON_CITIZEN_TAG = "requires_non_citizen"
        const val RELOCATION_OPPORTUNITY_EVENT_ID = "relocation_opportunity_system"
        private const val ONE_TIME_TAG = "one_time"

        const val NATURALIZATION_YEARS = 5
        const val VISA_RENEWAL_FEE_KENYA = 50_000
        const val CITIZENSHIP_FEE_KENYA = 200_000

        private const val MIN_RELOCATION_AGE = 18
        private const val MAX_RELOCATION_AGE = 55
        private const val RELOCATION_DESTINATION_COUNT = 3
        private const val RELOCATION_OFFER_CHANCE = 0.035f
        private const val RELOCATION_HAPPINESS_DIP_MIN = 6
        private const val RELOCATION_HAPPINESS_DIP_MAX = 10
        private const val DEPORTATION_HAPPINESS_DIP = 15
        private const val MIN_YEARS_BETWEEN_RELOCATIONS = 4
        private const val TOURIST_VISA_YEARS = 1
        private const val STUDENT_VISA_YEARS = 4
        private const val WORK_VISA_YEARS = 3
    }
}
