package com.maisha.game.data

/**
 * Wealth-tier achievement thresholds authored in Kenya-scale units; scaled per country via [EconomyScaler].
 */
object AchievementWealth {
    const val SIX_FIGURES_BASE_KENYA = 100_000
    const val FIRST_MILLION_BASE_KENYA = 1_000_000

    fun sixFiguresThreshold(countryCode: String): Int =
        EconomyScaler.scaleAmount(SIX_FIGURES_BASE_KENYA, countryCode)

    fun firstMillionThreshold(countryCode: String): Int =
        EconomyScaler.scaleAmount(FIRST_MILLION_BASE_KENYA, countryCode)
}
