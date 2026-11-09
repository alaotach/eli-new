package com.alaotach.eli5.assistant

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Region
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.alaotach.eli5.BuildConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

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

    // Threshold score above which we are confident we've captured a substantial block of text
    private const val DECISIVE_SCORE_THRESHOLD = 30

    /**
     * Extracts text from a bitmap using a hybrid OCR pipeline:
     * 1. Multi-script on-device ML Kit recognizers (Latin, Devanagari, Chinese, Japanese, Korean)
     * 2. High-accuracy Cloud Vision OCR fallback for scripts not covered on-device (Bengali, Tamil, Telugu,
     *    Gujarati, Kannada, Malayalam, Punjabi, Arabic, Urdu, Persian, Hebrew, Russian, Ukrainian, Greek, Thai).
     *
     * Supports all 35 languages in ELI5 with zero gaps.
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

            // If the user's active language uses a script not supported offline by ML Kit (e.g. Bengali, Tamil, Arabic),
            // prioritize Cloud Vision OCR directly for instantaneous recognition.
            if (isCloudPreferredScript(preferredLanguage)) {
                Log.d(TAG, "Preferred language '$preferredLanguage' is a cloud-primary script. Launching Cloud OCR...")
                performCloudOcr(bitmap, preferredLanguage) { cloudText ->
                    if (!cloudText.isNullOrBlank()) {
                        onResult(cloudText)
                    } else {
                        // Fallback to on-device cascade in case user is offline
                        runOnDeviceCascade(bitmap, pathRegion, preferredLanguage, onResult)
                    }
                }
                return
            }

            // For Latin, Devanagari (Hindi/Marathi), Chinese, Japanese, Korean:
            // Run fast on-device recognition first (15-30ms), with cloud fallback if no text found.
            runOnDeviceCascade(bitmap, pathRegion, preferredLanguage, onResult)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to run OCR", e)
            onResult(null)
        }
    }

    private fun runOnDeviceCascade(
        bitmap: Bitmap,
        pathRegion: Region?,
        preferredLanguage: String?,
        onResult: (String?) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val orderedRecognizers = getOrderedRecognizers(preferredLanguage)
        cascadeRecognition(bitmap, image, pathRegion, orderedRecognizers, 0, null, preferredLanguage, onResult)
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

    private fun cascadeRecognition(
        bitmap: Bitmap,
        image: InputImage,
        pathRegion: Region?,
        recognizers: List<Pair<String, TextRecognizer>>,
        index: Int,
        currentBestText: String?,
        preferredLanguage: String?,
        onResult: (String?) -> Unit
    ) {
        if (index >= recognizers.size) {
            Log.d(TAG, "On-device cascade complete. Best text: '$currentBestText'")
            val bestScore = evaluateTextQuality(currentBestText)

            // If on-device models found no text, very low score (< 8), or garbled pseudo-Latin,
            // this is a non-Latin script like Russian, Ukrainian, Bulgarian, Bengali, Tamil, Telugu, Arabic, etc.
            // Seamlessly run Cloud OCR fallback!
            if (bestScore < 8 || isLikelyGarbledLatin(currentBestText)) {
                Log.d(TAG, "On-device score is low ($bestScore) or garbled. Triggering Cloud OCR fallback for Russian/Cyrillic/Indic/etc...")
                performCloudOcr(bitmap, preferredLanguage) { cloudText ->
                    if (!cloudText.isNullOrBlank()) {
                        Log.d(TAG, "Cloud OCR fallback successfully extracted: '$cloudText'")
                        onResult(cloudText)
                    } else {
                        onResult(currentBestText)
                    }
                }
            } else {
                onResult(currentBestText)
            }
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

                // If candidateScore is high (>= DECISIVE_SCORE_THRESHOLD), verify whether it's genuine Latin
                // or garbled pseudo-Latin produced by Latin OCR on Russian/Cyrillic text.
                if (candidateScore >= DECISIVE_SCORE_THRESHOLD) {
                    if (isLikelyGarbledLatin(parsedText)) {
                        Log.d(TAG, "Recognizer [$scriptName] produced garbled pseudo-Latin from Russian/Cyrillic. Invoking Cloud OCR fallback...")
                        performCloudOcr(bitmap, preferredLanguage) { cloudText ->
                            if (!cloudText.isNullOrBlank()) {
                                Log.d(TAG, "Cloud OCR fallback successfully resolved Russian/Cyrillic text: '$cloudText'")
                                onResult(cloudText)
                            } else {
                                onResult(parsedText)
                            }
                        }
                    } else {
                        onResult(parsedText)
                    }
                } else {
                    // Otherwise continue checking remaining recognizers in case another script is a much richer match
                    cascadeRecognition(bitmap, image, pathRegion, recognizers, index + 1, updatedBest, preferredLanguage, onResult)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Recognizer [$scriptName] failed", e)
                cascadeRecognition(bitmap, image, pathRegion, recognizers, index + 1, currentBestText, preferredLanguage, onResult)
            }
    }

    /**
     * Performs Cloud Vision OCR using Gemini via the ELI5 backend proxy.
     * Accurately transcribes all 35 languages including Bengali, Telugu, Tamil, Gujarati,
     * Kannada, Malayalam, Punjabi, Arabic, Urdu, Persian, Hebrew, Russian, Ukrainian, Greek, Thai.
     */
    private fun performCloudOcr(
        bitmap: Bitmap,
        preferredLanguage: String?,
        onResult: (String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val extractedText: String? = try {
                Log.d(TAG, "performCloudOcr starting for bitmap ${bitmap.width}x${bitmap.height}, preferredLang=$preferredLanguage")
                val scaled = if (bitmap.width > 800 || bitmap.height > 800) {
                    val scale = 800f / maxOf(bitmap.width, bitmap.height)
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt().coerceAtLeast(1),
                        (bitmap.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    bitmap
                }

                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                Log.d(TAG, "performCloudOcr: image compressed, base64 length=${base64Image.length}")

                val endpoint = "${BuildConfig.BACKEND_URL}/api/ocr"
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 ELI5-Android/2.2.1")
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("model", "google/gemini-3-flash-preview")
                    val contentArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            val langHint = if (!preferredLanguage.isNullOrBlank() && !preferredLanguage.equals("English", ignoreCase = true)) {
                                " The text is likely in $preferredLanguage or related language."
                            } else ""
                            put(
                                "text",
                                "Extract and transcribe all the exact text visible in this image verbatim in its original script and language.$langHint Return ONLY the transcribed text, nothing else. Do not add markdown formatting, commentary, or quotes."
                            )
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")
                            })
                        })
                    }
                    val messageObj = JSONObject().apply {
                        put("role", "user")
                        put("content", contentArray)
                    }
                    put("messages", JSONArray().apply { put(messageObj) })
                }

                val postBytes = payload.toString().toByteArray(Charsets.UTF_8)
                conn.setFixedLengthStreamingMode(postBytes.size)
                conn.outputStream.use { os ->
                    os.write(postBytes)
                    os.flush()
                }

                val respCode = conn.responseCode
                Log.d(TAG, "performCloudOcr: HTTP responseCode=$respCode")

                if (respCode == 200) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseStr)
                    val choices = json.optJSONArray("choices")
                    val message = choices?.optJSONObject(0)?.optJSONObject("message")
                    val content = message?.optString("content")?.trim()
                    Log.d(TAG, "performCloudOcr: Extracted content='$content'")
                    if (!content.isNullOrBlank() && !content.equals("null", ignoreCase = true)) {
                        content
                    } else null
                } else {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.w(TAG, "Cloud OCR response code: $respCode, error: $err")
                    null
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Cloud OCR request failed", t)
                null
            }

            Log.d(TAG, "performCloudOcr: posting result to main handler: '$extractedText'")
            Handler(Looper.getMainLooper()).post {
                onResult(extractedText)
            }
        }
    }

    /**
     * Checks if the language uses a non-Latin script that is not supported offline
     * by ML Kit on-device models (e.g. Bengali, Tamil, Telugu, Arabic, Russian, etc.).
     */
    private fun isCloudPreferredScript(language: String?): Boolean {
        val lang = language?.lowercase()?.trim() ?: return false
        return lang.contains("russian") || lang == "ru" || lang == "rus" ||
               lang.contains("ukrainian") || lang == "uk" ||
               lang.contains("belarusian") || lang == "be" ||
               lang.contains("bulgarian") || lang == "bg" ||
               lang.contains("bengali") || lang == "bn" ||
               lang.contains("telugu") || lang == "te" ||
               lang.contains("tamil") || lang == "ta" ||
               lang.contains("gujarati") || lang == "gu" ||
               lang.contains("kannada") || lang == "kn" ||
               lang.contains("malayalam") || lang == "ml" ||
               lang.contains("punjabi") || lang == "pa" ||
               lang.contains("arabic") || lang == "ar" ||
               lang.contains("urdu") || lang == "ur" ||
               lang.contains("persian") || lang == "fa" ||
               lang.contains("hebrew") || lang == "he" || lang == "iw" ||
               lang.contains("russian") || lang == "ru" ||
               lang.contains("ukrainian") || lang == "uk" ||
               lang.contains("greek") || lang == "el" ||
               lang.contains("thai") || lang == "th"
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

    private val COMMON_LATIN_WORDS = setOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on", "with", "he",
        "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "we", "say", "her", "she", "or",
        "an", "will", "my", "one", "all", "would", "there", "their", "what", "so", "up", "out", "if", "about",
        "who", "get", "which", "go", "me", "when", "make", "can", "like", "time", "no", "just", "him", "know",
        "take", "people", "into", "year", "your", "good", "some", "could", "them", "see", "other", "than",
        "then", "now", "look", "only", "come", "its", "over", "think", "also", "back", "after", "use", "two",
        "how", "our", "work", "first", "well", "way", "even", "new", "want", "because", "any", "these", "give",
        "day", "most", "us", "is", "am", "are", "was", "were", "been", "has", "had", "love", "original", "video",
        "view", "reply", "translation", "hide", "replies", "english", "grammar", "verbs", "hate", "more", "cats",
        "cat", "rule", "words", "word", "post", "comment", "share", "follow", "story", "like", "reel", "reels",
        "de", "la", "que", "el", "en", "y", "los", "se", "del", "las", "por", "un", "para", "con", "una",
        "su", "al", "lo", "como", "le", "et", "les", "des", "est", "une", "dans", "pour", "qui", "der", "die",
        "das", "und", "nicht", "ist", "in", "zu", "den", "dem", "von", "mit"
    )

    fun isLikelyGarbledLatin(text: String?): Boolean {
        if (text.isNullOrBlank() || text.length < 5) return false

        // If text already has native non-Latin characters (Devanagari, CJK, Cyrillic, Arabic, etc.),
        // it was recognized by a dedicated script model or cloud vision, NOT garbled Latin!
        if (text.any { ch ->
            val ub = Character.UnicodeBlock.of(ch)
            ub == Character.UnicodeBlock.CYRILLIC ||
            ub == Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY ||
            ub == Character.UnicodeBlock.DEVANAGARI ||
            ub == Character.UnicodeBlock.BENGALI ||
            ub == Character.UnicodeBlock.ARABIC ||
            ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            ub == Character.UnicodeBlock.HIRAGANA ||
            ub == Character.UnicodeBlock.KATAKANA ||
            ub == Character.UnicodeBlock.HANGUL_SYLLABLES
        }) {
            return false
        }

        val words = Regex("""[A-Za-z0-9à-ÿ]+""").findAll(text).map { it.value }.toList()
        if (words.size < 3) return false

        // 1. Digits embedded inside words (e.g., '3TOM', 'o6o', '6onbwe')
        val digitInWord = words.count { Regex("""[A-Za-z][0-9]|[0-9][A-Za-z]""").containsMatchIn(it) }

        // 2. Unnatural mid-word uppercase (e.g., 'wyTka', 'aHrnu', 'npedMeTOB', 'cnoBa')
        val midWordUpper = words.count { Regex("""[a-z][A-Z]""").containsMatchIn(it) }

        // 3. Isolated single-letter Latin consonants (common in Cyrillic misreadings: 'B', 'c', 'H', 's')
        val isolatedSingle = words.count { it.length == 1 && it.lowercase() !in setOf("a", "i") }

        val cleanWords = words.filter { it.length > 1 && !it.any { ch -> ch.isDigit() } }.map { it.lowercase() }
        val recognizedWords = cleanWords.count { it in COMMON_LATIN_WORDS }

        val irregularCount = digitInWord + midWordUpper + isolatedSingle
        val recogRatio = if (cleanWords.isNotEmpty()) recognizedWords.toFloat() / cleanWords.size else 0f

        // If substantial text (> 3 words) with irregularities and low recognized common Latin words:
        if (irregularCount >= 2 && recogRatio < 0.25f) return true
        if (recogRatio < 0.12f && cleanWords.size >= 5) return true

        return false
    }
}
