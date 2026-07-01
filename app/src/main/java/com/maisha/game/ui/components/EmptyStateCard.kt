// app/src/main/java/com/maisha/game/ui/components/EmptyStateCard.kt (modified — illustration support)
package com.maisha.game.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maisha.game.ui.illustrations.EmptyStateIllustration
import com.maisha.game.ui.illustrations.EmptyStateIllustrationView
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.MaishaSpacing

@Composable
fun EmptyStateCard(
    illustration: EmptyStateIllustration,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaishaSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EmptyStateIllustrationView(type = illustration)
            Spacer(modifier = Modifier.height(MaishaSpacing.sm))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(MaishaSpacing.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(MaishaSpacing.md))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}
