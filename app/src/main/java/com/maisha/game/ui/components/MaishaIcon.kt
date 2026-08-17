package com.maisha.game.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maisha.game.ui.theme.AppIconIllustrations

/**
 * BitLife-style illustrated icon with vector fallback.
 */
@Composable
fun MaishaIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null
) {
    val ref = AppIconIllustrations.refFor(icon)
    if (ref != null) {
        IllustrationImage(
            ref = ref,
            size = size,
            modifier = modifier,
            contentDescription = contentDescription
        )
    } else {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = modifier.then(Modifier),
            tint = tint
        )
    }
}

@Composable
fun MaishaStatIcon(
    type: StatType,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    contentDescription: String? = null
) {
    IllustrationImage(
        ref = AppIconIllustrations.refForStat(type),
        size = size,
        modifier = modifier,
        contentDescription = contentDescription
    )
}

@Composable
fun MaishaNavIcon(
    tab: MainTab,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    contentDescription: String? = null
) {
    IllustrationImage(
        ref = AppIconIllustrations.refForMainTab(tab),
        size = size,
        modifier = modifier,
        contentDescription = contentDescription
    )
}
