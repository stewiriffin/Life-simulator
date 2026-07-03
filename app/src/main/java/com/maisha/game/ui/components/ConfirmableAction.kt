// app/src/main/java/com/maisha/game/ui/components/ConfirmableAction.kt
package com.maisha.game.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Holds pending confirmation state for tap → confirm → execute flows.
 */
@Stable
class ConfirmableActionState<T> {
    var pending by mutableStateOf<T?>(null)
        private set

    fun request(action: T) {
        pending = action
    }

    fun dismiss() {
        pending = null
    }
}

@Composable
fun <T> rememberConfirmableAction(): ConfirmableActionState<T> =
    remember { ConfirmableActionState() }

/**
 * Renders [dialog] when [state.pending] is set; [onConfirmed] runs before clearing state.
 */
@Composable
fun <T> ConfirmableActionHost(
    state: ConfirmableActionState<T>,
    onConfirmed: (T) -> Unit,
    dialog: @Composable (T, onConfirm: () -> Unit, onDismiss: () -> Unit) -> Unit
) {
    val pending = state.pending ?: return
    dialog(
        pending,
        {
            onConfirmed(pending)
            state.dismiss()
        },
        state::dismiss
    )
}
