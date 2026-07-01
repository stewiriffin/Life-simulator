package com.maisha.game.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.maisha.game.R
import com.maisha.game.data.model.Person
import com.maisha.game.ui.components.ConfirmActionDialog
import com.maisha.game.ui.components.ConfirmSeverity
import com.maisha.game.ui.components.PersonAvatar
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.TealPrimary

@Composable
fun HeirSelectionDialog(
    heirs: List<Person>,
    slotNumber: Int,
    selectedHeir: Person?,
    showConfirmation: Boolean,
    onHeirSelected: (Person) -> Unit,
    onConfirmContinue: () -> Unit,
    onDismissConfirmation: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showConfirmation && selectedHeir != null) {
        ConfirmActionDialog(
            title = stringResource(R.string.dialog_legacy_confirm_title),
            description = stringResource(
                R.string.dialog_legacy_confirm_body,
                selectedHeir.name,
                slotNumber
            ),
            confirmLabel = stringResource(R.string.btn_continue_legacy),
            severity = ConfirmSeverity.NEUTRAL,
            onConfirm = onConfirmContinue,
            onDismiss = onDismissConfirmation
        )
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.heir_selection_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.heir_selection_subtitle, slotNumber),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(heirs, key = { it.id }) { heir ->
                        HeirOptionCard(
                            heir = heir,
                            onSelect = { onHeirSelected(heir) }
                        )
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        }
    }
}

@Composable
private fun HeirOptionCard(
    heir: Person,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PersonAvatar(
                avatarConfig = heir.avatarConfig,
                size = 44,
                age = heir.age
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = heir.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.format_age, heir.age),
                    style = MaterialTheme.typography.bodySmall,
                    color = TealPrimary
                )
            }
            Button(
                onClick = onSelect,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = NavyDeep
                )
            ) {
                Text(
                    text = stringResource(R.string.btn_choose_heir),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
