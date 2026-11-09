package com.alaotach.eli5.assistant

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Region
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
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

    private val latinRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val devanagariRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    }

    private val chineseRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    private val japaneseRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }

    private val koreanRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }

    /**
     * Extracts text from a bitmap using a multi-script ML Kit cascade.
     * Supports Latin, Devanagari (Hindi, Marathi, Nepali), Chinese, Japanese, and Korean.
     *
     * @param bitmap Cropped screenshot of the selected area
     * @param path Optional selection polygon path to exclude text outside the drawn circle
     * @param preferredLanguage User's active app or explanation language to prioritize recognizers
     * @param onResult Callback delivering cleaned recognized text, or null if nothing legible found
     */
    fun extractText(
        bitmap: Bitmap,
        path: Path? = null,
        preferredLanguage: String? = null,
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

            val orderedRecognizers = getOrderedRecognizers(preferredLanguage)
            cascadeRecognition(image, pathRegion, orderedRecognizers, 0, null, onResult)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to create InputImage or run OCR", e)
            onResult(null)
        }
    }

    private fun getOrderedRecognizers(preferredLanguage: String?): List<Pair<String, TextRecognizer>> {
        val lang = preferredLanguage?.lowercase() ?: ""
        val primaryPair = when {
            lang.contains("hindi") || lang.contains("marathi") || lang.contains("nepali") ||
            lang.contains("sanskrit") || lang == "hi" || lang == "mr" || lang == "ne" -> {
                "Devanagari" to devanagariRecognizer
            }
            lang.contains("chinese") || lang.contains("mandarin") || lang == "zh" -> {
                "Chinese" to chineseRecognizer
            }
            lang.contains("japanese") || lang == "ja" -> {
                "Japanese" to japaneseRecognizer
            }
            lang.contains("korean") || lang == "ko" -> {
                "Korean" to koreanRecognizer
            }
            else -> {
                "Latin" to latinRecognizer
            }
        }

        val allPairs = listOf(
            "Latin" to latinRecognizer,
            "Devanagari" to devanagariRecognizer,
            "Japanese" to japaneseRecognizer,
            "Chinese" to chineseRecognizer,
            "Korean" to koreanRecognizer
        )

        return listOf(primaryPair) + allPairs.filter { it.first != primaryPair.first }
    }

    // Threshold score above which we are confident we've captured a substantial block of text
    private const val DECISIVE_SCORE_THRESHOLD = 30

    private fun cascadeRecognition(
        image: InputImage,
        pathRegion: Region?,
        recognizers: List<Pair<String, TextRecognizer>>,
        index: Int,
        currentBestText: String?,
        onResult: (String?) -> Unit
    ) {
        if (index >= recognizers.size) {
            Log.d(TAG, "Cascade complete. Final best text: '$currentBestText'")
            onResult(currentBestText)
            return
        }

        val (scriptName, recognizer) = recognizers[index]

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val parsedText = parseAndFilterText(visionText, pathRegion)
                val candidateScore = evaluateTextQuality(parsedText)
                val bestScore = evaluateTextQuality(currentBestText)

                Log.d(TAG, "Recognizer [$scriptName] found text: '$parsedText' (score=$candidateScore, bestScore=$bestScore)")

                val updatedBest = if (candidateScore > bestScore) parsedText else currentBestText

                // If candidateScore is very high (>= DECISIVE_SCORE_THRESHOLD: a complete paragraph or multi-sentence),
                // we have captured the full text and can return immediately.
                if (candidateScore >= DECISIVE_SCORE_THRESHOLD) {
                    onResult(parsedText)
                } else {
                    // Otherwise continue checking remaining recognizers in case another script is a much richer match
                    cascadeRecognition(image, pathRegion, recognizers, index + 1, updatedBest, onResult)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Recognizer [$scriptName] failed", e)
                cascadeRecognition(image, pathRegion, recognizers, index + 1, currentBestText, onResult)
            }
    }

    private fun parseAndFilterText(visionText: Text, pathRegion: Region?): String? {
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
            return collectedLines.joinToString("\n").trim()
        }

        // Fallback: If strict filtering stripped everything, check raw vision text
        val rawFallback = visionText.text.trim()
        return if (rawFallback.isNotBlank() && !isJunkLine(rawFallback)) {
            rawFallback
        } else {
            null
        }
    }

    /**
     * Scores the quality and significance of recognized text across all language scripts.
     * CJK ideographs/syllables carry high semantic density (1 char = 1 full word),
     * so they are given a higher per-character weighting.
     */
    private fun evaluateTextQuality(text: String?): Int {
        if (text.isNullOrBlank()) return 0

        var score = 0
        var letterCount = 0

        for (ch in text) {
            if (isCjkChar(ch)) {
                score += 3
                letterCount++
            } else if (Character.isLetter(ch)) {
                score += 1
                letterCount++
            } else if (Character.isDigit(ch)) {
                score += 0 // digits don't confirm language
            }
        }

        // Penalty if there are no actual linguistic letters
        if (letterCount == 0) return 0

        // Bonus for having multiple words or reasonable length
        if (text.contains(" ") || text.length > 5) {
            score += 2
        }

        return score
    }

    private fun isJunkLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return true

        // Single isolated character or edge fragment
        if (trimmed.length == 1) {
            val ch = trimmed[0]
            // Allow single CJK ideograph, Hiragana, Katakana, or Hangul (which represent complete words)
            if (!isCjkChar(ch)) {
                return true
            }
        }

        for (pattern in JUNK_PATTERNS) {
            if (pattern.matches(trimmed)) return true
        }
        return false
    }

    private fun isCjkChar(ch: Char): Boolean {
        val ub = Character.UnicodeBlock.of(ch) ?: return false
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
               ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
               ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
               ub == Character.UnicodeBlock.HIRAGANA ||
               ub == Character.UnicodeBlock.KATAKANA ||
               ub == Character.UnicodeBlock.HANGUL_SYLLABLES ||
               ub == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO ||
               ub == Character.UnicodeBlock.HANGUL_JAMO
    }
}
