package com.alaotach.eli5

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsHelper(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())
    var isSpeaking = false
        private set
    private var stateListener: ((Boolean) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                        mainHandler.post { stateListener?.invoke(true) }
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        mainHandler.post { stateListener?.invoke(false) }
                    }

                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        mainHandler.post { stateListener?.invoke(false) }
                    }
                })
            }
        }
    }

    fun setStateListener(listener: ((Boolean) -> Unit)?) {
        this.stateListener = listener
    }

    fun toggle(text: String, languageCode: String = "en", onStateChange: (Boolean) -> Unit): Boolean {
        this.stateListener = onStateChange
        if (isSpeaking) {
            stop()
            onStateChange(false)
            return false
        } else {
            speak(text, languageCode)
            onStateChange(true)
            return true
        }
    }

    fun speak(text: String, languageCode: String = "en") {
        if (!isInitialized || tts == null) return
        try {
            val resolvedTag = if (languageCode.equals("hi-Latn", ignoreCase = true)) "en" else languageCode
            val locale = Locale.forLanguageTag(resolvedTag)
            val res = tts?.setLanguage(locale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.getDefault()
            }
        } catch (_: Exception) {
            tts?.language = Locale.getDefault()
        }
        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "eli5_utterance_${System.currentTimeMillis()}")
        isSpeaking = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "eli5_utterance")
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        isSpeaking = false
        mainHandler.post { stateListener?.invoke(false) }
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
