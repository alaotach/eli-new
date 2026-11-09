package com.alaotach.eli5.assistant

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object OcrHelper {
    private const val TAG = "OcrHelper"

    // On-device ML Kit text recognizer (runs locally, free, fast, 100% Play Store compliant)
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Extracts text from a cropped or full-screen bitmap using ML Kit.
     * Returns trimmed text on success, or null if no readable text was detected.
     */
    fun extractText(bitmap: Bitmap, onResult: (String?) -> Unit) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val rawText = visionText.text.trim()
                    if (rawText.isNotBlank()) {
                        Log.d(TAG, "Extracted text length: ${rawText.length}")
                        onResult(rawText)
                    } else {
                        // Sometimes text blocks have text while root visionText.text is whitespace
                        val sb = StringBuilder()
                        for (block in visionText.textBlocks) {
                            if (block.text.isNotBlank()) {
                                if (sb.isNotEmpty()) sb.append("\n")
                                sb.append(block.text.trim())
                            }
                        }
                        val combined = sb.toString().trim()
                        if (combined.isNotBlank()) {
                            onResult(combined)
                        } else {
                            onResult(null)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "OCR Recognition failed", e)
                    onResult(null)
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to create InputImage or run OCR", e)
            onResult(null)
        }
    }
}
