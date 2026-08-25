package com.maisha.game.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.PartTimeDemand
import com.maisha.game.data.model.PartTimeJob
import com.maisha.game.domain.CareerEngine
import com.maisha.game.ui.components.StatBar
import com.maisha.game.ui.components.StatType
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.CreamBg
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.InkTertiary
import com.maisha.game.ui.theme.LifeGreen
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.formatMoney

@Composable
fun PartTimeEnergyHeader(
    character: Character,
    showStudentHints: Boolean,
    canRest: Boolean,
    onRestStudentEnergy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.section_part_time_marketplace),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TealPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.jobs_hustles_energy,
                    character.career.energyLevel
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            StatBar(
                type = StatType.HEALTH,
                value = character.career.energyLevel,
                modifier = Modifier.fillMaxWidth()
            )
            if (showStudentHints) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.jobs_hustles_balance_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = InkTertiary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.jobs_hustles_year_end_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = InkTertiary
                )
            }
            if (canRest) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onRestStudentEnergy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.btn_rest_energy))
                }
            }
        }
    }
}

@Composable
fun ActivePartTimeManagementCard(
    character: Character,
    careerEngine: CareerEngine,
    onQuitPartTimeJob: () -> Unit
) {
    val active = character.career.activePartTimeJob ?: return
    val hours = character.career.partTimeHoursWorked
        .takeIf { it > 0 }
        ?: careerEngine.partTimeHoursEstimate(active)
    val earnings = character.career.partTimeEarningsCollected

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(containerColor = LifeGreen.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.section_active_hustle),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = LifeGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = partTimeMarketplaceTitle(active),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.active_hustle_earnings,
                    formatMoney(earnings, character.countryCode)
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TealPrimary
            )
            Text(
                text = stringResource(R.string.active_hustle_hours, hours),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.active_hustle_energy_left,
                    character.career.energyLevel
                ),
                style = MaterialTheme.typography.labelSmall,
                color = InkTertiary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onQuitPartTimeJob,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralNegative)
            ) {
                Text(stringResource(R.string.btn_quit_part_time_prominent))
            }
        }
    }
}

@Composable
fun PartTimeJobMarketCard(
    character: Character,
    careerEngine: CareerEngine,
    job: PartTimeJob,
    onApply: () -> Unit
) {
    val available = careerEngine.isPartTimeListingAvailable(character, job)
    val (minPay, maxPay) = careerEngine.partTimePayoutRange(job, character.countryCode)
    val energyCost = careerEngine.partTimeEnergyCost(job)
    val hours = careerEngine.partTimeHoursEstimate(job)
    val requirement = careerEngine.partTimeRequirementHint(character, job)
    val shape = RoundedCornerShape(12.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (available) TealPrimary.copy(alpha = 0.3f) else CreamBg,
                shape = shape
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (available) Color.White else CreamBg
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = partTimeMarketplaceTitle(job),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.part_time_wage_range,
                    formatMoney(minPay, character.countryCode),
                    formatMoney(maxPay, character.countryCode)
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MarketplaceBadge(
                    text = stringResource(
                        R.string.part_time_energy_badge,
                        energyCost
                    )
                )
                MarketplaceBadge(
                    text = stringResource(R.string.part_time_hours_badge, hours)
                )
                MarketplaceBadge(text = partTimeDemandMarketplaceLabel(job.demand))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (available) {
                    stringResource(R.string.part_time_req_ok, requirement)
                } else if (character.career.partTimeWorkedThisYear) {
                    stringResource(R.string.msg_part_time_already)
                } else {
                    stringResource(R.string.part_time_req_locked, requirement)
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (available) TealPrimary else CoralNegative
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onApply,
                enabled = available,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text(stringResource(R.string.btn_apply_part_time))
            }
        }
    }
}

@Composable
private fun MarketplaceBadge(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TealPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CreamBg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun partTimeMarketplaceTitle(job: PartTimeJob): String = when (job) {
    PartTimeJob.RETAIL -> stringResource(R.string.part_time_retail)
    PartTimeJob.FAST_FOOD -> stringResource(R.string.part_time_fast_food)
    PartTimeJob.BARISTA -> stringResource(R.string.part_time_barista)
    PartTimeJob.BABYSITTING -> stringResource(R.string.part_time_babysitting)
    PartTimeJob.TUTORING -> stringResource(R.string.part_time_tutoring)
    PartTimeJob.FREELANCE_CODER -> stringResource(R.string.part_time_freelance_coder)
}

@Composable
private fun partTimeDemandMarketplaceLabel(demand: PartTimeDemand): String = when (demand) {
    PartTimeDemand.HIGH -> stringResource(R.string.part_time_demand_high)
    PartTimeDemand.MEDIUM -> stringResource(R.string.part_time_demand_medium)
    PartTimeDemand.LOW -> stringResource(R.string.part_time_demand_low)
}
