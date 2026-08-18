package com.maisha.game.domain

import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.CareerTrack
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.SchoolStage
import javax.inject.Inject
import javax.inject.Singleton

/** Real-world weekly challenge template (BitLife-style ribbon goals). */
data class WeeklyChallenge(
    val id: String,
    val titleKey: String,
    val descriptionKey: String,
    val ribbonKey: String
)

/**
 * Rotating weekly goals stored app-wide via [com.maisha.game.data.local.SettingsRepository].
 * Completion is evaluated against the active life in the current slot.
 */
@Singleton
class WeeklyChallengeEngine @Inject constructor() {

    fun currentChallenge(nowMillis: Long = System.currentTimeMillis()): WeeklyChallenge {
        val weekIndex = ((nowMillis / MILLIS_PER_WEEK).toInt() % CHALLENGES.size + CHALLENGES.size) % CHALLENGES.size
        return CHALLENGES[weekIndex]
    }

    fun isComplete(character: Character, challenge: WeeklyChallenge): Boolean =
        when (challenge.id) {
            "medical_residency" -> character.career.careerTrack == CareerTrack.MEDICAL &&
                character.career.trackLevel >= 2
            "legal_partner" -> character.career.careerTrack == CareerTrack.LEGAL &&
                character.career.trackLevel >= 2
            "adoptive_parent" -> character.family.any {
                it.relation == RelationType.CHILD && it.isAdopted
            }
            "home_improver" -> character.assets.any {
                it.type == AssetType.HOUSE && it.renovationLevel >= 1
            }
            "clean_slate" -> character.criminalRecord.recordExpunged
            "scholar" -> character.education.stage == SchoolStage.GRADUATED &&
                character.education.gpa >= 3.5f
            "teen_hustler" -> character.career.partTimeWorkedThisYear ||
                (character.age in 14..17 && character.stats.money >= 5_000)
            "portfolio_pro" -> character.investmentPortfolioValue >= 100_000
            else -> false
        }

    companion object {
        private const val MILLIS_PER_WEEK = 7L * 24L * 60L * 60L * 1000L

        val CHALLENGES: List<WeeklyChallenge> = listOf(
            WeeklyChallenge(
                id = "medical_residency",
                titleKey = "challenge_medical_title",
                descriptionKey = "challenge_medical_desc",
                ribbonKey = "ribbon_healer"
            ),
            WeeklyChallenge(
                id = "legal_partner",
                titleKey = "challenge_legal_title",
                descriptionKey = "challenge_legal_desc",
                ribbonKey = "ribbon_counsel"
            ),
            WeeklyChallenge(
                id = "adoptive_parent",
                titleKey = "challenge_adopt_title",
                descriptionKey = "challenge_adopt_desc",
                ribbonKey = "ribbon_guardian"
            ),
            WeeklyChallenge(
                id = "home_improver",
                titleKey = "challenge_renovate_title",
                descriptionKey = "challenge_renovate_desc",
                ribbonKey = "ribbon_architect"
            ),
            WeeklyChallenge(
                id = "clean_slate",
                titleKey = "challenge_expunge_title",
                descriptionKey = "challenge_expunge_desc",
                ribbonKey = "ribbon_redemption"
            ),
            WeeklyChallenge(
                id = "scholar",
                titleKey = "challenge_scholar_title",
                descriptionKey = "challenge_scholar_desc",
                ribbonKey = "ribbon_valedictorian"
            ),
            WeeklyChallenge(
                id = "teen_hustler",
                titleKey = "challenge_teen_job_title",
                descriptionKey = "challenge_teen_job_desc",
                ribbonKey = "ribbon_hustler"
            ),
            WeeklyChallenge(
                id = "portfolio_pro",
                titleKey = "challenge_portfolio_title",
                descriptionKey = "challenge_portfolio_desc",
                ribbonKey = "ribbon_investor"
            )
        )
    }
}
