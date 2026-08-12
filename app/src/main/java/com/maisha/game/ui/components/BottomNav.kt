// app/src/main/java/com/maisha/game/ui/components/BottomNav.kt
package com.maisha.game.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.ui.theme.AppIcons
import com.maisha.game.ui.theme.Hairline
import com.maisha.game.ui.theme.LifeGreen

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
        modifier = modifier.fillMaxWidth(),
        containerColor = Color.White,
        tonalElevation = 0.dp,
        contentColor = LifeGreen
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
                    Box(contentAlignment = Alignment.TopCenter) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(bottom = 28.dp)
                                    .size(width = 32.dp, height = 3.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(LifeGreen)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) LifeGreen.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = label,
                                tint = if (selected) LifeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LifeGreen,
                    selectedTextColor = LifeGreen,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
