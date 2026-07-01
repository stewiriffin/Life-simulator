// app/src/main/java/com/maisha/game/ui/notifications/NotificationPermissionEffect.kt (new)
package com.maisha.game.ui.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.NotificationManagerCompat
import androidx.compose.ui.platform.LocalContext

@Composable
fun NotificationPermissionEffect(
    requestPermission: Boolean,
    onHandled: (granted: Boolean) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onHandled(granted)
    }

    LaunchedEffect(requestPermission) {
        if (!requestPermission) return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                onHandled(true)
            } else {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            onHandled(NotificationManagerCompat.from(context).areNotificationsEnabled())
        }
    }
}
