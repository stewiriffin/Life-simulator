// app/src/main/java/com/maisha/game/domain/SkillEngine.kt
package com.maisha.game.domain

import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.SkillProgress
import com.maisha.game.data.model.SkillType
import com.maisha.game.util.clampStat
import com.maisha.game.util.formatMoney
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

sealed class SkillResult {
    data class Success(
        val character: Character,
        val skillType: SkillType,
        val levelGained: Int,
        val cost: Int = 0
    ) : SkillResult()

    data class Failed(val reason: SkillFailure) : SkillResult()
}

enum class SkillFailure {
    INELIGIBLE,
    INSUFFICIENT_FUNDS,
    ALREADY_MASTERED
}

@Singleton
class SkillEngine @Inject constructor() {

    fun practiceSkill(character: Character, skillType: SkillType): SkillResult {
        if (!canPractice(character)) {
            return SkillResult.Failed(SkillFailure.INELIGIBLE)
        }
        val current = skillLevel(character, skillType)
        if (current >= MAX_SKILL_LEVEL) {
            return SkillResult.Failed(SkillFailure.ALREADY_MASTERED)
        }

        val gain = Random.nextInt(PRACTICE_GAIN_MIN, PRACTICE_GAIN_MAX + 1)
        val newLevel = (current + gain).coerceAtMost(MAX_SKILL_LEVEL)
        val actualGain = newLevel - current
        val updated = withSkillLevel(character, skillType, newLevel).copy(
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness - PRACTICE_HAPPINESS_COST),
                health = clampStat(character.stats.health - PRACTICE_HEALTH_COST)
            ),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "Practiced ${skillLabel(skillType)} (now level $newLevel)."
            )
        )
        return SkillResult.Success(updated, skillType, actualGain)
    }

    fun takeMasterclass(character: Character, skillType: SkillType): SkillResult {
        if (!canPractice(character)) {
            return SkillResult.Failed(SkillFailure.INELIGIBLE)
        }
        val current = skillLevel(character, skillType)
        if (current >= MAX_SKILL_LEVEL) {
            return SkillResult.Failed(SkillFailure.ALREADY_MASTERED)
        }

        val cost = masterclassCost(character)
        if (character.stats.money < cost) {
            return SkillResult.Failed(SkillFailure.INSUFFICIENT_FUNDS)
        }

        val gain = Random.nextInt(MASTERCLASS_GAIN_MIN, MASTERCLASS_GAIN_MAX + 1)
        val newLevel = (current + gain).coerceAtMost(MAX_SKILL_LEVEL)
        val actualGain = newLevel - current
        val updated = withSkillLevel(character, skillType, newLevel).copy(
            stats = character.stats.copy(money = character.stats.money - cost),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "Took a ${skillLabel(skillType)} masterclass for ${formatMoney(cost, character.countryCode)} (now level $newLevel)."
            )
        )
        return SkillResult.Success(updated, skillType, actualGain, cost)
    }

    fun canPractice(character: Character): Boolean =
        character.alive &&
            character.age >= MIN_SKILL_AGE &&
            !character.criminalRecord.currentlyIncarcerated &&
            !character.criminalRecord.awaitingTrial

    fun skillLevel(character: Character, skillType: SkillType): Int =
        character.skills.find { it.type == skillType }?.level?.coerceIn(0, MAX_SKILL_LEVEL) ?: 0

    fun masterclassCost(character: Character): Int =
        EconomyScaler.scaleAmount(MASTERCLASS_BASE_COST_KENYA, character.countryCode)

    fun meetsSkillRequirement(character: Character, skillType: SkillType, minLevel: Int): Boolean =
        skillLevel(character, skillType) >= minLevel

    private fun withSkillLevel(character: Character, skillType: SkillType, level: Int): Character {
        val clamped = level.coerceIn(0, MAX_SKILL_LEVEL)
        val index = character.skills.indexOfFirst { it.type == skillType }
        val skills = if (index >= 0) {
            character.skills.toMutableList().also {
                it[index] = SkillProgress(skillType, clamped)
            }
        } else {
            character.skills + SkillProgress(skillType, clamped)
        }
        return character.copy(skills = skills)
    }

    private fun skillLabel(type: SkillType): String = when (type) {
        SkillType.GUITAR -> "guitar"
        SkillType.COOKING -> "cooking"
        SkillType.MARTIAL_ARTS -> "martial arts"
        SkillType.PROGRAMMING -> "programming"
        SkillType.WRITING -> "writing"
    }

    companion object {
        const val MIN_SKILL_AGE = 10
        const val MAX_SKILL_LEVEL = 100
        const val MASTERCLASS_BASE_COST_KENYA = 15_000
        private const val PRACTICE_GAIN_MIN = 3
        private const val PRACTICE_GAIN_MAX = 8
        private const val PRACTICE_HAPPINESS_COST = 2
        private const val PRACTICE_HEALTH_COST = 1
        private const val MASTERCLASS_GAIN_MIN = 15
        private const val MASTERCLASS_GAIN_MAX = 25

        /** Tag format: `requires_skill_<TYPE>_<LEVEL>` e.g. `requires_skill_guitar_50`. */
        const val SKILL_REQUIREMENT_TAG_PREFIX = "requires_skill_"

        fun parseSkillRequirementTag(tag: String): Pair<SkillType, Int>? {
            if (!tag.startsWith(SKILL_REQUIREMENT_TAG_PREFIX)) return null
            val rest = tag.removePrefix(SKILL_REQUIREMENT_TAG_PREFIX)
            val separator = rest.lastIndexOf('_')
            if (separator <= 0) return null
            val level = rest.substring(separator + 1).toIntOrNull() ?: return null
            val typeName = rest.substring(0, separator).uppercase()
            val type = runCatching { SkillType.valueOf(typeName) }.getOrNull() ?: return null
            return type to level
        }
    }
}
