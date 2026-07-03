// app/src/main/java/com/maisha/game/util/ClampUtils.kt
package com.maisha.game.util

private const val STAT_MIN = 0
private const val STAT_MAX = 100

/** Clamps core character/NPC stats (health, happiness, smarts, looks) to 0–100. */
fun clampStat(value: Int): Int = value.coerceIn(STAT_MIN, STAT_MAX)

/** Clamps [Person.relationshipLevel] to 0–100. */
fun clampRelationshipLevel(value: Int): Int = clampStat(value)

/** Clamps [Job.performanceScore] to 0–100. */
fun clampPerformanceScore(value: Int): Int = clampStat(value)

/** Clamps [Asset.condition] to 0–100. */
fun clampCondition(value: Int): Int = clampStat(value)

/** Clamps school GPA to 0.0–4.0. */
fun clampGpa(value: Float): Float = value.coerceIn(0f, 4f)
