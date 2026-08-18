// app/src/main/java/com/maisha/game/ui/charactercreation/CharacterCreationScreen.kt
package com.maisha.game.ui.charactercreation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maisha.game.R
import com.maisha.game.data.CountryCatalog
import com.maisha.game.data.model.Country
import com.maisha.game.data.model.Gender
import com.maisha.game.ui.components.CountryFlag
import com.maisha.game.ui.components.PersonAvatar
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.InkPrimary
import com.maisha.game.ui.theme.InkSecondary
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.NavyMid
import com.maisha.game.ui.theme.StatHappiness
import com.maisha.game.ui.theme.StatHealth
import com.maisha.game.ui.theme.StatLooks
import com.maisha.game.ui.theme.StatSmarts
import com.maisha.game.ui.theme.TealPrimary

// Country backdrop colors — one vibrant gradient per country group
private fun countryGradient(code: String): List<Color> = when (code) {
    "KE" -> listOf(Color(0xFF006600), Color(0xFF000000), Color(0xFFBB0000))
    "NG" -> listOf(Color(0xFF008751), Color(0xFF1A3A1A), Color(0xFF008751))
    "ZA" -> listOf(Color(0xFF007A4D), Color(0xFF1A1A1A), Color(0xFFFFB612))
    "US" -> listOf(Color(0xFF3C3B6E), Color(0xFF1A1A2E), Color(0xFFB22234))
    "UK", "GB" -> listOf(Color(0xFF012169), Color(0xFF0A0A2E), Color(0xFFC8102E))
    "IN" -> listOf(Color(0xFFFF6600), Color(0xFF1A0A00), Color(0xFF138808))
    "BR" -> listOf(Color(0xFF009C3B), Color(0xFF001A0D), Color(0xFFF8C300))
    "JP" -> listOf(Color(0xFFBC002D), Color(0xFF1A0008), Color(0xFFBC002D))
    "CN" -> listOf(Color(0xFFDE2910), Color(0xFF1A0000), Color(0xFFFFDE00))
    "DE" -> listOf(Color(0xFF000000), Color(0xFF1A1A00), Color(0xFFDD0000))
    "FR" -> listOf(Color(0xFF002395), Color(0xFF000D2E), Color(0xFFED2939))
    "MX" -> listOf(Color(0xFF006847), Color(0xFF001A12), Color(0xFFCE1126))
    "CA" -> listOf(Color(0xFFFF0000), Color(0xFF1A0000), Color(0xFFFF0000))
    "AU" -> listOf(Color(0xFF00008B), Color(0xFF000023), Color(0xFF8B0000))
    "RU" -> listOf(Color(0xFF0039A6), Color(0xFF00001A), Color(0xFFD52B1E))
    else -> listOf(Color(0xFF1A2A4A), Color(0xFF0B1628), Color(0xFF1E2F4A))
}

@Composable
fun CharacterCreationScreen(
    uiState: CharacterCreationUiState,
    filteredCountries: List<Country>,
    onNameChange: (String) -> Unit,
    onGenderSelected: (Gender) -> Unit,
    onCountrySelected: (String) -> Unit,
    onCountrySearchChange: (String) -> Unit,
    onRandomName: () -> Unit,
    onContinueToAvatar: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val countriesToShow = if (uiState.countrySearchQuery.isBlank()) {
        CountryCatalog.all()
    } else {
        filteredCountries
    }
    val selectedCountry = CountryCatalog.getCountry(uiState.selectedCountryCode)
    val gradient = remember(uiState.selectedCountryCode) {
        countryGradient(uiState.selectedCountryCode)
    }
    val bgColor1 by animateColorAsState(gradient[0], tween(600), label = "bg1")
    val bgColor2 by animateColorAsState(gradient[1], tween(600), label = "bg2")
    val bgColor3 by animateColorAsState(gradient[2], tween(600), label = "bg3")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(bgColor1, bgColor2, bgColor3))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── App title ──────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Avatar preview (live, uses current gender + avatarConfig) ──
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(3.dp, GoldAccent.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PersonAvatar(
                    avatarConfig = uiState.avatarConfig,
                    size = 110,
                    age = 0
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Birth stat preview bars ────────────────────────────────────
            BirthStatPreviewRow()

            Spacer(modifier = Modifier.height(28.dp))

            // ── Second wind bonus ──────────────────────────────────────────
            uiState.secondWindBonusLabel?.let { bonusLabel ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + scaleIn(initialScale = 0.9f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldAccent.copy(alpha = 0.18f))
                            .border(1.dp, GoldAccent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⭐", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.second_wind_bonus_active, bonusLabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Name field ────────────────────────────────────────────────
            SectionLabel(text = "What's your name?")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    placeholder = { Text("Enter name…", color = Color.White.copy(alpha = 0.45f)) },
                    singleLine = true,
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { err -> { Text(err, color = Color(0xFFFF8A80)) } },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = GoldAccent,
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        errorBorderColor = Color(0xFFFF8A80),
                        errorTextColor = Color.White
                    )
                )
                IconButton(
                    onClick = onRandomName,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.btn_random_name),
                        tint = GoldAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Gender selector ───────────────────────────────────────────
            SectionLabel(text = "Gender")
            Spacer(modifier = Modifier.height(8.dp))
            GenderToggle(
                selected = uiState.selectedGender,
                onSelect = onGenderSelected
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Country strip ─────────────────────────────────────────────
            SectionLabel(text = "Where are you born?")
            Spacer(modifier = Modifier.height(8.dp))

            // Selected country hero card
            SelectedCountryCard(country = selectedCountry)

            Spacer(modifier = Modifier.height(10.dp))

            // Search bar (optional)
            OutlinedTextField(
                value = uiState.countrySearchQuery,
                onValueChange = onCountrySearchChange,
                placeholder = { Text("Search countries…", color = Color.White.copy(alpha = 0.45f)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    cursorColor = TealPrimary,
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal country carousel
            val lazyState = rememberLazyListState()
            val selectedIndex = remember(uiState.selectedCountryCode, countriesToShow) {
                countriesToShow.indexOfFirst { it.code == uiState.selectedCountryCode }.coerceAtLeast(0)
            }
            LaunchedEffect(selectedIndex) {
                if (countriesToShow.isNotEmpty()) {
                    lazyState.animateScrollToItem(selectedIndex)
                }
            }
            LazyRow(
                state = lazyState,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
            ) {
                items(countriesToShow, key = { it.code }) { country ->
                    CountryChip(
                        country = country,
                        selected = country.code == uiState.selectedCountryCode,
                        onClick = { onCountrySelected(country.code) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── CTA button ────────────────────────────────────────────────
            Button(
                onClick = onContinueToAvatar,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = NavyDeep,
                    disabledContainerColor = GoldAccent.copy(alpha = 0.4f),
                    disabledContentColor = NavyDeep.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = if (uiState.isSaving) {
                        stringResource(R.string.btn_starting)
                    } else {
                        stringResource(R.string.btn_continue_avatar)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ── Sub-components ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.55f),
        letterSpacing = 1.5.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GenderToggle(
    selected: Gender,
    onSelect: (Gender) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GenderPill(
            emoji = "♂",
            label = stringResource(R.string.gender_male),
            selected = selected == Gender.MALE,
            selectedColor = Color(0xFF4B8EF0),
            onClick = { onSelect(Gender.MALE) },
            modifier = Modifier.weight(1f)
        )
        GenderPill(
            emoji = "♀",
            label = stringResource(R.string.gender_female),
            selected = selected == Gender.FEMALE,
            selectedColor = Color(0xFFE91E8C),
            onClick = { onSelect(Gender.FEMALE) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GenderPill(
    emoji: String,
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) selectedColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f),
        animationSpec = tween(250, easing = EaseOutCubic),
        label = "genderBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) selectedColor else Color.White.copy(alpha = 0.15f),
        animationSpec = tween(250, easing = EaseOutCubic),
        label = "genderBorder"
    )
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.7f)
            )
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = selectedColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectedCountryCard(country: Country) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CountryFlag(countryCode = country.code, size = 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = country.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${country.currencySymbol}  ${country.currencyCode}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(GoldAccent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = NavyDeep,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CountryChip(
    country: Country,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) GoldAccent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
        animationSpec = tween(250),
        label = "chipBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) GoldAccent else Color.White.copy(alpha = 0.12f),
        animationSpec = tween(250),
        label = "chipBorder"
    )
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CountryFlag(countryCode = country.code, size = 32.dp)
        Text(
            text = country.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.65f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontSize = 10.sp
        )
    }
}

/** Decorative mini-bars showing the stat range a new life can start with. */
@Composable
private fun BirthStatPreviewRow() {
    val stats = listOf(
        Triple("❤️", "Health", StatHealth),
        Triple("😊", "Happiness", StatHappiness),
        Triple("🧠", "Smarts", StatSmarts),
        Triple("✨", "Looks", StatLooks)
    )
    val barProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "barsIn"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        stats.forEach { (emoji, name, color) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(emoji, style = MaterialTheme.typography.titleSmall)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f * barProgress)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(3.dp))
                            .drawBehind {
                                drawRect(
                                    Brush.horizontalGradient(
                                        listOf(color.copy(alpha = 0.8f), color)
                                    )
                                )
                            }
                    )
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Stats are randomly assigned at birth",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.4f),
        textAlign = TextAlign.Center,
        fontSize = 10.sp
    )
}
