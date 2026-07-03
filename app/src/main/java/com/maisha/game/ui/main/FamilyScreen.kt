// app/src/main/java/com/maisha/game/ui/main/FamilyScreen.kt
package com.maisha.game.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.local.OnboardingTips
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import com.maisha.game.domain.GiftTier
import com.maisha.game.domain.InteractionType
import com.maisha.game.domain.hasSpouse
import com.maisha.game.domain.isMarried
import com.maisha.game.ui.avatar.ExpressionResolver
import com.maisha.game.ui.components.CountryFlag
import com.maisha.game.ui.components.countryDisplayName
import com.maisha.game.ui.components.DismissibleTipCard
import com.maisha.game.ui.components.EmptyStateCard
import com.maisha.game.ui.components.PersonAvatar
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.theme.AppIcons
import com.maisha.game.ui.components.PersonCard
import com.maisha.game.ui.components.PersonDetailSheet
import com.maisha.game.ui.theme.AccentPink
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.NavyDeep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    character: Character,
    uiState: LifeUiState,
    snackbarHostState: SnackbarHostState,
    onMemberClick: (Person) -> Unit,
    onMemberDismiss: () -> Unit,
    onInteraction: (String, InteractionType, GiftTier?) -> Unit,
    onMessageDismissed: () -> Unit,
    onFindDate: () -> Unit,
    onDismissDatingProspects: () -> Unit,
    onStartDating: (Person) -> Unit,
    onPropose: (String) -> Unit,
    onBreakUp: (String) -> Unit,
    onHaveChild: () -> Unit,
    onRelationshipMessageDismissed: () -> Unit,
    onDismissFamilyDatingTip: () -> Unit,
    onDismissFamilyDetailTip: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(uiState.familyInteractionMessage) {
        uiState.familyInteractionMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onMessageDismissed()
        }
    }

    LaunchedEffect(uiState.relationshipMessage) {
        uiState.relationshipMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onRelationshipMessageDismissed()
        }
    }

    val partner = character.family.filter { it.relation == RelationType.SPOUSE }
    val children = character.family.filter { it.relation == RelationType.CHILD }
    val parents = character.family.filter {
        it.relation == RelationType.MOTHER || it.relation == RelationType.FATHER
    }
    val siblings = character.family.filter { it.relation == RelationType.SIBLING }
    val friends = character.family.filter { it.relation == RelationType.FRIEND }
    val others = character.family.filter {
        it.relation != RelationType.SPOUSE &&
            it.relation != RelationType.CHILD &&
            it.relation != RelationType.MOTHER &&
            it.relation != RelationType.FATHER &&
            it.relation != RelationType.SIBLING &&
            it.relation != RelationType.FRIEND
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaishaSpacing.md, vertical = MaishaSpacing.sm)
    ) {
        Text(
            text = stringResource(R.string.screen_family),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.format_family_member_count, character.family.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        val showDatingTip = uiState.tipsLoaded &&
            OnboardingTips.FAMILY_DATING !in uiState.seenTipIds &&
            !character.hasSpouse()
        if (showDatingTip) {
            DismissibleTipCard(
                text = stringResource(R.string.tip_family_dating),
                onDismiss = onDismissFamilyDatingTip,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        val showDetailTip = uiState.tipsLoaded &&
            OnboardingTips.FAMILY_DETAIL !in uiState.seenTipIds &&
            character.family.isNotEmpty()
        if (showDetailTip) {
            DismissibleTipCard(
                text = stringResource(R.string.tip_family_detail),
                onDismiss = onDismissFamilyDetailTip,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        if (character.age >= 18 && !character.hasSpouse()) {
            Button(
                onClick = onFindDate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPink,
                    contentColor = NavyDeep
                )
            ) {
                Icon(
                    imageVector = AppIcons.Family,
                    contentDescription = null
                )
                Text(
                    text = "  ${stringResource(R.string.btn_find_date)}",
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (character.family.isEmpty()) {
            EmptyStateCard(
                illustration = EmptyStateIllustration.FAMILY,
                title = stringResource(R.string.empty_family_title),
                message = stringResource(R.string.empty_family),
                actionLabel = if (character.age >= 18 && !character.hasSpouse()) {
                    stringResource(R.string.btn_find_date)
                } else {
                    null
                },
                onAction = if (character.age >= 18 && !character.hasSpouse()) onFindDate else null,
                modifier = Modifier.padding(top = MaishaSpacing.sm)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (parents.isNotEmpty()) {
                    item { FamilySectionHeader(stringResource(R.string.section_parents)) }
                    items(parents, key = { it.id }) { member ->
                        PersonCard(
                            person = member,
                            relationLabel = relationLabel(member),
                            playerCountryCode = character.countryCode,
                            onClick = { onMemberClick(member) }
                        )
                    }
                }
                if (siblings.isNotEmpty()) {
                    item { FamilySectionHeader(stringResource(R.string.section_siblings)) }
                    items(siblings, key = { it.id }) { member ->
                        PersonCard(
                            person = member,
                            relationLabel = relationLabel(member),
                            playerCountryCode = character.countryCode,
                            onClick = { onMemberClick(member) }
                        )
                    }
                }
                if (partner.isNotEmpty()) {
                    item { FamilySectionHeader(stringResource(R.string.section_partner)) }
                    items(partner, key = { it.id }) { member ->
                        PersonCard(
                            person = member,
                            relationLabel = relationLabel(member),
                            playerCountryCode = character.countryCode,
                            onClick = { onMemberClick(member) }
                        )
                    }
                }
                if (children.isNotEmpty()) {
                    item { FamilySectionHeader(stringResource(R.string.section_children)) }
                    items(children, key = { it.id }) { member ->
                        PersonCard(
                            person = member,
                            relationLabel = relationLabel(member),
                            playerCountryCode = character.countryCode,
                            onClick = { onMemberClick(member) }
                        )
                    }
                }
                if (friends.isNotEmpty()) {
                    item { FamilySectionHeader(stringResource(R.string.section_friends)) }
                    items(friends, key = { it.id }) { member ->
                        PersonCard(
                            person = member,
                            relationLabel = relationLabel(member),
                            playerCountryCode = character.countryCode,
                            onClick = { onMemberClick(member) }
                        )
                    }
                }
                if (others.isNotEmpty()) {
                    item { FamilySectionHeader(stringResource(R.string.section_other)) }
                    items(others, key = { it.id }) { member ->
                        PersonCard(
                            person = member,
                            relationLabel = relationLabel(member),
                            playerCountryCode = character.countryCode,
                            onClick = { onMemberClick(member) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showDatingProspects) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissDatingProspects,
            sheetState = sheetState
        ) {
            DatingProspectsSheet(
                prospects = uiState.datingProspects,
                playerCountryCode = character.countryCode,
                onStartDating = onStartDating
            )
        }
    }

    uiState.selectedFamilyMember?.let { member ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onMemberDismiss,
            sheetState = sheetState
        ) {
            PersonDetailSheet(
                member = member,
                playerCountryCode = character.countryCode,
                playerMoney = character.stats.money,
                playerAge = character.age,
                isIncarcerated = character.criminalRecord.currentlyIncarcerated,
                isMarried = character.isMarried(),
                relationLabel = relationLabel(member),
                onInteraction = { type, giftTier -> onInteraction(member.id, type, giftTier) },
                onPropose = { onPropose(member.id) },
                onBreakUp = { onBreakUp(member.id) },
                onHaveChild = onHaveChild
            )
        }
    }
}

@Composable
private fun FamilySectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun DatingProspectsSheet(
    prospects: List<Person>,
    playerCountryCode: String,
    onStartDating: (Person) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.sheet_dating_prospects_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.sheet_dating_prospects_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        prospects.forEach { prospect ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    PersonAvatar(
                        avatarConfig = prospect.avatarConfig,
                        age = prospect.age,
                        expression = ExpressionResolver.resolvePersonExpression(prospect)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (prospect.countryCode != playerCountryCode) {
                            CountryFlag(countryCode = prospect.countryCode, size = 18.dp)
                        }
                        Text(
                            text = prospect.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (prospect.countryCode != playerCountryCode) {
                        Text(
                            text = stringResource(
                                R.string.format_prospect_from_country,
                                countryDisplayName(prospect.countryCode)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.format_prospect_chemistry,
                            stringResource(R.string.format_age, prospect.age),
                            prospect.relationshipLevel
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onStartDating(prospect) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.btn_start_dating))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun rememberFamilySnackbarHostState(): SnackbarHostState = remember { SnackbarHostState() }

@Composable
private fun relationLabel(member: Person): String = when (member.relation) {
    RelationType.MOTHER -> stringResource(R.string.relation_mother)
    RelationType.FATHER -> stringResource(R.string.relation_father)
    RelationType.SIBLING -> stringResource(R.string.relation_sibling)
    RelationType.SPOUSE -> when {
        member.isMarried -> stringResource(R.string.relation_spouse)
        else -> stringResource(R.string.relation_partner_dating)
    }
    RelationType.CHILD -> stringResource(R.string.relation_child)
    RelationType.FRIEND -> stringResource(R.string.relation_friend)
}
