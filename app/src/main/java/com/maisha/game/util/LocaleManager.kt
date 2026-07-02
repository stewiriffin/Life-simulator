// app/src/main/java/com/maisha/game/util/LocaleManager.kt (modified — fr/pt/es/hi locales)
package com.maisha.game.util

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.maisha.game.R

data class SupportedLanguage(
    val code: String,
    @StringRes val labelRes: Int
)

object LocaleManager {

    const val LANG_EN = "en"
    const val LANG_SW = "sw"
    const val LANG_FR = "fr"
    const val LANG_PT = "pt"
    const val LANG_ES = "es"
    const val LANG_HI = "hi"

    val supportedLanguages: List<SupportedLanguage> = listOf(
        SupportedLanguage(LANG_EN, R.string.settings_language_english),
        SupportedLanguage(LANG_SW, R.string.settings_language_swahili),
        SupportedLanguage(LANG_FR, R.string.settings_language_french),
        SupportedLanguage(LANG_PT, R.string.settings_language_portuguese),
        SupportedLanguage(LANG_ES, R.string.settings_language_spanish),
        SupportedLanguage(LANG_HI, R.string.settings_language_hindi)
    )

    private val supportedCodes: Set<String> = supportedLanguages.map { it.code }.toSet()

    fun applyLocale(languageCode: String) {
        val tag = if (languageCode in supportedCodes) languageCode else LANG_EN
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    fun systemDefaultLanguage(): String {
        val systemLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        supportedLanguages.forEach { lang ->
            if (systemLang.startsWith(lang.code)) return lang.code
        }
        val default = java.util.Locale.getDefault().language
        return if (default in supportedCodes) default else LANG_EN
    }
}
