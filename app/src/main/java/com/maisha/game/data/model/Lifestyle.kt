package com.maisha.game.data.model

import kotlinx.serialization.Serializable

/** Active wellness subscriptions billed during yearly health progression. */
@Serializable
data class LifestyleState(
    val hasGymMembership: Boolean = false,
    val isVegan: Boolean = false,
    val hasTherapist: Boolean = false
)

enum class LifestyleOption {
    GYM,
    DIET,
    THERAPIST
}
