// app/src/main/java/com/maisha/game/data/model/Skill.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class SkillType {
    GUITAR,
    COOKING,
    MARTIAL_ARTS,
    PROGRAMMING,
    WRITING
}

/**
 * Progress in a hobby/skill for a [Character], level 0–100.
 * Persisted in [com.maisha.game.data.local.CharacterEntity.skillsJson].
 */
@Serializable
data class SkillProgress(
    val type: SkillType,
    val level: Int = 0
)
