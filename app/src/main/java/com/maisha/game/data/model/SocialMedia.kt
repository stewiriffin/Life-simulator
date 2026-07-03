// app/src/main/java/com/maisha/game/data/model/SocialMedia.kt
package com.maisha.game.data.model

import kotlinx.serialization.Serializable

/**
 * Online presence for a [Character]: account, followers, and verification status.
 * Persisted as [com.maisha.game.data.local.CharacterEntity.socialMediaJson].
 */
@Serializable
data class SocialMediaState(
    val hasAccount: Boolean = false,
    val followers: Int = 0,
    val isVerified: Boolean = false,
    val monetizedThisYear: Boolean = false
)
