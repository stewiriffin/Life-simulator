// app/src/main/java/com/maisha/game/ui/theme/Theme.kt
package com.maisha.game.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
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

/**
 * Radii from Figma Make reference: cards ~16, Age Up ~18, sheets ~20.
 */
object MaishaRadius {
    val card = 16.dp
    val button = 12.dp
    val ageUp = 18.dp
    val sheet = 20.dp
    val avatar = 20.dp
    val cardShape = RoundedCornerShape(card)
    val buttonShape = RoundedCornerShape(button)
    val ageUpShape = RoundedCornerShape(ageUp)
}

/** Elevation: light cream canvas uses soft 1–6dp card shadows conceptually. */
object MaishaElevation {
    val card = 1.dp
    val raised = 2.dp
    val dialog = 8.dp
}

/**
 * Light game-loop scheme from the zip prototype (cream body, green primary).
 * Navy hero headers still paint with [NavyDeep] / [NavySurface] explicitly.
 */
private val MaishaLightColorScheme = lightColorScheme(
    primary = LifeGreen,
    onPrimary = Color.White,
    primaryContainer = LifeGreenSoft,
    onPrimaryContainer = NavyDeep,
    secondary = GoldAccent,
    onSecondary = NavyDeep,
    secondaryContainer = Color(0x33F4B942),
    onSecondaryContainer = NavyDeep,
    tertiary = AccentPink,
    onTertiary = Color.White,
    background = CreamBg,
    onBackground = InkPrimary,
    surface = Color.White,
    onSurface = InkPrimary,
    surfaceVariant = CreamMuted,
    onSurfaceVariant = InkSecondary,
    error = CoralNegative,
    onError = Color.White,
    outline = Hairline,
    outlineVariant = HairlineSoft
)

@Composable
fun MaishaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaishaLightColorScheme,
        typography = MaishaTypography,
        content = content
    )
}
