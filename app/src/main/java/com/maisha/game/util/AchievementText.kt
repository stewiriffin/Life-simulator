package com.maisha.game.util

import android.content.Context
import com.maisha.game.R
import com.maisha.game.data.AchievementWealth
import com.maisha.game.data.model.Achievement

/**
 * Resolves achievement description copy at display time so wealth tiers use [formatMoney] for the player's country.
 */
fun achievementDescription(
    context: Context,
    achievement: Achievement,
    countryCode: String
): String = when (achievement.id) {
    "six_figures" -> context.getString(
        R.string.achievement_six_figures_description,
        formatMoney(AchievementWealth.sixFiguresThreshold(countryCode), countryCode)
    )
    "first_million" -> context.getString(
        R.string.achievement_first_million_description,
        formatMoney(AchievementWealth.firstMillionThreshold(countryCode), countryCode)
    )
    else -> context.getString(achievement.descriptionRes)
}
