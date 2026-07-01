// app/src/main/java/com/maisha/game/ui/settings/SettingsScreen.kt (new)
package com.maisha.game.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maisha.game.BuildConfig
import com.maisha.game.R
import com.maisha.game.data.CountryCatalog
import com.maisha.game.ui.components.ConfirmActionDialog
import com.maisha.game.ui.components.CountryFlagWithName
import com.maisha.game.ui.components.ConfirmSeverity
import com.maisha.game.ui.theme.CoralNegative
import com.maisha.game.ui.theme.TealPrimary
import com.maisha.game.util.LocaleManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onResetAllDataRequested: () -> Unit,
    onConfirmResetAllData: () -> Unit,
    onDismissResetConfirm: () -> Unit,
    onFlagFallbackChanged: (Boolean) -> Unit,
    preferIsoFlagFallback: Boolean = false,
    modifier: Modifier = Modifier
) {
    var regionsExpanded by remember { mutableStateOf(false) }

    if (uiState.showResetConfirm) {
        ConfirmActionDialog(
            title = stringResource(R.string.settings_reset_all_data),
            description = stringResource(R.string.settings_reset_warning),
            confirmLabel = stringResource(R.string.btn_confirm),
            severity = ConfirmSeverity.WARNING,
            onConfirm = onConfirmResetAllData,
            onDismiss = onDismissResetConfirm
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_screen_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsToggleRow(
                label = stringResource(R.string.settings_sound),
                checked = uiState.soundEnabled,
                onCheckedChange = onSoundChanged
            )
            SettingsToggleRow(
                label = stringResource(R.string.settings_haptics),
                checked = uiState.hapticsEnabled,
                onCheckedChange = onHapticsChanged
            )
            SettingsToggleRow(
                label = stringResource(R.string.settings_notifications),
                checked = uiState.notificationsEnabled,
                onCheckedChange = onNotificationsChanged
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = uiState.language != LocaleManager.LANG_SW,
                            onClick = { onLanguageSelected(LocaleManager.LANG_EN) },
                            label = { Text(stringResource(R.string.settings_language_english)) }
                        )
                        FilterChip(
                            selected = uiState.language == LocaleManager.LANG_SW,
                            onClick = { onLanguageSelected(LocaleManager.LANG_SW) },
                            label = { Text(stringResource(R.string.settings_language_swahili)) }
                        )
                    }
                }
            }

            SettingsToggleRow(
                label = stringResource(R.string.settings_flag_fallback),
                checked = preferIsoFlagFallback,
                onCheckedChange = onFlagFallbackChanged
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_regions_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.settings_regions_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { regionsExpanded = !regionsExpanded }) {
                            Text(
                                text = stringResource(
                                    if (regionsExpanded) R.string.settings_regions_collapse
                                    else R.string.settings_regions_expand
                                )
                            )
                        }
                    }
                    if (regionsExpanded) {
                        CountryCatalog.all().forEach { country ->
                            CountryFlagWithName(
                                countryCode = country.code,
                                displayName = country.displayName,
                                preferIsoFallback = preferIsoFlagFallback
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onResetAllDataRequested,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralNegative)
            ) {
                Text(
                    text = stringResource(R.string.settings_reset_all_data),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
