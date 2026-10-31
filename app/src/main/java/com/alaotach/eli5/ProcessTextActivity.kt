package com.alaotach.eli5

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class ProcessTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra("copy_text")) {
            val copyText = intent.getStringExtra("copy_text")
            val toastMsg = intent.getStringExtra("toast_msg") ?: "Explanation copied"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("ELI5 Copy", copyText))
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val text = intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        if (!text.isNullOrBlank()) {
            val overlayIntent = Intent(this, ExplainOverlayActivity::class.java).apply {
                putExtra(ExplainOverlayActivity.EXTRA_TEXT, text)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(overlayIntent)
        }
        finish()
    }
}
