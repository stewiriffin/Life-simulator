// app/src/main/java/com/maisha/game/ui/legacy/AncestryScreen.kt (new)
package com.maisha.game.ui.legacy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.domain.AncestryHistoryCap
import com.maisha.game.data.CountryCatalog
import com.maisha.game.data.model.AncestryEntry
import com.maisha.game.data.model.Character
import com.maisha.game.ui.components.CountryFlag
import com.maisha.game.ui.components.EmptyStateCard
import com.maisha.game.ui.components.PersonAvatar
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.TealPrimary

/**
 * Timeline ordering: oldest ancestors at the top, current living character at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AncestryScreen(
    character: Character,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedAncestors = character.ancestryHistory.sortedBy { it.generationNumber }
    val showEmptyState = sortedAncestors.isEmpty() && character.generationNumber <= 1
    val historyTruncated = (character.generationNumber - 1) > sortedAncestors.size &&
        (character.generationNumber - 1) > AncestryHistoryCap.MAX_ENTRIES

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_family_heritage),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (showEmptyState) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                EmptyStateCard(
                    illustration = EmptyStateIllustration.FAMILY,
                    title = stringResource(R.string.empty_ancestry_title),
                    message = stringResource(R.string.empty_ancestry_message)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.ancestry_timeline_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (historyTruncated) {
                    item {
                        Text(
                            text = stringResource(
                                R.string.ancestry_history_truncated,
                                AncestryHistoryCap.MAX_ENTRIES
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                items(sortedAncestors, key = { "${it.generationNumber}-${it.characterName}" }) { entry ->
                    AncestorTimelineCard(entry = entry)
                    TimelineConnector()
                }

                item {
                    CurrentCharacterTimelineCard(character = character)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Compact horizontal dynasty strip for the life-summary legacy hook.
 * Draws a continuous gold line behind generation nodes.
 */
@Composable
fun AncestryTimelinePreview(
    character: Character,
    modifier: Modifier = Modifier
) {
    val ancestors = character.ancestryHistory.sortedBy { it.generationNumber }
    val nodes = ancestors.map { entry ->
        TimelineNode(
            generation = entry.generationNumber,
            name = entry.characterName,
            countryCode = entry.countryCode,
            isCurrent = false
        )
    } + TimelineNode(
        generation = character.generationNumber,
        name = character.name,
        countryCode = character.countryCode,
        isCurrent = true
    )

    if (nodes.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.section_ancestry_preview),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TealPrimary
            )
            val lineColor = GoldAccent.copy(alpha = 0.55f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.width((nodes.size * 88).dp)) {
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(top = 24.dp)
                            .height(44.dp)
                    ) {
                        val y = 22.dp.toPx()
                        drawLine(
                            color = lineColor,
                            start = Offset(28.dp.toPx(), y),
                            end = Offset(size.width - 28.dp.toPx(), y),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(10.dp.toPx(), 8.dp.toPx())
                            )
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        nodes.forEach { node ->
                            AncestryPreviewNode(node = node)
                        }
                    }
                }
            }
        }
    }
}

private data class TimelineNode(
    val generation: Int,
    val name: String,
    val countryCode: String,
    val isCurrent: Boolean
)

@Composable
private fun AncestryPreviewNode(node: TimelineNode) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(76.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            PersonAvatar(
                name = node.name,
                size = 40
            )
        }
        Text(
            text = stringResource(R.string.format_generation_short, node.generation),
            style = MaterialTheme.typography.labelSmall,
            color = if (node.isCurrent) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (node.isCurrent) FontWeight.Bold else FontWeight.Medium
        )
        Text(
            text = node.name.substringBefore(" "),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = if (node.isCurrent) GoldAccent else MaterialTheme.colorScheme.onSurface
        )
        CountryFlag(countryCode = node.countryCode, size = 14.dp)
    }
}

@Composable
private fun TimelineConnector() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "↓",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AncestorTimelineCard(entry: AncestryEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.format_generation_label, entry.generationNumber),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = entry.characterName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            RelocationFlagChain(
                originCountryCode = entry.countryCode,
                relocatedTo = entry.relocatedTo
            )
            val age = entry.ageAtDeath
            val cause = entry.cause
            if (age != null && cause != null) {
                Text(
                    text = stringResource(R.string.format_ancestry_death_summary, age, cause),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CurrentCharacterTimelineCard(character: Character) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = TealPrimary.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.ancestry_you_are_here),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = TealPrimary
            )
            Text(
                text = stringResource(R.string.format_generation_label, character.generationNumber),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = character.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            RelocationFlagChain(
                originCountryCode = character.birthCountryCode,
                relocatedTo = character.relocationHistory
            )
            Text(
                text = stringResource(
                    R.string.format_ancestry_living_summary,
                    character.age,
                    CountryCatalog.getCountry(character.countryCode).displayName
                ),
                style = MaterialTheme.typography.bodySmall,
                color = GoldAccent,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RelocationFlagChain(
    originCountryCode: String,
    relocatedTo: List<String>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CountryFlag(countryCode = originCountryCode, size = 18.dp)
        if (relocatedTo.isNotEmpty()) {
            relocatedTo.forEach { code ->
                Text(
                    text = "→",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CountryFlag(countryCode = code, size = 18.dp)
            }
        }
    }
}
