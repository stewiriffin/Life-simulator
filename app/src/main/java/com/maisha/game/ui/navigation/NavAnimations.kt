// app/src/main/java/com/maisha/game/ui/navigation/NavAnimations.kt (new)
package com.maisha.game.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * Shared navigation transitions — 250ms slide+fade for stack navigation.
 * Kept shallow (1/4 width slide) for snappy feel on budget devices like itel A665L.
 */
object NavAnimations {
    private const val DURATION_MS = 250

    val enterForward: EnterTransition
        get() = slideInHorizontally(
            animationSpec = tween(DURATION_MS),
            initialOffsetX = { fullWidth -> fullWidth / 4 }
        ) + fadeIn(tween(DURATION_MS))

    val exitForward: ExitTransition
        get() = slideOutHorizontally(
            animationSpec = tween(DURATION_MS),
            targetOffsetX = { fullWidth -> -fullWidth / 4 }
        ) + fadeOut(tween(DURATION_MS))

    val enterBack: EnterTransition
        get() = slideInHorizontally(
            animationSpec = tween(DURATION_MS),
            initialOffsetX = { fullWidth -> -fullWidth / 4 }
        ) + fadeIn(tween(DURATION_MS))

    val exitBack: ExitTransition
        get() = slideOutHorizontally(
            animationSpec = tween(DURATION_MS),
            targetOffsetX = { fullWidth -> fullWidth / 4 }
        ) + fadeOut(tween(DURATION_MS))

    /** Bottom-nav sibling tabs — crossfade only, no horizontal slide. */
    val tabEnter: EnterTransition get() = fadeIn(tween(DURATION_MS))
    val tabExit: ExitTransition get() = fadeOut(tween(DURATION_MS))
}

fun AnimatedContentTransitionScope<*>.defaultEnterForward(): EnterTransition =
    NavAnimations.enterForward

fun AnimatedContentTransitionScope<*>.defaultExitForward(): ExitTransition =
    NavAnimations.exitForward

fun AnimatedContentTransitionScope<*>.defaultPopEnter(): EnterTransition =
    NavAnimations.enterBack

fun AnimatedContentTransitionScope<*>.defaultPopExit(): ExitTransition =
    NavAnimations.exitBack
