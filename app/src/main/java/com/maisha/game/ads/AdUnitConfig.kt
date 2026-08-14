package com.maisha.game.ads

import com.maisha.game.BuildConfig

/**
 * AdMob unit IDs from [BuildConfig].
 * Debug always uses Google sample units. Release reads `ADMOB_*` from gradle.properties
 * (falls back to sample units until you set real IDs — do not ship that mix).
 */
object AdUnitConfig {
    val INTERSTITIAL: String = BuildConfig.AD_INTERSTITIAL
    val REWARDED: String = BuildConfig.AD_REWARDED
    val BANNER: String = BuildConfig.AD_BANNER
    val usingTestAds: Boolean = BuildConfig.USE_TEST_ADS
}
