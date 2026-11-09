package com.alaotach.eli5.assistant

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class Eli5TileService : TileService() {

    override fun onClick() {
        super.onClick()

        if (Eli5AssistantManager.isDefaultAssistant(this)) {
            // Trigger Voice Assistant session directly
            val serviceIntent = Intent(this, Eli5VoiceInteractionService::class.java).apply {
                action = Eli5VoiceInteractionService.ACTION_TRIGGER_ASSISTANT
            }
            startService(serviceIntent)
        } else {
            // Open CircleSnipActivity (demo / fallback snip) or setup
            val intent = Intent(this, CircleSnipActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val isDefault = Eli5AssistantManager.isDefaultAssistant(this)
        tile.state = if (isDefault) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Circle to ELI5"
        tile.updateTile()
    }
}
