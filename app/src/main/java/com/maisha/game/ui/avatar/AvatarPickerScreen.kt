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
import androidx.compose.material3.FilterChipDefaults
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
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.Expression
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.EyewearStyle
import com.maisha.game.data.model.FacialHairStyle
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.InkPrimary
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

private enum class AvatarStep(val title: String) {
    FACE("Face"),
    TONE("Tone & Hair"),
    OUTFIT("Outfit"),
    DETAILS("Details"),
    ACCESSORY("Accessory")
}

@Composable
fun AvatarPickerScreen(
    avatarConfig: AvatarConfig,
    gender: Gender,
    isSaving: Boolean,
    onAvatarChange: (AvatarConfig) -> Unit,
    onContinueToStats: () -> Unit
) {
    // DiceBear faces need a stable seed so the face reacts to gender + the limited appearance controls
    // this page exposes (skin tone, facial hair, eyewear).
    val diceSeed = remember(avatarConfig, gender) {
        "${gender.name}-skin${avatarConfig.skinTone}-beard${avatarConfig.facialHair?.name ?: "none"}-glasses${avatarConfig.eyewear?.name ?: "none"}"
    }
    val previewAge = 0 // newborn face (matching CharacterCreation start age)
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Ensure readable contrast even if the previous screen used a strong background.
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.avatar_picker_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = InkPrimary
        )
        Text(
            text = stringResource(R.string.avatar_picker_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = InkPrimary.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                AvatarImage(
                    config = avatarConfig,
                    size = 160.dp,
                    age = previewAge,
                    useDiceBear = true,
                    seed = diceSeed
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
        OptionalEnumRow(
            label = stringResource(R.string.avatar_facial_hair),
            noneLabel = stringResource(R.string.avatar_none),
            options = FacialHairStyle.entries,
            selected = avatarConfig.facialHair,
            labelFor = { facialHairLabel(it) },
            onSelect = { onAvatarChange(avatarConfig.copy(facialHair = it)) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OptionalEnumRow(
            label = stringResource(R.string.avatar_eyewear),
            noneLabel = stringResource(R.string.avatar_none),
            options = EyewearStyle.entries,
            selected = avatarConfig.eyewear,
            labelFor = { eyewearLabel(it) },
            onSelect = { onAvatarChange(avatarConfig.copy(eyewear = it)) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinueToStats,
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NavyDeep)
        ) {
            Text(
                text = if (isSaving) stringResource(R.string.btn_starting) else "Customize stats",
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
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = InkPrimary
        )
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
                        .background(Color.White)
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    AvatarImage(
                        config = avatarConfig.copy(hairStyle = index),
                        size = 34.dp,
                        age = previewAge.coerceAtLeast(8),
                        expression = Expression.NEUTRAL,
                        useDiceBear = true
                    )
                }
            }
        }
    }
}

private fun expressionEmoji(expression: Expression): String = when (expression) {
    Expression.NEUTRAL -> "🙂"
    Expression.HAPPY -> "😄"
    Expression.SAD -> "🙁"
    Expression.ANGRY -> "😠"
    Expression.SURPRISED -> "😮"
}

@Composable
private fun <T> OptionalEnumRow(
    label: String,
    noneLabel: String,
    options: List<T>,
    selected: T?,
    labelFor: @Composable (T) -> String,
    onSelect: (T?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = InkPrimary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(noneLabel, color = InkPrimary) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White,
                    labelColor = InkPrimary.copy(alpha = 0.75f),
                    selectedContainerColor = GoldAccent.copy(alpha = 0.18f),
                    selectedLabelColor = InkPrimary
                )
            )
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(labelFor(option), color = InkPrimary) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White,
                        labelColor = InkPrimary.copy(alpha = 0.75f),
                        selectedContainerColor = GoldAccent.copy(alpha = 0.18f),
                        selectedLabelColor = InkPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun facialHairLabel(style: FacialHairStyle): String = when (style) {
    FacialHairStyle.STUBBLE -> stringResource(R.string.facial_hair_stubble)
    FacialHairStyle.MUSTACHE -> stringResource(R.string.facial_hair_mustache)
    FacialHairStyle.BEARD -> stringResource(R.string.facial_hair_beard)
    FacialHairStyle.GOATEE -> stringResource(R.string.facial_hair_goatee)
}

@Composable
private fun eyewearLabel(style: EyewearStyle): String = when (style) {
    EyewearStyle.GLASSES -> stringResource(R.string.eyewear_glasses)
    EyewearStyle.SUNGLASSES -> stringResource(R.string.eyewear_sunglasses)
    EyewearStyle.READING_GLASSES -> stringResource(R.string.eyewear_reading_glasses)
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
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = InkPrimary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(noneLabel, color = InkPrimary) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White,
                    labelColor = InkPrimary.copy(alpha = 0.75f),
                    selectedContainerColor = GoldAccent.copy(alpha = 0.18f),
                    selectedLabelColor = InkPrimary
                )
            )
            repeat(count) { index ->
                FilterChip(
                    selected = selected == index,
                    onClick = { onSelect(index) },
                    label = { Text("${index + 1}", color = InkPrimary) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White,
                        labelColor = InkPrimary.copy(alpha = 0.75f),
                        selectedContainerColor = GoldAccent.copy(alpha = 0.18f),
                        selectedLabelColor = InkPrimary
                    )
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
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = InkPrimary
        )
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
