// app/src/main/java/com/maisha/game/ui/components/BottomNav.kt (modified — AppIcons tab glyphs)
package com.maisha.game.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.ui.theme.AppIcons
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.TealPrimary

enum class MainTab {
    LIFE,
    FAMILY,
    CAREER,
    ASSETS,
    ACTIONS
}

private data class TabItem(
    val tab: MainTab,
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

private val tabs = listOf(
    TabItem(MainTab.LIFE, R.string.nav_life, AppIcons.NavLife),
    TabItem(MainTab.FAMILY, R.string.nav_family, AppIcons.NavFamily),
    TabItem(MainTab.CAREER, R.string.nav_career, AppIcons.NavCareer),
    TabItem(MainTab.ASSETS, R.string.nav_assets, AppIcons.NavAssets),
    TabItem(MainTab.ACTIONS, R.string.nav_actions, AppIcons.NavActions)
)

@Composable
fun MaishaBottomNav(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    disabledTabs: Set<MainTab> = emptySet(),
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        tabs.forEach { item ->
            val selected = selectedTab == item.tab
            val disabled = item.tab in disabledTabs
            val label = stringResource(item.labelRes)
            NavigationBarItem(
                selected = selected,
                onClick = { if (!disabled) onTabSelected(item.tab) },
                enabled = !disabled,
                icon = {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(TealPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = label,
                                tint = if (selected) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = label
                        )
                    }
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GoldAccent,
                    selectedTextColor = GoldAccent,
                    indicatorColor = MaterialTheme.colorScheme.surface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
