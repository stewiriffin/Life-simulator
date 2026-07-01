// app/src/main/java/com/maisha/game/util/LocaleManager.kt (new)
package com.maisha.game.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleManager {

    const val LANG_EN = "en"
    const val LANG_SW = "sw"

    fun applyLocale(languageCode: String) {
        val tag = when (languageCode) {
            LANG_SW -> LANG_SW
            else -> LANG_EN
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    fun systemDefaultLanguage(): String {
        val systemLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (systemLang.startsWith(LANG_SW)) return LANG_SW
        val default = java.util.Locale.getDefault().language
        return if (default == LANG_SW) LANG_SW else LANG_EN
    }
}
