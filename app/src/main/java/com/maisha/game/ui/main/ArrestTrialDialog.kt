// app/src/main/java/com/maisha/game/ui/main/ArrestTrialDialog.kt
package com.maisha.game.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.LawyerTier
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.formatMoney

@Composable
fun ArrestTrialDialog(
    character: Character,
    publicDefenderAffordable: Boolean,
    averageFee: Int,
    averageAffordable: Boolean,
    expensiveFee: Int,
    expensiveAffordable: Boolean,
    onSelectLawyer: (LawyerTier) -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = stringResource(R.string.dialog_arrest_title),
                fontWeight = FontWeight.Bold,
                color = CoralNegative
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.dialog_arrest_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                LawyerOptionButton(
                    label = stringResource(R.string.lawyer_public_defender),
                    description = stringResource(R.string.lawyer_public_defender_desc),
                    feeLabel = stringResource(R.string.lawyer_fee_free),
                    enabled = publicDefenderAffordable,
                    highlighted = false,
                    onClick = { onSelectLawyer(LawyerTier.PUBLIC_DEFENDER) }
                )
                LawyerOptionButton(
                    label = stringResource(R.string.lawyer_average),
                    description = stringResource(R.string.lawyer_average_desc),
                    feeLabel = stringResource(
                        R.string.lawyer_fee_amount,
                        formatMoney(averageFee, character.countryCode)
                    ),
                    enabled = averageAffordable,
                    highlighted = false,
                    onClick = { onSelectLawyer(LawyerTier.AVERAGE) }
                )
                LawyerOptionButton(
                    label = stringResource(R.string.lawyer_expensive),
                    description = stringResource(R.string.lawyer_expensive_desc),
                    feeLabel = stringResource(
                        R.string.lawyer_fee_amount,
                        formatMoney(expensiveFee, character.countryCode)
                    ),
                    enabled = expensiveAffordable,
                    highlighted = true,
                    onClick = { onSelectLawyer(LawyerTier.EXPENSIVE) }
                )
            }
        },
        confirmButton = { },
        dismissButton = { }
    )
}

@Composable
private fun LawyerOptionButton(
    label: String,
    description: String,
    feeLabel: String,
    enabled: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val colors = if (highlighted) {
        ButtonDefaults.buttonColors(
            containerColor = GoldAccent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        ButtonDefaults.outlinedButtonColors(
            contentColor = TealPrimary,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (highlighted) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = colors
        ) {
            LawyerButtonContent(label, description, feeLabel)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = colors
        ) {
            LawyerButtonContent(label, description, feeLabel)
        }
    }
}

@Composable
private fun LawyerButtonContent(label: String, description: String, feeLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold)
        Text(text = description, style = MaterialTheme.typography.bodySmall)
        Text(text = feeLabel, style = MaterialTheme.typography.labelSmall, color = GoldAccent)
    }
}
