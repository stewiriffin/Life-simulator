// app/src/main/java/com/maisha/game/ui/charactercreation/CharacterCreationScreen.kt
package com.maisha.game.ui.charactercreation

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.CountryCatalog
import com.maisha.game.data.model.Country
import com.maisha.game.data.model.Gender
import com.maisha.game.ui.components.CountryFlag
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.TealPrimary

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
    val countriesToShow = if (uiState.countrySearchQuery.isBlank()) {
        CountryCatalog.all()
    } else {
        filteredCountries
    }
    val selectedCountry = CountryCatalog.getCountry(uiState.selectedCountryCode)
    val selectedFlavor = CountryCatalog.flavorFor(uiState.selectedCountryCode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        WelcomeHeader()

        uiState.secondWindBonusLabel?.let { bonusLabel ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = GoldAccent.copy(alpha = 0.15f)
                )
            ) {
                Text(
                    text = stringResource(R.string.second_wind_bonus_active, bonusLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldAccent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.label_your_name)) },
                    singleLine = true,
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        cursorColor = TealPrimary
                    )
                )

                OutlinedButton(
                    onClick = onRandomName,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_random_name))
                }

                Text(
                    text = stringResource(R.string.label_gender),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = uiState.selectedGender == Gender.MALE,
                        onClick = { onGenderSelected(Gender.MALE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(
                            stringResource(R.string.gender_male),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                    SegmentedButton(
                        selected = uiState.selectedGender == Gender.FEMALE,
                        onClick = { onGenderSelected(Gender.FEMALE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(
                            stringResource(R.string.gender_female),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.label_country),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = uiState.countrySearchQuery,
                    onValueChange = onCountrySearchChange,
                    label = { Text(stringResource(R.string.label_search_country)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        cursorColor = TealPrimary
                    )
                )

                CountryWelcomeBanner(
                    country = selectedCountry,
                    greeting = selectedFlavor.greetingPhrase ?: "Hello",
                    transport = selectedFlavor.commonTransportMode
                )

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    userScrollEnabled = true
                ) {
                    items(countriesToShow, key = { it.code }) { country ->
                        CountrySelectCard(
                            country = country,
                            selected = country.code == uiState.selectedCountryCode,
                            onClick = { onCountrySelected(country.code) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinueToAvatar,
            enabled = !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                contentColor = NavyDeep
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Text(
                text = if (uiState.isSaving) {
                    stringResource(R.string.btn_starting)
                } else {
                    stringResource(R.string.btn_continue_avatar)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CountryWelcomeBanner(
    country: Country,
    greeting: String,
    transport: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = TealPrimary.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CountryFlag(countryCode = country.code, size = 28.dp)
                Text(
                    text = stringResource(R.string.format_welcome_to_country, country.displayName),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = stringResource(R.string.format_country_greeting, greeting),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(
                    R.string.format_country_currency,
                    country.currencySymbol,
                    country.currencyCode
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.format_country_transport, transport),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CountrySelectCard(
    country: Country,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) GoldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                GoldAccent.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CountryFlag(countryCode = country.code, size = 28.dp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = country.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = country.currencySymbol,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WelcomeHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(TealPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🌍",
                style = MaterialTheme.typography.displayMedium
            )
        }
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

