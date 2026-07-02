// app/src/main/java/com/maisha/game/data/FlavorInterpolator.kt (modified — holiday placeholders)
package com.maisha.game.data

import com.maisha.game.data.model.CountryFlavor
import com.maisha.game.data.model.HolidayFlavor
import com.maisha.game.data.model.LifeEvent

/**
 * Optional template metadata for events that use country-flavor placeholders.
 * Placeholders are resolved at runtime via [FlavorInterpolator].
 */
data class FlavorTemplate(
    val id: String,
    val genericText: String,
    val placeholders: List<String>
)

object FlavorInterpolator {

    const val PLACEHOLDER_TRANSPORT = "transportMode"
    const val PLACEHOLDER_MONEY_APP = "moneyApp"
    const val PLACEHOLDER_PRIMARY_EXAM = "primaryExam"
    const val PLACEHOLDER_SECONDARY_EXAM = "secondaryExam"
    const val PLACEHOLDER_GREETING = "greeting"
    const val PLACEHOLDER_HOLIDAY_NAME = "holidayName"
    const val PLACEHOLDER_HOLIDAY_DESCRIPTION = "holidayDescription"

    val knownPlaceholders: Set<String> = setOf(
        PLACEHOLDER_TRANSPORT,
        PLACEHOLDER_MONEY_APP,
        PLACEHOLDER_PRIMARY_EXAM,
        PLACEHOLDER_SECONDARY_EXAM,
        PLACEHOLDER_GREETING,
        PLACEHOLDER_HOLIDAY_NAME,
        PLACEHOLDER_HOLIDAY_DESCRIPTION
    )

    /** Events authored with flavor placeholders (documentation + validation aid). */
    val templateRegistry: List<FlavorTemplate> = listOf(
        FlavorTemplate(
            id = "school_commute",
            genericText = "You ride a {transportMode} to school for the first time.",
            placeholders = listOf(PLACEHOLDER_TRANSPORT)
        ),
        FlavorTemplate(
            id = "secondary_school_choice",
            genericText = "After your {secondaryExam} results, your family debates secondary school options.",
            placeholders = listOf(PLACEHOLDER_SECONDARY_EXAM)
        ),
        FlavorTemplate(
            id = "exam_season",
            genericText = "{secondaryExam} season looms. The revision timetable is pinned to the classroom wall.",
            placeholders = listOf(PLACEHOLDER_SECONDARY_EXAM)
        ),
        FlavorTemplate(
            id = "phone_credit_hustle",
            genericText = "Sell {moneyApp} credit to classmates",
            placeholders = listOf(PLACEHOLDER_MONEY_APP)
        ),
        FlavorTemplate(
            id = "corner_shop_credit",
            genericText = "A small shop selling {moneyApp} credit and snacks near your neighbourhood.",
            placeholders = listOf(PLACEHOLDER_MONEY_APP)
        ),
        FlavorTemplate(
            id = "rainy_commute",
            genericText = "Heavy rains flood the path to school. You miss the {transportMode}.",
            placeholders = listOf(PLACEHOLDER_TRANSPORT)
        ),
        FlavorTemplate(
            id = "school_fees_transfer",
            genericText = "School fees sent through {moneyApp}.",
            placeholders = listOf(PLACEHOLDER_MONEY_APP)
        ),
        FlavorTemplate(
            id = "transport_route_investment",
            genericText = "Invest in a {transportMode} route.",
            placeholders = listOf(PLACEHOLDER_TRANSPORT)
        ),
        FlavorTemplate(
            id = "national_holiday",
            genericText = "{holidayDescription} Families gather to mark {holidayName}.",
            placeholders = listOf(PLACEHOLDER_HOLIDAY_NAME, PLACEHOLDER_HOLIDAY_DESCRIPTION)
        )
    )

    fun containsPlaceholders(text: String): Boolean =
        text.contains('{') && knownPlaceholders.any { placeholder ->
            "{$placeholder}" in text
        }

    fun interpolate(text: String, flavor: CountryFlavor, holiday: HolidayFlavor? = null): String {
        if (!text.contains('{')) return text
        var resolved = text
        resolved = resolved.replace("{${PLACEHOLDER_TRANSPORT}}", flavor.commonTransportMode)
        resolved = resolved.replace("{${PLACEHOLDER_PRIMARY_EXAM}}", flavor.primaryExamName)
        resolved = resolved.replace("{${PLACEHOLDER_SECONDARY_EXAM}}", flavor.secondaryExamName)
        resolved = resolved.replace(
            "{${PLACEHOLDER_MONEY_APP}}",
            flavor.popularMoneyAppOrBank ?: "mobile banking"
        )
        resolved = resolved.replace(
            "{${PLACEHOLDER_GREETING}}",
            flavor.greetingPhrase ?: "Hello"
        )
        holiday?.let {
            resolved = resolved.replace("{${PLACEHOLDER_HOLIDAY_NAME}}", it.name)
            resolved = resolved.replace("{${PLACEHOLDER_HOLIDAY_DESCRIPTION}}", it.approxAgeRelevantDescription)
        }
        return resolved
    }

    fun resolveEvent(event: LifeEvent, countryCode: String): LifeEvent {
        val flavor = CountryCatalog.flavorFor(countryCode)
        val holiday = pickHolidayForEvent(event, flavor)
        return resolveEventWithFlavor(event, flavor, holiday)
    }

    fun resolveHolidayEvent(event: LifeEvent, countryCode: String): LifeEvent? {
        val flavor = CountryCatalog.flavorFor(countryCode)
        val holiday = flavor.notableHolidays.randomOrNull() ?: return null
        return resolveEventWithFlavor(event, flavor, holiday)
    }

    private fun pickHolidayForEvent(event: LifeEvent, flavor: CountryFlavor): HolidayFlavor? {
        if (HOLIDAY_TAG !in event.tags) return null
        return flavor.notableHolidays.randomOrNull()
    }

    private fun resolveEventWithFlavor(
        event: LifeEvent,
        flavor: CountryFlavor,
        holiday: HolidayFlavor?
    ): LifeEvent {
        val needsText = containsPlaceholders(event.text)
        val needsChoices = event.choices.any { choice ->
            containsPlaceholders(choice.label) || containsPlaceholders(choice.resultText)
        }
        if (!needsText && !needsChoices) return event

        return event.copy(
            text = interpolate(event.text, flavor, holiday),
            choices = event.choices.map { choice ->
                choice.copy(
                    label = interpolate(choice.label, flavor, holiday),
                    resultText = interpolate(choice.resultText, flavor, holiday)
                )
            }
        )
    }

    const val HOLIDAY_TAG = "holiday"
}
