// app/src/main/java/com/maisha/game/ui/main/FamilyScreen.kt
package com.maisha.game.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.local.OnboardingTips
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.Pet
import com.maisha.game.data.model.PetSpecies
import com.maisha.game.data.model.RelationType
import com.maisha.game.domain.GiftTier
import com.maisha.game.domain.InteractionType
import com.maisha.game.domain.PetCareAction
import com.maisha.game.domain.RelationshipEngine
import com.maisha.game.domain.YearQuestKind
import com.maisha.game.domain.hasSpouse
import com.maisha.game.domain.isMarried
import com.maisha.game.domain.isPlatonicAlly
import com.maisha.game.ui.avatar.ExpressionResolver
import com.maisha.game.ui.components.CategoryFilterChipRow
import com.maisha.game.ui.components.ConfirmActionDialog
import com.maisha.game.ui.components.ConfirmSeverity
import com.maisha.game.ui.components.ConfirmableActionHost
import com.maisha.game.ui.components.CountryFlag
import com.maisha.game.ui.components.countryDisplayName
import com.maisha.game.ui.components.DismissibleTipCard
import com.maisha.game.ui.components.EmptyStateCard
import com.maisha.game.ui.components.PersonAvatar
import com.maisha.game.ui.components.PersonCard
import com.maisha.game.ui.components.PersonDetailSheet
import com.maisha.game.ui.components.MaishaIcon
import com.maisha.game.ui.components.PetCard
import com.maisha.game.ui.components.PetDetailSheet
import com.maisha.game.ui.components.StatBar
import com.maisha.game.ui.components.StatType
import com.maisha.game.ui.components.TabPageHero
import com.maisha.game.ui.components.rememberConfirmableAction
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.theme.AccentPink
import com.maisha.game.ui.theme.AppIcons
import com.maisha.game.ui.theme.CreamBg
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.InkTertiary
import com.maisha.game.ui.theme.LifeGreen
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.util.formatMoney

private enum class FamilyCategory { ALL, FAMILY, FRIENDS, PETS }

private enum class FamilyListContentType {
    SectionHeader,
    PersonCard,
    PetCard
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    character: Character,
    uiState: LifeUiState,
    snackbarHostState: SnackbarHostState,
    onMemberClick: (Person) -> Unit,
    onMemberDismiss: () -> Unit,
    onPetClick: (Pet) -> Unit,
    onPetDismiss: () -> Unit,
    onPetCare: (String, PetCareAction) -> Unit,
    onInteraction: (String, InteractionType, GiftTier?) -> Unit,
    onMessageDismissed: () -> Unit,
    onFindDate: () -> Unit,
    onSeekFriendship: () -> Unit,
    onDismissDatingProspects: () -> Unit,
    onStartDating: (Person) -> Unit,
    onPropose: (String) -> Unit,
    onProposeWithPrenup: (String) -> Unit = onPropose,
    onBreakUp: (String) -> Unit,
    onHaveChild: () -> Unit,
    onRelationshipMessageDismissed: () -> Unit,
    onDismissFamilyDatingTip: () -> Unit,
    onDismissFamilyDetailTip: () -> Unit,
    onThrowParty: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val partyConfirm = rememberConfirmableAction<Int>()
    var showPartyTiers by remember { mutableStateOf(false) }
    val partyModest = EconomyScaler.scaleAmount(
        RelationshipEngine.PARTY_BUDGET_MIN_KENYA,
        character.countryCode
    )
    val partyNice = EconomyScaler.scaleAmount(
        RelationshipEngine.partyBudgetNiceKenya(),
        character.countryCode
    )
    val partyLavish = EconomyScaler.scaleAmount(
        RelationshipEngine.PARTY_BUDGET_MAX_KENYA,
        character.countryCode
    )
    val dateFee = EconomyScaler.scaleAmount(
        RelationshipEngine.FIRST_DATE_COST_KENYA,
        character.countryCode
    )
    val seekCost = EconomyScaler.scaleAmount(
        RelationshipEngine.SEEK_FRIEND_COST_KENYA,
        character.countryCode
    )
    val feedCost = EconomyScaler.scaleAmount(
        RelationshipEngine.PET_FEED_COST_KENYA,
        character.countryCode
    )
    val vetCost = EconomyScaler.scaleAmount(
        RelationshipEngine.PET_VET_COST_KENYA,
        character.countryCode
    )
    val childCost = EconomyScaler.scaleAmount(
        RelationshipEngine.CHILD_HOSPITAL_COST_KENYA,
        character.countryCode
    )
    val divorceCost = EconomyScaler.scaleAmount(
        RelationshipEngine.DIVORCE_SETTLEMENT_KENYA,
        character.countryCode
    )
    val dateNightCost = EconomyScaler.scaleRelationshipCost(
        RelationshipEngine.DATE_NIGHT_COST_KENYA,
        character.countryCode,
        character.age
    )
    val friendCount = character.family.count { it.alive && it.isPlatonicAlly() }
    val canHostParty = character.alive &&
        character.family.any {
            it.alive && (it.isPlatonicAlly() || it.relation == RelationType.SIBLING)
        }
    val canMeetPeople = character.alive &&
        character.age >= 6 &&
        !character.lifestyle.socializedThisYear &&
        friendCount < RelationshipEngine.MAX_FRIENDS &&
        !character.criminalRecord.currentlyIncarcerated &&
        !character.criminalRecord.awaitingTrial

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
    val socialCircle = character.family.filter {
        it.relation == RelationType.FRIEND ||
            it.relation == RelationType.BEST_FRIEND ||
            it.relation == RelationType.ENEMY
    }
    val others = character.family.filter {
        it.relation != RelationType.SPOUSE &&
            it.relation != RelationType.CHILD &&
            it.relation != RelationType.MOTHER &&
            it.relation != RelationType.FATHER &&
            it.relation != RelationType.SIBLING &&
            it.relation != RelationType.FRIEND &&
            it.relation != RelationType.BEST_FRIEND &&
            it.relation != RelationType.ENEMY
    }

    ConfirmableActionHost(
        state = partyConfirm,
        onConfirmed = { budget -> onThrowParty(budget) }
    ) { budget, onConfirm, onDismiss ->
        val estimatedBoost = run {
            val minB = partyModest.coerceAtLeast(1)
            val maxB = partyLavish.coerceAtLeast(minB + 1)
            val t = ((budget - minB).toFloat() / (maxB - minB)).coerceIn(0f, 1f)
            (RelationshipEngine.PARTY_BOOST_MIN +
                (RelationshipEngine.PARTY_BOOST_MAX - RelationshipEngine.PARTY_BOOST_MIN) * t)
                .toInt()
                .coerceIn(RelationshipEngine.PARTY_BOOST_MIN, RelationshipEngine.PARTY_BOOST_MAX)
        }
        ConfirmActionDialog(
            title = stringResource(R.string.dialog_throw_party_title),
            description = stringResource(
                R.string.dialog_throw_party_tier_body,
                formatMoney(budget, character.countryCode),
                estimatedBoost
            ),
            confirmLabel = stringResource(R.string.btn_host_party),
            severity = ConfirmSeverity.NEUTRAL,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    val partnerStatus = when {
        character.isMarried() -> stringResource(R.string.social_status_married)
        character.hasSpouse() -> stringResource(R.string.social_status_dating)
        else -> stringResource(R.string.social_status_single)
    }
    var selectedCategory by rememberSaveable { mutableIntStateOf(0) }
    val category = FamilyCategory.entries.getOrElse(selectedCategory) { FamilyCategory.ALL }
    fun show(cat: FamilyCategory): Boolean =
        category == FamilyCategory.ALL || category == cat
    val showFamily = show(FamilyCategory.FAMILY)
    val showFriends = show(FamilyCategory.FRIENDS)
    val showPets = show(FamilyCategory.PETS)
    val chipLabels = listOf(
        stringResource(R.string.chip_family_all),
        stringResource(R.string.chip_family_family),
        stringResource(R.string.chip_family_friends),
        stringResource(R.string.chip_family_pets)
    )
    val hasBondFamilyQuest = uiState.yearQuests.any { it.kind == YearQuestKind.BOND_FAMILY }
    val listEmpty = when (category) {
        FamilyCategory.ALL -> character.family.isEmpty() && character.pets.isEmpty()
        FamilyCategory.FAMILY ->
            parents.isEmpty() && siblings.isEmpty() && partner.isEmpty() &&
                children.isEmpty() && others.isEmpty()
        FamilyCategory.FRIENDS -> socialCircle.isEmpty()
        FamilyCategory.PETS -> character.pets.isEmpty()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CreamBg)
        ) {
            TabPageHero(
                title = stringResource(R.string.screen_relationships),
                subtitle = partnerStatus,
                primaryChip = stringResource(
                    R.string.format_friend_slots,
                    friendCount,
                    RelationshipEngine.MAX_FRIENDS
                ),
                secondaryChip = if (character.pets.isNotEmpty()) {
                    stringResource(R.string.format_pet_count_short, character.pets.size)
                } else {
                    null
                }
            )

            if (hasBondFamilyQuest) {
                Text(
                    text = stringResource(R.string.family_quest_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = LifeGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            CategoryFilterChipRow(
                labels = chipLabels,
                selectedIndex = selectedCategory,
                onSelected = { selectedCategory = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
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
                            containerColor = LifeGreen,
                            contentColor = Color.White
                        )
                    ) {
                        MaishaIcon(icon = AppIcons.Family, contentDescription = null, size = 24.dp)
                        Text(
                            text = "  ${stringResource(R.string.btn_find_date)}",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (canMeetPeople) {
                    OutlinedButton(
                        onClick = onSeekFriendship,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.btn_meet_people,
                                formatMoney(seekCost, character.countryCode)
                            ),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (listEmpty) {
                    EmptyStateCard(
                        illustration = EmptyStateIllustration.FAMILY,
                        title = stringResource(R.string.empty_family_title),
                        message = stringResource(R.string.empty_family),
                        actionLabel = if (character.age >= 18 && !character.hasSpouse()) {
                            stringResource(R.string.btn_find_date)
                        } else {
                            null
                        },
                        onAction = if (character.age >= 18 && !character.hasSpouse()) {
                            onFindDate
                        } else {
                            null
                        },
                        modifier = Modifier.padding(top = MaishaSpacing.sm)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showFamily && parents.isNotEmpty()) {
                            item(
                                key = "header_parents",
                                contentType = FamilyListContentType.SectionHeader
                            ) {
                                FamilySectionHeader(stringResource(R.string.section_parents))
                            }
                            items(
                                parents,
                                key = { it.id },
                                contentType = { FamilyListContentType.PersonCard }
                            ) { member ->
                                FamilyPersonCard(member, character.countryCode) {
                                    onMemberClick(member)
                                }
                            }
                        }
                        if (showFamily && siblings.isNotEmpty()) {
                            item(
                                key = "header_siblings",
                                contentType = FamilyListContentType.SectionHeader
                            ) {
                                FamilySectionHeader(stringResource(R.string.section_siblings))
                            }
                            items(
                                siblings,
                                key = { it.id },
                                contentType = { FamilyListContentType.PersonCard }
                            ) { member ->
                                FamilyPersonCard(member, character.countryCode) {
                                    onMemberClick(member)
                                }
                            }
                        }
                        if (showFamily && partner.isNotEmpty()) {
                            item(
                                key = "header_partner",
                                contentType = FamilyListContentType.SectionHeader
                            ) {
                                FamilySectionHeader(stringResource(R.string.section_partner))
                            }
                            items(
                                partner,
                                key = { it.id },
                                contentType = { FamilyListContentType.PersonCard }
                            ) { member ->
                                FamilyPersonCard(member, character.countryCode) {
                                    onMemberClick(member)
                                }
                            }
                        }
                        if (showFamily && children.isNotEmpty()) {
                            item(
                                key = "header_children",
                                contentType = FamilyListContentType.SectionHeader
                            ) {
                                FamilySectionHeader(stringResource(R.string.section_children))
                            }
                            items(
                                children,
                                key = { it.id },
                                contentType = { FamilyListContentType.PersonCard }
                            ) { member ->
                                FamilyPersonCard(member, character.countryCode) {
                                    onMemberClick(member)
                                }
                            }
                        }
                        if (showFriends && socialCircle.isNotEmpty()) {
                            item(
                                key = "header_friends",
                                contentType = FamilyListContentType.SectionHeader
                            ) {
                                FamilySectionHeader(stringResource(R.string.section_friends_rivals))
                            }
                            items(
                                socialCircle,
                                key = { it.id },
                                contentType = { FamilyListContentType.PersonCard }
                            ) { member ->
                                FamilyPersonCard(member, character.countryCode) {
                                    onMemberClick(member)
                                }
                            }
                        }
                        if (showFamily && others.isNotEmpty()) {
                            item(
                                key = "header_other",
                                contentType = FamilyListContentType.SectionHeader
                            ) {
                                FamilySectionHeader(stringResource(R.string.section_other))
                            }
                            items(
                                others,
                                key = { it.id },
                                contentType = { FamilyListContentType.PersonCard }
                            ) { member ->
                                FamilyPersonCard(member, character.countryCode) {
                                    onMemberClick(member)
                                }
                            }
                        }
                        if (showPets && character.pets.isNotEmpty()) {
                            item(
                                key = "header_pets",
                                contentType = FamilyListContentType.SectionHeader
                            ) {
                                FamilySectionHeader(stringResource(R.string.section_pets))
                            }
                            items(
                                character.pets,
                                key = { it.id },
                                contentType = { FamilyListContentType.PetCard }
                            ) { pet ->
                                PetCard(
                                    pet = pet,
                                    speciesLabel = petSpeciesLabel(pet.species),
                                    onClick = { onPetClick(pet) }
                                )
                            }
                        }
                        item(key = "fab_spacer") {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }

        if (canHostParty) {
            FloatingActionButton(
                onClick = { showPartyTiers = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(MaishaSpacing.md),
                containerColor = LifeGreen,
                contentColor = Color.White
            ) {
                MaishaIcon(
                    icon = AppIcons.Family,
                    contentDescription = stringResource(R.string.btn_host_party),
                    size = 24.dp
                )
            }
        }
    }

    if (showPartyTiers) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPartyTiers = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.sheet_party_tiers_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                PartyTierButton(
                    label = stringResource(R.string.party_tier_modest),
                    costLabel = formatMoney(partyModest, character.countryCode),
                    enabled = character.stats.money >= partyModest
                ) {
                    showPartyTiers = false
                    partyConfirm.request(partyModest)
                }
                PartyTierButton(
                    label = stringResource(R.string.party_tier_nice),
                    costLabel = formatMoney(partyNice, character.countryCode),
                    enabled = character.stats.money >= partyNice
                ) {
                    showPartyTiers = false
                    partyConfirm.request(partyNice)
                }
                PartyTierButton(
                    label = stringResource(R.string.party_tier_lavish),
                    costLabel = formatMoney(partyLavish, character.countryCode),
                    enabled = character.stats.money >= partyLavish
                ) {
                    showPartyTiers = false
                    partyConfirm.request(partyLavish)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (uiState.showDatingProspects) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val pendingDate = rememberConfirmableAction<Person>()
        ConfirmableActionHost(
            state = pendingDate,
            onConfirmed = { prospect -> onStartDating(prospect) }
        ) { prospect, onConfirm, onDismiss ->
            ConfirmActionDialog(
                title = stringResource(R.string.btn_start_dating),
                description = stringResource(
                    R.string.confirm_first_date_body,
                    prospect.name,
                    formatMoney(dateFee, character.countryCode)
                ),
                confirmLabel = stringResource(R.string.btn_start_dating),
                severity = ConfirmSeverity.NEUTRAL,
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        }
        ModalBottomSheet(
            onDismissRequest = onDismissDatingProspects,
            sheetState = sheetState
        ) {
            DatingProspectsSheet(
                prospects = uiState.datingProspects,
                playerCountryCode = character.countryCode,
                dateFeeLabel = formatMoney(dateFee, character.countryCode),
                onStartDating = { pendingDate.request(it) }
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
                dateNightCost = dateNightCost,
                childHospitalCost = childCost,
                divorceSettlementCost = divorceCost,
                onInteraction = { type, giftTier -> onInteraction(member.id, type, giftTier) },
                onPropose = { onPropose(member.id) },
                onProposeWithPrenup = { onProposeWithPrenup(member.id) },
                onBreakUp = { onBreakUp(member.id) },
                onHaveChild = onHaveChild
            )
        }
    }

    uiState.selectedPet?.let { pet ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onPetDismiss,
            sheetState = sheetState
        ) {
            PetDetailSheet(
                pet = pet,
                speciesLabel = petSpeciesLabel(pet.species),
                playerCountryCode = character.countryCode,
                playerMoney = character.stats.money,
                feedCost = feedCost,
                vetCost = vetCost,
                onCare = { action -> onPetCare(pet.id, action) }
            )
        }
    }
}

@Composable
private fun PartyTierButton(
    label: String,
    costLabel: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GoldAccent.copy(alpha = 0.85f),
            contentColor = NavyDeep
        )
    ) {
        Text("$label · $costLabel", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FamilyPersonCard(
    member: Person,
    playerCountryCode: String,
    onClick: () -> Unit
) {
    key(member.id, member.relation, member.isMarried) {
        PersonCard(
            person = member,
            relationLabel = relationLabel(member),
            playerCountryCode = playerCountryCode,
            onClick = onClick
        )
    }
}

@Composable
private fun petSpeciesLabel(species: PetSpecies): String = when (species) {
    PetSpecies.DOG -> stringResource(R.string.pet_species_dog)
    PetSpecies.CAT -> stringResource(R.string.pet_species_cat)
    PetSpecies.BIRD -> stringResource(R.string.pet_species_bird)
    PetSpecies.FISH -> stringResource(R.string.pet_species_fish)
    PetSpecies.EXOTIC -> stringResource(R.string.pet_species_exotic)
}

@Composable
private fun FamilySectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = InkTertiary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun DatingProspectsSheet(
    prospects: List<Person>,
    playerCountryCode: String,
    dateFeeLabel: String,
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
            text = stringResource(R.string.sheet_dating_prospects_subtitle_fee, dateFeeLabel),
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PersonAvatar(
                            avatarConfig = prospect.avatarConfig,
                            age = prospect.age,
                            expression = ExpressionResolver.resolvePersonExpression(prospect),
                            seed = prospect.id
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CountryFlag(countryCode = prospect.countryCode, size = 16.dp)
                                Text(
                                    text = prospect.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = stringResource(
                                    R.string.format_prospect_from_country,
                                    countryDisplayName(prospect.countryCode)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(
                                    R.string.format_prospect_chemistry,
                                    stringResource(R.string.format_age, prospect.age),
                                    prospect.relationshipLevel
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    StatBar(
                        type = StatType.LOOKS,
                        value = prospect.stats.looks,
                        label = stringResource(R.string.stat_looks),
                        showIcon = false
                    )
                    StatBar(
                        type = StatType.SMARTS,
                        value = prospect.stats.smarts,
                        label = stringResource(R.string.stat_smarts),
                        showIcon = false
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onStartDating(prospect) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPink,
                            contentColor = NavyDeep
                        )
                    ) {
                        Text(
                            stringResource(R.string.btn_start_dating_fee, dateFeeLabel)
                        )
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
    RelationType.BEST_FRIEND -> stringResource(R.string.relation_best_friend)
    RelationType.ENEMY -> stringResource(R.string.relation_enemy)
}
