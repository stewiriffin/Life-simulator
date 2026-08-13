package com.maisha.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.InkPrimary
import com.maisha.game.ui.theme.LifeGreen
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.NavyElevated
import com.maisha.game.ui.theme.NavySurface

/**
 * Shared navy hero used by Career / Assets / Actions-style tabs.
 */
@Composable
fun TabPageHero(
    title: String,
    subtitle: String,
    primaryChip: String,
    secondaryChip: String? = null,
    tertiaryChip: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(NavyDeep, NavySurface, NavyElevated))
            )
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroMoneyChip(text = primaryChip)
            secondaryChip?.let { HeroMoneyChip(text = it, muted = true) }
            tertiaryChip?.let { HeroMoneyChip(text = it, muted = true) }
        }
    }
}

@Composable
private fun HeroMoneyChip(text: String, muted: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = if (muted) Color.White.copy(alpha = 0.85f) else GoldAccent,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(
                if (muted) Color.White.copy(alpha = 0.12f) else GoldAccent.copy(alpha = 0.18f)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
fun CategoryFilterChipRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val active = selectedIndex == index
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (active) Color.White else InkPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (active) LifeGreen else Color.White)
                    .clickable { onSelected(index) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}
