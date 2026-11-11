package com.alaotach.eli5

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsHelper(context: Context) {
    private var tts: TextToSpeech? = null
    var isInitialized = false
        private set
    private val mainHandler = Handler(Looper.getMainLooper())
    var isSpeaking = false
        private set
    private var stateListener: ((Boolean) -> Unit)? = null
    private val initListeners = mutableListOf<() -> Unit>()

    companion object {
        // Languages officially supported by Google Speech Services / Text-to-Speech
        // Persian (fa) is excluded by default as Google TTS does not supply a voice package for it.
        val KNOWN_GOOGLE_TTS_CODES = setOf(
            "en", "hi", "es", "fr", "de", "ja", "zh", "ar", "pt", "ru",
            "bn", "mr", "te", "ta", "gu", "kn", "ml", "pa", "ur", "ko",
            "it", "tr", "id", "vi", "nl", "pl", "th", "tl", "el", "he",
            "sv", "uk", "ms", "cs"
        )
    }

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
                mainHandler.post {
                    val listenersCopy = ArrayList(initListeners)
                    listenersCopy.forEach { it.invoke() }
                }
            }
        }
    }

    fun addInitListener(listener: () -> Unit) {
        if (isInitialized) {
            listener()
        } else {
            initListeners.add(listener)
        }
    }

    fun removeInitListener(listener: () -> Unit) {
        initListeners.remove(listener)
    }

    fun setStateListener(listener: ((Boolean) -> Unit)?) {
        this.stateListener = listener
    }

    /**
     * Resolves the given identifier (name or code) to candidate Locales.
     * Hinglish maps to Indian English or English.
     */
    fun getCandidateLocales(languageCodeOrName: String): List<Locale> {
        val langObj = LanguageManager.findLanguage(languageCodeOrName)
        val code = langObj.code
        val clean = if (code.equals("hi-Latn", ignoreCase = true)) "en" else code.lowercase().trim()
        return when (clean) {
            "en" -> listOf(Locale.forLanguageTag("en-US"), Locale.forLanguageTag("en-IN"), Locale.ENGLISH)
            "hi" -> listOf(Locale.forLanguageTag("hi-IN"), Locale.forLanguageTag("hi"))
            "he" -> listOf(Locale.forLanguageTag("he-IL"), Locale.forLanguageTag("iw-IL"), Locale.forLanguageTag("he"), Locale.forLanguageTag("iw"))
            "id" -> listOf(Locale.forLanguageTag("id-ID"), Locale.forLanguageTag("in-ID"), Locale.forLanguageTag("id"), Locale.forLanguageTag("in"))
            "tl" -> listOf(Locale.forLanguageTag("fil-PH"), Locale.forLanguageTag("tl-PH"), Locale.forLanguageTag("fil"), Locale.forLanguageTag("tl"))
            "zh" -> listOf(Locale.SIMPLIFIED_CHINESE, Locale.CHINA, Locale.CHINESE, Locale.forLanguageTag("zh-CN"))
            "bn" -> listOf(Locale.forLanguageTag("bn-IN"), Locale.forLanguageTag("bn-BD"), Locale.forLanguageTag("bn"))
            "mr" -> listOf(Locale.forLanguageTag("mr-IN"), Locale.forLanguageTag("mr"))
            "te" -> listOf(Locale.forLanguageTag("te-IN"), Locale.forLanguageTag("te"))
            "ta" -> listOf(Locale.forLanguageTag("ta-IN"), Locale.forLanguageTag("ta"))
            "gu" -> listOf(Locale.forLanguageTag("gu-IN"), Locale.forLanguageTag("gu"))
            "kn" -> listOf(Locale.forLanguageTag("kn-IN"), Locale.forLanguageTag("kn"))
            "ml" -> listOf(Locale.forLanguageTag("ml-IN"), Locale.forLanguageTag("ml"))
            "pa" -> listOf(Locale.forLanguageTag("pa-IN"), Locale.forLanguageTag("pa"))
            "ur" -> listOf(Locale.forLanguageTag("ur-PK"), Locale.forLanguageTag("ur-IN"), Locale.forLanguageTag("ur"))
            "fa" -> listOf(Locale.forLanguageTag("fa-IR"), Locale.forLanguageTag("fa"))
            "ar" -> listOf(Locale.forLanguageTag("ar-SA"), Locale.forLanguageTag("ar"))
            "pt" -> listOf(Locale.forLanguageTag("pt-BR"), Locale.forLanguageTag("pt-PT"), Locale.forLanguageTag("pt"))
            "es" -> listOf(Locale.forLanguageTag("es-ES"), Locale.forLanguageTag("es-US"), Locale.forLanguageTag("es"))
            "fr" -> listOf(Locale.forLanguageTag("fr-FR"), Locale.FRENCH)
            "de" -> listOf(Locale.forLanguageTag("de-DE"), Locale.GERMAN)
            "ja" -> listOf(Locale.JAPAN, Locale.JAPANESE)
            "ko" -> listOf(Locale.KOREA, Locale.KOREAN)
            "it" -> listOf(Locale.ITALY, Locale.ITALIAN)
            "ru" -> listOf(Locale.forLanguageTag("ru-RU"), Locale.forLanguageTag("ru"))
            "tr" -> listOf(Locale.forLanguageTag("tr-TR"), Locale.forLanguageTag("tr"))
            "vi" -> listOf(Locale.forLanguageTag("vi-VN"), Locale.forLanguageTag("vi"))
            "nl" -> listOf(Locale.forLanguageTag("nl-NL"), Locale.forLanguageTag("nl"))
            "pl" -> listOf(Locale.forLanguageTag("pl-PL"), Locale.forLanguageTag("pl"))
            "th" -> listOf(Locale.forLanguageTag("th-TH"), Locale.forLanguageTag("th"))
            "el" -> listOf(Locale.forLanguageTag("el-GR"), Locale.forLanguageTag("el"))
            "sv" -> listOf(Locale.forLanguageTag("sv-SE"), Locale.forLanguageTag("sv"))
            "uk" -> listOf(Locale.forLanguageTag("uk-UA"), Locale.forLanguageTag("uk"))
            "ms" -> listOf(Locale.forLanguageTag("ms-MY"), Locale.forLanguageTag("ms"))
            "cs" -> listOf(Locale.forLanguageTag("cs-CZ"), Locale.forLanguageTag("cs"))
            else -> listOf(Locale.forLanguageTag(clean))
        }
    }

    /**
     * Determines whether the active TextToSpeech engine can speak the requested language.
     * If the engine is ready, checks availability against installed voices.
     * Otherwise falls back to known Google TTS supported language codes.
     */
    fun isLanguageSupported(languageCodeOrName: String): Boolean {
        val langObj = LanguageManager.findLanguage(languageCodeOrName)
        val code = langObj.code
        val clean = if (code.equals("hi-Latn", ignoreCase = true)) "en" else code.lowercase().trim()

        val engine = tts
        if (isInitialized && engine != null) {
            try {
                val candidates = getCandidateLocales(code)
                val available = engine.availableLanguages
                if (!available.isNullOrEmpty()) {
                    val matched = candidates.any { cand ->
                        available.any { avail ->
                            avail.language.equals(cand.language, ignoreCase = true)
                        }
                    }
                    if (matched) return true
                }
                for (candidate in candidates) {
                    val res = engine.isLanguageAvailable(candidate)
                    if (res >= TextToSpeech.LANG_AVAILABLE) {
                        return true
                    }
                }
                return false
            } catch (_: Exception) {}
        }
        return KNOWN_GOOGLE_TTS_CODES.contains(clean)
    }

    fun getBestLocale(languageCodeOrName: String): Locale {
        val candidates = getCandidateLocales(languageCodeOrName)
        val engine = tts
        if (engine != null && isInitialized) {
            for (candidate in candidates) {
                try {
                    val res = engine.isLanguageAvailable(candidate)
                    if (res >= TextToSpeech.LANG_AVAILABLE) {
                        return candidate
                    }
                } catch (_: Exception) {}
            }
        }
        return candidates.firstOrNull() ?: Locale.getDefault()
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
            val targetLocale = getBestLocale(languageCode)
            val res = tts?.setLanguage(targetLocale)
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
        initListeners.clear()
    }
}
