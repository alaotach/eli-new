package com.alaotach.eli5.assistant

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Region
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object OcrHelper {
    private const val TAG = "OcrHelper"

    // Patterns for peripheral screen noise / accidental artifacts
    private val JUNK_PATTERNS = listOf(
        // Timestamps (e.g., "12:44 PM", "4:40", "09:15 am")
        Regex("""^\d{1,2}:\d{2}(\s*(AM|PM|am|pm))?$"""),
        // Relative times (e.g., "2h", "5m", "14h ago", "1d ago", "3wks")
        Regex("""^\d+\s*(s|sec|secs|m|min|mins|h|hr|hrs|d|day|days|w|wk|wks|mo|yr|y)(\s+ago)?$""", RegexOption.IGNORE_CASE),
        // Relative dates
        Regex("""^(Yesterday|Today|Tomorrow|Just now)$""", RegexOption.IGNORE_CASE),
        // Mobile network / status bar / battery
        Regex("""^(5G|4G|3G|2G|LTE|VoLTE|Wi-Fi|WiFi|\d{1,3}%)$""", RegexOption.IGNORE_CASE),
        // Day date strings (e.g. "Mon, Nov 9", "Sunday, 12 Oct")
        Regex("""^(Mon|Tue|Wed|Thu|Fri|Sat|Sun)[a-z]*,?\s+[A-Za-z]+\s+\d{1,2}$""", RegexOption.IGNORE_CASE),
        // Isolated social media handles (e.g., "@elonmusk")
        Regex("""^@[\w.-]+$"""),
        // Social media counts (e.g., "4.5k Likes", "1.2M views", "340 retweets")
        Regex("""^\d+(\.\d+)?[kKmMbB]?\s*(views|likes|comments|retweets|shares|replies|upvotes)?$""", RegexOption.IGNORE_CASE),
        // Standalone UI buttons & navigation labels
        Regex("""^(Like|Likes|Reply|Replies|Retweet|Retweets|Comment|Comments|Share|Shares|Follow|Following|Subscribed?|Save|Saved)$""", RegexOption.IGNORE_CASE),
        Regex("""^(Home|Search|Explore|Notifications|Messages|Profile|Settings|Menu|Send|Cancel|Done|Edit|Delete|Report|Hide|Copy|Paste)$""", RegexOption.IGNORE_CASE),
        // Punctuation dividers and standalone bullets (e.g., "•", "---", "|", "..")
        Regex("""^[•·|\s.,:;*#<>^/\\_\-–—]+$""")
    )

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Extracts text from a bitmap using ML Kit.
     * If a selection [path] is provided, only text whose bounding box centers fall inside
     * the path polygon are retained, eliminating corner artifacts outside the circle.
     * Also automatically filters out incidental noise (timestamps, status icons, buttons).
     */
    fun extractText(
        bitmap: Bitmap,
        path: Path? = null,
        onResult: (String?) -> Unit
    ) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)

            // Prepare region for polygon containment check if path is present
            val pathRegion = if (path != null) {
                try {
                    val closed = Path(path).apply { close() }
                    val r = Region()
                    r.setPath(closed, Region(0, 0, bitmap.width, bitmap.height))
                    if (!r.isEmpty) r else null
                } catch (e: Exception) {
                    null
                }
            } else null

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val collectedLines = mutableListOf<String>()

                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val lineBox = line.boundingBox

                            // 1. Polygon containment filter: drop text outside the user's drawn circle
                            if (pathRegion != null && lineBox != null) {
                                val centerX = lineBox.centerX()
                                val centerY = lineBox.centerY()
                                if (!pathRegion.contains(centerX, centerY)) {
                                    continue // Outside the drawn circle/polygon
                                }
                            }

                            val rawLineText = line.text.trim()
                            if (rawLineText.isBlank()) continue

                            // 2. Junk / noise filter: eliminate timestamps, status bar, and buttons
                            if (isJunkLine(rawLineText)) {
                                Log.d(TAG, "Filtered out noise line: '$rawLineText'")
                                continue
                            }

                            collectedLines.add(rawLineText)
                        }
                    }

                    if (collectedLines.isNotEmpty()) {
                        val cleanText = collectedLines.joinToString("\n").trim()
                        Log.d(TAG, "Final cleaned text: '$cleanText'")
                        onResult(cleanText)
                    } else {
                        // Fallback: If strict filtering stripped everything, check raw vision text
                        val rawFallback = visionText.text.trim()
                        if (rawFallback.isNotBlank()) {
                            onResult(rawFallback)
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

    private fun isJunkLine(line: String): Boolean {
        val trimmed = line.trim()
        // Single isolated character or edge fragment
        if (trimmed.length <= 1) return true

        for (pattern in JUNK_PATTERNS) {
            if (pattern.matches(trimmed)) return true
        }
        return false
    }
}
