// app/src/main/java/com/maisha/game/ui/avatar/AvatarPickerScreen.kt (modified — Prompt 26: all options + hair previews + facialFeature)
package com.maisha.game.ui.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.TealPrimary

private val skinSwatches = listOf(
    Color(0xFFFFDBAC), Color(0xFFFFE0BD), Color(0xFFE8B88A), Color(0xFFD4A574),
    Color(0xFFC68642), Color(0xFF8D5524), Color(0xFF6B4423), Color(0xFF4A2912)
)
private val hairSwatches = listOf(
    Color(0xFF1A1A1A), Color(0xFF4A3728), Color(0xFF8B6914), Color(0xFFB8860B), Color(0xFF6B4423), Color(0xFF808080)
)
private val outfitSwatches = listOf(
    Color(0xFF1A8A8A), Color(0xFF2E5AAC), Color(0xFFE85D5D), Color(0xFFF4B942),
    Color(0xFF7E57C2), Color(0xFF4CAF50), Color(0xFFCE93D8), Color(0xFF455A64)
)

@Composable
fun AvatarPickerScreen(
    avatarConfig: AvatarConfig,
    isSaving: Boolean,
    onAvatarChange: (AvatarConfig) -> Unit,
    onStartLife: () -> Unit
) {
    var previewAge by remember { mutableIntStateOf(18) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.avatar_picker_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.avatar_picker_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                AvatarImage(config = avatarConfig, size = 120.dp, age = previewAge)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.avatar_age_preview),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(1 to R.string.age_stage_baby, 8 to R.string.age_stage_child, 15 to R.string.age_stage_teen, 30 to R.string.age_stage_adult, 65 to R.string.age_stage_senior)
                .forEach { (age, labelRes) ->
                    FilterChip(
                        selected = previewAge == age,
                        onClick = { previewAge = age },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SwatchRow(
            label = stringResource(R.string.avatar_skin_tone),
            count = AvatarConfig.SKIN_TONE_COUNT,
            selected = avatarConfig.skinTone,
            colors = skinSwatches,
            onSelect = { onAvatarChange(avatarConfig.copy(skinTone = it)) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        HairStyleRow(
            label = stringResource(R.string.avatar_hair_style),
            avatarConfig = avatarConfig,
            previewAge = previewAge,
            onSelect = { onAvatarChange(avatarConfig.copy(hairStyle = it)) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        SwatchRow(
            label = stringResource(R.string.avatar_hair_color),
            count = AvatarConfig.HAIR_COLOR_COUNT,
            selected = avatarConfig.hairColor,
            colors = hairSwatches,
            onSelect = { onAvatarChange(avatarConfig.copy(hairColor = it)) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        SwatchRow(
            label = stringResource(R.string.avatar_outfit_color),
            count = AvatarConfig.OUTFIT_COLOR_COUNT,
            selected = avatarConfig.outfitColor,
            colors = outfitSwatches,
            onSelect = { onAvatarChange(avatarConfig.copy(outfitColor = it)) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OptionalFeatureRow(
            label = stringResource(R.string.avatar_facial_feature),
            noneLabel = stringResource(R.string.avatar_none),
            count = AvatarConfig.FACIAL_FEATURE_COUNT,
            selected = avatarConfig.facialFeature,
            onSelect = { onAvatarChange(avatarConfig.copy(facialFeature = it)) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OptionalFeatureRow(
            label = stringResource(R.string.avatar_accessory),
            noneLabel = stringResource(R.string.avatar_none),
            count = AvatarConfig.ACCESSORY_COUNT,
            selected = avatarConfig.accessoryId,
            onSelect = { onAvatarChange(avatarConfig.copy(accessoryId = it)) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartLife,
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NavyDeep)
        ) {
            Text(
                text = if (isSaving) stringResource(R.string.btn_starting) else stringResource(R.string.btn_start_life),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HairStyleRow(
    label: String,
    avatarConfig: AvatarConfig,
    previewAge: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(AvatarConfig.HAIR_STYLE_COUNT) { index ->
                val selected = index == avatarConfig.hairStyle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) GoldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    AvatarImage(
                        config = avatarConfig.copy(hairStyle = index),
                        size = 34.dp,
                        age = previewAge.coerceAtLeast(8)
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionalFeatureRow(
    label: String,
    noneLabel: String,
    count: Int,
    selected: Int?,
    onSelect: (Int?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(noneLabel) }
            )
            repeat(count) { index ->
                FilterChip(
                    selected = selected == index,
                    onClick = { onSelect(index) },
                    label = { Text("${index + 1}") }
                )
            }
        }
    }
}

@Composable
private fun SwatchRow(
    label: String,
    count: Int,
    selected: Int,
    colors: List<Color>? = null,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(count) { index ->
                val swatchColor = colors?.getOrNull(index) ?: TealPrimary.copy(alpha = 0.4f + index * 0.15f)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(
                            width = if (index == selected) 3.dp else 1.dp,
                            color = if (index == selected) GoldAccent else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onSelect(index) }
                )
            }
        }
    }
}
