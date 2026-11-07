package com.alaotach.eli5

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "eli5_settings"
    private const val KEY_THEME = "theme_preference"
    private const val KEY_DEFAULT_MODE = "default_mode"
    private const val KEY_LAST_MODE = "last_used_mode"
    private const val KEY_LANGUAGE = "explanation_language"
    private const val KEY_PROOFREAD_TONE = "proofread_default_tone"
    private const val KEY_HISTORY_ENABLED = "history_enabled"

    const val PRIVACY_URL = "https://github.com/alaotach/eli-new/blob/main/PRIVACY.md"

    fun getTheme(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, "system") ?: "system"
    }

    fun setTheme(context: Context, theme: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme)
            .apply()
    }

    fun isDarkTheme(context: Context): Boolean {
        val theme = getTheme(context)
        if (theme == "dark") return true
        if (theme == "light") return false
        return (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    fun getDefaultMode(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DEFAULT_MODE, "ELI5") ?: "ELI5"
    }

    fun setDefaultMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DEFAULT_MODE, mode)
            .apply()
    }

    fun getLastMode(context: Context): String {
        val defaultM = getDefaultMode(context)
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_MODE, defaultM) ?: defaultM
    }

    fun setLastMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_MODE, mode)
            .apply()
    }

    fun getLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "English") ?: "English"
    }

    fun setLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    fun getProofreadDefaultTone(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROOFREAD_TONE, "Casual") ?: "Casual"
    }

    fun setProofreadDefaultTone(context: Context, tone: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROOFREAD_TONE, tone)
            .apply()
    }

    fun isHistoryEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HISTORY_ENABLED, true)
    }

    fun setHistoryEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HISTORY_ENABLED, enabled)
            .apply()
    }
}
