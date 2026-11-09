package com.alaotach.eli5.assistant

import android.content.Intent
import android.speech.RecognitionService

/**
 * Speech Recognition Service required by Android's VoiceInteractionManagerService
 * contract. Android requires digital assistants to provide a valid RecognitionService
 * component alongside VoiceInteractionService to be selectable in system Default Apps settings.
 */
class Eli5RecognitionService : RecognitionService() {

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        try {
            listener?.beginningOfSpeech()
        } catch (_: Exception) {}
    }

    override fun onStopListening(listener: Callback?) {
        try {
            listener?.endOfSpeech()
        } catch (_: Exception) {}
    }

    override fun onCancel(listener: Callback?) {
        // No-op cancellation
    }
}
