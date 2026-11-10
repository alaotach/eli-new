package com.alaotach.eli5

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsHelper(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
            }
        }
    }

    fun speak(text: String, languageCode: String = "en") {
        if (!isInitialized || tts == null) return
        try {
            val locale = Locale.forLanguageTag(languageCode)
            tts?.language = locale
        } catch (_: Exception) {}
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "eli5_tts")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
