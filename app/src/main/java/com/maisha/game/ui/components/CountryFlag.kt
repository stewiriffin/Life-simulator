// app/src/main/java/com/maisha/game/ui/components/CountryFlag.kt (modified — glyph detection + settings fallback)
package com.maisha.game.ui.components

import android.graphics.Paint
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maisha.game.R
import com.maisha.game.data.CountryCatalog

/**
 * Renders a country flag using Unicode regional-indicator emoji (e.g. KE → 🇰🇪).
 * Zero bundled assets; works on Android 13+ with Noto Color Emoji.
 *
 * Fallback: ISO code chip when emoji glyphs are missing on device ([preferIsoFallback]).
 */
@Composable
fun CountryFlag(
    countryCode: String,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    fontSize: TextUnit = (size.value * 0.7f).sp,
    preferIsoFallback: Boolean = false
) {
    val emoji = countryCodeToFlagEmoji(countryCode)
    val emojiSupported = remember(emoji) { emojiRendersOnDevice(emoji) }
    val useFallback = preferIsoFallback || !emojiSupported || emoji.length > 4

    val flagDescription = stringResource(R.string.content_desc_country_flag)
    if (useFallback) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics { contentDescription = flagDescription },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = countryCode.uppercase().take(2),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.35f).sp
            )
        }
    } else {
        Text(
            text = emoji,
            fontSize = fontSize,
            modifier = modifier
                .padding(0.dp)
                .semantics { contentDescription = flagDescription }
        )
    }
}

@Composable
fun CountryFlagWithName(
    countryCode: String,
    displayName: String,
    modifier: Modifier = Modifier,
    preferIsoFallback: Boolean = false
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        CountryFlag(countryCode = countryCode, size = 22.dp, preferIsoFallback = preferIsoFallback)
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

fun countryCodeToFlagEmoji(countryCode: String): String {
    val code = countryCode.uppercase()
    if (code.length != 2 || !code.all { it in 'A'..'Z' }) return code
    val first = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
    val second = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

fun emojiRendersOnDevice(emoji: String): Boolean {
    if (emoji.isEmpty()) return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        emoji.codePoints().allMatch { codePoint ->
            Paint().hasGlyph(String(Character.toChars(codePoint)))
        }
    } else {
        emoji.length <= 4
    }
}

fun countryDisplayName(countryCode: String): String =
    CountryCatalog.getCountry(countryCode).displayName

@Composable
fun HeritageCountryFlags(
    primaryCountryCode: String,
    secondaryCountryCode: String?,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    if (secondaryCountryCode != null && secondaryCountryCode != primaryCountryCode) {
        androidx.compose.foundation.layout.Row(
            modifier = modifier,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountryFlag(countryCode = primaryCountryCode, size = size)
            CountryFlag(countryCode = secondaryCountryCode, size = size)
        }
    } else {
        CountryFlag(countryCode = primaryCountryCode, size = size, modifier = modifier)
    }
}
