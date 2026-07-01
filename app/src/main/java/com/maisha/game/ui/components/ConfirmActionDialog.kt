// app/src/main/java/com/maisha/game/ui/components/ConfirmActionDialog.kt
package com.maisha.game.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.feedback.HapticType
import com.maisha.game.ui.feedback.LocalFeedbackManager
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.TealPrimary

enum class ConfirmSeverity {
    NEUTRAL,
    WARNING
}

@Composable
fun ConfirmActionDialog(
    title: String,
    description: String,
    confirmLabel: String? = null,
    severity: ConfirmSeverity = ConfirmSeverity.NEUTRAL,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val resolvedConfirmLabel = confirmLabel ?: stringResource(R.string.btn_confirm)
    val view = LocalView.current
    val feedbackManager = LocalFeedbackManager.current
    val confirmColors = when (severity) {
        ConfirmSeverity.NEUTRAL -> ButtonDefaults.buttonColors(
            containerColor = TealPrimary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        ConfirmSeverity.WARNING -> ButtonDefaults.buttonColors(
            containerColor = CoralNegative,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    feedbackManager.triggerHaptic(view, HapticType.LIGHT_TAP)
                    onConfirm()
                },
                shape = RoundedCornerShape(10.dp),
                colors = confirmColors
            ) {
                Text(resolvedConfirmLabel, maxLines = 1)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.btn_cancel),
                    color = GoldAccent,
                    maxLines = 1
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
