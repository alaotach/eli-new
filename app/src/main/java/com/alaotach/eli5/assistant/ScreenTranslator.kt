package com.alaotach.eli5.assistant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.alaotach.eli5.BuildConfig
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
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

data class TranslatedBlock(
    val id: Int,
    val bounds: Rect,
    val originalText: String,
    val translatedText: String,
    val bgColor: Int,
    val textColor: Int
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

        // Step 1: Detect all text blocks using multi-script ML Kit
        detectBlocks(image, targetLanguage) { detectedBlocks ->
            if (detectedBlocks.isEmpty()) {
                Log.d(TAG, "No text blocks detected on screen")
                Handler(Looper.getMainLooper()).post { onResult(emptyList()) }
                return@detectBlocks
            }

            Log.d(TAG, "Detected ${detectedBlocks.size} text blocks on screen. Requesting batch translation to $targetLanguage...")

            // Step 2: Request batch translation from Gemini proxy
            CoroutineScope(Dispatchers.IO).launch {
                val translatedList = performBatchTranslation(screenshot, detectedBlocks, targetLanguage)
                Handler(Looper.getMainLooper()).post {
                    onResult(translatedList)
                }
            }
        }
    }

    private fun detectBlocks(
        image: InputImage,
        preferredLanguage: String,
        onComplete: (List<Pair<Rect, String>>) -> Unit
    ) {
        val recognizer = when {
            preferredLanguage.contains("hindi", true) || preferredLanguage.contains("marathi", true) -> devanagariRecognizer
            preferredLanguage.contains("chinese", true) -> chineseRecognizer
            preferredLanguage.contains("japanese", true) -> japaneseRecognizer
            preferredLanguage.contains("korean", true) -> koreanRecognizer
            else -> latinRecognizer
        }

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val primaryBlocks = extractValidBlocks(visionText)
                if (primaryBlocks.isNotEmpty()) {
                    onComplete(primaryBlocks)
                } else {
                    // Fallback to Latin if primary was non-Latin or vice-versa
                    val fallbackRecognizer = if (recognizer != latinRecognizer) latinRecognizer else devanagariRecognizer
                    fallbackRecognizer.process(image)
                        .addOnSuccessListener { fallbackText ->
                            onComplete(extractValidBlocks(fallbackText))
                        }
                        .addOnFailureListener {
                            onComplete(emptyList())
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Primary block detection failed, trying Latin fallback", e)
                latinRecognizer.process(image)
                    .addOnSuccessListener { fallbackText ->
                        onComplete(extractValidBlocks(fallbackText))
                    }
                    .addOnFailureListener {
                        onComplete(emptyList())
                    }
            }
    }

    private fun extractValidBlocks(visionText: Text): List<Pair<Rect, String>> {
        val list = mutableListOf<Pair<Rect, String>>()
        for (block in visionText.textBlocks) {
            val rect = block.boundingBox ?: continue
            val raw = block.text.trim()
            if (raw.length < 2 && !raw.any { Character.isLetter(it) }) continue
            if (rect.width() < 12 || rect.height() < 10) continue

            // Exclude extreme status bar / nav bar noise if tiny
            if (rect.top < 40 && rect.height() < 30) continue

            list.add(rect to raw)
        }
        // Limit to top 24 blocks to ensure rapid translation
        return list.take(24)
    }

    private fun performBatchTranslation(
        screenshot: Bitmap,
        blocks: List<Pair<Rect, String>>,
        targetLanguage: String
    ): List<TranslatedBlock> {
        Log.d(TAG, "performBatchTranslation started for ${blocks.size} blocks to $targetLanguage")
        val numberedPrompt = blocks.mapIndexed { index, pair ->
            "${index + 1}. ${pair.second.replace("\n", " ").trim()}"
        }.joinToString("\n")

        val prompt = """
            Translate the following numbered texts into $targetLanguage.
            Keep the meaning, nuances, and tone intact.
            Return strictly valid JSON mapping each number string to its translated text (e.g. {"1": "...", "2": "..."}).
            Do not include any explanation or extra text outside the JSON object.

            $numberedPrompt
        """.trimIndent()

        val translationMap = mutableMapOf<String, String>()

        try {
            val endpoint = "${BuildConfig.BACKEND_URL}/api/ocr"
            Log.d(TAG, "performBatchTranslation connecting to $endpoint")
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
            Log.d(TAG, "performBatchTranslation payload written, awaiting response...")

            val respCode = conn.responseCode
            Log.d(TAG, "performBatchTranslation responseCode=$respCode")

            if (respCode == 200) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "performBatchTranslation responseStr length=${responseStr.length}")
                val rootJson = JSONObject(responseStr)
                val choices = rootJson.optJSONArray("choices")
                val content = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""

                val clean = content.substringAfter("{", "").substringBeforeLast("}", "")
                if (clean.isNotBlank()) {
                    val mapJson = JSONObject("{$clean}")
                    val keys = mapJson.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        translationMap[k] = mapJson.optString(k)
                    }
                }
                Log.d(TAG, "performBatchTranslation parsed ${translationMap.size} translated entries")
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.w(TAG, "Batch translation failed with HTTP $respCode: $err")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Batch translation request failed", t)
        }

        // Map each detected block to its TranslatedBlock with sampled color styling
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
        Log.d(TAG, "performBatchTranslation returning ${result.size} translated blocks")
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
