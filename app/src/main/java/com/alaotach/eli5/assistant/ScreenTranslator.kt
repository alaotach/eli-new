package com.alaotach.eli5.assistant

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import android.util.Log
import com.alaotach.eli5.BuildConfig
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
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
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.min

data class TranslatedBlock(
    val id: Int,
    val bounds: Rect,
    val originalText: String,
    val translatedText: String,
    val bgColor: Int,
    val textColor: Int
)

private data class DetectedCandidate(
    val rect: Rect,
    val text: String
)

object ScreenTranslator {
    private const val TAG = "ScreenTranslator"

    private val latinRecognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val devanagariRecognizer by lazy { TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build()) }
    private val chineseRecognizer by lazy { TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) }
    private val japaneseRecognizer by lazy { TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()) }
    private val koreanRecognizer by lazy { TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()) }

    fun translateScreen(
        screenshot: Bitmap,
        targetLanguage: String,
        onResult: (List<TranslatedBlock>) -> Unit
    ) {
        val image = InputImage.fromBitmap(screenshot, 0)

        // Step 1: Detect all text blocks using multi-script parallel ML Kit recognizers
        detectBlocksMultiScript(image, screenshot, targetLanguage) { detectedBlocks ->
            if (detectedBlocks.isEmpty()) {
                Log.d(TAG, "On-device detected 0 blocks, falling back to Cloud Vision for non-MLKit scripts...")
                CoroutineScope(Dispatchers.IO).launch {
                    val cloudBlocks = performCloudVisionTranslate(screenshot, targetLanguage)
                    Handler(Looper.getMainLooper()).post { onResult(cloudBlocks) }
                }
                return@detectBlocksMultiScript
            }

            // Check if detected blocks contain substantial content or if they are just peripheral icons/time
            val hasSubstantialContent = isSubstantialContentDetected(detectedBlocks, screenshot.height)
            if (!hasSubstantialContent && isCloudPreferredScript(targetLanguage)) {
                Log.d(TAG, "On-device blocks lack body content and target language is $targetLanguage; querying Cloud Vision...")
                CoroutineScope(Dispatchers.IO).launch {
                    val cloudBlocks = performCloudVisionTranslate(screenshot, targetLanguage)
                    Handler(Looper.getMainLooper()).post {
                        if (cloudBlocks.isNotEmpty()) {
                            onResult(cloudBlocks)
                        } else {
                            // Fallback to translating the detected on-device blocks
                            CoroutineScope(Dispatchers.IO).launch {
                                val translated = performBatchTranslation(screenshot, detectedBlocks, targetLanguage)
                                Handler(Looper.getMainLooper()).post { onResult(translated) }
                            }
                        }
                    }
                }
                return@detectBlocksMultiScript
            }

            Log.d(TAG, "Multi-script detected ${detectedBlocks.size} blocks. Requesting batch translation to $targetLanguage...")

            // Step 2: Request batch translation from Gemini proxy
            CoroutineScope(Dispatchers.IO).launch {
                val translatedList = performBatchTranslation(screenshot, detectedBlocks, targetLanguage)
                if (translatedList.isEmpty()) {
                    Log.d(TAG, "Batch translation empty, trying Cloud Vision...")
                    val cloudBlocks = performCloudVisionTranslate(screenshot, targetLanguage)
                    Handler(Looper.getMainLooper()).post { onResult(cloudBlocks) }
                } else {
                    Handler(Looper.getMainLooper()).post { onResult(translatedList) }
                }
            }
        }
    }

    /**
     * Runs Latin, Devanagari, Chinese, Japanese, and Korean recognizers in parallel,
     * merging and deduplicating blocks across all scripts.
     */
    private fun detectBlocksMultiScript(
        image: InputImage,
        screenshot: Bitmap,
        targetLanguage: String,
        onComplete: (List<Pair<Rect, String>>) -> Unit
    ) {
        val tasks = listOf(
            latinRecognizer.process(image),
            devanagariRecognizer.process(image),
            chineseRecognizer.process(image),
            japaneseRecognizer.process(image),
            koreanRecognizer.process(image)
        )

        Tasks.whenAllComplete(tasks)
            .addOnCompleteListener {
                val rawCandidates = mutableListOf<DetectedCandidate>()

                for (task in tasks) {
                    if (task.isSuccessful) {
                        val visionText = task.result as? Text ?: continue
                        for (block in visionText.textBlocks) {
                            val rect = block.boundingBox ?: continue
                            val raw = block.text.trim()
                            if (raw.length < 2 && !raw.any { Character.isLetter(it) }) continue
                            if (rect.width() < 12 || rect.height() < 10) continue
                            if (rect.top < 40 && rect.height() < 30) continue // Status bar noise
                            rawCandidates.add(DetectedCandidate(rect, raw))
                        }
                    }
                }

                val deduplicated = deduplicateBlocks(rawCandidates)
                onComplete(deduplicated)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Multi-script detection failed", e)
                onComplete(emptyList())
            }
    }

    /**
     * Deduplicates overlapping bounding boxes across multiple scripts.
     * Prioritizes candidates with non-ASCII characters over Latin gibberish,
     * and keeps longer/richer text blocks.
     */
    private fun deduplicateBlocks(candidates: List<DetectedCandidate>): List<Pair<Rect, String>> {
        if (candidates.isEmpty()) return emptyList()

        // Sort by text length descending so richer blocks are evaluated first
        val sorted = candidates.sortedByDescending { it.text.length }
        val accepted = mutableListOf<DetectedCandidate>()

        for (cand in sorted) {
            var isDuplicate = false
            for (i in accepted.indices) {
                val acc = accepted[i]
                val ratio = overlapRatio(cand.rect, acc.rect)
                if (ratio > 0.40f) {
                    // Check if candidate has non-ASCII characters (Devanagari, CJK, etc.) while accepted is ASCII/gibberish
                    val candHasNonAscii = cand.text.any { it.code > 127 }
                    val accHasNonAscii = acc.text.any { it.code > 127 }

                    if (candHasNonAscii && !accHasNonAscii) {
                        accepted[i] = cand
                    }
                    isDuplicate = true
                    break
                }
            }
            if (!isDuplicate) {
                accepted.add(cand)
            }
        }

        return accepted.take(24).map { it.rect to it.text }
    }

    private fun overlapRatio(r1: Rect, r2: Rect): Float {
        val left = max(r1.left, r2.left)
        val top = max(r1.top, r2.top)
        val right = min(r1.right, r2.right)
        val bottom = min(r1.bottom, r2.bottom)
        if (right <= left || bottom <= top) return 0f
        val intersectionArea = (right - left).toFloat() * (bottom - top).toFloat()
        val minArea = min(r1.width() * r1.height(), r2.width() * r2.height()).toFloat()
        return if (minArea > 0f) intersectionArea / minArea else 0f
    }

    private fun isSubstantialContentDetected(blocks: List<Pair<Rect, String>>, screenHeight: Int): Boolean {
        return blocks.any { pair ->
            val rect = pair.first
            val text = pair.second
            val inCenterY = rect.top > screenHeight * 0.12f && rect.bottom < screenHeight * 0.88f
            inCenterY && (text.length >= 15 || text.contains(" "))
        }
    }

    private fun isCloudPreferredScript(language: String?): Boolean {
        val lang = language?.lowercase()?.trim() ?: return false
        return lang.contains("bengali") || lang == "bn" ||
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

    /**
     * Translates detected blocks in a single batch request to Gemini.
     * Supports multiple languages simultaneously in the input texts.
     */
    private fun performBatchTranslation(
        screenshot: Bitmap,
        blocks: List<Pair<Rect, String>>,
        targetLanguage: String
    ): List<TranslatedBlock> {
        val numberedPrompt = blocks.mapIndexed { index, pair ->
            "${index + 1}. ${pair.second.replace("\n", " ").trim()}"
        }.joinToString("\n")

        val prompt = """
            You are an expert multi-language translator for mobile screens.
            The following numbered texts were extracted from a mobile screen and may contain MULTIPLE DIFFERENT LANGUAGES simultaneously (for example, English, Hindi, Bengali, Spanish, Japanese, Chinese, Arabic, French, German, etc.).
            
            Translate EVERY SINGLE numbered text into $targetLanguage.
            - If a text is already in $targetLanguage, leave it unchanged.
            - If a text is in ANY other language, translate it accurately into $targetLanguage.
            - Every number from 1 to ${blocks.size} MUST have an entry in the returned JSON.
            - Preserve numbers, URLs, and punctuation appropriately.
            - Return strictly valid JSON mapping each number as a string to its translated text (e.g. {"1": "...", "2": "..."}).
            - Do not include any code block ticks, commentary, or text outside the JSON object.

            $numberedPrompt
        """.trimIndent()

        val translationMap = mutableMapOf<String, String>()

        try {
            val endpoint = "${BuildConfig.BACKEND_URL}/api/ocr"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 ELI5-Android/2.2.1")
            conn.connectTimeout = 12000
            conn.readTimeout = 15000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("model", "google/gemini-3-flash-preview")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            val postBytes = payload.toString().toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(postBytes.size)
            conn.outputStream.use { os ->
                os.write(postBytes)
                os.flush()
            }

            val respCode = conn.responseCode
            if (respCode == 200) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(responseStr)
                val choices = rootJson.optJSONArray("choices")
                val content = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""

                val clean = content.replace("```json", "").replace("```", "").trim()
                val startIdx = clean.indexOf('{')
                val endIdx = clean.lastIndexOf('}')
                if (startIdx != -1 && endIdx > startIdx) {
                    val jsonSub = clean.substring(startIdx, endIdx + 1)
                    val mapJson = JSONObject(jsonSub)
                    val keys = mapJson.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        translationMap[k] = mapJson.optString(k)
                    }
                }
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.w(TAG, "Batch translation failed with HTTP $respCode: $err")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Batch translation request failed", t)
        }

        val result = mutableListOf<TranslatedBlock>()
        for ((index, pair) in blocks.withIndex()) {
            val id = index + 1
            val original = pair.second
            val translated = translationMap[id.toString()] ?: original
            val rect = pair.first
            val bgColor = sampleBlockBgColor(screenshot, rect)
            val textColor = getContrastTextColor(bgColor)

            result.add(
                TranslatedBlock(
                    id = id,
                    bounds = rect,
                    originalText = original,
                    translatedText = translated,
                    bgColor = bgColor,
                    textColor = textColor
                )
            )
        }
        return result
    }

    /**
     * Cloud Vision fallback using Gemini 3 Flash.
     * Directly detects 2D bounding boxes and translates texts across all 35 languages
     * including Bengali, Tamil, Telugu, Arabic, Urdu, Russian, Greek, Thai, etc.
     */
    private fun performCloudVisionTranslate(
        screenshot: Bitmap,
        targetLanguage: String
    ): List<TranslatedBlock> {
        val result = mutableListOf<TranslatedBlock>()
        try {
            val scaled = if (screenshot.width > 960 || screenshot.height > 960) {
                val scale = 960f / maxOf(screenshot.width, screenshot.height)
                Bitmap.createScaledBitmap(
                    screenshot,
                    (screenshot.width * scale).toInt().coerceAtLeast(1),
                    (screenshot.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                screenshot
            }

            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

            val endpoint = "${BuildConfig.BACKEND_URL}/api/ocr"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 ELI5-Android/2.2.1")
            conn.connectTimeout = 14000
            conn.readTimeout = 18000
            conn.doOutput = true

            val prompt = """
                Detect all distinct text blocks, buttons, and paragraphs visible on this mobile phone screen.
                The screen may contain text in multiple languages (such as Bengali, Hindi, English, Tamil, Telugu, Arabic, Russian, Japanese, etc.).
                For every text block, detect its 2D bounding box coordinates [ymin, xmin, ymax, xmax] (normalized from 0 to 1000) and translate its text into $targetLanguage.
                Return strictly a valid JSON array of objects:
                [
                  {
                    "box": [ymin, xmin, ymax, xmax],
                    "original": "exact original text",
                    "translated": "translated text in $targetLanguage"
                  }
                ]
                Do not output markdown code blocks, explanation, or any text outside the JSON array.
            """.trimIndent()

            val payload = JSONObject().apply {
                put("model", "google/gemini-3-flash-preview")
                val contentArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", prompt)
                    })
                    put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            put("url", "data:image/jpeg;base64,$base64Image")
                        })
                    })
                }
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", contentArray)
                    })
                })
            }

            val postBytes = payload.toString().toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(postBytes.size)
            conn.outputStream.use { os ->
                os.write(postBytes)
                os.flush()
            }

            val respCode = conn.responseCode
            if (respCode == 200) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(responseStr)
                val choices = rootJson.optJSONArray("choices")
                val content = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""

                val clean = content.replace("```json", "").replace("```", "").trim()
                val startIdx = clean.indexOf('[')
                val endIdx = clean.lastIndexOf(']')
                if (startIdx != -1 && endIdx > startIdx) {
                    val jsonArr = JSONArray(clean.substring(startIdx, endIdx + 1))
                    for (i in 0 until jsonArr.length()) {
                        val obj = jsonArr.optJSONObject(i) ?: continue
                        val boxArr = obj.optJSONArray("box") ?: obj.optJSONArray("box_2d") ?: obj.optJSONArray("bounding_box") ?: continue
                        if (boxArr.length() < 4) continue

                        val ymin = boxArr.optDouble(0).toFloat()
                        val xmin = boxArr.optDouble(1).toFloat()
                        val ymax = boxArr.optDouble(2).toFloat()
                        val xmax = boxArr.optDouble(3).toFloat()

                        val left = (xmin * screenshot.width / 1000f).toInt().coerceIn(0, screenshot.width - 1)
                        val top = (ymin * screenshot.height / 1000f).toInt().coerceIn(0, screenshot.height - 1)
                        val right = (xmax * screenshot.width / 1000f).toInt().coerceIn(left + 1, screenshot.width)
                        val bottom = (ymax * screenshot.height / 1000f).toInt().coerceIn(top + 1, screenshot.height)

                        val rect = Rect(left, top, right, bottom)
                        if (rect.width() < 12 || rect.height() < 10) continue
                        if (rect.top < 40 && rect.height() < 30) continue

                        val original = obj.optString("original").ifEmpty { obj.optString("original_text") }
                        val translated = obj.optString("translated").ifEmpty {
                            obj.optString("translated_to_$targetLanguage").ifEmpty {
                                obj.optString("translation")
                            }
                        }
                        if (translated.isBlank()) continue

                        val bgColor = sampleBlockBgColor(screenshot, rect)
                        val textColor = getContrastTextColor(bgColor)

                        result.add(
                            TranslatedBlock(
                                id = i + 1,
                                bounds = rect,
                                originalText = original,
                                translatedText = translated,
                                bgColor = bgColor,
                                textColor = textColor
                            )
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "performCloudVisionTranslate failed", t)
        }
        return result
    }

    private fun sampleBlockBgColor(bitmap: Bitmap, rect: Rect): Int {
        try {
            val x1 = (rect.left + 3).coerceIn(0, bitmap.width - 1)
            val y1 = (rect.top + 3).coerceIn(0, bitmap.height - 1)
            val x2 = (rect.right - 3).coerceIn(0, bitmap.width - 1)
            val y2 = (rect.bottom - 3).coerceIn(0, bitmap.height - 1)

            val p1 = bitmap.getPixel(x1, y1)
            val p2 = bitmap.getPixel(x2, y1)
            val p3 = bitmap.getPixel(x1, y2)
            val p4 = bitmap.getPixel(x2, y2)

            val r = (Color.red(p1) + Color.red(p2) + Color.red(p3) + Color.red(p4)) / 4
            val g = (Color.green(p1) + Color.green(p2) + Color.green(p3) + Color.green(p4)) / 4
            val b = (Color.blue(p1) + Color.blue(p2) + Color.blue(p3) + Color.blue(p4)) / 4
            return Color.rgb(r, g, b)
        } catch (_: Exception) {
            return Color.parseColor("#1E293B")
        }
    }

    private fun getContrastTextColor(bgColor: Int): Int {
        val r = Color.red(bgColor) / 255.0
        val g = Color.green(bgColor) / 255.0
        val b = Color.blue(bgColor) / 255.0
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return if (luminance < 0.52) Color.WHITE else Color.parseColor("#0F172A")
    }

    fun calculateOptimalTextSize(
        text: String,
        availableWidth: Float,
        availableHeight: Float,
        dp: Float,
        basePaint: TextPaint
    ): Float {
        var size = (availableHeight * 0.42f).coerceIn(12f * dp, 32f * dp)
        val testPaint = TextPaint(basePaint)

        for (step in 0..5) {
            testPaint.textSize = size
            val widthInt = max(1, availableWidth.toInt())
            val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, 0, text.length, testPaint, widthInt)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.15f)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(text, testPaint, widthInt, Layout.Alignment.ALIGN_NORMAL, 1.15f, 0f, false)
            }

            if (layout.height <= availableHeight || size <= 10f * dp) {
                break
            }
            size *= 0.88f
        }
        return size.coerceAtLeast(10f * dp)
    }
}
