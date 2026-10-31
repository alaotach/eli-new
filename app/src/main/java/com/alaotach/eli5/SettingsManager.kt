package com.alaotach.eli5

import android.content.Context

object SettingsManager {
    private const val KEY_THEME = "theme_preference"
    private const val PREFS_NAME = "eli5_settings"

    fun getTheme(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_THEME, "system") ?: "system"
    }

    fun setTheme(context: Context, theme: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme)
            .apply()
    }

    fun isDarkTheme(context: Context): Boolean {
        val theme = getTheme(context)
        if (theme == "dark") {
            return true
        }
        return theme != "light" && (context.resources.configuration.uiMode and 48) == 32
    }
}
