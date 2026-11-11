package com.alaotach.eli5

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

data class Language(
    val code: String,
    val englishName: String,
    val nativeName: String,
    val flag: String
) {
    val displayName: String
        get() = if (englishName.equals(nativeName, ignoreCase = true)) englishName else "$englishName ($nativeName)"
}

object LanguageManager {
    val SUPPORTED_LANGUAGES = listOf(
        Language("en", "English", "English", "🇺🇸"),
        Language("hi", "Hindi", "हिन्दी", "🇮🇳"),
        Language("hi-Latn", "Hinglish", "Hinglish", "🇮🇳"),
        Language("es", "Spanish", "Español", "🇪🇸"),
        Language("fr", "French", "Français", "🇫🇷"),
        Language("de", "German", "Deutsch", "🇩🇪"),
        Language("ja", "Japanese", "日本語", "🇯🇵"),
        Language("zh", "Chinese", "简体中文", "🇨🇳"),
        Language("ar", "Arabic", "العربية", "🇸🇦"),
        Language("pt", "Portuguese", "Português", "🇧🇷"),
        Language("ru", "Russian", "Русский", "🇷🇺"),
        Language("bn", "Bengali", "বাংলা", "🇮🇳"),
        Language("mr", "Marathi", "मराठी", "🇮🇳"),
        Language("te", "Telugu", "తెలుగు", "🇮🇳"),
        Language("ta", "Tamil", "தமிழ்", "🇮🇳"),
        Language("gu", "Gujarati", "ગુજરાતી", "🇮🇳"),
        Language("kn", "Kannada", "ಕನ್ನಡ", "🇮🇳"),
        Language("ml", "Malayalam", "മലയാളം", "🇮🇳"),
        Language("pa", "Punjabi", "ਪੰਜਾਬੀ", "🇮🇳"),
        Language("ur", "Urdu", "اردو", "🇵🇰"),
        Language("ko", "Korean", "한국어", "🇰🇷"),
        Language("it", "Italian", "Italiano", "🇮🇹"),
        Language("tr", "Turkish", "Türkçe", "🇹🇷"),
        Language("id", "Indonesian", "Bahasa Indonesia", "🇮🇩"),
        Language("vi", "Vietnamese", "Tiếng Việt", "🇻🇳"),
        Language("nl", "Dutch", "Nederlands", "🇳🇱"),
        Language("pl", "Polish", "Polski", "🇵🇱"),
        Language("th", "Thai", "ไทย", "🇹🇭"),
        Language("tl", "Filipino", "Tagalog", "🇵🇭"),
        Language("fa", "Persian", "فارسی", "🇮🇷"),
        Language("el", "Greek", "Ελληνικά", "🇬🇷"),
        Language("he", "Hebrew", "עברית", "🇮🇱"),
        Language("sv", "Swedish", "Svenska", "🇸🇪"),
        Language("uk", "Ukrainian", "Українська", "🇺🇦"),
        Language("ms", "Malay", "Bahasa Melayu", "🇲🇾"),
        Language("cs", "Czech", "Čeština", "🇨🇿")
    )

    fun getLanguageByEnglishName(name: String): Language {
        return SUPPORTED_LANGUAGES.find { it.englishName.equals(name, ignoreCase = true) }
            ?: SUPPORTED_LANGUAGES.first()
    }

    fun getLanguageByCode(code: String): Language {
        return SUPPORTED_LANGUAGES.find { it.code.equals(code, ignoreCase = true) }
            ?: SUPPORTED_LANGUAGES.first()
    }

    fun findLanguage(identifier: String): Language {
        val trimmed = identifier.trim()
        return SUPPORTED_LANGUAGES.find {
            it.code.equals(trimmed, ignoreCase = true) ||
            it.englishName.equals(trimmed, ignoreCase = true) ||
            it.nativeName.equals(trimmed, ignoreCase = true)
        } ?: getLanguageByEnglishName(identifier)
    }

    fun searchLanguages(query: String): List<Language> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return SUPPORTED_LANGUAGES
        return SUPPORTED_LANGUAGES.filter {
            it.englishName.lowercase().contains(q) ||
            it.nativeName.lowercase().contains(q) ||
            it.code.lowercase().contains(q)
        }
    }

    fun applyAppLocale(languageCode: String) {
        applyAppLocale(null, languageCode)
    }

    fun applyAppLocale(context: Context?, languageCode: String) {
        val locale = java.util.Locale.forLanguageTag(languageCode)
        try {
            java.util.Locale.setDefault(locale)
        } catch (_: Exception) {}
        try {
            val localeList = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(localeList)
        } catch (_: Exception) {}
        context?.let { ctx ->
            try {
                val res = ctx.resources
                val config = res.configuration
                config.setLocale(locale)
                config.setLayoutDirection(locale)
                res.updateConfiguration(config, res.displayMetrics)
            } catch (_: Exception) {}
        }
    }
}
