package com.alaotach.eli5.assistant

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.util.Log

class Eli5VoiceInteractionService : VoiceInteractionService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TRIGGER_ASSISTANT) {
            triggerAssistSession()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun triggerAssistSession() {
        try {
            val args = Bundle()
            val sessionFlags = VoiceInteractionSession.SHOW_WITH_ASSIST or VoiceInteractionSession.SHOW_WITH_SCREENSHOT
            showSession(args, sessionFlags)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show assistant session", e)
        }
    }

    companion object {
        private const val TAG = "Eli5VoiceInteractionService"
        const val ACTION_TRIGGER_ASSISTANT = "com.alaotach.eli5.action.TRIGGER_ASSISTANT"
    }
}
