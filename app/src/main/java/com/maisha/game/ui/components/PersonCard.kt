// app/src/main/java/com/maisha/game/ui/components/PersonCard.kt (modified — Prompt 27: MaishaRadius/spacing, a11y, 360dp ellipsis)
package com.maisha.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Expression
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.relationshipTierFor
import com.maisha.game.ui.avatar.AvatarImage
import com.maisha.game.ui.avatar.ExpressionResolver
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.TealPrimary

@Composable
fun PersonCard(
    person: Person,
    relationLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    playerCountryCode: String? = null
) {
    val expression = remember(person.id, person.relationshipLevel) {
        ExpressionResolver.resolvePersonExpression(person)
    }
    val tier = remember(person.relationshipLevel) {
        relationshipTierFor(person.relationshipLevel)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaishaSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(MaishaSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PersonAvatar(
                avatarConfig = person.avatarConfig,
                size = 44,
                age = person.age,
                expression = expression
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaishaSpacing.xs)
                ) {
                    if (person.secondaryCountryCode != null) {
                        HeritageCountryFlags(
                            primaryCountryCode = person.countryCode,
                            secondaryCountryCode = person.secondaryCountryCode,
                            size = 16.dp
                        )
                    } else if (playerCountryCode != null && person.countryCode != playerCountryCode) {
                        CountryFlag(countryCode = person.countryCode, size = 16.dp)
                    }
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Text(
                    text = relationLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = relationshipTierLabel(tier),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = stringResource(R.string.format_age, person.age),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PersonAvatar(
    avatarConfig: AvatarConfig,
    modifier: Modifier = Modifier,
    size: Int = 44,
    age: Int = 18,
    expression: Expression = Expression.NEUTRAL
) {
    val description = stringResource(R.string.content_desc_avatar)
    AvatarImage(
        config = avatarConfig,
        size = size.dp,
        modifier = modifier.semantics { contentDescription = description },
        age = age,
        expression = expression
    )
}

@Composable
fun PersonAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 44
) {
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifEmpty { "?" }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(TealPrimary.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TealPrimary
        )
    }
}
