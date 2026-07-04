// app/src/main/java/com/maisha/game/domain/PoliticsEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.PoliticalOffice
import com.maisha.game.data.model.PoliticalState
import com.maisha.game.util.clampStat
import com.maisha.game.util.formatMoney
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

sealed class CampaignResult {
    data class Won(val character: Character) : CampaignResult()
    data class Lost(val character: Character) : CampaignResult()
    data class Failed(val reason: CampaignFailure) : CampaignResult()
}

enum class CampaignFailure {
    INELIGIBLE,
    ALREADY_IN_OFFICE,
    INSUFFICIENT_FUNDS,
    ALREADY_CAMPAIGNED,
    INVALID_INVESTMENT
}

data class GovernanceTickResult(
    val character: Character,
    val impeachmentEvent: LifeEvent? = null
)

@Singleton
class PoliticsEngine @Inject constructor(
    private val financeEngine: FinanceEngine
) {

    fun holdsOffice(character: Character): Boolean =
        character.politics.currentOffice != null

    fun canRunForOffice(character: Character, office: PoliticalOffice): Boolean {
        if (!character.alive) return false
        if (character.age < MIN_OFFICE_AGE) return false
        if (character.criminalRecord.hasRecord) return false
        if (character.criminalRecord.currentlyIncarcerated) return false
        if (!character.holdsCitizenship(character.countryCode)) return false
        if (character.politics.currentOffice != null) return false
        if (character.politics.campaignedThisYear) return false
        if (character.stats.smarts < minSmartsFor(office)) return false
        val netWorth = financeEngine.calculateNetWorth(character)
        if (netWorth < minNetWorthFor(office, character.countryCode)) return false
        return true
    }

    fun minCampaignInvestment(office: PoliticalOffice, countryCode: String): Int =
        EconomyScaler.scaleAmount(baseInvestmentFor(office), countryCode)

    /**
     * Win probability in 0f..1f from smarts, looks, campaign investment, and social reach.
     * Exposed for tests and UI previews.
     */
    fun campaignWinChance(
        character: Character,
        office: PoliticalOffice,
        investment: Int
    ): Float {
        val minInvest = minCampaignInvestment(office, character.countryCode).coerceAtLeast(1)
        val investmentFactor = (investment.toFloat() / minInvest).coerceIn(0.25f, 3f) * 0.22f
        val smartsFactor = character.stats.smarts / 100f * 0.28f
        val looksFactor = character.stats.looks / 100f * 0.18f
        val followers = character.socialMedia.followers.coerceAtLeast(0)
        val socialFactor = (followers / 50_000f).coerceIn(0f, 1f) * 0.18f
        val officeDifficulty = when (office) {
            PoliticalOffice.MAYOR -> 0.12f
            PoliticalOffice.GOVERNOR -> 0.06f
            PoliticalOffice.PRESIDENT -> 0f
        }
        return (0.12f + investmentFactor + smartsFactor + looksFactor + socialFactor + officeDifficulty)
            .coerceIn(0.05f, 0.92f)
    }

    /**
     * Spends [investment] on a campaign for [office]. Success uses [campaignWinChance].
     */
    fun launchCampaign(
        character: Character,
        office: PoliticalOffice,
        investment: Int
    ): CampaignResult {
        if (character.politics.currentOffice != null) {
            return CampaignResult.Failed(CampaignFailure.ALREADY_IN_OFFICE)
        }
        if (character.politics.campaignedThisYear) {
            return CampaignResult.Failed(CampaignFailure.ALREADY_CAMPAIGNED)
        }
        if (!canRunForOffice(character, office)) {
            return CampaignResult.Failed(CampaignFailure.INELIGIBLE)
        }
        val minInvest = minCampaignInvestment(office, character.countryCode)
        if (investment < minInvest) {
            return CampaignResult.Failed(CampaignFailure.INVALID_INVESTMENT)
        }
        if (character.stats.money < investment) {
            return CampaignResult.Failed(CampaignFailure.INSUFFICIENT_FUNDS)
        }

        val afterSpend = character.copy(
            stats = character.stats.copy(money = character.stats.money - investment),
            politics = character.politics.copy(
                campaignFunds = character.politics.campaignFunds + investment,
                campaignedThisYear = true
            )
        )

        val chance = campaignWinChance(afterSpend, office, investment)
        val won = Random.nextFloat() < chance
        val officeLabel = officeLabel(office)
        return if (won) {
            val elected = afterSpend.copy(
                politics = PoliticalState(
                    currentOffice = office,
                    approvalRating = STARTING_APPROVAL,
                    campaignFunds = 0,
                    activeTaxPolicy = null,
                    yearsInOffice = 0,
                    campaignedThisYear = true
                ),
                stats = afterSpend.stats.copy(
                    happiness = clampStat(afterSpend.stats.happiness + 8)
                ),
                eventLog = EventLogCap.prepend(
                    afterSpend.eventLog,
                    "You won the race for $officeLabel after spending " +
                        "${formatMoney(investment, afterSpend.countryCode)} on the campaign."
                )
            )
            CampaignResult.Won(elected)
        } else {
            val lost = afterSpend.copy(
                politics = afterSpend.politics.copy(campaignFunds = 0),
                stats = afterSpend.stats.copy(
                    happiness = clampStat(afterSpend.stats.happiness - 6)
                ),
                eventLog = EventLogCap.prepend(
                    afterSpend.eventLog,
                    "You lost the race for $officeLabel despite spending " +
                        "${formatMoney(investment, afterSpend.countryCode)}."
                )
            )
            CampaignResult.Lost(lost)
        }
    }

    /**
     * Yearly approval drift for officeholders. Approval below [IMPEACHMENT_THRESHOLD] forces resignation.
     */
    fun tickGovernance(character: Character): GovernanceTickResult {
        val office = character.politics.currentOffice
            ?: return GovernanceTickResult(
                character.copy(
                    politics = character.politics.copy(campaignedThisYear = false)
                )
            )

        val drift = Random.nextInt(APPROVAL_DRIFT_MIN, APPROVAL_DRIFT_MAX + 1)
        val newApproval = (character.politics.approvalRating + drift).coerceIn(0, 100)
        val yearsInOffice = character.politics.yearsInOffice + 1
        var updated = character.copy(
            politics = character.politics.copy(
                approvalRating = newApproval,
                yearsInOffice = yearsInOffice,
                campaignedThisYear = false
            )
        )

        if (newApproval < IMPEACHMENT_THRESHOLD) {
            val event = buildImpeachmentEvent(updated, office)
            updated = resignFromOffice(
                updated,
                reason = "Public approval collapsed to $newApproval%. You resigned under pressure."
            )
            return GovernanceTickResult(updated, impeachmentEvent = event)
        }

        return GovernanceTickResult(updated)
    }

    fun resignFromOffice(character: Character, reason: String): Character {
        if (character.politics.currentOffice == null) return character
        return character.copy(
            politics = PoliticalState(campaignedThisYear = character.politics.campaignedThisYear),
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness - 10)
            ),
            eventLog = EventLogCap.prepend(character.eventLog, reason)
        )
    }

    fun buildImpeachmentEvent(character: Character, office: PoliticalOffice): LifeEvent {
        val label = officeLabel(office)
        return LifeEvent(
            id = "politics_impeachment_${character.age}",
            minAge = character.age,
            maxAge = character.age,
            text = "Your approval as $label has collapsed. Allies abandon you; " +
                "impeachment hearings dominate the news.",
            choices = listOf(
                EventChoice(
                    label = "Resign with dignity",
                    statEffects = mapOf("happiness" to -4),
                    resultText = "You step down. History will argue about your legacy."
                ),
                EventChoice(
                    label = "Fight to the end",
                    statEffects = mapOf("happiness" to -8, "smarts" to 1),
                    resultText = "You lose anyway — and burn what goodwill remained."
                )
            ),
            tags = listOf(POLITICS_SYSTEM_TAG, REQUIRES_OFFICE_TAG),
            weight = 1
        )
    }

    fun officeLabel(office: PoliticalOffice): String = when (office) {
        PoliticalOffice.MAYOR -> "Mayor"
        PoliticalOffice.GOVERNOR -> "Governor"
        PoliticalOffice.PRESIDENT -> "President"
    }

    private fun minSmartsFor(office: PoliticalOffice): Int = when (office) {
        PoliticalOffice.MAYOR -> 55
        PoliticalOffice.GOVERNOR -> 65
        PoliticalOffice.PRESIDENT -> 75
    }

    private fun minNetWorthFor(office: PoliticalOffice, countryCode: String): Int =
        EconomyScaler.scaleAmount(
            when (office) {
                PoliticalOffice.MAYOR -> 200_000
                PoliticalOffice.GOVERNOR -> 800_000
                PoliticalOffice.PRESIDENT -> 2_000_000
            },
            countryCode
        )

    private fun baseInvestmentFor(office: PoliticalOffice): Int = when (office) {
        PoliticalOffice.MAYOR -> 50_000
        PoliticalOffice.GOVERNOR -> 200_000
        PoliticalOffice.PRESIDENT -> 500_000
    }

    companion object {
        const val REQUIRES_OFFICE_TAG = "requires_office"
        const val POLITICS_SYSTEM_TAG = "politics_system"
        const val POLITICS_TAG = "politics"
        const val MIN_OFFICE_AGE = 30
        const val IMPEACHMENT_THRESHOLD = 20
        const val STARTING_APPROVAL = 55
        private const val APPROVAL_DRIFT_MIN = -8
        private const val APPROVAL_DRIFT_MAX = 8
    }
}
