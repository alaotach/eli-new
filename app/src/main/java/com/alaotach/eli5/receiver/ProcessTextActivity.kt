package com.alaotach.eli5.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import com.alaotach.eli5.service.OverlayService
import com.alaotach.eli5.util.Constants

class ProcessTextActivity : Activity() {

    private val replaceTextReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val replacementText = intent?.getStringExtra(Constants.EXTRA_REPLACEMENT_TEXT)
            if (replacementText != null) {
                val resultIntent = Intent()
                resultIntent.putExtra(Intent.EXTRA_PROCESS_TEXT, replacementText)
                setResult(RESULT_OK, resultIntent)
            }
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        } else {
            null
        }

        val isReadOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)

        if (text != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    replaceTextReceiver,
                    IntentFilter(Constants.ACTION_REPLACE_TEXT),
                    RECEIVER_NOT_EXPORTED
                )
            } else {
                registerReceiver(replaceTextReceiver, IntentFilter(Constants.ACTION_REPLACE_TEXT))
            }

            val targetMode = if (intent.component?.className?.contains("ProcessTextProofreadActivity") == true) {
                "Proofread"
            } else {
                "ELI5"
            }

            val serviceIntent = Intent(this, OverlayService::class.java).apply {
                putExtra(Constants.EXTRA_ORIGINAL_TEXT, text.toString())
                putExtra(Constants.EXTRA_IS_READ_ONLY, isReadOnly)
                putExtra(Constants.EXTRA_MODE, targetMode)
            }
            startService(serviceIntent)
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(replaceTextReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
}
