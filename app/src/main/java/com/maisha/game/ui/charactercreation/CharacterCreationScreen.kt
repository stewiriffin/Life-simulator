// app/src/main/java/com/maisha/game/ui/charactercreation/CharacterCreationScreen.kt (modified)
package com.maisha.game.ui.charactercreation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.data.CountryCatalog
import com.maisha.game.ui.components.CountryFlagWithName
import com.maisha.game.data.model.Gender
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.TealPrimary

@Composable
fun CharacterCreationScreen(
    uiState: CharacterCreationUiState,
    filteredCountries: List<com.maisha.game.data.model.Country>,
    onNameChange: (String) -> Unit,
    onGenderSelected: (Gender) -> Unit,
    onCountrySelected: (String) -> Unit,
    onCountrySearchChange: (String) -> Unit,
    onRandomName: () -> Unit,
    onContinueToAvatar: () -> Unit
) {
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

        Spacer(modifier = Modifier.height(28.dp))

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
                    value = uiState.countrySearchQuery.ifEmpty {
                        CountryCatalog.getCountry(uiState.selectedCountryCode).displayName
                    },
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

                if (uiState.countrySearchQuery.isBlank()) {
                    CountryFlagWithName(
                        countryCode = uiState.selectedCountryCode,
                        displayName = CountryCatalog.getCountry(uiState.selectedCountryCode).displayName,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (uiState.countrySearchQuery.isNotBlank()) {
                    filteredCountries.take(6).forEach { country ->
                        OutlinedButton(
                            onClick = { onCountrySelected(country.code) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            CountryFlagWithName(
                                countryCode = country.code,
                                displayName = "${country.displayName} (${country.currencySymbol})"
                            )
                        }
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
