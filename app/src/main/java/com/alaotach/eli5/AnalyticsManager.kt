package com.alaotach.eli5

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsManager {
    @Volatile
    private var firebaseAnalytics: FirebaseAnalytics? = null

    private fun getInstance(context: Context): FirebaseAnalytics? {
        if (firebaseAnalytics == null) {
            synchronized(this) {
                if (firebaseAnalytics == null) {
                    try {
                        firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
                    } catch (_: Exception) {}
                }
            }
        }
        return firebaseAnalytics
    }

    fun logEvent(context: Context, eventName: String, params: Bundle? = null) {
        try {
            getInstance(context)?.logEvent(eventName, params)
        } catch (_: Exception) {}
    }

    fun logExplainRequest(context: Context, mode: String, language: String, isSnip: Boolean, length: Int) {
        val bundle = Bundle().apply {
            putString("mode", mode)
            putString("language", language)
            putBoolean("is_circle_to_search", isSnip)
            putInt("text_length", length)
        }
        logEvent(context, "explain_requested", bundle)
    }

    fun logExplainSuccess(context: Context, mode: String, language: String, isCached: Boolean) {
        val bundle = Bundle().apply {
            putString("mode", mode)
            putString("language", language)
            putBoolean("is_cached", isCached)
        }
        logEvent(context, "explain_success", bundle)
    }

    fun logLanguageChanged(context: Context, language: String, origin: String) {
        val bundle = Bundle().apply {
            putString("selected_language", language)
            putString("origin", origin)
        }
        logEvent(context, "language_changed", bundle)
    }

    fun logCircleGesture(context: Context) {
        logEvent(context, "circle_gesture_triggered")
    }

    fun logCopyResult(context: Context, mode: String, language: String) {
        val bundle = Bundle().apply {
            putString("mode", mode)
            putString("language", language)
        }
        logEvent(context, "copy_result", bundle)
    }

    fun logShareCard(context: Context, mode: String, language: String) {
        val bundle = Bundle().apply {
            putString("mode", mode)
            putString("language", language)
        }
        logEvent(context, "share_card", bundle)
    }
}
