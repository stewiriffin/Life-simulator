// app/src/main/java/com/maisha/game/ui/theme/Theme.kt
package com.maisha.game.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Consistent spacing scale — prefer these over ad-hoc dp in new UI. */
object MaishaSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

/** Card/button corner radii — two-tier system for visual consistency. */
object MaishaRadius {
    val card = 14.dp
    val button = 12.dp
    val sheet = 20.dp
    val cardShape = RoundedCornerShape(card)
    val buttonShape = RoundedCornerShape(button)
}

/** Elevation: cards use 0–2dp in dark theme; dialogs use 8dp. */
object MaishaElevation {
    val card = 0.dp
    val raised = 2.dp
    val dialog = 8.dp
}

private val MaishaDarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealLight,
    onPrimaryContainer = NavyDeep,
    secondary = GoldAccent,
    onSecondary = NavyDeep,
    secondaryContainer = GoldMuted,
    onSecondaryContainer = NavyDeep,
    tertiary = AccentPink,
    onTertiary = Color.White,
    background = NavyDeep,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavyElevated,
    onSurfaceVariant = TextSecondary,
    error = CoralNegative,
    onError = Color.White,
    outline = DividerDark,
    outlineVariant = DividerDark
)

@Composable
fun MaishaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaishaDarkColorScheme,
        typography = MaishaTypography,
        content = content
    )
}
