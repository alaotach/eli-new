package com.alaotach.eli5.assistant

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import com.alaotach.eli5.ExplainOverlayActivity
import com.alaotach.eli5.SettingsManager

class Eli5VoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    private lateinit var selectionView: CircleSelectionView

    override fun onCreate() {
        super.onCreate()

        // Configure full-screen translucent window layout
        val window = window?.window
        if (window != null) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            val lp = window.attributes
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
            lp.gravity = Gravity.FILL
            window.attributes = lp
        }
    }

    override fun onCreateContentView(): View {
        val root = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        selectionView = CircleSelectionView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            onSelectionCompleted = { croppedBitmap, _ ->
                processCroppedBitmap(croppedBitmap)
            }

            onDismissRequested = {
                hide()
            }
        }

        root.addView(selectionView)
        return root
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)
        if (screenshot != null) {
            selectionView.setScreenshot(screenshot)
        } else {
            Toast.makeText(context, "Cannot capture protected screen", Toast.LENGTH_SHORT).show()
            hide()
        }
    }

    override fun onHide() {
        super.onHide()
        if (::selectionView.isInitialized) {
            selectionView.resetSelection()
        }
    }

    private fun processCroppedBitmap(croppedBitmap: Bitmap) {
        OcrHelper.extractText(croppedBitmap) { extractedText ->
            if (!extractedText.isNullOrBlank()) {
                // Launch ExplainOverlayActivity with extracted text
                val intent = Intent(context, ExplainOverlayActivity::class.java).apply {
                    putExtra(ExplainOverlayActivity.EXTRA_TEXT, extractedText)
                    putExtra(ExplainOverlayActivity.EXTRA_MODE, SettingsManager.getDefaultMode(context))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(intent)
                hide()
            } else {
                Toast.makeText(
                    context,
                    "No text found. Circle closer to the text.",
                    Toast.LENGTH_SHORT
                ).show()
                selectionView.resetSelection()
            }
        }
    }
}
