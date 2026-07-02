// app/src/main/java/com/maisha/game/data/FlavorInterpolator.kt (new)
package com.maisha.game.data

import com.maisha.game.data.model.CountryFlavor
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

    val knownPlaceholders: Set<String> = setOf(
        PLACEHOLDER_TRANSPORT,
        PLACEHOLDER_MONEY_APP,
        PLACEHOLDER_PRIMARY_EXAM,
        PLACEHOLDER_SECONDARY_EXAM,
        PLACEHOLDER_GREETING
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
        )
    )

    fun containsPlaceholders(text: String): Boolean =
        text.contains('{') && knownPlaceholders.any { placeholder ->
            "{$placeholder}" in text
        }

    fun interpolate(text: String, flavor: CountryFlavor): String {
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
        return resolved
    }

    fun resolveEvent(event: LifeEvent, countryCode: String): LifeEvent {
        val flavor = CountryCatalog.flavorFor(countryCode)
        val needsText = containsPlaceholders(event.text)
        val needsChoices = event.choices.any { choice ->
            containsPlaceholders(choice.label) || containsPlaceholders(choice.resultText)
        }
        if (!needsText && !needsChoices) return event

        return event.copy(
            text = interpolate(event.text, flavor),
            choices = event.choices.map { choice ->
                choice.copy(
                    label = interpolate(choice.label, flavor),
                    resultText = interpolate(choice.resultText, flavor)
                )
            }
        )
    }
}
