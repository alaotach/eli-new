package com.alaotach.eli5.assistant

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

object Eli5AssistantManager {
    private const val TAG = "Eli5AssistantManager"

    /**
     * Checks if ELI5 is currently configured as the default digital assistant.
     */
    fun isDefaultAssistant(context: Context): Boolean {
        try {
            // Check via RoleManager on Android 10+ (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                        return true
                    }
                }
            }

            // Fallback check via Settings.Secure "assistant" string
            val assistantSetting = Settings.Secure.getString(context.contentResolver, "assistant")
            if (assistantSetting != null && assistantSetting.contains(context.packageName)) {
                return true
            }

            // Check voice_interaction_service setting
            val voiceInteractionSetting = Settings.Secure.getString(context.contentResolver, "voice_interaction_service")
            if (voiceInteractionSetting != null && voiceInteractionSetting.contains(context.packageName)) {
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking default assistant status", e)
        }
        return false
    }

    /**
     * Opens system settings so the user can easily set ELI5 as their Digital Assistant.
     */
    fun openAssistantSettings(context: Context) {
        // Priority 1: Direct assistant settings
        val directIntent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (canResolve(context, directIntent)) {
            context.startActivity(directIntent)
            return
        }

        // Priority 2: Manage default apps settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val manageDefaultIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (canResolve(context, manageDefaultIntent)) {
                context.startActivity(manageDefaultIntent)
                return
            }
        }

        // Priority 3: General app settings
        val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(appSettingsIntent)
    }

    private fun canResolve(context: Context, intent: Intent): Boolean {
        return intent.resolveActivity(context.packageManager) != null
    }
}
