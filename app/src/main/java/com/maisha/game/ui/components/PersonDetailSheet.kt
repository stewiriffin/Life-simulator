// app/src/main/java/com/maisha/game/ui/components/PersonDetailSheet.kt
package com.maisha.game.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.relationshipTierFor
import com.maisha.game.domain.GiftTier
import com.maisha.game.domain.InteractionType
import com.maisha.game.domain.RelationshipEngine
import com.maisha.game.ui.avatar.AvatarImage
import com.maisha.game.ui.avatar.ExpressionResolver
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.theme.AccentPink
import com.maisha.game.ui.theme.InkTertiary
import com.maisha.game.ui.theme.LifeGreen
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.util.formatMoney

@Composable
fun PersonDetailSheet(
    member: Person,
    playerCountryCode: String,
    playerMoney: Int,
    playerAge: Int,
    isIncarcerated: Boolean = false,
    isMarried: Boolean,
    relationLabel: String,
    dateNightCost: Int = 0,
    childHospitalCost: Int = 0,
    divorceSettlementCost: Int = 0,
    onInteraction: (InteractionType, GiftTier?) -> Unit,
    onPropose: () -> Unit,
    onBreakUp: () -> Unit,
    onHaveChild: () -> Unit
) {
    val isSpouse = member.relation == RelationType.SPOUSE
    val isChild = member.relation == RelationType.CHILD
    val isEnemy = member.relation == RelationType.ENEMY
    val isMinorChild = RelationshipEngine.isMinorChild(member)
    val isAdultChild = RelationshipEngine.isAdultChild(member)
    val canAskForMoney = !isSpouse && !isChild
    val canSetUpDate = member.relation == RelationType.SIBLING ||
        member.relation == RelationType.FRIEND ||
        member.relation == RelationType.BEST_FRIEND
    val pendingGiftTier = rememberConfirmableAction<GiftTier>()
    val pendingTravel = rememberConfirmableAction<Unit>()
    val pendingAllowance = rememberConfirmableAction<Unit>()
    val pendingBreakUp = rememberConfirmableAction<Unit>()
    val pendingDivorce = rememberConfirmableAction<Unit>()
    val pendingHaveChild = rememberConfirmableAction<Unit>()
    val pendingDateNight = rememberConfirmableAction<Unit>()
    val pendingFinancialSupport = rememberConfirmableAction<Unit>()
    val pendingCelebrateMilestone = rememberConfirmableAction<Unit>()
    val travelCost = EconomyScaler.scaleRelationshipCost(
        RelationshipEngine.TRAVEL_BASE_COST_KENYA,
        playerCountryCode,
        playerAge
    )
    val allowanceCost = EconomyScaler.scaleAmount(
        RelationshipEngine.ALLOWANCE_BASE_COST_KENYA,
        playerCountryCode
    )
    val resolvedDateNightCost = if (dateNightCost > 0) {
        dateNightCost
    } else {
        EconomyScaler.scaleRelationshipCost(
            RelationshipEngine.DATE_NIGHT_COST_KENYA,
            playerCountryCode,
            playerAge
        )
    }
    val resolvedChildCost = if (childHospitalCost > 0) {
        childHospitalCost
    } else {
        EconomyScaler.scaleAmount(
            RelationshipEngine.CHILD_HOSPITAL_COST_KENYA,
            playerCountryCode
        )
    }
    val resolvedDivorceCost = if (divorceSettlementCost > 0) {
        divorceSettlementCost
    } else {
        EconomyScaler.scaleAmount(
            RelationshipEngine.DIVORCE_SETTLEMENT_KENYA,
            playerCountryCode
        )
    }
    val financialSupportCost = EconomyScaler.scaleAmount(
        RelationshipEngine.FINANCIAL_SUPPORT_COST_KENYA,
        playerCountryCode
    )
    val celebrateMilestoneCost = EconomyScaler.scaleAmount(
        RelationshipEngine.CELEBRATE_MILESTONE_COST_KENYA,
        playerCountryCode
    )
    val tier = relationshipTierFor(member.relationshipLevel)
    val expression = ExpressionResolver.resolvePersonExpression(member)
    val travelEnabled = RelationshipEngine.canTravelTogether(member) && !isIncarcerated

    fun giftCost(tier: GiftTier) = EconomyScaler.scaleRelationshipCost(
        tier.baseCostKenya,
        playerCountryCode,
        playerAge
    )

    ConfirmableActionHost(
        state = pendingGiftTier,
        onConfirmed = { giftTier -> onInteraction(InteractionType.GIFT, giftTier) }
    ) { giftTier, onConfirm, onDismiss ->
        ConfirmActionDialog(
            title = stringResource(R.string.confirm_gift_title),
            description = stringResource(
                R.string.confirm_cost_body,
                formatMoney(giftCost(giftTier), playerCountryCode)
            ),
            confirmLabel = stringResource(R.string.btn_confirm),
            severity = ConfirmSeverity.NEUTRAL,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    ConfirmableActionHost(
        state = pendingTravel,
        onConfirmed = { onInteraction(InteractionType.TRAVEL_TOGETHER, null) }
    ) { _, onConfirm, onDismiss ->
        ConfirmActionDialog(
            title = stringResource(R.string.confirm_travel_title),
            description = stringResource(
                R.string.confirm_cost_body,
                formatMoney(travelCost, playerCountryCode)
            ),
            confirmLabel = stringResource(R.string.btn_confirm),
            severity = ConfirmSeverity.NEUTRAL,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    ConfirmableActionHost(
        state = pendingAllowance,
        onConfirmed = { onInteraction(InteractionType.PAY_ALLOWANCE, null) }
    ) { _, onConfirm, onDismiss ->
        ConfirmActionDialog(
            title = stringResource(R.string.confirm_allowance_title),
            description = stringResource(
                R.string.confirm_allowance_body,
                member.name,
                formatMoney(allowanceCost, playerCountryCode)
            ),
            confirmLabel = stringResource(R.string.btn_confirm),
            severity = ConfirmSeverity.NEUTRAL,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    ConfirmableActionHost(
        state = pendingDateNight,
        onConfirmed = { onInteraction(InteractionType.DATE_NIGHT, null) }
    ) { _, onConfirm, onDismiss ->
        ConfirmActionDialog(
            title = stringResource(R.string.btn_date_night),
            description = stringResource(
                R.string.confirm_cost_body,
                formatMoney(resolvedDateNightCost, playerCountryCode)
            ),
            confirmLabel = stringResource(R.string.btn_confirm),
            severity = ConfirmSeverity.NEUTRAL,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    ConfirmableActionHost(
        state = pendingHaveChild,
        onConfirmed = { onHaveChild() }
    ) { _, onConfirm, onDismiss ->
        ConfirmActionDialog(
            title = stringResource(R.string.confirm_have_child_title),
            description = stringResource(
                R.string.confirm_have_child_body,
                formatMoney(resolvedChildCost, playerCountryCode)
            ),
            confirmLabel = stringResource(R.string.btn_have_child),
            severity = ConfirmSeverity.NEUTRAL,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    ConfirmableActionHost(
        state = pendingCelebrateMilestone,
        onConfirmed = { onInteraction(InteractionType.CELEBRATE_MILESTONE, null) }
    ) { _, onConfirm, onDismiss ->
        ConfirmActionDialog(
            title = stringResource(R.string.btn_celebrate_milestone, formatMoney(celebrateMilestoneCost, playerCountryCode)),
            description = stringResource(
                R.string.confirm_cost_body,
                formatMoney(celebrateMilestoneCost, playerCountryCode)
            ),
            confirmLabel = stringResource(R.string.btn_confirm),
            severity = ConfirmSeverity.NEUTRAL,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    ConfirmableActionHost(
        state = pendingFinancialSupport,
        onConfirmed = { onInteraction(InteractionType.FINANCIAL_SUPPORT, null) }
    ) { _, onConfirm, onDismiss ->
        ConfirmActionDialog(
            title = stringResource(
                R.string.btn_financial_support,
                formatMoney(financialSupportCost, playerCountryCode)
            ),
            description = stringResource(
                R.string.confirm_cost_body,
                formatMoney(financialSupportCost, playerCountryCode)
            ),
            confirmLabel = stringResource(R.string.btn_confirm),
            severity = ConfirmSeverity.NEUTRAL,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    if (isSpouse && !member.isMarried) {
        ConfirmableActionHost(
            state = pendingBreakUp,
            onConfirmed = { onBreakUp() }
        ) { _, onConfirm, onDismiss ->
            ConfirmActionDialog(
                title = stringResource(R.string.confirm_break_up_title),
                description = stringResource(R.string.confirm_break_up_body, member.name),
                confirmLabel = stringResource(R.string.btn_break_up),
                severity = ConfirmSeverity.NEUTRAL,
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        }
    }

    if (isSpouse && member.isMarried) {
        ConfirmableActionHost(
            state = pendingDivorce,
            onConfirmed = { onBreakUp() }
        ) { _, onConfirm, onDismiss ->
            ConfirmActionDialog(
                title = stringResource(R.string.confirm_divorce_title),
                description = stringResource(
                    R.string.confirm_divorce_body,
                    member.name,
                    formatMoney(resolvedDivorceCost.coerceAtMost(playerMoney), playerCountryCode)
                ),
                confirmLabel = stringResource(R.string.btn_divorce),
                severity = ConfirmSeverity.WARNING,
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AvatarImage(
                    config = member.avatarConfig,
                    size = 64.dp,
                    age = member.age,
                    expression = expression,
                    seed = member.id
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (member.secondaryCountryCode != null) {
                            HeritageCountryFlags(
                                primaryCountryCode = member.countryCode,
                                secondaryCountryCode = member.secondaryCountryCode,
                                size = 18.dp
                            )
                        } else if (member.countryCode != playerCountryCode) {
                            CountryFlag(countryCode = member.countryCode, size = 18.dp)
                        }
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$relationLabel · ${stringResource(R.string.format_age, member.age)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = relationshipTierLabel(tier),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            StatBar(
                type = StatType.RELATIONSHIP,
                value = member.relationshipLevel,
                label = stringResource(R.string.stat_relationship)
            )

            Text(
                text = stringResource(
                    R.string.format_person_vitals,
                    member.stats.health,
                    member.stats.happiness,
                    member.stats.looks
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (member.milestones.isNotEmpty()) {
                SheetSectionTitle(stringResource(R.string.person_memories_title))
                MilestoneTimeline(milestones = member.milestones)
            } else {
                EmptyStateCard(
                    illustration = EmptyStateIllustration.FAMILY,
                    title = stringResource(R.string.person_memories_title),
                    message = stringResource(R.string.empty_person_memories),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (isAdultChild) {
                SheetSectionTitle(stringResource(R.string.section_adult_child_actions))
                DetailSheetButton(
                    text = stringResource(R.string.btn_visit_adult_child),
                    onClick = { onInteraction(InteractionType.SPEND_TIME, null) }
                )
                DetailSheetButton(
                    text = stringResource(
                        R.string.btn_financial_support,
                        formatMoney(financialSupportCost, playerCountryCode)
                    ),
                    onClick = { pendingFinancialSupport.request(Unit) },
                    enabled = playerMoney >= financialSupportCost
                )
                DetailSheetButton(
                    text = stringResource(
                        R.string.btn_celebrate_milestone,
                        formatMoney(celebrateMilestoneCost, playerCountryCode)
                    ),
                    onClick = { pendingCelebrateMilestone.request(Unit) },
                    enabled = playerMoney >= celebrateMilestoneCost
                )
                DetailSheetButton(
                    text = stringResource(R.string.btn_discuss_life),
                    onClick = { onInteraction(InteractionType.DISCUSS_LIFE_CHOICES, null) }
                )
            }

            if (isMinorChild) {
                SheetSectionTitle(stringResource(R.string.section_parenting_actions))
                DetailSheetButton(
                    text = stringResource(R.string.btn_play_together),
                    onClick = { onInteraction(InteractionType.SPEND_TIME, null) }
                )
                DetailSheetButton(
                    text = stringResource(R.string.btn_help_homework),
                    onClick = { onInteraction(InteractionType.HELP_WITH_HOMEWORK, null) }
                )
                DetailSheetButton(
                    text = "${stringResource(R.string.btn_pay_allowance)} (${formatMoney(allowanceCost, playerCountryCode)})",
                    onClick = { pendingAllowance.request(Unit) },
                    enabled = playerMoney >= allowanceCost
                )
                DetailSheetButton(
                    text = "${stringResource(R.string.btn_gift_small)} (${formatMoney(giftCost(GiftTier.SMALL), playerCountryCode)})",
                    onClick = { pendingGiftTier.request(GiftTier.SMALL) },
                    enabled = playerMoney >= giftCost(GiftTier.SMALL)
                )
                DetailSheetButton(
                    text = "${stringResource(R.string.btn_gift_medium)} (${formatMoney(giftCost(GiftTier.MEDIUM), playerCountryCode)})",
                    onClick = { pendingGiftTier.request(GiftTier.MEDIUM) },
                    enabled = playerMoney >= giftCost(GiftTier.MEDIUM)
                )
                OutlinedButton(
                    onClick = { onInteraction(InteractionType.DISCIPLINE, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_discipline))
                }
            } else {
                if (!isAdultChild) {
                    SheetSectionTitle(stringResource(R.string.section_bond_actions))
                    DetailSheetButton(
                        text = if (isChild) {
                            stringResource(R.string.btn_play_together)
                        } else {
                            stringResource(R.string.btn_spend_time)
                        },
                        onClick = { onInteraction(InteractionType.SPEND_TIME, null) }
                    )
                } else {
                    SheetSectionTitle(stringResource(R.string.section_bond_actions))
                }
                DetailSheetButton(
                    text = stringResource(R.string.btn_compliment),
                    onClick = { onInteraction(InteractionType.COMPLIMENT, null) }
                )
                DetailSheetButton(
                    text = stringResource(R.string.btn_ask_advice),
                    onClick = { onInteraction(InteractionType.ASK_FOR_ADVICE, null) }
                )

                SheetSectionTitle(stringResource(R.string.section_spend_actions))
                DetailSheetButton(
                    text = "${stringResource(R.string.btn_gift_small)} (${formatMoney(giftCost(GiftTier.SMALL), playerCountryCode)})",
                    onClick = { pendingGiftTier.request(GiftTier.SMALL) },
                    enabled = playerMoney >= giftCost(GiftTier.SMALL)
                )
                DetailSheetButton(
                    text = "${stringResource(R.string.btn_gift_medium)} (${formatMoney(giftCost(GiftTier.MEDIUM), playerCountryCode)})",
                    onClick = { pendingGiftTier.request(GiftTier.MEDIUM) },
                    enabled = playerMoney >= giftCost(GiftTier.MEDIUM)
                )
                DetailSheetButton(
                    text = "${stringResource(R.string.btn_gift_large)} (${formatMoney(giftCost(GiftTier.LARGE), playerCountryCode)})",
                    onClick = { pendingGiftTier.request(GiftTier.LARGE) },
                    enabled = playerMoney >= giftCost(GiftTier.LARGE)
                )
                DetailSheetButton(
                    text = "${stringResource(R.string.btn_travel_together)} (${formatMoney(travelCost, playerCountryCode)})",
                    onClick = { pendingTravel.request(Unit) },
                    enabled = travelEnabled && playerMoney >= travelCost
                )
                if (!travelEnabled && isIncarcerated) {
                    Text(
                        text = stringResource(R.string.travel_blocked_incarcerated),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!travelEnabled) {
                    Text(
                        text = stringResource(R.string.travel_requires_quality_time),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SheetSectionTitle(stringResource(R.string.section_fun_risk_actions))
                DetailSheetButton(
                    text = stringResource(R.string.btn_prank),
                    onClick = { onInteraction(InteractionType.PRANK, null) }
                )
                if (canSetUpDate) {
                    DetailSheetButton(
                        text = stringResource(R.string.btn_set_up_date),
                        onClick = { onInteraction(InteractionType.SET_UP_ON_DATE, null) }
                    )
                }
                OutlinedButton(
                    onClick = { onInteraction(InteractionType.ARGUE, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isChild) stringResource(R.string.btn_scold) else stringResource(R.string.btn_argue))
                }
                OutlinedButton(
                    onClick = { onInteraction(InteractionType.INSULT, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_insult))
                }
                if (canAskForMoney) {
                    DetailSheetButton(
                        text = if (member.relationshipLevel > 60) {
                            stringResource(R.string.btn_ask_for_money)
                        } else {
                            stringResource(R.string.btn_ask_for_money_locked)
                        },
                        onClick = { onInteraction(InteractionType.ASK_FOR_MONEY, null) },
                        enabled = member.relationshipLevel > 60
                    )
                }
            }

            if (isEnemy) {
                SheetSectionTitle(stringResource(R.string.section_rival_actions))
                DetailSheetButton(
                    text = stringResource(R.string.btn_make_peace),
                    onClick = { onInteraction(InteractionType.MAKE_PEACE, null) },
                    emphasizeGreen = true
                )
            }

            if (isSpouse) {
                SheetSectionTitle(stringResource(R.string.section_life_together))
                if (!member.isMarried) {
                    DetailSheetButton(
                        text = if (member.relationshipLevel >= RelationshipEngine.PROPOSAL_THRESHOLD) {
                            stringResource(R.string.btn_propose_marriage)
                        } else {
                            stringResource(
                                R.string.btn_propose_locked,
                                RelationshipEngine.PROPOSAL_THRESHOLD
                            )
                        },
                        onClick = onPropose,
                        enabled = member.relationshipLevel >= RelationshipEngine.PROPOSAL_THRESHOLD,
                        emphasizePink = true
                    )
                }
                if (isMarried && member.isMarried) {
                    DetailSheetButton(
                        text = "${stringResource(R.string.btn_date_night)} (${formatMoney(resolvedDateNightCost, playerCountryCode)})",
                        onClick = { pendingDateNight.request(Unit) },
                        enabled = playerMoney >= resolvedDateNightCost,
                        emphasizePink = true
                    )
                    DetailSheetButton(
                        text = "${stringResource(R.string.btn_have_child)} (${formatMoney(resolvedChildCost, playerCountryCode)})",
                        onClick = { pendingHaveChild.request(Unit) },
                        enabled = playerMoney >= resolvedChildCost,
                        emphasizeGreen = true
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (member.isMarried) pendingDivorce.request(Unit)
                        else pendingBreakUp.request(Unit)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (member.isMarried) {
                            stringResource(R.string.btn_divorce)
                        } else {
                            stringResource(R.string.btn_break_up)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SheetSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = InkTertiary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun DetailSheetButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    emphasizePink: Boolean = false,
    emphasizeGreen: Boolean = false
) {
    val colors = when {
        emphasizePink -> ButtonDefaults.buttonColors(
            containerColor = AccentPink,
            contentColor = NavyDeep
        )
        emphasizeGreen -> ButtonDefaults.buttonColors(
            containerColor = LifeGreen,
            contentColor = NavyDeep
        )
        else -> ButtonDefaults.buttonColors()
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = colors
    ) {
        Text(text, maxLines = 2)
    }
}

@Composable
fun relationshipTierLabel(tier: com.maisha.game.data.model.RelationshipTier): String = when (tier) {
    com.maisha.game.data.model.RelationshipTier.ESTRANGED -> stringResource(R.string.tier_estranged)
    com.maisha.game.data.model.RelationshipTier.DISTANT -> stringResource(R.string.tier_distant)
    com.maisha.game.data.model.RelationshipTier.COOL -> stringResource(R.string.tier_cool)
    com.maisha.game.data.model.RelationshipTier.FRIENDLY -> stringResource(R.string.tier_friendly)
    com.maisha.game.data.model.RelationshipTier.CLOSE -> stringResource(R.string.tier_close)
    com.maisha.game.data.model.RelationshipTier.INSEPARABLE -> stringResource(R.string.tier_inseparable)
}
