package com.maisha.game.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class BucketGoalKind {
    OWN_HOME,
    REACH_FAME,
    START_BUSINESS,
    WIN_OFFICE,
    RAISE_CHILD,
    HIT_WEALTH,
    MASTER_SKILL
}

@Serializable
data class BucketGoal(
    val id: String,
    val kind: BucketGoalKind,
    /** Template id from catalog. */
    val templateId: String,
    val targetValue: Int = 0,
    val commitmentPaid: Int = 0,
    val completed: Boolean = false
)
