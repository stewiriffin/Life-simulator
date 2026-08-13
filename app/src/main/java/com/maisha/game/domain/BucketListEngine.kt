package com.maisha.game.domain

import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.BucketGoal
import com.maisha.game.data.model.BucketGoalKind
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.FameTier
import com.maisha.game.data.model.RelationType
import com.maisha.game.util.clampStat
import com.maisha.game.util.formatMoney
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class BucketTemplate(
    val id: String,
    val kind: BucketGoalKind,
    val titleResHint: String,
    val descriptionResHint: String,
    /** Kenya-baseline commitment deposit; 0 = free to adopt. */
    val commitmentKenya: Int,
    val defaultTarget: Int = 0
)

sealed class BucketAdoptResult {
    data class Success(val character: Character) : BucketAdoptResult()
    data object Full : BucketAdoptResult()
    data object InsufficientFunds : BucketAdoptResult()
    data object AlreadyTracking : BucketAdoptResult()
    data object Ineligible : BucketAdoptResult()
}

@Singleton
class BucketListEngine @Inject constructor() {

    fun templates(): List<BucketTemplate> = TEMPLATES

    fun availableTemplates(character: Character): List<BucketTemplate> {
        val activeKinds = character.bucketList.filter { !it.completed }.map { it.kind }.toSet()
        return TEMPLATES.filter { it.kind !in activeKinds && isEligible(character, it) }
    }

    fun adopt(
        character: Character,
        templateId: String
    ): BucketAdoptResult {
        if (!character.alive) return BucketAdoptResult.Ineligible
        val active = character.bucketList.count { !it.completed }
        if (active >= MAX_ACTIVE) return BucketAdoptResult.Full
        val template = TEMPLATES.find { it.id == templateId }
            ?: return BucketAdoptResult.Ineligible
        if (character.bucketList.any { !it.completed && it.kind == template.kind }) {
            return BucketAdoptResult.AlreadyTracking
        }
        if (!isEligible(character, template)) return BucketAdoptResult.Ineligible

        val cost = EconomyScaler.scaleAmount(template.commitmentKenya, character.countryCode)
        if (character.stats.money < cost) return BucketAdoptResult.InsufficientFunds

        val target = when (template.kind) {
            BucketGoalKind.HIT_WEALTH ->
                EconomyScaler.scaleAmount(template.defaultTarget, character.countryCode)
            BucketGoalKind.REACH_FAME -> FameTier.NATIONAL.ordinal
            BucketGoalKind.MASTER_SKILL -> SkillEngine.TIER_MASTER_MIN
            else -> template.defaultTarget
        }
        val goal = BucketGoal(
            id = UUID.randomUUID().toString(),
            kind = template.kind,
            templateId = template.id,
            targetValue = target,
            commitmentPaid = cost
        )
        return BucketAdoptResult.Success(
            character.copy(
                stats = character.stats.copy(money = character.stats.money - cost),
                bucketList = character.bucketList + goal,
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    if (cost > 0) {
                        "Committed to a bucket-list goal (${formatMoney(cost, character.countryCode)} deposit)."
                    } else {
                        "Added a bucket-list goal."
                    }
                )
            )
        )
    }

    fun evaluate(character: Character, netWorth: Int): Character {
        var updated = character
        val revised = character.bucketList.map { goal ->
            if (goal.completed) return@map goal
            if (isComplete(updated, goal, netWorth)) {
                updated = reward(updated, goal)
                goal.copy(completed = true)
            } else {
                goal
            }
        }
        return updated.copy(bucketList = revised)
    }

    private fun reward(character: Character, goal: BucketGoal): Character {
        return character.copy(
            stats = character.stats.copy(
                karma = (character.stats.karma + REWARD_KARMA).coerceIn(0, 100),
                happiness = clampStat(character.stats.happiness + REWARD_HAPPINESS)
            ),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "Bucket list complete: ${kindLabel(goal.kind)}! Karma +$REWARD_KARMA."
            )
        )
    }

    private fun isComplete(character: Character, goal: BucketGoal, netWorth: Int): Boolean =
        when (goal.kind) {
            BucketGoalKind.OWN_HOME ->
                character.assets.any { it.type == AssetType.HOUSE }
            BucketGoalKind.REACH_FAME ->
                character.socialMedia.fameTier.ordinal >= goal.targetValue.coerceAtLeast(
                    FameTier.NATIONAL.ordinal
                )
            BucketGoalKind.START_BUSINESS ->
                character.businesses.isNotEmpty()
            BucketGoalKind.WIN_OFFICE ->
                character.politics.currentOffice != null
            BucketGoalKind.RAISE_CHILD ->
                character.family.any { it.relation == RelationType.CHILD && it.alive }
            BucketGoalKind.HIT_WEALTH ->
                netWorth >= goal.targetValue
            BucketGoalKind.MASTER_SKILL ->
                character.skills.any { it.level >= SkillEngine.TIER_MASTER_MIN }
        }

    private fun isEligible(character: Character, template: BucketTemplate): Boolean =
        when (template.kind) {
            BucketGoalKind.OWN_HOME -> character.age >= 18
            BucketGoalKind.REACH_FAME -> character.age >= 14
            BucketGoalKind.START_BUSINESS -> character.age >= 18
            BucketGoalKind.WIN_OFFICE -> character.age >= 25
            BucketGoalKind.RAISE_CHILD -> character.age >= 18
            BucketGoalKind.HIT_WEALTH -> character.age >= 18
            BucketGoalKind.MASTER_SKILL -> character.age >= SkillEngine.MIN_SKILL_AGE
        }

    private fun kindLabel(kind: BucketGoalKind): String = when (kind) {
        BucketGoalKind.OWN_HOME -> "own a home"
        BucketGoalKind.REACH_FAME -> "reach national fame"
        BucketGoalKind.START_BUSINESS -> "start a business"
        BucketGoalKind.WIN_OFFICE -> "win public office"
        BucketGoalKind.RAISE_CHILD -> "raise a child"
        BucketGoalKind.HIT_WEALTH -> "hit a wealth target"
        BucketGoalKind.MASTER_SKILL -> "master a skill"
    }

    companion object {
        const val MAX_ACTIVE = 3
        private const val REWARD_KARMA = 5
        private const val REWARD_HAPPINESS = 6

        val TEMPLATES: List<BucketTemplate> = listOf(
            BucketTemplate(
                id = "own_home",
                kind = BucketGoalKind.OWN_HOME,
                titleResHint = "bucket_own_home_title",
                descriptionResHint = "bucket_own_home_desc",
                commitmentKenya = 5_000
            ),
            BucketTemplate(
                id = "reach_fame",
                kind = BucketGoalKind.REACH_FAME,
                titleResHint = "bucket_reach_fame_title",
                descriptionResHint = "bucket_reach_fame_desc",
                commitmentKenya = 2_000
            ),
            BucketTemplate(
                id = "start_business",
                kind = BucketGoalKind.START_BUSINESS,
                titleResHint = "bucket_start_business_title",
                descriptionResHint = "bucket_start_business_desc",
                commitmentKenya = 8_000
            ),
            BucketTemplate(
                id = "win_office",
                kind = BucketGoalKind.WIN_OFFICE,
                titleResHint = "bucket_win_office_title",
                descriptionResHint = "bucket_win_office_desc",
                commitmentKenya = 10_000
            ),
            BucketTemplate(
                id = "raise_child",
                kind = BucketGoalKind.RAISE_CHILD,
                titleResHint = "bucket_raise_child_title",
                descriptionResHint = "bucket_raise_child_desc",
                commitmentKenya = 0
            ),
            BucketTemplate(
                id = "hit_wealth",
                kind = BucketGoalKind.HIT_WEALTH,
                titleResHint = "bucket_hit_wealth_title",
                descriptionResHint = "bucket_hit_wealth_desc",
                commitmentKenya = 3_000,
                defaultTarget = 2_000_000
            ),
            BucketTemplate(
                id = "master_skill",
                kind = BucketGoalKind.MASTER_SKILL,
                titleResHint = "bucket_master_skill_title",
                descriptionResHint = "bucket_master_skill_desc",
                commitmentKenya = 1_500
            )
        )
    }
}
