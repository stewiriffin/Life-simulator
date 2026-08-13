package com.maisha.game.domain

import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.RelationType
import javax.inject.Inject
import javax.inject.Singleton

data class LifeMilestoneDef(
    val id: String,
    val titleResHint: String,
    val celebration: Boolean = true
)

data class MilestoneUnlock(
    val id: String,
    val titleResHint: String
)

@Singleton
class MilestoneEngine @Inject constructor() {

    fun checkNewUnlocks(before: Character, after: Character, netWorthAfter: Int): List<MilestoneUnlock> {
        val already = after.unlockedMilestoneIds.toSet()
        val newly = mutableListOf<MilestoneUnlock>()

        fun unlock(id: String, titleHint: String, condition: Boolean) {
            if (condition && id !in already && newly.none { it.id == id }) {
                newly += MilestoneUnlock(id, titleHint)
            }
        }

        unlock(ID_AGE_18, "milestone_age_18", after.age >= 18 && before.age < 18)
        unlock(ID_AGE_50, "milestone_age_50", after.age >= 50 && before.age < 50)
        unlock(ID_AGE_75, "milestone_age_75", after.age >= 75 && before.age < 75)
        unlock(ID_AGE_100, "milestone_age_100", after.age >= 100 && before.age < 100)

        unlock(
            ID_FIRST_JOB,
            "milestone_first_job",
            after.career.currentJob != null && before.career.currentJob == null &&
                before.career.jobHistory.isEmpty()
        )
        unlock(
            ID_DRIVING,
            "milestone_driving_license",
            after.hasDrivingLicense && !before.hasDrivingLicense
        )
        unlock(
            ID_MARRIAGE,
            "milestone_marriage",
            after.family.any { it.relation == RelationType.SPOUSE && it.alive } &&
                before.family.none { it.relation == RelationType.SPOUSE && it.alive }
        )
        unlock(
            ID_FIRST_CHILD,
            "milestone_first_child",
            after.family.count { it.relation == RelationType.CHILD } >
                before.family.count { it.relation == RelationType.CHILD } &&
                before.family.none { it.relation == RelationType.CHILD }
        )
        unlock(
            ID_FIRST_HOME,
            "milestone_first_home",
            after.assets.any { it.type == AssetType.HOUSE } &&
                before.assets.none { it.type == AssetType.HOUSE }
        )
        unlock(
            ID_FIRST_BUSINESS,
            "milestone_first_business",
            after.businesses.isNotEmpty() && before.businesses.isEmpty()
        )
        unlock(
            ID_ELECTED,
            "milestone_elected",
            after.politics.currentOffice != null && before.politics.currentOffice == null
        )
        unlock(
            ID_VERIFIED,
            "milestone_verified",
            after.socialMedia.isVerified && !before.socialMedia.isVerified
        )
        unlock(
            ID_GRADUATED,
            "milestone_graduated",
            after.education.stage == com.maisha.game.data.model.SchoolStage.GRADUATED &&
                before.education.stage != com.maisha.game.data.model.SchoolStage.GRADUATED
        )

        val wealthTarget = EconomyScaler.scaleAmount(WEALTH_MILESTONE_KENYA, after.countryCode)
        unlock(
            ID_WEALTHY,
            "milestone_wealthy",
            netWorthAfter >= wealthTarget &&
                EconomyScaler.scaleAmount(WEALTH_MILESTONE_KENYA, before.countryCode)
                    .let { /* compare using after threshold only */ true } &&
                // only if newly crossed: before net worth unknown here — use money+assets approx
                (before.stats.money + before.assets.sumOf { it.currentValue } +
                    before.investmentPortfolioValue + before.savingsBalance) < wealthTarget
        )

        return newly
    }

    fun applyUnlocks(character: Character, unlocks: List<MilestoneUnlock>): Character {
        if (unlocks.isEmpty()) return character
        var updated = character
        val ids = character.unlockedMilestoneIds.toMutableList()
        unlocks.forEach { unlock ->
            if (unlock.id !in ids) {
                ids += unlock.id
                updated = updated.copy(
                    eventLog = EventLogCap.prepend(
                        updated.eventLog,
                        "Life milestone: ${humanTitle(unlock.id)}."
                    )
                )
            }
        }
        return updated.copy(unlockedMilestoneIds = ids)
    }

    fun recentMilestones(character: Character, limit: Int = 4): List<String> =
        character.unlockedMilestoneIds.takeLast(limit).reversed()

    private fun humanTitle(id: String): String = when (id) {
        ID_AGE_18 -> "Came of age"
        ID_AGE_50 -> "Half a century"
        ID_AGE_75 -> "Diamond years"
        ID_AGE_100 -> "Centenarian"
        ID_FIRST_JOB -> "First job"
        ID_DRIVING -> "Earned a driving license"
        ID_MARRIAGE -> "Got married"
        ID_FIRST_CHILD -> "Became a parent"
        ID_FIRST_HOME -> "Bought a home"
        ID_FIRST_BUSINESS -> "Started a business"
        ID_ELECTED -> "Won public office"
        ID_VERIFIED -> "Went verified online"
        ID_GRADUATED -> "Graduated"
        ID_WEALTHY -> "Built serious wealth"
        else -> id
    }

    companion object {
        const val ID_AGE_18 = "age_18"
        const val ID_AGE_50 = "age_50"
        const val ID_AGE_75 = "age_75"
        const val ID_AGE_100 = "age_100"
        const val ID_FIRST_JOB = "first_job"
        const val ID_DRIVING = "driving_license"
        const val ID_MARRIAGE = "marriage"
        const val ID_FIRST_CHILD = "first_child"
        const val ID_FIRST_HOME = "first_home"
        const val ID_FIRST_BUSINESS = "first_business"
        const val ID_ELECTED = "elected"
        const val ID_VERIFIED = "verified_social"
        const val ID_GRADUATED = "graduated"
        const val ID_WEALTHY = "wealthy"
        private const val WEALTH_MILESTONE_KENYA = 5_000_000

        fun titleResHint(id: String): String = when (id) {
            ID_AGE_18 -> "milestone_age_18"
            ID_AGE_50 -> "milestone_age_50"
            ID_AGE_75 -> "milestone_age_75"
            ID_AGE_100 -> "milestone_age_100"
            ID_FIRST_JOB -> "milestone_first_job"
            ID_DRIVING -> "milestone_driving_license"
            ID_MARRIAGE -> "milestone_marriage"
            ID_FIRST_CHILD -> "milestone_first_child"
            ID_FIRST_HOME -> "milestone_first_home"
            ID_FIRST_BUSINESS -> "milestone_first_business"
            ID_ELECTED -> "milestone_elected"
            ID_VERIFIED -> "milestone_verified"
            ID_GRADUATED -> "milestone_graduated"
            ID_WEALTHY -> "milestone_wealthy"
            else -> "milestone_generic"
        }
    }
}
