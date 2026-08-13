package com.maisha.game.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class FameTier {
    UNKNOWN,
    LOCAL,
    REGIONAL,
    NATIONAL,
    GLOBAL
}

/**
 * Online presence for a [Character]: account, followers, and verification status.
 * Persisted as [com.maisha.game.data.local.CharacterEntity.socialMediaJson].
 */
@Serializable
data class SocialMediaState(
    val hasAccount: Boolean = false,
    val followers: Int = 0,
    val isVerified: Boolean = false,
    val monetizedThisYear: Boolean = false,
    /** Highest fame tier reached (for tier-up detection / UI). */
    val fameTier: FameTier = FameTier.UNKNOWN
)
