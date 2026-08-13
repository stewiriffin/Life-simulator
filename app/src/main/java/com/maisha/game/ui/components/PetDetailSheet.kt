// app/src/main/java/com/maisha/game/ui/components/PetDetailSheet.kt
package com.maisha.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maisha.game.R
import com.maisha.game.data.model.Pet
import com.maisha.game.data.model.PetSpecies
import com.maisha.game.domain.PetCareAction
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.formatMoney

@Composable
fun PetDetailSheet(
    pet: Pet,
    speciesLabel: String,
    playerCountryCode: String,
    playerMoney: Int,
    feedCost: Int,
    vetCost: Int,
    onCare: (PetCareAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(TealPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = petEmoji(pet.species), fontSize = 32.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$speciesLabel · ${stringResource(R.string.format_age, pet.age)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        StatBar(
            type = StatType.RELATIONSHIP,
            value = pet.relationshipLevel,
            label = stringResource(R.string.stat_pet_bond)
        )
        StatBar(
            type = StatType.HEALTH,
            value = pet.health,
            label = stringResource(R.string.stat_health)
        )

        Text(
            text = stringResource(R.string.section_pet_care),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        Button(
            onClick = { onCare(PetCareAction.PLAY) },
            enabled = !pet.playedThisYear,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (pet.playedThisYear) {
                    stringResource(R.string.btn_play_pet_done)
                } else {
                    stringResource(R.string.btn_play_pet)
                }
            )
        }

        Button(
            onClick = { onCare(PetCareAction.FEED) },
            enabled = playerMoney >= feedCost,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                stringResource(
                    R.string.btn_feed_pet,
                    formatMoney(feedCost, playerCountryCode)
                )
            )
        }

        OutlinedButton(
            onClick = { onCare(PetCareAction.VET) },
            enabled = !pet.caredThisYear && playerMoney >= vetCost,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (pet.caredThisYear) {
                    stringResource(R.string.btn_vet_pet_done)
                } else {
                    stringResource(
                        R.string.btn_vet_pet,
                        formatMoney(vetCost, playerCountryCode)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun petEmoji(species: PetSpecies): String = when (species) {
    PetSpecies.DOG -> "🐕"
    PetSpecies.CAT -> "🐈"
    PetSpecies.BIRD -> "🦜"
    PetSpecies.FISH -> "🐠"
    PetSpecies.EXOTIC -> "🦎"
}
