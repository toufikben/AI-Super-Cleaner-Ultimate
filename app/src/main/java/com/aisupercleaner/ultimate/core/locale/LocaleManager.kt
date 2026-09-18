package com.aisupercleaner.ultimate.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleManager {

    val supportedLanguages = listOf(
        Language("system", "System", ""),
        Language("en", "English", "US"),
        Language("ar", "العربية", "SA"),
        Language("fr", "Français", "FR"),
        Language("es", "Español", "ES"),
        Language("de", "Deutsch", "DE"),
        Language("tr", "Türkçe", "TR"),
        Language("hi", "हिन्दी", "IN"),
        Language("id", "Bahasa Indonesia", "ID"),
        Language("it", "Italiano", "IT"),
        Language("pt-BR", "Português (Brasil)", "BR"),
        Language("ru", "Русский", "RU"),
        Language("ur", "اردو", "PK"),
    )

    fun applyLanguage(languageCode: String) {
        val locales = if (languageCode == "system") LocaleListCompat.getEmptyLocaleList()
        else LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun currentLanguageTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) "system" else locales.toLanguageTags()
    }

    data class Language(val code: String, val displayName: String, val region: String)
}
