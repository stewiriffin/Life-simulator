// app/src/main/java/com/maisha/game/ui/summary/CharacterStatsScreen.kt (modified — originally-from label after relocation)
package com.maisha.game.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.data.CountryCatalog
import com.maisha.game.R
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import com.maisha.game.ui.components.ConditionBadge
import com.maisha.game.ui.components.MoneyStatRow
import com.maisha.game.ui.components.PersonCard
import com.maisha.game.ui.components.RecordBadge
import com.maisha.game.ui.components.StatBar
import com.maisha.game.ui.components.StatType
import com.maisha.game.ui.main.CareerFormatter
import com.maisha.game.ui.main.EducationFormatter
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterStatsScreen(
    character: Character,
    netWorth: Int,
    onBack: () -> Unit,
    onViewAchievements: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_full_life),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                CurrentLifeHeader(character = character)
            }

            item {
                OutlinedButton(
                    onClick = onViewAchievements,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.btn_view_achievements), maxLines = 1)
                }
            }

            item {
                StatsSectionCard(
                    stats = character.stats,
                    netWorth = netWorth,
                    countryCode = character.countryCode
                )
            }

            item {
                OverviewSectionCard(
                    character = character,
                    netWorth = netWorth
                )
            }

            if (character.criminalRecord.hasRecord) {
                item {
                    RecordBadge(timesArrested = character.criminalRecord.timesArrested)
                }
            }

            val activeConditions = character.activeConditions.filter { !it.treated }
            if (activeConditions.isNotEmpty()) {
                item {
                    SectionTitle(title = stringResource(R.string.section_active_conditions))
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeConditions.forEach { condition ->
                            ConditionBadge(
                                condition = condition,
                                onClick = {},
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            if (character.criminalRecord.currentlyIncarcerated) {
                item {
                    Text(
                        text = stringResource(
                            R.string.msg_incarcerated,
                            character.criminalRecord.yearsRemaining
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = com.maisha.game.ui.theme.CoralNegative
                    )
                }
            }

            if (character.family.isNotEmpty()) {
                item {
                    SectionTitle(title = stringResource(R.string.label_family))
                }
                itemsIndexed(character.family, key = { _, person -> person.id }) { _, person ->
                    PersonCard(
                        person = person,
                        relationLabel = relationLabel(person),
                        playerCountryCode = character.countryCode,
                        onClick = {}
                    )
                }
            }

            item {
                SectionTitle(title = stringResource(R.string.section_event_log))
            }

            val logEntries = character.eventLog.filterNot { it.startsWith("::DEATH:") }
            if (logEntries.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.empty_event_log),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                itemsIndexed(logEntries) { index, entry ->
                    CharacterEventLogCard(
                        entry = entry,
                        ageTag = character.age - index
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun CurrentLifeHeader(character: Character) {
    val genderLabel = when (character.gender) {
        Gender.MALE -> stringResource(R.string.gender_male)
        Gender.FEMALE -> stringResource(R.string.gender_female)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        com.maisha.game.ui.components.PersonAvatar(
            avatarConfig = character.avatarConfig,
            size = 64,
            age = character.age
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            com.maisha.game.ui.components.CountryFlag(countryCode = character.countryCode, size = 20.dp)
            Text(
                text = character.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (character.birthCountryCode != character.countryCode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                com.maisha.game.ui.components.CountryFlag(
                    countryCode = character.birthCountryCode,
                    size = 16.dp
                )
                Text(
                    text = stringResource(
                        R.string.format_originally_from,
                        CountryCatalog.getCountry(character.birthCountryCode).displayName
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = stringResource(
                R.string.format_character_stats_header,
                genderLabel,
                character.age,
                character.birthYear
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TealPrimary)
        ) {
            Text(
                text = stringResource(R.string.status_living),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun StatsSectionCard(stats: Stats, netWorth: Int, countryCode: String) {
    SectionCard(title = stringResource(R.string.section_current_stats)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBar(type = StatType.HEALTH, value = stats.health)
            StatBar(type = StatType.HAPPINESS, value = stats.happiness)
            StatBar(type = StatType.SMARTS, value = stats.smarts)
            StatBar(type = StatType.LOOKS, value = stats.looks)
            MoneyStatRow(amount = stats.money, countryCode = countryCode)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_net_worth),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatMoney(netWorth, countryCode),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun OverviewSectionCard(character: Character, netWorth: Int) {
    val resources = LocalContext.current.resources
    SectionCard(title = stringResource(R.string.section_life_overview)) {
        RecapRow(
            label = stringResource(R.string.label_education),
            value = EducationFormatter.formatStatus(character.education, resources)
        )
        if (character.education.gpa > 0f &&
            character.education.stage != SchoolStage.NONE &&
            character.education.stage != SchoolStage.GRADUATED
        ) {
            RecapRow(
                label = stringResource(R.string.label_gpa),
                value = stringResource(R.string.format_gpa, character.education.gpa)
            )
        }
        RecapRow(
            label = stringResource(R.string.label_career),
            value = CareerFormatter.formatStatus(character.career, resources)
        )
        RecapRow(
            label = stringResource(R.string.label_net_worth),
            value = formatMoney(netWorth, character.countryCode)
        )
        val childCount = character.family.count { it.relation == RelationType.CHILD }
        RecapRow(
            label = stringResource(R.string.label_children),
            value = if (childCount == 0) {
                stringResource(R.string.value_none)
            } else {
                childCount.toString()
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
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
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun RecapRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.65f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CharacterEventLogCard(entry: String, ageTag: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.format_age, ageTag.coerceAtLeast(0)),
                style = MaterialTheme.typography.labelSmall,
                color = TealPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = entry,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun relationLabel(person: Person): String = when (person.relation) {
    RelationType.MOTHER -> stringResource(R.string.relation_mother)
    RelationType.FATHER -> stringResource(R.string.relation_father)
    RelationType.SIBLING -> stringResource(R.string.relation_sibling)
    RelationType.SPOUSE -> if (person.isMarried) {
        stringResource(R.string.relation_spouse)
    } else {
        stringResource(R.string.relation_partner)
    }
    RelationType.CHILD -> stringResource(R.string.relation_child)
    RelationType.FRIEND -> stringResource(R.string.relation_friend)
}
