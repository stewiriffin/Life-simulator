package com.maisha.game.domain

/** Action families used to soft-match year quests on the Actions board. */
enum class ActionFamily {
    TREATMENT,
    LIFESTYLE_WELLNESS,
    LEISURE,
    SIDE_HUSTLE,
    SOCIAL_POST,
    SOCIAL_MONETIZE,
    SKILL_PRACTICE,
    SKILL_SHOWCASE,
    CRIME,
    VOLUNTEER,
    DONATE
}

object ActionQuestHints {
    fun helpsQuest(kind: YearQuestKind, family: ActionFamily): Boolean = when (family) {
        ActionFamily.TREATMENT, ActionFamily.LIFESTYLE_WELLNESS ->
            kind == YearQuestKind.RAISE_HEALTH
        ActionFamily.LEISURE ->
            kind == YearQuestKind.RAISE_HAPPINESS || kind == YearQuestKind.RAISE_HEALTH
        ActionFamily.SIDE_HUSTLE, ActionFamily.SOCIAL_MONETIZE, ActionFamily.SKILL_SHOWCASE ->
            kind == YearQuestKind.EARN_MONEY
        ActionFamily.SOCIAL_POST ->
            kind == YearQuestKind.GROW_FOLLOWERS
        ActionFamily.SKILL_PRACTICE ->
            kind == YearQuestKind.RAISE_SKILL
        ActionFamily.CRIME -> false
        ActionFamily.VOLUNTEER, ActionFamily.DONATE ->
            kind == YearQuestKind.RAISE_HAPPINESS
    }

    fun anyMatch(quests: List<YearQuest>, family: ActionFamily): Boolean =
        quests.any { helpsQuest(it.kind, family) }
}
