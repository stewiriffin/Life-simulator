package com.maisha.game.domain

import com.maisha.game.data.model.Character
import com.maisha.game.data.model.SchoolStage
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Serializable
enum class YearQuestKind {
    RAISE_HAPPINESS,
    RAISE_HEALTH,
    EARN_MONEY,
    STUDY_SMARTS,
    BOND_FAMILY,
    STAY_OUT_OF_TROUBLE,
    GROW_FOLLOWERS,
    RAISE_SKILL,
    HOLD_JOB,
    GROW_SAVINGS
}

@Serializable
data class YearQuest(
    val kind: YearQuestKind,
    /** Absolute target delta or threshold depending on [kind]. */
    val target: Int,
    val titleResHint: String,
    val rewardKarma: Int = 3,
    /** Occasional cash reward (Kenya baseline; scaled by caller if needed). 0 = none. */
    val rewardCashKenya: Int = 0,
    /** Flat happiness bump on completion. */
    val rewardHappiness: Int = 0
)

data class YearQuestProgress(
    val quest: YearQuest,
    val current: Int,
    val completed: Boolean
) {
    val fraction: Float
        get() = if (quest.target <= 0) 1f else (current.toFloat() / quest.target).coerceIn(0f, 1f)
}

/**
 * Soft in-life goals that refresh each year. Pure Kotlin — no Android APIs.
 * Snapshot [before] at year start; evaluate against [after] at year end.
 */
@Singleton
class YearQuestEngine @Inject constructor() {

    fun generate(character: Character, random: Random = Random.Default): List<YearQuest> {
        if (!character.alive || character.age < MIN_SOFT_QUEST_AGE) return emptyList()
        val softOnly = character.age < MIN_QUEST_AGE
        val pool = mutableListOf<YearQuest>()
        val childTargets = character.age < 14

        if (character.stats.happiness < 90) {
            pool += YearQuest(
                kind = YearQuestKind.RAISE_HAPPINESS,
                target = if (childTargets) 5 else 8,
                titleResHint = "year_quest_raise_happiness",
                rewardKarma = 3
            )
        }
        if (character.stats.health < 90) {
            pool += YearQuest(
                kind = YearQuestKind.RAISE_HEALTH,
                target = if (childTargets) 4 else 6,
                titleResHint = "year_quest_raise_health",
                rewardKarma = 3
            )
        }
        if (!softOnly) {
            if (character.age >= 16) {
                pool += YearQuest(
                    kind = YearQuestKind.EARN_MONEY,
                    target = moneyTarget(character),
                    titleResHint = "year_quest_earn_money",
                    rewardKarma = 2
                )
            }
            if (character.education.stage == SchoolStage.PRIMARY ||
                character.education.stage == SchoolStage.SECONDARY ||
                character.education.stage == SchoolStage.UNIVERSITY
            ) {
                pool += YearQuest(
                    kind = YearQuestKind.STUDY_SMARTS,
                    target = 5,
                    titleResHint = "year_quest_study_smarts",
                    rewardKarma = 3
                )
            }
            if (character.family.any { it.alive }) {
                pool += YearQuest(
                    kind = YearQuestKind.BOND_FAMILY,
                    target = 8,
                    titleResHint = "year_quest_bond_family",
                    rewardKarma = 4
                )
            }
            if (character.age >= 14) {
                pool += YearQuest(
                    kind = YearQuestKind.STAY_OUT_OF_TROUBLE,
                    target = 1,
                    titleResHint = "year_quest_stay_clean",
                    rewardKarma = 4
                )
            }
            if (character.socialMedia.hasAccount) {
                pool += YearQuest(
                    kind = YearQuestKind.GROW_FOLLOWERS,
                    target = 500,
                    titleResHint = "year_quest_grow_followers",
                    rewardKarma = 2
                )
            }
            if (character.age >= SkillEngine.MIN_SKILL_AGE &&
                (character.skills.isEmpty() ||
                    character.skills.any { it.level < SkillEngine.MAX_SKILL_LEVEL })
            ) {
                pool += YearQuest(
                    kind = YearQuestKind.RAISE_SKILL,
                    target = 10,
                    titleResHint = "year_quest_raise_skill",
                    rewardKarma = 3
                )
            }
            if (character.age >= 18 && character.career.currentJob != null) {
                pool += YearQuest(
                    kind = YearQuestKind.HOLD_JOB,
                    target = 1,
                    titleResHint = "year_quest_hold_job",
                    rewardKarma = 3,
                    rewardCashKenya = 5_000,
                    rewardHappiness = 2
                )
            }
            if (character.age >= 16) {
                pool += YearQuest(
                    kind = YearQuestKind.GROW_SAVINGS,
                    target = savingsTarget(character),
                    titleResHint = "year_quest_grow_savings",
                    rewardKarma = 2,
                    rewardCashKenya = 2_500
                )
            }
        }

        if (pool.isEmpty()) return emptyList()
        return if (softOnly) {
            pool.shuffled(random).take(SOFT_QUESTS_PER_YEAR)
        } else {
            pool.shuffled(random).take(QUESTS_PER_YEAR)
        }
    }

    fun evaluate(
        quests: List<YearQuest>,
        before: Character,
        after: Character
    ): List<YearQuestProgress> = quests.map { quest ->
        val current = progressToward(quest, before, after)
        YearQuestProgress(
            quest = quest,
            current = current,
            completed = current >= quest.target
        )
    }

    fun applyRewards(character: Character, completed: List<YearQuestProgress>): Character {
        if (completed.isEmpty()) return character
        val karmaGain = completed.sumOf { it.quest.rewardKarma }
        val cashGain = completed.sumOf { it.quest.rewardCashKenya }
        val happinessGain = completed.sumOf { it.quest.rewardHappiness }
        val scaledCash = if (cashGain > 0) {
            com.maisha.game.data.EconomyScaler.scaleAmount(cashGain, character.countryCode)
        } else {
            0
        }
        val labels = completed.joinToString(", ") { humanLabel(it.quest.kind) }
        return character.copy(
            stats = character.stats.copy(
                karma = (character.stats.karma + karmaGain).coerceIn(0, 100),
                money = character.stats.money + scaledCash,
                happiness = (character.stats.happiness + happinessGain).coerceIn(0, 100)
            ),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "Year quest complete: $labels. Karma +$karmaGain."
            )
        )
    }

    /**
     * Updates [Character.questYearStreak]: +1 when every active quest completed, else reset to 0.
     * Clean-year streak bonus karma is capped.
     */
    fun applyStreak(
        character: Character,
        quests: List<YearQuest>,
        progress: List<YearQuestProgress>
    ): Character {
        if (quests.isEmpty()) {
            return character.copy(questsCompletedThisYear = 0)
        }
        val completedCount = progress.count { it.completed }
        val cleanYear = completedCount == quests.size
        val nextStreak = if (cleanYear) character.questYearStreak + 1 else 0
        var updated = character.copy(
            questYearStreak = nextStreak,
            questsCompletedThisYear = completedCount
        )
        if (cleanYear && nextStreak > 0) {
            val bonus = nextStreak.coerceAtMost(STREAK_KARMA_CAP)
            updated = updated.copy(
                stats = updated.stats.copy(
                    karma = (updated.stats.karma + bonus).coerceIn(0, 100)
                ),
                eventLog = EventLogCap.prepend(
                    updated.eventLog,
                    "Quest streak: $nextStreak year${if (nextStreak == 1) "" else "s"}! Karma +$bonus."
                )
            )
        }
        return updated
    }

    private fun humanLabel(kind: YearQuestKind): String = when (kind) {
        YearQuestKind.RAISE_HAPPINESS -> "raise happiness"
        YearQuestKind.RAISE_HEALTH -> "raise health"
        YearQuestKind.EARN_MONEY -> "earn money"
        YearQuestKind.STUDY_SMARTS -> "study hard"
        YearQuestKind.BOND_FAMILY -> "bond with family"
        YearQuestKind.STAY_OUT_OF_TROUBLE -> "stay out of trouble"
        YearQuestKind.GROW_FOLLOWERS -> "grow followers"
        YearQuestKind.RAISE_SKILL -> "improve a skill"
        YearQuestKind.HOLD_JOB -> "keep your job"
        YearQuestKind.GROW_SAVINGS -> "grow savings"
    }

    private fun progressToward(quest: YearQuest, before: Character, after: Character): Int =
        when (quest.kind) {
            YearQuestKind.RAISE_HAPPINESS ->
                (after.stats.happiness - before.stats.happiness).coerceAtLeast(0)
            YearQuestKind.RAISE_HEALTH ->
                (after.stats.health - before.stats.health).coerceAtLeast(0)
            YearQuestKind.EARN_MONEY ->
                (after.stats.money - before.stats.money).coerceAtLeast(0)
            YearQuestKind.STUDY_SMARTS ->
                (after.stats.smarts - before.stats.smarts).coerceAtLeast(0)
            YearQuestKind.BOND_FAMILY -> {
                val beforeAvg = avgBond(before)
                val afterAvg = avgBond(after)
                ((afterAvg - beforeAvg) * 10f).toInt().coerceAtLeast(0)
            }
            YearQuestKind.STAY_OUT_OF_TROUBLE -> {
                val arrested = after.criminalRecord.timesArrested > before.criminalRecord.timesArrested
                val incarcerated = after.criminalRecord.currentlyIncarcerated
                if (arrested || incarcerated) 0 else 1
            }
            YearQuestKind.GROW_FOLLOWERS ->
                (after.socialMedia.followers - before.socialMedia.followers).coerceAtLeast(0)
            YearQuestKind.RAISE_SKILL -> {
                val beforeMax = before.skills.maxOfOrNull { it.level } ?: 0
                val afterSum = after.skills.sumOf { it.level }
                val beforeSum = before.skills.sumOf { it.level }
                (afterSum - beforeSum).coerceAtLeast(0).coerceAtLeast(
                    (after.skills.maxOfOrNull { it.level } ?: 0) - beforeMax
                )
            }
            YearQuestKind.HOLD_JOB -> {
                val keptSameJob = before.career.currentJob != null &&
                    after.career.currentJob?.id == before.career.currentJob?.id &&
                    !after.career.isRetired
                if (keptSameJob) 1 else 0
            }
            YearQuestKind.GROW_SAVINGS ->
                (after.savingsBalance - before.savingsBalance).coerceAtLeast(0)
        }

    private fun avgBond(character: Character): Float {
        val people = character.family.filter { it.alive }
        if (people.isEmpty()) return 0f
        return people.map { it.relationshipLevel }.average().toFloat()
    }

    private fun moneyTarget(character: Character): Int {
        val salary = character.career.currentJob?.baseSalary ?: 0
        val baseline = when {
            salary > 0 -> (salary * 0.35f).toInt()
            else -> 25_000
        }
        return baseline.coerceIn(10_000, 500_000)
    }

    private fun savingsTarget(character: Character): Int {
        val baseline = when {
            character.career.currentJob != null ->
                (character.career.currentJob!!.baseSalary * 0.1f).toInt()
            else -> 10_000
        }
        return baseline.coerceIn(5_000, 200_000)
    }

    companion object {
        const val MIN_SOFT_QUEST_AGE = 3
        const val MIN_QUEST_AGE = 6
        const val SOFT_QUESTS_PER_YEAR = 1
        const val QUESTS_PER_YEAR = 2
        const val STREAK_KARMA_CAP = 5
    }
}
